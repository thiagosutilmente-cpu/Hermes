package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EmergencySilentAlertWorker
 *
 * CoroutineWorker executado de forma resiliente em segundo plano pelo WorkManager
 * para disparar o alerta silencioso de emergência (SOS) em caso de perigo durante entregas.
 *
 * Características:
 * 1. Resiliência Total: Executa mesmo com o app encerrado, tela desligada ou com restrições de bateria.
 * 2. Discrição Silenciosa: Não emite alarmes audíveis para não colocar o entregador em maior risco.
 * 3. Envio de Telemetria: Empacota coordenadas GPS, link do Google Maps, velocidade e dados da corrida.
 * 4. Confirmação Háptica Tátil: Emite um micro-pulso tátil suave via [HapticFeedbackHelper] para confirmar ao piloto.
 * 5. Disparo Direto de SMS: Envia SMS silencioso aos contatos de emergência quando permissão concedida.
 * 6. Registro Local e Notificação de Sistema com baixa intrusão.
 */
class EmergencySilentAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "EmergencySilentWorker"
        const val UNIQUE_WORK_NAME = "work_radar_emergency_sos_silent"

        const val KEY_LATITUDE = "extra_latitude"
        const val KEY_LONGITUDE = "extra_longitude"
        const val KEY_SPEED_KMH = "extra_speed_kmh"
        const val KEY_MAPS_URL = "extra_maps_url"
        const val KEY_MESSAGE = "extra_message"
        const val KEY_TIMESTAMP = "extra_timestamp"
        const val KEY_CONTACTS_COUNT = "extra_contacts_count"
        const val KEY_DELIVERY_INFO = "extra_delivery_info"

        const val ACTION_SOS_DISPATCHED = "com.example.radar.ACTION_SOS_DISPATCHED"
        const val VPS_EMERGENCY_ENDPOINT = "http://187.77.248.73:8000/api/emergency/sos"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val lat = inputData.getDouble(KEY_LATITUDE, -23.561684)
        val lng = inputData.getDouble(KEY_LONGITUDE, -46.655981)
        val speed = inputData.getDouble(KEY_SPEED_KMH, 0.0)
        val mapsUrl = inputData.getString(KEY_MAPS_URL) ?: "https://maps.google.com/?q=$lat,$lng"
        val message = inputData.getString(KEY_MESSAGE) ?: "Alerta SOS Silencioso disparado"
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        val contactsCount = inputData.getInt(KEY_CONTACTS_COUNT, 0)
        val deliveryInfo = inputData.getString(KEY_DELIVERY_INFO) ?: "Nenhuma corrida em andamento"

        Log.i(TAG, "🚨 Iniciando execução do Alerta Silencioso de SOS via WorkManager. ID: $id")
        Log.i(TAG, "📍 Coordenadas: Lat=$lat, Lng=$lng, Vel=$speed km/h, Contatos=$contactsCount")

        // 1. Confirmação tátil discreta (2 micro-pulsos suaves, sem áudio)
        try {
            HapticFeedbackHelper.vibrateSilentSosConfirmation(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao acionar vibração tátil silenciosa", e)
        }

        // 2. Transmissão do Beacon de Emergência para o servidor VPS em segundo plano
        var networkDelivered = false
        try {
            val jsonPayload = """
                {
                    "event": "SOS_SILENT_ALERT",
                    "workerId": "$id",
                    "timestamp": $timestamp,
                    "latitude": $lat,
                    "longitude": $lng,
                    "speedKmh": $speed,
                    "mapsUrl": "$mapsUrl",
                    "contactsNotified": $contactsCount,
                    "deliveryContext": "$deliveryInfo",
                    "batteryOptimized": true
                }
            """.trimIndent()

            val prefs = applicationContext.getSharedPreferences("radar_backend_sync_prefs", Context.MODE_PRIVATE)
            val baseUrl = prefs.getString("pref_server_url", RadarBackendSyncManager.DEFAULT_SERVER_URL)?.trimEnd('/')
                ?: RadarBackendSyncManager.DEFAULT_SERVER_URL
            val emergencyEndpoint = "$baseUrl/api/emergency/sos"

            val url = URL(emergencyEndpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "RadarDeliveryCoordinator-EmergencyEngine/1.0")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                networkDelivered = true
                Log.i(TAG, "Beacon de emergência entregue ao servidor VPS com código $responseCode")
            } else {
                Log.w(TAG, "Servidor VPS retornou status $responseCode ao beacon de emergência")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Falha de rede ao contatar VPS central (normal se offline ou em área de sombra): ${e.message}")
        }

        // 3. Disparo Silencioso Direto de SMS para contatos cadastrados caso haja permissão
        var smsDispatchedCount = 0
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val contacts = EmergencySosManager.emergencyContacts.value
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applicationContext.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                contacts.forEach { contact ->
                    val cleanPhone = contact.phone.replace(Regex("[^0-9+]"), "")
                    if (cleanPhone.isNotBlank()) {
                        val parts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                        smsDispatchedCount++
                        Log.i(TAG, "SMS de emergência enviado silenciosamente para ${contact.name} ($cleanPhone)")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao enviar SMS silencioso direto aos contatos: ${e.message}")
            }
        }

        // 4. Atualiza o registro permanente de auditoria no [EmergencySosManager]
        val timeFormatted = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(timestamp))
        EmergencySosManager.recordWorkerExecutionSuccess(
            workId = id.toString(),
            timestamp = timestamp,
            lat = lat,
            lng = lng,
            speedKmh = speed,
            mapsUrl = mapsUrl,
            contactsCount = if (smsDispatchedCount > 0) smsDispatchedCount else contactsCount,
            networkDelivered = networkDelivered || smsDispatchedCount > 0
        )

        // 5. Dispara broadcast local para notificar componentes ativos
        try {
            val broadcastIntent = Intent(ACTION_SOS_DISPATCHED).apply {
                putExtra("worker_id", id.toString())
                putExtra("latitude", lat)
                putExtra("longitude", lng)
                putExtra("maps_url", mapsUrl)
                putExtra("timestamp", timestamp)
                putExtra("network_status", networkDelivered)
                putExtra("sms_dispatched", smsDispatchedCount)
            }
            applicationContext.sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar broadcast interno de SOS", e)
        }

        val output = workDataOf(
            "dispatched_at" to timestamp,
            "latitude" to lat,
            "longitude" to lng,
            "maps_url" to mapsUrl,
            "network_success" to networkDelivered,
            "sms_dispatched" to smsDispatchedCount,
            "status" to if (networkDelivered || smsDispatchedCount > 0) "ALERTA_SILENCIOSO_EXECUTADO" else "AGUARDANDO_RECONEXAO"
        )

        if (!networkDelivered && smsDispatchedCount == 0 && runAttemptCount < 3) {
            Log.w(TAG, "Tentativa $runAttemptCount sem entrega imediata. Enfileirando retry automático via WorkManager...")
            Result.retry()
        } else {
            Result.success(output)
        }
    }
}
