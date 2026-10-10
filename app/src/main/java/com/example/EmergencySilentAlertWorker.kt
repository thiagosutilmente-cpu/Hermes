package com.example

import android.content.Context
import android.content.Intent
import android.util.Log
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
 * 5. Registro Local e Notificação de Sistema com baixa intrusão.
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
        const val VPS_EMERGENCY_ENDPOINT = "http://187.77.248.73:8080/api/emergency/sos"
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

            val url = URL(VPS_EMERGENCY_ENDPOINT)
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
            // Mesmo se o servidor estiver temporariamente indisponível, consideramos sucesso local
            // pois o alerta aos contatos e o registro no dispositivo já foram efetuados.
        }

        // 3. Atualiza o registro permanente de auditoria no [EmergencySosManager]
        val timeFormatted = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(timestamp))
        EmergencySosManager.recordWorkerExecutionSuccess(
            workId = id.toString(),
            timestamp = timestamp,
            lat = lat,
            lng = lng,
            speedKmh = speed,
            mapsUrl = mapsUrl,
            contactsCount = contactsCount,
            networkDelivered = networkDelivered
        )

        // 4. Dispara broadcast local para notificar componentes ativos
        try {
            val broadcastIntent = Intent(ACTION_SOS_DISPATCHED).apply {
                putExtra("worker_id", id.toString())
                putExtra("latitude", lat)
                putExtra("longitude", lng)
                putExtra("maps_url", mapsUrl)
                putExtra("timestamp", timestamp)
                putExtra("network_status", networkDelivered)
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
            "status" to "ALERTA_SILENCIOSO_EXECUTADO"
        )

        Result.success(output)
    }
}
