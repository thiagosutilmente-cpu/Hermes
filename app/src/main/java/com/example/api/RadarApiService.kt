package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// POST Request models
data class ActiveDeliveryRequest(
    @field:Json(name = "destination_address") val destinationAddress: String
)

data class AnalyzeRequest(
    val image: String,
    val latitude: Double,
    val longitude: Double,
    @field:Json(name = "active_delivery") val activeDelivery: ActiveDeliveryRequest? = null,
    @field:Json(name = "risk_zones_keywords") val riskZonesKeywords: String? = null,
    @field:Json(name = "min_value_per_km") val minValuePerKm: Double? = null,
    @field:Json(name = "min_fare_value") val minFareValue: Double? = null,
    @field:Json(name = "pattern_string") val patternString: String? = null,
    @field:Json(name = "rider_id") val riderId: String? = null
)

// Response models
data class ExtractedData(
    @field:Json(name = "pickup_address") val pickupAddress: String?,
    @field:Json(name = "delivery_address") val deliveryAddress: String?,
    @field:Json(name = "fare_value") val fareValue: String?,
    @field:Json(name = "delivery_app") val deliveryApp: String?
)

data class RouteData(
    @field:Json(name = "total_distance") val totalDistance: Double?,
    @field:Json(name = "total_time") val totalTime: Double?,
    @field:Json(name = "detour_distance") val detourDistance: Double?,
    @field:Json(name = "detour_time") val detourTime: Double?,
    @field:Json(name = "chained_distance") val chainedDistance: Double?,
    @field:Json(name = "chained_time") val chainedTime: Double?
)

data class Metrics(
    @field:Json(name = "fare_value") val fareValue: Double?,
    @field:Json(name = "value_per_km") val valuePerKm: Double?,
    @field:Json(name = "value_per_minute") val valuePerMinute: Double?
)

data class DetailData(
    @field:Json(name = "extracted_data") val extractedData: ExtractedData?,
    @field:Json(name = "route_data") val routeData: RouteData?,
    val metrics: Metrics?
)

data class AnalyzeResponse(
    val suggestion: String, // aceitar, considerar, recusar
    val reason: String,
    val confidence: Double,
    val details: DetailData?,
    val mode: String? = null
)

data class AppBreakdownItem(
    @field:Json(name = "offers_accepted") val offersAccepted: Int,
    @field:Json(name = "estimated_earnings") val estimatedEarnings: Double
)

data class DailyReportItem(
    val date: String,
    @field:Json(name = "total_offers_evaluated") val totalOffersEvaluated: Int,
    @field:Json(name = "total_offers_accepted") val totalOffersAccepted: Int,
    @field:Json(name = "total_offers_rejected") val totalOffersRejected: Int,
    @field:Json(name = "total_offers_considered") val totalOffersConsidered: Int,
    @field:Json(name = "estimated_earnings") val estimatedEarnings: Double,
    @field:Json(name = "total_distance_km") val totalDistanceKm: Double,
    @field:Json(name = "total_time_min") val totalTimeMin: Double,
    @field:Json(name = "average_fare_value") val averageFareValue: Double,
    @field:Json(name = "earnings_per_km") val earningsPerKm: Double,
    @field:Json(name = "app_breakdown") val appBreakdown: Map<String, AppBreakdownItem>?
)

data class HotZoneItem(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @field:Json(name = "offers_count") val offersCount: Int,
    @field:Json(name = "avg_fare") val avgFare: Double,
    @field:Json(name = "avg_value_per_km") val avgValuePerKm: Double,
    @field:Json(name = "predominant_app") val predominantApp: String
)

data class DecisionRequest(
    val value: Double,
    val distance: Double,
    val app: String = "iFood",
    @field:Json(name = "user_id") val userId: String? = null
)

data class DecisionResponse(
    val decision: String,
    val confidence: Double,
    val reason: String,
    @field:Json(name = "gain_per_km") val gainPerKm: Double?,
    val app: String?
)

data class StackItem(
    val id: String,
    val apps: List<String>,
    val restaurant: String,
    val destination: String,
    @field:Json(name = "total_value") val totalValue: Double,
    @field:Json(name = "distance_km") val distanceKm: Double,
    @field:Json(name = "time_min") val timeMin: Double,
    @field:Json(name = "gain_per_km") val gainPerKm: Double,
    val status: String
)

data class StackActionRequest(
    @field:Json(name = "stack_id") val stackId: String
)

data class StackActionResponse(
    val status: String,
    val message: String
)

data class EarningsSummaryResponse(
    val today: Double,
    val week: Double,
    val month: Double,
    val totalKm: Double,
    val profitPerKm: Double,
    val deliveredCount: Int
)

data class HealthPulseResponse(
    val score: Int,
    val gpsAccuracyMeters: Double,
    val latencyMs: Long,
    val temperatureCelsius: Double,
    val status: String,
    val activeAnomalies: List<String>
)

interface RadarApi {
    @POST("analyze")
    suspend fun analyzeOffer(
        @Header("X-API-Token") token: String,
        @Body request: AnalyzeRequest
    ): AnalyzeResponse

    @POST("api/decision")
    suspend fun getDecision(
        @Header("X-API-Token") token: String,
        @Body request: DecisionRequest
    ): DecisionResponse

    @GET("api/stacks")
    suspend fun getPendingStacks(
        @Header("X-API-Token") token: String
    ): List<StackItem>

    @POST("api/stacks/accept")
    suspend fun acceptStack(
        @Header("X-API-Token") token: String,
        @Body request: StackActionRequest
    ): StackActionResponse

    @POST("api/stacks/decline")
    suspend fun declineStack(
        @Header("X-API-Token") token: String,
        @Body request: StackActionRequest
    ): StackActionResponse

    @GET("api/earnings")
    suspend fun getEarningsSummary(
        @Header("X-API-Token") token: String
    ): EarningsSummaryResponse

    @GET("api/health")
    suspend fun getHealthPulse(
        @Header("X-API-Token") token: String
    ): HealthPulseResponse

    @GET("audit_logs/daily_report")
    suspend fun getDailyReport(
        @Header("X-API-Token") token: String,
        @Query("rider_id") riderId: String? = null
    ): List<DailyReportItem>

    @GET("audit_logs/hot_zones")
    suspend fun getHotZones(
        @Header("X-API-Token") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): List<HotZoneItem>
}

object RadarApiFactory {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun create(baseUrl: String): RadarApi {
        // Ensure trailing slash
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadarApi::class.java)
    }
}
