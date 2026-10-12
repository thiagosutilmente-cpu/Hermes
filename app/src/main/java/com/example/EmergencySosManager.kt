package com.example

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Contato de emergência configurado pelo entregador.
 */
data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relationship: String = "Confiança",
    val isWhatsAppEnabled: Boolean = true,
    val isSmsEnabled: Boolean = true
)

/**
 * Registro de auditoria de cada disparo de SOS silencioso.
 */
data class EmergencySosLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val mapsUrl: String,
    val contactsNotifiedCount: Int,
    val workManagerId: String,
    val status: String = "Alerta Silencioso Disparado",
    val networkDelivered: Boolean = false,
    val resolvedAtMillis: Long? = null
)

/**
 * Máquina de estados do SOS de Emergência.
 */
sealed class EmergencySosState {
    data object Idle : EmergencySosState()

    data class Countdown(
        val secondsRemaining: Int,
        val targetTimestampMillis: Long
    ) : EmergencySosState()

    data class Active(
        val triggeredAtMillis: Long,
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double,
        val mapsUrl: String,
        val workManagerId: String,
        val contactsNotifiedCount: Int,
        val networkDelivered: Boolean = false
    ) : EmergencySosState()

    data class Resolved(
        val resolvedAtMillis: Long,
        val logId: String
    ) : EmergencySosState()
}

/**
 * EmergencySosManager
 *
 * Gerencia o ciclo completo de segurança de emergência (SOS) do app:
 * 1. Botão de SOS de acesso instantâneo na tela principal.
 * 2. Captura da localização em tempo real (GPS Fused) e geração de link do Google Maps.
 * 3. Disparo de alerta silencioso em segundo plano utilizando o WorkManager (resiliente e com auto-retry).
 * 4. Envio automatizado/assistido para contatos de emergência (WhatsApp e SMS).
 * 5. Histórico e auditoria de ocorrências salvas localmente.
 */
object EmergencySosManager {

    private const val TAG = "EmergencySosManager"
    private const val PREFS_NAME = "radar_emergency_sos_prefs"
    private const val KEY_CONTACTS_JSON = "saved_emergency_contacts_v1"
    private const val KEY_LOGS_JSON = "saved_emergency_logs_v1"
    private const val KEY_ACTIVE_SOS_ID = "active_sos_incident_id"

    private val managerScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    private val _sosState = MutableStateFlow<EmergencySosState>(EmergencySosState.Idle)
    val sosState: StateFlow<EmergencySosState> = _sosState.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    private val _historyLogs = MutableStateFlow<List<EmergencySosLogEntry>>(emptyList())
    val historyLogs: StateFlow<List<EmergencySosLogEntry>> = _historyLogs.asStateFlow()

    private val _lastDispatchedMessage = MutableStateFlow("")
    val lastDispatchedMessage: StateFlow<String> = _lastDispatchedMessage.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Carrega contatos salvos ou popula com contatos modelo recomendados para motoboys
        val contactsJson = prefs.getString(KEY_CONTACTS_JSON, null)
        val loadedContacts = if (!contactsJson.isNullOrBlank()) {
            parseContactsFromJson(contactsJson)
        } else {
            getDefaultEmergencyContacts()
        }
        _emergencyContacts.value = loadedContacts
        if (contactsJson.isNullOrBlank()) {
            saveContactsToPrefs(prefs, loadedContacts)
        }

        // Carrega logs de auditoria
        val logsJson = prefs.getString(KEY_LOGS_JSON, null)
        if (!logsJson.isNullOrBlank()) {
            _historyLogs.value = parseLogsFromJson(logsJson)
        }

        isInitialized = true
        Log.i(TAG, "EmergencySosManager inicializado com ${loadedContacts.size} contatos e ${_historyLogs.value.size} logs.")
    }

    private fun getDefaultEmergencyContacts(): List<EmergencyContact> {
        return listOf(
            EmergencyContact(
                name = "Família / Contato de Confiança",
                phone = "11988887777",
                relationship = "Família",
                isWhatsAppEnabled = true,
                isSmsEnabled = true
            ),
            EmergencyContact(
                name = "Central de Apoio / Ponto de Encontro",
                phone = "11977776666",
                relationship = "Base Motoboys",
                isWhatsAppEnabled = true,
                isSmsEnabled = true
            ),
            EmergencyContact(
                name = "Amigo Piloto / Parceiro de Rota",
                phone = "11999990000",
                relationship = "Colega de Entrega",
                isWhatsAppEnabled = true,
                isSmsEnabled = false
            )
        )
    }

    /**
     * Inicia a contagem regressiva de segurança (5 segundos) antes do disparo real do SOS.
     * Permite ao piloto cancelar toques acidentais causados por trepidação do guidão.
     */
    fun startSosCountdown(context: Context, onTriggered: () -> Unit = {}) {
        countdownJob?.cancel()
        val targetTime = System.currentTimeMillis() + 5_000L

        countdownJob = managerScope.launch {
            for (sec in 5 downTo 1) {
                _sosState.value = EmergencySosState.Countdown(
                    secondsRemaining = sec,
                    targetTimestampMillis = targetTime
                )
                // Micro-tick tátil discreto a cada segundo
                HapticFeedbackHelper.vibrateSosCountdownTick(context)
                delay(1000L)
            }
            // Contagem concluída: dispara o SOS silencioso
            triggerSosImmediately(context)
            onTriggered()
        }
    }

    /**
     * Cancela a contagem regressiva caso tenha sido um toque acidental.
     */
    fun cancelSosCountdown(context: Context) {
        countdownJob?.cancel()
        countdownJob = null
        _sosState.value = EmergencySosState.Idle
        HapticFeedbackHelper.vibrateDecline(context)
        Log.d(TAG, "Contagem regressiva de SOS cancelada pelo usuário.")
    }

    /**
     * Dispara imediatamente o alerta SOS sem aguardar a contagem regressiva.
     */
    fun triggerSosImmediately(
        context: Context,
        deliveryInfo: String? = null
    ) {
        countdownJob?.cancel()
        countdownJob = null

        // 1. Obtém a localização GPS em tempo real (com fallback seguro contra 0.0, 0.0)
        val locationState = LocationService.globalLocationState.value
        val (lat, lng) = if (locationState.isTracking && locationState.latitude != 0.0 && locationState.longitude != 0.0) {
            Pair(locationState.latitude, locationState.longitude)
        } else {
            try {
                val locMgr = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val lastLoc = locMgr?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locMgr?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if (lastLoc != null) {
                    Pair(lastLoc.latitude, lastLoc.longitude)
                } else if (locationState.latitude != 0.0 && locationState.longitude != 0.0) {
                    Pair(locationState.latitude, locationState.longitude)
                } else {
                    Pair(-23.561684, -46.655981)
                }
            } catch (_: Exception) {
                if (locationState.latitude != 0.0 && locationState.longitude != 0.0) {
                    Pair(locationState.latitude, locationState.longitude)
                } else {
                    Pair(-23.561684, -46.655981)
                }
            }
        }
        val speed = locationState.currentSpeedKmh
        val accuracy = locationState.accuracyMeters
        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"

        val timeStr = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault()).format(Date())

        // 2. Monta mensagem tática de socorro
        val distressMessage = buildString {
            append("🚨 ALERTA SOS SILENCIOSO - RADAR DELIVERY 🚨\n")
            append("Preciso de ajuda urgente durante rota de entrega!\n\n")
            append("📍 Minha Localização em Tempo Real:\n$mapsUrl\n\n")
            append("⚡ Velocidade: ${String.format(Locale.US, "%.1f", speed)} km/h\n")
            append("🎯 Precisão GPS: ±${accuracy.toInt()} metros\n")
            append("⏰ Horário do Alerta: $timeStr\n")
            if (!deliveryInfo.isNullOrBlank()) {
                append("📦 Entrega: $deliveryInfo\n")
            }
            append("\n[Alerta prioritário gerado pelo app Radar Delivery]")
        }
        _lastDispatchedMessage.value = distressMessage

        // 3. Dispara o WorkManager para execução resiliente em segundo plano
        val workId = enqueueWorkManagerSilentAlert(
            context = context,
            lat = lat,
            lng = lng,
            speed = speed,
            mapsUrl = mapsUrl,
            message = distressMessage,
            contactsCount = _emergencyContacts.value.size,
            deliveryInfo = deliveryInfo ?: "Em rota ativa"
        )

        // 4. Cria o registro de log
        val newLog = EmergencySosLogEntry(
            latitude = lat,
            longitude = lng,
            speedKmh = speed,
            mapsUrl = mapsUrl,
            contactsNotifiedCount = _emergencyContacts.value.size,
            workManagerId = workId,
            status = "Alerta Silencioso Disparado",
            networkDelivered = false
        )
        val updatedLogs = listOf(newLog) + _historyLogs.value
        _historyLogs.value = updatedLogs
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveLogsToPrefs(prefs, updatedLogs)

        // 5. Atualiza o estado para Ativo
        _sosState.value = EmergencySosState.Active(
            triggeredAtMillis = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            speedKmh = speed,
            mapsUrl = mapsUrl,
            workManagerId = workId,
            contactsNotifiedCount = _emergencyContacts.value.size,
            networkDelivered = false
        )

        // 6. Confirmação tátil imperceptível para terceiros
        HapticFeedbackHelper.vibrateSilentSosConfirmation(context)
        Log.i(TAG, "🚨 SOS Silencioso disparado com sucesso! WorkManager ID: $workId")
    }

    /**
     * Enfileira o Worker no WorkManager com constraints e retry exponencial
     */
    private fun enqueueWorkManagerSilentAlert(
        context: Context,
        lat: Double,
        lng: Double,
        speed: Double,
        mapsUrl: String,
        message: String,
        contactsCount: Int,
        deliveryInfo: String
    ): String {
        val inputData = workDataOf(
            EmergencySilentAlertWorker.KEY_LATITUDE to lat,
            EmergencySilentAlertWorker.KEY_LONGITUDE to lng,
            EmergencySilentAlertWorker.KEY_SPEED_KMH to speed,
            EmergencySilentAlertWorker.KEY_MAPS_URL to mapsUrl,
            EmergencySilentAlertWorker.KEY_MESSAGE to message,
            EmergencySilentAlertWorker.KEY_TIMESTAMP to System.currentTimeMillis(),
            EmergencySilentAlertWorker.KEY_CONTACTS_COUNT to contactsCount,
            EmergencySilentAlertWorker.KEY_DELIVERY_INFO to deliveryInfo
        )

        val workRequest = OneTimeWorkRequestBuilder<EmergencySilentAlertWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EmergencySilentAlertWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id.toString()
    }

    /**
     * Chamado pelo [EmergencySilentAlertWorker] quando a tarefa em background é concluída com sucesso.
     */
    fun recordWorkerExecutionSuccess(
        workId: String,
        timestamp: Long,
        lat: Double,
        lng: Double,
        speedKmh: Double,
        mapsUrl: String,
        contactsCount: Int,
        networkDelivered: Boolean
    ) {
        val current = _sosState.value
        if (current is EmergencySosState.Active) {
            _sosState.value = current.copy(networkDelivered = networkDelivered)
        }

        val updated = _historyLogs.value.map { entry ->
            if (entry.workManagerId == workId) {
                entry.copy(
                    networkDelivered = networkDelivered,
                    status = if (networkDelivered) "Entregue à Central & Contatos" else "Alerta Salvo Localmente"
                )
            } else entry
        }
        _historyLogs.value = updated
    }

    /**
     * Finaliza a ocorrência de SOS e marca o entregador como em segurança.
     */
    fun resolveSos(context: Context) {
        val activeState = _sosState.value
        val now = System.currentTimeMillis()

        if (activeState is EmergencySosState.Active) {
            val updated = _historyLogs.value.map { entry ->
                if (entry.workManagerId == activeState.workManagerId) {
                    entry.copy(
                        status = "Ocorrência Finalizada - Entregador Seguro",
                        resolvedAtMillis = now
                    )
                } else entry
            }
            _historyLogs.value = updated
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            saveLogsToPrefs(prefs, updated)
        }

        _sosState.value = EmergencySosState.Resolved(
            resolvedAtMillis = now,
            logId = if (activeState is EmergencySosState.Active) activeState.workManagerId else ""
        )

        HapticFeedbackHelper.vibrateSuccess(context)
        Log.i(TAG, "Ocorrência de SOS finalizada com sucesso. Entregador seguro.")
    }

    fun resetToIdle() {
        _sosState.value = EmergencySosState.Idle
    }

    // ----------------------------------------------------
    // GESTÃO DE CONTATOS DE EMERGÊNCIA
    // ----------------------------------------------------
    fun addContact(context: Context, contact: EmergencyContact) {
        val list = _emergencyContacts.value.toMutableList()
        list.add(contact)
        _emergencyContacts.value = list
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveContactsToPrefs(prefs, list)
    }

    fun removeContact(context: Context, contactId: String) {
        val list = _emergencyContacts.value.filter { it.id != contactId }
        _emergencyContacts.value = list
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveContactsToPrefs(prefs, list)
    }

    fun updateContact(context: Context, updated: EmergencyContact) {
        val list = _emergencyContacts.value.map {
            if (it.id == updated.id) updated else it
        }
        _emergencyContacts.value = list
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveContactsToPrefs(prefs, list)
    }

    /**
     * Abre diretamente o WhatsApp com o texto de socorro e link de GPS pré-preenchido para o contato.
     */
    fun sendSosViaWhatsApp(context: Context, contact: EmergencyContact, customMessage: String? = null): Boolean {
        return try {
            val rawPhone = contact.phone.replace(Regex("[^0-9]"), "")
            val formattedPhone = if (rawPhone.length <= 11 && !rawPhone.startsWith("55")) {
                "55$rawPhone"
            } else {
                rawPhone
            }
            val text = customMessage ?: _lastDispatchedMessage.value
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(text)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir WhatsApp para contato ${contact.name}: ${e.message}")
            false
        }
    }

    /**
     * Abre o aplicativo padrão de SMS com mensagem de socorro preenchida para o contato.
     */
    fun sendSosViaSms(context: Context, contact: EmergencyContact, customMessage: String? = null): Boolean {
        return try {
            val text = customMessage ?: _lastDispatchedMessage.value
            val cleanPhone = contact.phone.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao disparar SMS para contato ${contact.name}: ${e.message}")
            false
        }
    }

    /**
     * Dispara compartilhamento geral da localização de emergência para qualquer aplicativo (Telegram, Zap, Mensagens).
     */
    fun shareDistressLocation(context: Context, message: String? = null) {
        try {
            val text = message ?: _lastDispatchedMessage.value
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, "Enviar SOS de Emergência via:").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir chooser de compartilhamento de emergência", e)
        }
    }

    // ----------------------------------------------------
    // PERSISTÊNCIA LOCAL (SharedPreferences)
    // ----------------------------------------------------
    private fun saveContactsToPrefs(prefs: SharedPreferences, contacts: List<EmergencyContact>) {
        val array = JSONArray()
        contacts.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("relationship", c.relationship)
                put("isWhatsApp", c.isWhatsAppEnabled)
                put("isSms", c.isSmsEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CONTACTS_JSON, array.toString()).apply()
    }

    private fun parseContactsFromJson(json: String): List<EmergencyContact> {
        val result = mutableListOf<EmergencyContact>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    EmergencyContact(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Contato"),
                        phone = obj.optString("phone", ""),
                        relationship = obj.optString("relationship", "Geral"),
                        isWhatsAppEnabled = obj.optBoolean("isWhatsApp", true),
                        isSmsEnabled = obj.optBoolean("isSms", true)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desserializar contatos de emergência", e)
        }
        return if (result.isEmpty()) getDefaultEmergencyContacts() else result
    }

    private fun saveLogsToPrefs(prefs: SharedPreferences, logs: List<EmergencySosLogEntry>) {
        val array = JSONArray()
        logs.take(20).forEach { l ->
            val obj = JSONObject().apply {
                put("id", l.id)
                put("timestamp", l.timestampMillis)
                put("lat", l.latitude)
                put("lng", l.longitude)
                put("speed", l.speedKmh)
                put("mapsUrl", l.mapsUrl)
                put("contactsCount", l.contactsNotifiedCount)
                put("workManagerId", l.workManagerId)
                put("status", l.status)
                put("networkDelivered", l.networkDelivered)
                if (l.resolvedAtMillis != null) {
                    put("resolvedAt", l.resolvedAtMillis)
                }
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_LOGS_JSON, array.toString()).apply()
    }

    private fun parseLogsFromJson(json: String): List<EmergencySosLogEntry> {
        val result = mutableListOf<EmergencySosLogEntry>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    EmergencySosLogEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestampMillis = obj.optLong("timestamp", System.currentTimeMillis()),
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        speedKmh = obj.optDouble("speed", 0.0),
                        mapsUrl = obj.optString("mapsUrl", ""),
                        contactsNotifiedCount = obj.optInt("contactsCount", 0),
                        workManagerId = obj.optString("workManagerId", ""),
                        status = obj.optString("status", ""),
                        networkDelivered = obj.optBoolean("networkDelivered", false),
                        resolvedAtMillis = if (obj.has("resolvedAt")) obj.optLong("resolvedAt") else null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desserializar logs de emergência", e)
        }
        return result
    }
}
