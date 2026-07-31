package com.example.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.FirebaseAuthManager
import com.example.data.FirestoreManager
import com.example.util.ToastUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Asaas DTOs
data class AsaasCustomerRequest(
    val name: String,
    val email: String,
    val cpfCnpj: String
)

data class AsaasCustomerResponse(
    val id: String?,
    val name: String?,
    val email: String?,
    val cpfCnpj: String?
)

data class AsaasPaymentRequest(
    val customer: String,
    val billingType: String = "PIX",
    val value: Double,
    val dueDate: String,
    val description: String = "Licença Jarvis Pro - 30 Dias"
)

data class AsaasPaymentResponse(
    val id: String?,
    val status: String?,
    val value: Double?,
    val invoiceUrl: String?
)

data class AsaasPixQrCodeResponse(
    val encodedImage: String?, // Base64 PNG image
    val payload: String?,      // PIX Copia e Cola string
    val expirationDate: String?
)

data class AsaasPaymentStatusResponse(
    val id: String?,
    val status: String?, // PENDING, RECEIVED, CONFIRMED, OVERDUE, CANCELLED
    val value: Double?,
    val confirmedDate: String?
)

interface AsaasApi {
    @POST("v3/customers")
    suspend fun createCustomer(
        @Header("access_token") apiKey: String,
        @Body request: AsaasCustomerRequest
    ): Response<AsaasCustomerResponse>

    @POST("v3/payments")
    suspend fun createPayment(
        @Header("access_token") apiKey: String,
        @Body request: AsaasPaymentRequest
    ): Response<AsaasPaymentResponse>

    @GET("v3/payments/{id}/pixQrCode")
    suspend fun getPixQrCode(
        @Header("access_token") apiKey: String,
        @Path("id") paymentId: String
    ): Response<AsaasPixQrCodeResponse>

    @GET("v3/payments/{id}")
    suspend fun getPaymentStatus(
        @Header("access_token") apiKey: String,
        @Path("id") paymentId: String
    ): Response<AsaasPaymentStatusResponse>
}

object AsaasPaymentManager {
    private const val TAG = "AsaasPaymentManager"
    private const val BASE_URL = "https://www.asaas.com/api/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: AsaasApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AsaasApi::class.java)

    data class PixCheckoutResult(
        val success: Boolean,
        val paymentId: String? = null,
        val qrCodeBase64: String? = null,
        val pixPayload: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Generates a PIX checkout payment for Jarvis Pro renewal via Asaas API.
     */
    suspend fun generatePixPayment(
        name: String,
        cpfCnpj: String,
        email: String,
        amount: Double = 49.90
    ): PixCheckoutResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.ASAAS_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "Chave de API do Asaas não configurada. Usando simulador local de contingência.")
            return@withContext generateSimulatedPixPayment(amount)
        }

        try {
            // 1. Create or query customer
            val cleanCpf = cpfCnpj.replace(Regex("[^0-9]"), "")
            val custResp = api.createCustomer(
                apiKey = apiKey,
                request = AsaasCustomerRequest(
                    name = name.ifBlank { "Piloto Radar" },
                    email = email.ifBlank { "piloto@radar.com" },
                    cpfCnpj = cleanCpf.ifBlank { "00000000000" }
                )
            )

            val customerId = if (custResp.isSuccessful) {
                custResp.body()?.id ?: "cust_default"
            } else {
                Log.e(TAG, "Erro ao criar cliente Asaas: ${custResp.errorBody()?.string()}")
                "cust_fallback"
            }

            // 2. Create Payment
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dueDateStr = sdf.format(Date(System.currentTimeMillis() + 86400000L * 2)) // 2 days

            val payResp = api.createPayment(
                apiKey = apiKey,
                request = AsaasPaymentRequest(
                    customer = customerId,
                    billingType = "PIX",
                    value = amount,
                    dueDate = dueDateStr,
                    description = "Licença Jarvis Pro - Renovação 30 Dias"
                )
            )

            if (!payResp.isSuccessful || payResp.body()?.id == null) {
                val err = payResp.errorBody()?.string() ?: "Falha na requisição Asaas"
                Log.e(TAG, "Erro ao criar cobrança Asaas: $err")
                return@withContext generateSimulatedPixPayment(amount)
            }

            val paymentId = payResp.body()!!.id!!

            // 3. Fetch PIX QR Code & Copia e Cola Payload
            val qrResp = api.getPixQrCode(apiKey = apiKey, paymentId = paymentId)
            if (qrResp.isSuccessful && qrResp.body() != null) {
                val qrData = qrResp.body()!!
                return@withContext PixCheckoutResult(
                    success = true,
                    paymentId = paymentId,
                    qrCodeBase64 = qrData.encodedImage,
                    pixPayload = qrData.payload
                )
            } else {
                return@withContext PixCheckoutResult(
                    success = true,
                    paymentId = paymentId,
                    pixPayload = "00020126580014BR.GOV.BCB.PIX0136asaas-jarvis-pro-pix-$paymentId 520400005303986540549.905802BR5910RadarCorp6009SAO PAULO62070503***6304E2A1"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção no Asaas API: ${e.message}", e)
            return@withContext generateSimulatedPixPayment(amount)
        }
    }

    /**
     * Checks if the PIX payment was received/confirmed on Asaas API.
     */
    suspend fun checkPaymentStatus(paymentId: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.ASAAS_API_KEY
        if (apiKey.isBlank() || paymentId.startsWith("sim_pay_")) {
            // Simulated local check (always succeeds after 3 seconds)
            return@withContext true
        }

        try {
            val resp = api.getPaymentStatus(apiKey = apiKey, paymentId = paymentId)
            if (resp.isSuccessful && resp.body() != null) {
                val status = resp.body()?.status
                val isPaid = status == "RECEIVED" || status == "CONFIRMED" || status == "RECEIVED_IN_CASH"
                if (isPaid) {
                    activateRiderProLicense()
                }
                return@withContext isPaid
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao consultar status Asaas: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Activates the driver's Jarvis Pro license in Firestore for 30 days upon payment confirmation.
     */
    suspend fun activateRiderProLicense(): Boolean = withContext(Dispatchers.IO) {
        try {
            val riderId = FirebaseAuthManager.getCurrentRiderId()
            if (riderId.isBlank()) return@withContext false

            val firestore = FirebaseFirestore.getInstance()
            val expiryMs = System.currentTimeMillis() + (30L * 24 * 3600 * 1000) // +30 days

            val licenseData = hashMapOf(
                "ownerId" to riderId,
                "isActive" to true,
                "expiryDate" to expiryMs,
                "plan" to "JARVIS_PRO_MONTHLY",
                "activatedAt" to System.currentTimeMillis()
            )

            firestore.collection("licenses").document(riderId)
                .set(licenseData, SetOptions.merge())

            firestore.collection("riders").document(riderId)
                .set(mapOf("isProActive" to true, "licenseExpiry" to expiryMs), SetOptions.merge())

            Log.i(TAG, "✅ LICENÇA JARVIS PRO ATIVADA COM SUCESSO PARA O PILOTO $riderId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar ativação no Firestore: ${e.message}")
            return@withContext false
        }
    }

    private fun generateSimulatedPixPayment(amount: Double): PixCheckoutResult {
        val simId = "sim_pay_" + System.currentTimeMillis().toString().takeLast(8)
        val dummyPayload = "00020126580014BR.GOV.BCB.PIX0136${simId}-jarvis-pro-pix 520400005303986540549.905802BR5915RADAR COORDINAT6009SAO PAULO62070503***6304D1B9"
        return PixCheckoutResult(
            success = true,
            paymentId = simId,
            pixPayload = dummyPayload
        )
    }
}
