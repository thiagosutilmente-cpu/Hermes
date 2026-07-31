package com.example

import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import com.example.util.RouteOptimizer
import com.example.util.GeminiManager
import com.example.util.StopPoint
import com.example.util.StopType
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Analytics


import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapType
import android.location.Geocoder
import java.io.IOException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coordinator.ActiveOffer
import com.example.coordinator.LogEntry
import com.example.coordinator.LogType
import com.example.coordinator.RadarCoordinator
import com.example.ui.components.JarvisVoiceHUD
import com.example.coordinator.RadarSettings
import com.example.coordinator.RadarState
import com.example.coordinator.SpeedState
import com.example.data.AppDatabase
import com.example.data.OfferEntity
import com.example.data.FirestoreManager
import com.example.data.FirebaseAuthManager
import com.example.data.awaitTask
import com.example.service.RadarCoordinatorService
import com.example.ui.theme.MyApplicationTheme
import com.example.api.RadarApiFactory
import com.example.api.DailyReportItem
import com.example.api.AppBreakdownItem
import com.example.api.HotZoneItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. Theme and color override helper to fit dark premium luxury theme for motorcyclists or light day theme
val DarkSlateBg: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFF05050A) else Color(0xFFF0F2F5)

val CardSlateBg: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0x6614141E) else Color(0xCCFFFFFF)

val TextLight: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)

val TextDim: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFF64748B) else Color(0xFF4B5563)

val AccentGreen = Color(0xFF00F5D4)
val AccentRed = Color(0xFFFF006E)
val AccentAmber = Color(0xFFFFBE0B)
val AccentBlue = Color(0xFF00E5FF)

data class EliteRegion(
    val name: String,
    val city: String,
    val predominantApp: String,
    val demandLevel: String,
    val waitTimeText: String,
    val avgValuePerKm: Double,
    val avgFare: Double,
    val strategy: String,
    val latitude: Double,
    val longitude: Double,
    val simFare: Double,
    val simDist: Double,
    val simTime: Double,
    val simPickup: String,
    val simDelivery: String
)

val eliteRegionsList = listOf(
    EliteRegion(
        name = "Shopping Eldorado (iFood Central)",
        city = "São Paulo",
        predominantApp = "iFood",
        demandLevel = "Crítica",
        waitTimeText = "8 min",
        avgValuePerKm = 3.10,
        avgFare = 18.50,
        strategy = "Concentração maciça de restaurantes premium. Dica: Aguarde na área de recuo de motos do G2. Perfeito para acumular coletas duplas de alto valor.",
        latitude = -23.5727,
        longitude = -46.6961,
        simFare = 19.50,
        simDist = 4.2,
        simTime = 15.0,
        simPickup = "Restaurante Madero (Shopping Eldorado)",
        simDelivery = "Av. Faria Lima, 3500 - Pinheiros"
    ),
    EliteRegion(
        name = "Metrô Consolação (Uber Moto Hub)",
        city = "São Paulo",
        predominantApp = "Uber",
        demandLevel = "Extrema",
        waitTimeText = "3 min",
        avgValuePerKm = 2.65,
        avgFare = 12.80,
        strategy = "Demanda frenética de passageiros e pequenos pacotes (Uber Flash). Dica: Corrida aceita instantaneamente. Mantenha os ganhos acima de R$ 2,20/km.",
        latitude = -23.5587,
        longitude = -46.6612,
        simFare = 14.20,
        simDist = 5.4,
        simTime = 12.0,
        simPickup = "Saída Metrô Consolação (Av. Paulista)",
        simDelivery = "Av. Brigadeiro Luís Antônio, 2300"
    ),
    EliteRegion(
        name = "Shopping Morumbi (iFood & 99 Central)",
        city = "São Paulo",
        predominantApp = "99",
        demandLevel = "Alta",
        waitTimeText = "6 min",
        avgValuePerKm = 2.90,
        avgFare = 15.20,
        strategy = "Região excelente com entregas residenciais rápidas na Zona Sul. Dica: Ajuste seu filtro para R$ 2,50/km para emendar corridas sem intervalo.",
        latitude = -23.6212,
        longitude = -46.6983,
        simFare = 16.50,
        simDist = 5.1,
        simTime = 18.0,
        simPickup = "Shopping Morumbi (Ponto de Apoio)",
        simDelivery = "Rua Verbo Divino, 1200 - Chácara Sto Antônio"
    ),
    EliteRegion(
        name = "Alphaville Comercial (Rappi & iFood Premium)",
        city = "São Paulo/Barueri",
        predominantApp = "iFood",
        demandLevel = "Alta",
        waitTimeText = "5 min",
        avgValuePerKm = 3.80,
        avgFare = 24.00,
        strategy = "Ticket médio altíssimo em restaurantes premium e condomínios fechados de luxo. Dica: Ideal aos fins de semana. Altos valores e boas gorjetas.",
        latitude = -23.4983,
        longitude = -46.8471,
        simFare = 28.50,
        simDist = 7.5,
        simTime = 22.0,
        simPickup = "Al. Rio Negro, 500 (Pobre Juan)",
        simDelivery = "Residencial Alphaville 3 - Portaria 1"
    ),
    EliteRegion(
        name = "Rodoviária do Tietê (99Moto Passageiros)",
        city = "São Paulo",
        predominantApp = "99",
        demandLevel = "Extrema",
        waitTimeText = "2 min",
        avgValuePerKm = 2.40,
        avgFare = 11.50,
        strategy = "Fluxo contínuo 24h de passageiros com mala de mão desembarcando. Dica: Excelentes bônus dinâmicos locais. Ideal para faturar rápido com giros curtos.",
        latitude = -23.5164,
        longitude = -46.6247,
        simFare = 10.80,
        simDist = 3.8,
        simTime = 10.0,
        simPickup = "Rodoviária do Tietê (Setor Desembarque)",
        simDelivery = "Praça da República, 50 - Centro"
    )
)

class MainViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    fun activateGodMode() {
        RadarCoordinator.activateAll(appContext)
    }

    private val _showUpdateModal = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showUpdateModal: kotlinx.coroutines.flow.StateFlow<Boolean> = _showUpdateModal.asStateFlow()
    fun setShowUpdateModal(show: Boolean) { _showUpdateModal.value = show }
    
    private val appContext = application.applicationContext
    private val database = AppDatabase.getDatabase(application)
    private val offerDao = database.offerDao()

    fun refreshFromFirestore() {
        viewModelScope.launch {
            try {
                val cached = FirestoreManager.loadDailyReports()
                _dailyReport.value = cached
                Log.d("MainViewModel", "Loaded ${cached.size} daily report items from Firestore after auth change")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading daily reports from Firestore: ${e.message}")
            }
        }
    }

    init {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000L) // Clean up every 60 seconds
                RadarCoordinator.pruneOldLogs()
            }
        }
        viewModelScope.launch {
            try {
                val cached = FirestoreManager.loadDailyReports()
                if (cached.isNotEmpty()) {
                    _dailyReport.value = cached
                    Log.d("MainViewModel", "Preloaded ${cached.size} daily report items from Firestore")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error preloading daily reports from Firestore: ${e.message}")
            }
        }
        viewModelScope.launch {
            FirebaseAuthManager.currentUser.collect { user ->
                Log.d("MainViewModel", "Auth state observed change in ViewModel. User: ${user?.email ?: "None"}")
                // Sync cloud settings and route logs in RadarCoordinator
                RadarCoordinator.syncWithCloud(appContext)
                // Reload daily reports
                refreshFromFirestore()
                // Reload user profile
                loadUserProfile()
            }
        }
    }

    private val _userProfile = MutableStateFlow(com.example.data.UserProfile())
    val userProfile: StateFlow<com.example.data.UserProfile> = _userProfile.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = FirestoreManager.loadUserProfile()
                if (profile != null) {
                    _userProfile.value = profile
                    RadarCoordinator.updateUserProfile(profile)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading user profile: ${e.message}")
            }
        }
    }

    fun saveUserProfile(profile: com.example.data.UserProfile) {
        viewModelScope.launch {
            try {
                FirestoreManager.saveUserProfile(profile)
                _userProfile.value = profile
                RadarCoordinator.updateUserProfile(profile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving user profile: ${e.message}")
            }
        }
    }

    val historyOffers: StateFlow<List<OfferEntity>> = offerDao.getAllOffers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _dailyReport = MutableStateFlow<List<DailyReportItem>>(emptyList())
    val dailyReport: StateFlow<List<DailyReportItem>> = _dailyReport.asStateFlow()

    private val _reportLoading = MutableStateFlow(false)
    val reportLoading: StateFlow<Boolean> = _reportLoading.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<com.example.data.AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<com.example.data.AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    fun checkUpdateAndConfig() {
        viewModelScope.launch {
            try {
                com.example.data.RemoteConfigManager.fetchAndApplyRemoteConfig()
                val updateInfo = com.example.data.RemoteConfigManager.checkAppUpdate()
                if (updateInfo != null && updateInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                    _appUpdateInfo.value = updateInfo
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error checking update: ${e.message}")
            }
        }
    }

    private val _jarvisState = MutableStateFlow("IDLE") // IDLE, GREETING, LISTENING, ANALYZING, SPEAKING_RESPONSE, FINISHED
    val jarvisState: StateFlow<String> = _jarvisState.asStateFlow()

    private val _jarvisTranscription = MutableStateFlow("")
    val jarvisTranscription: StateFlow<String> = _jarvisTranscription.asStateFlow()

    private val _jarvisResponse = MutableStateFlow("")
    val jarvisResponse: StateFlow<String> = _jarvisResponse.asStateFlow()

    private val _jarvisThoughtProcess = MutableStateFlow("")
    val jarvisThoughtProcess: StateFlow<String> = _jarvisThoughtProcess.asStateFlow()

    private val _jarvisStressLevel = MutableStateFlow("LOW")
    val jarvisStressLevel: StateFlow<String> = _jarvisStressLevel.asStateFlow()

    private val _jarvisStrategyLabel = MutableStateFlow("ESTRATÉGIA PADRÃO")
    val jarvisStrategyLabel: StateFlow<String> = _jarvisStrategyLabel.asStateFlow()

    private val _jarvisCategory = MutableStateFlow("")
    val jarvisCategory: StateFlow<String> = _jarvisCategory.asStateFlow()

    fun updateJarvisResponse(response: String) {
        _jarvisResponse.value = response
        _jarvisState.value = "SPEAKING_RESPONSE"
    }

    fun resetJarvis() {
        _jarvisState.value = "IDLE"
        _jarvisTranscription.value = ""
        _jarvisResponse.value = ""
        _jarvisThoughtProcess.value = ""
        _jarvisStressLevel.value = "LOW"
        _jarvisCategory.value = ""
        try {
            RadarCoordinator.voiceInputManager?.stopListening()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping voice recognizer: ${e.message}")
        }
    }

    fun startJarvisSession() {
        viewModelScope.launch {
            _jarvisState.value = "GREETING"
            _jarvisTranscription.value = ""
            _jarvisResponse.value = ""
            _jarvisCategory.value = ""
            
            // Speak greeting
            val greeting = "Olá! Sou o Jarvis. Dica rápida: use intercomunicador de capacete em vez de fones para evitar multas de trânsito. Diga o seu feedback, sugestão ou algum problema que esteja enfrentando."
            RadarCoordinator.voiceManager?.speak(greeting)
            
            // Wait for speaking to finish (approx 7.5 seconds)
            kotlinx.coroutines.delay(7500L)
            
            if (_jarvisState.value != "GREETING") return@launch // session was reset
            
            _jarvisState.value = "LISTENING"
            RadarCoordinator.voiceInputManager?.startListening(isJarvis = true) { spokenText ->
                if (_jarvisState.value == "LISTENING") {
                    processJarvisInput(spokenText)
                }
            }
        }
    }

    private fun processJarvisInput(text: String) {
        viewModelScope.launch {
            _jarvisState.value = "ANALYZING"
            _jarvisTranscription.value = text

            if (text.isEmpty() || text == "Ouvindo comando..." || text.startsWith("Erro:")) {
                _jarvisResponse.value = "Não consegui ouvir nenhum feedback claramente. Por favor, tente falar novamente."
                _jarvisState.value = "SPEAKING_RESPONSE"
                RadarCoordinator.voiceManager?.speak(_jarvisResponse.value)
                return@launch
            }

            // Build context for Jarvis with rich telemetry
            val offer = RadarCoordinator.activeOffer.value
            val speed = RadarCoordinator.currentSpeedKmh.value
            val batteryIntent = appContext.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) (level / scale.toFloat() * 100).toInt() else -1
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            val contextInfo = StringBuilder().apply {
                append("Thiago está operando. Hora atual: $time. Bateria: $batteryPct%. Velocidade: ${speed.toInt()} km/h. ")
                
                val memories = com.example.coordinator.RadarCoordinator.jarvisMemories.value
                if (memories.isNotEmpty()) {
                    append("Regras e memórias salvas: ${memories.joinToString("; ")}. ")
                }

                val zones = com.example.coordinator.RadarCoordinator.hotZones.value
                if (zones.isNotEmpty()) {
                    append("Hot Zones detectadas pelo Radar Central: ")
                    zones.take(3).forEach { zone ->
                        append("${zone.address} (Média R$ ${zone.avgFare}, ${zone.offersCount} ofertas recentemente). ")
                    }
                }

                if (offer != null) {
                    append("Oferta ativa: R$ ${offer.fareValue} por ${offer.totalDistance}km do app ${offer.appName}. Coleta em ${offer.pickupAddress}, Entrega em ${offer.deliveryAddress}.")
                } else {
                    append("Nenhuma oferta na tela, Thiago está aguardando.")
                }
            }.toString()

            // Fetch Hot Zones in background to update for next time
            viewModelScope.launch {
                try {
                    val serverBaseUrl = RadarCoordinator.settings.value.serverBaseUrl
                    val api = com.example.api.RadarApiFactory.create(serverBaseUrl)
                    val loc = RadarCoordinator.currentLocation.value
                    if (loc != null) {
                        val zones = api.getHotZones(RadarCoordinator.settings.value.apiToken, loc.latitude, loc.longitude)
                        RadarCoordinator.updateHotZones(zones)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Erro ao buscar Hot Zones: ${e.message}")
                }
            }

            // Process via Jarvis Persona Engine
            val result = com.example.voice.JarvisPersonaEngine.processCommand(text, contextInfo)
            
            when (result.action) {
                "ACCEPT_OFFER" -> {
                    val intent = android.content.Intent(appContext, com.example.service.RadarCoordinatorService::class.java).apply {
                        putExtra("ACCEPT_OFFER_MANUAL", true)
                    }
                    appContext.startService(intent)
                }
                "REJECT_OFFER" -> {
                    val intent = android.content.Intent(appContext, com.example.service.RadarCoordinatorService::class.java).apply {
                        putExtra("DISMISS_OFFER_MANUAL", true)
                    }
                    appContext.startService(intent)
                }
                "ADD_MEMORY" -> {
                    if (result.memoryContent.isNotEmpty()) {
                        com.example.coordinator.RadarCoordinator.addJarvisMemory(appContext, result.memoryContent)
                    }
                }
                "TACTICAL_ADVICE" -> {
                    com.example.coordinator.RadarCoordinator.addLog("CONSELHO TÁTICO: ${result.voiceResponse}", com.example.coordinator.LogType.INFO)
                }
                "EMERGENCY_MODE" -> {
                    // Could trigger a real SOS call or signal
                    Log.w("MainViewModel", "EMERGENCY MODE TRIGGERED BY JARVIS")
                    com.example.coordinator.RadarCoordinator.addLog("PROTOCOLO DE EMERGÊNCIA ATIVADO PELO JARVIS", com.example.coordinator.LogType.ALERT)
                }
                "SURREAL_CLICK" -> {
                    if (result.neuralKeywords.isNotEmpty()) {
                        val intent = android.content.Intent(appContext, com.example.service.RadarCoordinatorService::class.java).apply {
                            putExtra("NEURAL_ACTION", true)
                            putStringArrayListExtra("KEYWORDS", ArrayList(result.neuralKeywords))
                        }
                        appContext.startService(intent)
                    }
                }
            }
            
            _jarvisResponse.value = result.voiceResponse
            _jarvisThoughtProcess.value = result.thoughtProcess
            _jarvisStressLevel.value = result.stressLevel
            _jarvisStrategyLabel.value = result.strategyLabel
            _jarvisState.value = "SPEAKING_RESPONSE"
            com.example.coordinator.RadarCoordinator.voiceManager?.speak(result.voiceResponse)
            
            kotlinx.coroutines.delay(10000L)
            if (_jarvisState.value == "SPEAKING_RESPONSE") {
                _jarvisState.value = "FINISHED"
            }
        }
    }

    private val _reportError = MutableStateFlow<String?>(null)
    val reportError: StateFlow<String?> = _reportError.asStateFlow()

    private val _hotZones = MutableStateFlow<List<HotZoneItem>>(emptyList())
    val hotZones: StateFlow<List<HotZoneItem>> = _hotZones.asStateFlow()

    private val _hotZonesLoading = MutableStateFlow(false)
    val hotZonesLoading: StateFlow<Boolean> = _hotZonesLoading.asStateFlow()

    private val _hotZonesError = MutableStateFlow<String?>(null)
    val hotZonesError: StateFlow<String?> = _hotZonesError.asStateFlow()

    fun clearHistory() {
        viewModelScope.launch {
            offerDao.clearHistory()
        }
    }

    fun fetchDailyReport(serverBaseUrl: String, apiToken: String) {
        viewModelScope.launch {
            _reportLoading.value = true
            _reportError.value = null
            try {
                val api = RadarApiFactory.create(serverBaseUrl)
                val report = api.getDailyReport(apiToken)
                _dailyReport.value = report
                try {
                    FirestoreManager.saveDailyReports(report)
                } catch (fe: Exception) {
                    Log.e("MainActivity", "Failed to save daily reports to Firestore: ${fe.message}")
                }
            } catch (e: Exception) {
                _reportError.value = e.message ?: "Erro desconhecido ao carregar relatório"
                try {
                    val cachedReport = FirestoreManager.loadDailyReports()
                    if (cachedReport.isNotEmpty()) {
                        _dailyReport.value = cachedReport
                        _reportError.value = null // Clear error since we loaded cached data!
                        Log.d("MainActivity", "Successfully fell back to Firestore daily reports cache")
                    }
                } catch (fe: Exception) {
                    Log.e("MainActivity", "Failed to load cached daily reports from Firestore: ${fe.message}")
                }
            } finally {
                _reportLoading.value = false
            }
        }
    }

    fun fetchHotZones(serverBaseUrl: String, apiToken: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _hotZonesLoading.value = true
            _hotZonesError.value = null
            try {
                val api = RadarApiFactory.create(serverBaseUrl)
                val zones = api.getHotZones(apiToken, latitude, longitude)
                _hotZones.value = zones
            } catch (e: Exception) {
                _hotZonesError.value = e.message ?: "Erro desconhecido ao carregar zonas quentes"
            } finally {
                _hotZonesLoading.value = false
            }
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

val globalChatMessages = androidx.compose.runtime.mutableStateListOf(ChatMessage("Olá! Sou o OpenJarvis, sua inteligência artificial pessoal. Como posso te ajudar hoje?", false))
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
    }



    private val chatReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val message = intent.getStringExtra("MESSAGE") ?: ""
            val isUser = intent.getBooleanExtra("IS_USER", false)
            if (message.isNotEmpty()) {
                globalChatMessages.add(ChatMessage(message, isUser))
            }
        }
    }


    private lateinit var viewModel: MainViewModel

    // Multi-permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (fineGranted || coarseGranted) {
            com.example.util.ToastUtils.showToast(this, "Permissão de localização concedida!", Toast.LENGTH_SHORT)
        } else {
            com.example.util.ToastUtils.showToast(this, "Localização necessária para monitorar velocidade real.", Toast.LENGTH_LONG)
        }

        if (audioGranted) {
            Log.d("MainActivity", "Permissão de áudio concedida para o Jarvis.")
        } else {
            com.example.util.ToastUtils.showToast(this, "Permissão de áudio necessária para comandos de voz do Jarvis.", Toast.LENGTH_LONG)
        }

        if (!notificationGranted) {
            com.example.util.ToastUtils.showToast(this, "Notificações necessárias para rodar o serviço em segundo plano.", Toast.LENGTH_LONG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mantém a tela ligada para os motoboys não perderem o app de vista
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize coordinator
        RadarCoordinator.init(this)
        com.example.util.OfflineMapManager.init(this)
        viewModel = MainViewModel(this.application)
        viewModel.checkUpdateAndConfig()

        enableEdgeToEdge()

        // Request permissions on startup
        checkAndRequestPermissions()

        setContent {
            val settings by RadarCoordinator.settings.collectAsStateWithLifecycle()
            val updateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = settings.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CardSlateBg
                ) {
                    if (updateInfo != null) {
                        AlertDialog(
                            onDismissRequest = { 
                                // do nothing if forced
                            },
                            title = { Text("Nova Atualização") },
                            text = { Text("Uma nova versão do Jarvis Sovereign V22 está disponível para download.\n\n${updateInfo?.releaseNotes ?: ""}") },
                            confirmButton = {
                                Button(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo?.downloadUrl ?: ""))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    try {
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error opening link: ${e.message}")
                                    }
                                }) {
                                    Text("Baixar APK")
                                }
                            },
                            dismissButton = if (updateInfo?.forceUpdate != true) {
                                {
                                    TextButton(onClick = { viewModel.setShowUpdateModal(false) }) {
                                        Text("Depois")
                                    }
                                }
                            } else null
                        )
                    }

                    RadarDashboardScreen(viewModel)
                    
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDashboardScreen(viewModel: MainViewModel) {
    RadarDashboardScreen_Legacy(viewModel)
}

@Composable
fun RadarDashboardScreen_Legacy(viewModel: MainViewModel, initialTab: Int = 0) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Saudação inicial do Jarvis ao entrar no cockpit com Consciência de Contexto
    val settingsState by com.example.coordinator.RadarCoordinator.settings.collectAsState()
    val batteryLevel by com.example.coordinator.RadarCoordinator.batteryLevel.collectAsState()
    val strategy by com.example.coordinator.RadarCoordinator.currentStrategy.collectAsState()
    val stats by com.example.coordinator.RadarCoordinator.sessionStats.collectAsState()
    val coordinatorUserProfile by com.example.coordinator.RadarCoordinator.userProfile.collectAsState()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        val timeNow = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        
        val shiftDurationMillis = System.currentTimeMillis() - com.example.coordinator.RadarCoordinator.sessionStartTime.value
        val minutes = (shiftDurationMillis / (1000 * 60)) % 60
        val shiftText = if (minutes > 1) "Turno iniciado há $minutes minutos." else "Acabamos de ligar os motores."

        val contextPrompt = """
            O Senhor Thiago acaba de acessar o cockpit. 
            ESTADO DO TURNO: $shiftText
            CONTEXTO ATUAL:
            - Hora: $timeNow
            - Estratégia Ativa: $strategy
            - Bateria: $batteryLevel%
            - Ganhos: R$ ${String.format("%.2f", stats.totalEarnings)}
            - Ranking: ${com.example.coordinator.RadarCoordinator.getDriverLevel(coordinatorUserProfile.driverXP).name}
            
            Dê as boas-vindas de forma calorosa, humana e proativa. 
            Comente sobre o horário ou o status do turno de forma natural. 
            Diga algo que mostre que você está "vivo" e atento ao cockpit.
        """.trimIndent()

        com.example.coordinator.RadarCoordinator.voiceManager?.speakIntelligent(
            "Iniciando turno agora.",
            contextPrompt
        )
    }

    // Collect variables from Coordinator
    val serviceState by remember { mutableStateOf(RadarCoordinatorService.isServiceRunning) }
    var isServiceActive by remember { mutableStateOf(RadarCoordinatorService.isServiceRunning) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isNotificationListenerEnabled by remember { mutableStateOf(false) }

    var geminiSummary by remember { mutableStateOf<String?>(null) }
    var isGeneratingSummary by remember { mutableStateOf(false) }
    
    // Periodically sync service, accessibility and notification status
    LaunchedEffect(Unit) {
        while (true) {
            isServiceActive = RadarCoordinatorService.isServiceRunning
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            isNotificationListenerEnabled = isNotificationListenerServiceEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    val currentRadarState by RadarCoordinator.currentState.collectAsStateWithLifecycle()
    val speedState by RadarCoordinator.speedState.collectAsStateWithLifecycle()
    val currentSpeedKmh by RadarCoordinator.currentSpeedKmh.collectAsStateWithLifecycle()
    val settings by RadarCoordinator.settings.collectAsStateWithLifecycle()
    val activeOffer by RadarCoordinator.activeOffer.collectAsStateWithLifecycle()
    val lastDecision by RadarCoordinator.lastDecision.collectAsStateWithLifecycle()
    val lastReason by RadarCoordinator.lastReason.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyOffers.collectAsStateWithLifecycle()
    val logs by RadarCoordinator.logs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val deliveryActive by RadarCoordinator.deliveryActive.collectAsStateWithLifecycle()
    val deliveryStartTimestamp by RadarCoordinator.deliveryStartTimestamp.collectAsStateWithLifecycle()
    val deliveryAccumulatedDistanceMeters by RadarCoordinator.deliveryAccumulatedDistanceMeters.collectAsStateWithLifecycle()
    val deliveryFare by RadarCoordinator.deliveryFare.collectAsStateWithLifecycle()
    val deliveryAppName by RadarCoordinator.deliveryAppName.collectAsStateWithLifecycle()
    val deliveryEstimatedDistanceKm by RadarCoordinator.deliveryEstimatedDistanceKm.collectAsStateWithLifecycle()
    val deliveryEstimatedTimeMin by RadarCoordinator.deliveryEstimatedTimeMin.collectAsStateWithLifecycle()
    val deliveryCompletedCount by RadarCoordinator.deliveryCompletedCount.collectAsStateWithLifecycle()
    val deliveryTotalEarnings by RadarCoordinator.deliveryTotalEarnings.collectAsStateWithLifecycle()
    val deliveryTotalDistanceKm by RadarCoordinator.deliveryTotalDistanceKm.collectAsStateWithLifecycle()
    val deliveryTotalTimeMinutes by RadarCoordinator.deliveryTotalTimeMinutes.collectAsStateWithLifecycle()
    val blockedByGeofenceCount by RadarCoordinator.blockedByGeofenceCount.collectAsStateWithLifecycle()
    val currentGPSLocation by RadarCoordinator.currentLocation.collectAsStateWithLifecycle()
    val sosActive by RadarCoordinator.sosActive.collectAsStateWithLifecycle()

    val isVoiceListening by (RadarCoordinator.voiceInputManager?.isListening ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val voiceRecognizedText by (RadarCoordinator.voiceInputManager?.recognizedText ?: MutableStateFlow("")).collectAsStateWithLifecycle()

    // Observar mensagens proativas do Jarvis para exibir na UI
    val proactiveMessage by RadarCoordinator.jarvisProactiveMessage.collectAsStateWithLifecycle()
    LaunchedEffect(proactiveMessage) {
        proactiveMessage?.let { msg ->
            viewModel.updateJarvisResponse(msg)
            // Limpa após alguns segundos para não ficar preso na tela
            kotlinx.coroutines.delay(10000)
            if (RadarCoordinator.jarvisProactiveMessage.value == msg) {
                RadarCoordinator.setJarvisProactiveMessage(null)
            }
        }
    }

    // Form states for customizable simulation
    var simAppName by remember { mutableStateOf("iFood") }
    var simFareValue by remember { mutableStateOf("18.50") }
    var simDistance by remember { mutableStateOf("3.0") }
    var simPickup by remember { mutableStateOf("McDonalds - Shopping") }
    var simDelivery by remember { mutableStateOf("Rua das Flores, 123") }

    // Toggle states for panels
    var showConfetti by remember { mutableStateOf(false) }
    var showCustomSimPanel by remember { mutableStateOf(false) }
    var showConfigPanel by remember { mutableStateOf(false) }
    var showGeofenceModal by remember { mutableStateOf(false) }
    var showVoiceConfigModal by remember { mutableStateOf(false) }
    var showAdminLoginModal by remember { mutableStateOf(false) }
    var showAdminDashboardModal by remember { mutableStateOf(false) }
    var showOfflineMapsModal by remember { mutableStateOf(false) }
    var showProfilePanel by remember { mutableStateOf(false) }
    var firebaseErrorLogs by remember { mutableStateOf<List<com.example.data.AppErrorLog>>(emptyList()) }
    val showUpdateModal by viewModel.showUpdateModal.collectAsState()
    var showSafetyTip by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(initialTab) }

    

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (RadarCoordinator.settings.value.isDarkMode) 
                        listOf(Color(0xFF05050A), Color(0xFF0A0F1A), Color(0xFF020617)) 
                    else 
                        listOf(Color(0xFFF8F9FA), Color(0xFFE2E8F0))
                )
            )
    ) {
        if (showVoiceConfigModal) {
            val currentSettings = settings
            var voiceStyle by remember { mutableStateOf(currentSettings.jarvisVoiceStyle) }
            var voiceTone by remember { mutableStateOf(currentSettings.jarvisVoiceTone) }
            var voicePitch by remember { mutableStateOf(currentSettings.jarvisVoicePitch) }
            var voiceRate by remember { mutableStateOf(currentSettings.jarvisVoiceRate) }
            var voiceVolume by remember { mutableStateOf(currentSettings.jarvisVoiceVolume) }
            var continuousFrequency by remember { mutableStateOf(currentSettings.jarvisContinuousFrequency) }

            AlertDialog(
                onDismissRequest = { showVoiceConfigModal = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Configurações de Voz (TTS)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Personalize o estilo, humor, velocidade e tom do assistente de voz Jarvis para as instruções e alertas de corrida.",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Estilo da Voz
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Estilo de Voz",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "PADRAO" to "Jarvis",
                                    "MASCULINA" to "Masc",
                                    "FEMININA" to "Fem",
                                    "ACELERADA" to "⚡ Fast"
                                ).forEach { (style, label) ->
                                    val isSelected = voiceStyle == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentGreen else CardSlateBg.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) AccentGreen else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                voiceStyle = style
                                                val updated = currentSettings.copy(jarvisVoiceStyle = style)
                                                RadarCoordinator.saveSettings(context, updated)
                                                RadarCoordinator.voiceManager?.speak("Estilo alterado.")
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Humor/Tone
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Persona / Humor",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("FORMAL", "AMIGÁVEL", "DIRETO").forEach { tone ->
                                    val isSelected = voiceTone == tone
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentBlue else CardSlateBg.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) AccentBlue else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                voiceTone = tone
                                                val updated = currentSettings.copy(jarvisVoiceTone = tone)
                                                RadarCoordinator.saveSettings(context, updated)
                                                RadarCoordinator.voiceManager?.speak("Tom de voz atualizado.")
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tone,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Velocidade Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Velocidade (Speech Rate)",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1fx", voiceRate),
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = voiceRate,
                                onValueChange = { 
                                    voiceRate = it
                                    val updated = currentSettings.copy(jarvisVoiceRate = it)
                                    RadarCoordinator.saveSettings(context, updated)
                                },
                                onValueChangeFinished = {
                                    RadarCoordinator.voiceManager?.speak("Velocidade ajustada.")
                                },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("voice_rate_slider")
                            )
                        }

                        // Pitch Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ajuste de Tom (Pitch)",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1fx", voicePitch),
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = voicePitch,
                                onValueChange = { 
                                    voicePitch = it
                                    val updated = currentSettings.copy(jarvisVoicePitch = it)
                                    RadarCoordinator.saveSettings(context, updated)
                                },
                                onValueChangeFinished = {
                                    RadarCoordinator.voiceManager?.speak("Tom ajustado.")
                                },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("voice_pitch_slider")
                            )
                        }

                        // Volume Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Volume do Assistente",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%d%%", (voiceVolume * 100).toInt()),
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = voiceVolume,
                                onValueChange = { 
                                    voiceVolume = it
                                    val updated = currentSettings.copy(jarvisVoiceVolume = it)
                                    RadarCoordinator.saveSettings(context, updated)
                                },
                                onValueChangeFinished = {
                                    RadarCoordinator.voiceManager?.speak("Volume alterado.")
                                },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("voice_volume_slider")
                            )
                        }

                        // Sintonia de Frequência Contínua de Voz (Sintonizado com o Jarvis)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentBlue.copy(alpha = 0.05f))
                                .border(1.dp, AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Sintonia de Frequência",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Escuta contínua de comandos sem as mãos",
                                            color = Color.LightGray.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = continuousFrequency,
                                    onCheckedChange = { checked ->
                                        continuousFrequency = checked
                                        val updated = currentSettings.copy(jarvisContinuousFrequency = checked)
                                        RadarCoordinator.saveSettings(context, updated)
                                        if (checked) {
                                            RadarCoordinator.voiceManager?.speak("Frequência sintonizada. Estou te ouvindo.")
                                        } else {
                                            RadarCoordinator.voiceManager?.speak("Sintonia desativada.")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentBlue,
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.testTag("switch_continuous_frequency")
                                )
                            }
                        }

                        // Botão de Teste Completo
                        Button(
                            onClick = {
                                RadarCoordinator.voiceManager?.speak("Olá Thiago, eu sou o Jarvis. Minha síntese de voz está totalmente configurada e otimizada para o seu trajeto.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("btn_test_speech_phrase")
                        ) {
                            Text("Ouvir Frase de Teste Completa", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showVoiceConfigModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.testTag("btn_close_voice_config_dialog")
                    ) {
                        Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardSlateBg,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        if (showAdminLoginModal) {
            com.example.ui.AdminLoginModal(
                onDismiss = { showAdminLoginModal = false },
                onLoginSuccess = {
                    showAdminLoginModal = false
                    showAdminDashboardModal = true
                }
            )
        }

        if (showAdminDashboardModal) {
            com.example.ui.AdminDashboardModal(
                onDismiss = { showAdminDashboardModal = false }
            )
        }

        if (showOfflineMapsModal) {
            val regionsList by com.example.util.OfflineMapManager.regions.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            
            var searchQuery by remember { mutableStateOf("") }
            var selectedCityFilter by remember { mutableStateOf("Todos") }
            var autoDeleteEnabled by remember { mutableStateOf(false) }

            val filteredRegions = regionsList.filter { region ->
                val matchesSearch = region.name.contains(searchQuery, ignoreCase = true) || 
                                    region.city.contains(searchQuery, ignoreCase = true)
                val matchesFilter = selectedCityFilter == "Todos" || region.city == selectedCityFilter
                matchesSearch && matchesFilter
            }

            val totalUsedMB = regionsList.filter { it.isDownloaded }.sumOf { it.sizeMB }
            val storageLimitMB = 2048.0 // 2 GB
            val storageProgress = (totalUsedMB / storageLimitMB).toFloat().coerceIn(0f, 1f)

            AlertDialog(
                onDismissRequest = { showOfflineMapsModal = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Central de Mapas Offline",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showOfflineMapsModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextLight)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Storage Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Armazenamento no Dispositivo",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f MB / 2.0 GB", totalUsedMB),
                                        color = if (totalUsedMB > 1500) AccentRed else AccentGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { storageProgress },
                                    color = if (totalUsedMB > 1500) AccentRed else AccentBlue,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                
                                Text(
                                    text = "Baixe áreas de alta demanda do iFood, Uber e 99 para continuar navegando mesmo sem sinal de internet na sua rota.",
                                    color = TextDim,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar região ou cidade...", color = TextDim, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar", tint = TextLight, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkSlateBg,
                                unfocusedContainerColor = DarkSlateBg,
                                focusedBorderColor = AccentBlue.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )

                        // City Filter Chips
                        val cities = listOf("Todos", "São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cities.forEach { city ->
                                val isSelected = selectedCityFilter == city
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) AccentBlue else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedCityFilter = city }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = city,
                                        color = if (isSelected) Color.White else TextDim,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Region List
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredRegions.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhuma região encontrada.",
                                        color = TextDim,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                filteredRegions.forEach { region ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = region.name,
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = region.city,
                                                                color = TextDim,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Tamanho: ${region.sizeMB} MB • Atualizado: ${region.lastUpdated}",
                                                        color = TextDim,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                // Action button column
                                                Box(
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    if (region.isDownloading) {
                                                        IconButton(
                                                            onClick = { com.example.util.OfflineMapManager.removeRegion(region.id, context) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Cancelar Download",
                                                                tint = AccentRed,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    } else if (region.isDownloaded) {
                                                        IconButton(
                                                            onClick = { com.example.util.OfflineMapManager.removeRegion(region.id, context) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Remover",
                                                                tint = AccentRed,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    } else {
                                                        IconButton(
                                                            onClick = { com.example.util.OfflineMapManager.startDownload(region.id, context, coroutineScope) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Baixar",
                                                                tint = AccentBlue,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Download progress UI if downloading
                                            if (region.isDownloading) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Baixando mapa... ${String.format(Locale.getDefault(), "%.1f", region.progress * 100)}%",
                                                            color = AccentBlue,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = region.downloadSpeed,
                                                            color = TextDim,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    LinearProgressIndicator(
                                                        progress = { region.progress },
                                                        color = AccentBlue,
                                                        trackColor = Color.White.copy(alpha = 0.05f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Auto Cleanup Setting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-limpeza Inteligente",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Deletar mapas não utilizados após 7 dias para liberar espaço.",
                                    color = TextDim,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = autoDeleteEnabled,
                                onCheckedChange = { autoDeleteEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentGreen,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showOfflineMapsModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardSlateBg,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
        HolographicCockpit(
            viewModel = viewModel,
            currentTab = currentTab,
            onTabChange = { currentTab = it }
        ) {
            val innerPadding = PaddingValues(0.dp)
        
            val totalAnalyzed = historyLogs.size
            val totalAccepted = historyLogs.count { it.suggestion.lowercase() == "aceitar" }
            val totalRejected = historyLogs.count { it.suggestion.lowercase() == "recusar" }
            val totalEstimatedEarnings = historyLogs.filter { it.suggestion.lowercase() == "aceitar" }.sumOf { it.fareValue }
            val totalAcceptedKm = historyLogs.filter { it.suggestion.lowercase() == "aceitar" }.sumOf { it.totalDistance }
            
            val avgEarningsPerKm = if (totalAcceptedKm > 0.0) totalEstimatedEarnings / totalAcceptedKm else 0.0
            val acceptanceRate = if (totalAnalyzed > 0) (totalAccepted.toDouble() / totalAnalyzed.toDouble()) * 100.0 else 0.0

        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            // TAB 1: Simulator
            androidx.compose.animation.AnimatedVisibility(visible = currentTab == 1) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
            // 4. Offer simulation section
            Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Simulador de Ofertas de Corrida",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    var showPresetsPanel by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPresetsPanel = !showPresetsPanel }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Text("Treinamento da IA (Simulação)", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(if (showPresetsPanel) "Recolher" else "Expandir", color = AccentAmber, fontSize = 11.sp)
                    }

                    AnimatedVisibility(visible = showPresetsPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Escolha um cenário de treinamento simulado:", color = Color.Gray, fontSize = 12.sp)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (!isServiceActive) {
                                    Toast.makeText(context, "Ative o coordenador antes!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val serviceIntent = Intent(context, RadarCoordinatorService::class.java)
                                context.startService(serviceIntent)
                                val service = context.getSystemService(RadarCoordinatorService::class.java)
                                if (RadarCoordinatorService.isServiceRunning) {
                                    val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                        putExtra("SIMULATE", true)
                                        putExtra("APP_NAME", "iFood")
                                        putExtra("FARE_VALUE", 18.50)
                                        putExtra("PICKUP_ADDRESS", "Av. Paulista, 1500 - Shopping Cidade São Paulo")
                                        putExtra("DELIVERY_ADDRESS", "Av. Rebouças, 2500 - Pinheiros")
                                    }
                                    context.startService(triggerIntent)
                                    Toast.makeText(context, "Simulando Corrida Excelente...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .testTag("simulate_good_offer_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Corrida Excelente (Sugere Aceitar)", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("R$ 18,50 | 3,0 km | R$ 6.16/km", color = TextLight.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (!isServiceActive) {
                                    Toast.makeText(context, "Ative o coordenador antes!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                    putExtra("SIMULATE", true)
                                    putExtra("APP_NAME", "iFood")
                                    putExtra("FARE_VALUE", 5.00)
                                    putExtra("PICKUP_ADDRESS", "Pizzaria Local - Rua Augusta")
                                    putExtra("DELIVERY_ADDRESS", "Rua do Trânsito, 12 - Centro")
                                }
                                context.startService(triggerIntent)
                                Toast.makeText(context, "Simulando Corrida Ruim...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .testTag("simulate_bad_offer_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Corrida Ruim (Sugere Recusar)", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("R$ 5,00 | 4,5 km | R$ 1.11/km (Valor baixo)", color = TextLight.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (!isServiceActive) {
                                    Toast.makeText(context, "Ative o coordenador antes!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                    putExtra("SIMULATE", true)
                                    putExtra("APP_NAME", "iFood")
                                    putExtra("FARE_VALUE", 12.0)
                                    putExtra("PICKUP_ADDRESS", "Burguer Express - Av. Consolacao")
                                    putExtra("DELIVERY_ADDRESS", "Rodovia Sul, km 40 - Zona Rural")
                                }
                                context.startService(triggerIntent)
                                Toast.makeText(context, "Simulando Corrida Longe...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Desvio Muito Longo (Sugere Recusar)", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("R$ 12,00 | 10,0 km | R$ 1.20/km (Distância alta)", color = TextLight.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }
                    }
                    }

                    // Chained delivery (A+B) settings
                    HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Modo Entrega Ativa (Chained A+B)", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Simular ofertas enquanto está em outra entrega", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.isActiveDeliveryEnabled,
                            onCheckedChange = {
                                RadarCoordinator.saveSettings(context, settings.copy(isActiveDeliveryEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (settings.isActiveDeliveryEnabled) {
                        OutlinedTextField(
                            value = settings.activeDeliveryDestination,
                            onValueChange = {
                                RadarCoordinator.saveSettings(context, settings.copy(activeDeliveryDestination = it))
                            },
                            label = { Text("Destino da Entrega Ativa") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Expandable custom simulation panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomSimPanel = !showCustomSimPanel }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Text("Criar corrida personalizada...", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(if (showCustomSimPanel) "Recolher" else "Expandir", color = AccentBlue, fontSize = 11.sp)
                    }

                    AnimatedVisibility(visible = showCustomSimPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = simAppName,
                                onValueChange = { simAppName = it },
                                label = { Text("App de Corrida") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
modifier = Modifier.fillMaxWidth()
)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = simFareValue,
                                    onValueChange = { simFareValue = it },
                                    label = { Text("Valor (R$)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = simDistance,
                                    onValueChange = { simDistance = it },
                                    label = { Text("Distância (km)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = simPickup,
                                onValueChange = { simPickup = it },
                                label = { Text("Endereço Coleta") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
modifier = Modifier.fillMaxWidth()
)
                            OutlinedTextField(
                                value = simDelivery,
                                onValueChange = { simDelivery = it },
                                label = { Text("Endereço Entrega") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
modifier = Modifier.fillMaxWidth()
)
                            Button(
                                onClick = {
                                    if (!isServiceActive) {
                                        Toast.makeText(context, "Ative o coordenador antes!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val valDouble = simFareValue.toDoubleOrNull() ?: 10.0
                                    val distDouble = simDistance.toDoubleOrNull() ?: 4.0
                                    val timeDouble = distDouble * 3.0
                                    val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                        putExtra("SIMULATE", true)
                                        putExtra("APP_NAME", simAppName)
                                        putExtra("FARE_VALUE", valDouble)
                                        putExtra("PICKUP_ADDRESS", simPickup)
                                        putExtra("DELIVERY_ADDRESS", simDelivery)
                                        putExtra("DISTANCE_VALUE", distDouble)
                                        putExtra("TIME_VALUE", timeDouble)
                                    }
                                    context.startService(triggerIntent)
                                    Toast.makeText(context, "Injetando corrida personalizada...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(20.dp)) {
                                Text("Injetar Corrida Personalizada", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // MULTIPLEX NEURAL GRID - 4 TELAS DE VISUALIZAÇÃO DE APPS
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Multiplexadora Neural (4 Telas)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Instâncias Virtuais Ativas em 2º Plano",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                color = AccentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2x2 Grid using columns and rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tela 1: UBER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "VM_01: UBER_DRV", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "ROOT", color = Color.White, fontSize = 7.sp, modifier = Modifier.background(AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Directions, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        Text(text = "Aguardando...", color = Color.White, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "PING: 1.2ms", color = Color.Gray, fontSize = 8.sp)
                                        Text(text = "HOOK: ACTV", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Tela 2: IFOOD
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "VM_02: IFOOD_LOG", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "INJ", color = Color.White, fontSize = 7.sp, modifier = Modifier.background(AccentRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                        Text(text = "Interceptando...", color = Color.White, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "PING: 0.8ms", color = Color.Gray, fontSize = 8.sp)
                                        Text(text = "SYNC: 99%", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tela 3: GPS / WAZE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "VM_03: G_MAPS_SYS", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "LIVE", color = Color.White, fontSize = 7.sp, modifier = Modifier.background(AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                        Text(text = "Mapeando...", color = Color.White, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "SATS: 14 ON", color = Color.Gray, fontSize = 8.sp)
                                        Text(text = "TRAFFIC: BYP", color = AccentRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Tela 4: RAPPI
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "VM_04: RAPPI_LOG", color = AccentAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "STEALTH", color = Color.White, fontSize = 7.sp, modifier = Modifier.background(AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                        Text(text = "Scraping...", color = Color.White, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "PING: 1.5ms", color = Color.Gray, fontSize = 8.sp)
                                        Text(text = "SPOOF: ON", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Simulated live sniffer logs at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = ">> [GHOST_MATRIX] Interceptando APIs via Framebuffer local...\n>> [SSL_BYPASS] Injetando payloads em br.com.ifood...\n>> [TELEMETRY] Sincronização paralela em 4 threads estável.",
                            color = AccentGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            // 5. Last analyzed decision results with Framer-motion style high-fidelity animations
            AnimatedContent(
                targetState = activeOffer,
                transitionSpec = {
                    if (targetState != null && initialState == null) {
                        // Entry transition (slide up with bounciness and fade in)
                        (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)) { it / 2 } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutVertically(animationSpec = tween(150)) { -it / 2 } + fadeOut(animationSpec = tween(150)))
                    } else if (targetState == null && initialState != null) {
                        // Exit transition (fade out and slide down/away)
                        (slideInVertically(animationSpec = tween(150)) { -it / 2 } + fadeIn(animationSpec = tween(150)))
                            .togetherWith(slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it / 2 } + fadeOut(animationSpec = tween(200)))
                    } else {
                        // Transition between different active offers (filtering, switching)
                        (scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(300)))
                            .togetherWith(scaleOut(targetScale = 0.95f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                    }
                },
                label = "ActiveOfferTransition"
            ) { targetActiveOffer ->
                if (targetActiveOffer != null && lastDecision != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Última Análise da Inteligência",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            // Visual decision card
                            val decisionText = lastDecision?.lowercase() ?: "considerar"
                            val isChained = decisionText == "aceitar" && (
                                (lastReason ?: "").lowercase().contains("mesclar") || 
                                (lastReason ?: "").lowercase().contains("casar") || 
                                (lastReason ?: "").lowercase().contains("rota") ||
                                (lastReason ?: "").lowercase().contains("encadeada")
                            ) && (settings.isActiveDeliveryEnabled || deliveryActive)

                            val (decisionColor, decisionTitle, decisionDesc) = when {
                                isChained -> Triple(AccentBlue, "SUGESTÃO: MESCLAR APPS", "Alinhada com sua rota atual (Ganhos duplos)")
                                decisionText == "aceitar" -> Triple(AccentGreen, "SUGESTÃO: ACEITAR", "Corrida altamente rentável")
                                decisionText == "recusar" -> Triple(AccentRed, "SUGESTÃO: RECUSAR", "Corrida com baixo rendimento ou desvio longo")
                                else -> Triple(AccentAmber, "SUGESTÃO: CONSIDERAR", "Abaixo do ideal, decida com cuidado")
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(decisionColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .border(1.dp, decisionColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (decisionText == "aceitar") Icons.Default.CheckCircle else Icons.Default.Block,
                                            contentDescription = null,
                                            tint = decisionColor
                                        )
                                        Text(
                                            text = decisionTitle,
                                            color = decisionColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = lastReason ?: "",
                                        color = TextLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = decisionDesc,
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Info detail card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardSlateBg, RoundedCornerShape(20.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Dados extraídos da corrida:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("App:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(targetActiveOffer.appName, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Valor:", color = Color.LightGray, fontSize = 12.sp)
                                    Text("R$ ${String.format(Locale.US, "%.2f", targetActiveOffer.fareValue)}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Coleta:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(targetActiveOffer.pickupAddress, color = TextLight, fontSize = 12.sp, maxLines = 1)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Entrega:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(targetActiveOffer.deliveryAddress, color = TextLight, fontSize = 12.sp, maxLines = 1)
                                }

                                val distVal = targetActiveOffer.totalDistance
                                val timeVal = targetActiveOffer.totalTime
                                if (distVal > 0.0) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Distância:", color = Color.LightGray, fontSize = 12.sp)
                                        Text("${String.format(Locale.US, "%.1f", distVal)} km", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                if (timeVal > 0.0) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Tempo Estimado:", color = Color.LightGray, fontSize = 12.sp)
                                        Text("${String.format(Locale.US, "%.0f", timeVal)} min", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                val actualEff = if (distVal > 0.0) targetActiveOffer.fareValue / distVal else 0.0
                                if (actualEff > 0.0) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Eficiência Real:", color = Color.LightGray, fontSize = 12.sp)
                                        Text("${String.format(Locale.US, "%.2f", actualEff)} R$/km", color = if (actualEff >= settings.minValuePerKm) AccentGreen else AccentRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Telemetry and financial feasibility breakdown based on vehicle type and autonomy settings
                            val distValOffer = targetActiveOffer.totalDistance
                            if (distValOffer > 0.0 && settings.motorcycleConsumptionKmPerL > 0.0 && settings.fuelPrice > 0.0) {
                                val fuelCost = (distValOffer / settings.motorcycleConsumptionKmPerL) * settings.fuelPrice
                                val wearRate = when (settings.vehicleType) {
                                    "MOTO" -> 0.15
                                    "CARRO" -> 0.35
                                    "CARRO_GNV" -> 0.30
                                    "ELETRICO" -> 0.20
                                    else -> 0.15
                                }
                                val wearCost = distValOffer * wearRate // Wear and tear depreciation
                                val totalOperCost = fuelCost + wearCost
                                val netProfit = targetActiveOffer.fareValue - totalOperCost
                                val profitMargin = if (targetActiveOffer.fareValue > 0) (netProfit / targetActiveOffer.fareValue) * 100 else 0.0
                                val isViable = netProfit > 0 && (targetActiveOffer.fareValue / distValOffer) >= settings.minValuePerKm

                                val vehEmoji = when (settings.vehicleType) {
                                    "MOTO" -> "🛵 Moto"
                                    "CARRO" -> "🚗 Carro Flex"
                                    "CARRO_GNV" -> "🔥 Carro GNV"
                                    "ELETRICO" -> "⚡ Veículo Elétrico"
                                    else -> "🚗 Veículo"
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isViable) AccentGreen.copy(alpha = 0.3f) else AccentRed.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("📊", fontSize = 14.sp)
                                                Text(
                                                    text = "Viabilidade Financeira ($vehEmoji)",
                                                    color = TextLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isViable) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isViable) "VIÁVEL" else "NÃO VIÁVEL",
                                                    color = if (isViable) AccentGreen else AccentRed,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)

                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Gasto de Combustível/Energia:", color = Color.Gray, fontSize = 11.sp)
                                            Text("R$ ${String.format(Locale.US, "%.2f", fuelCost)}", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Desgaste & Manutenção (Est.):", color = Color.Gray, fontSize = 11.sp)
                                            Text("R$ ${String.format(Locale.US, "%.2f", wearCost)}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Custo Operacional Total:", color = Color.Gray, fontSize = 11.sp)
                                            Text("R$ ${String.format(Locale.US, "%.2f", totalOperCost)}", color = AccentRed.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.05f), thickness = 1.dp)

                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Lucro Líquido Real:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("R$ ${String.format(Locale.US, "%.2f", netProfit)}", color = if (netProfit > 0) AccentGreen else AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Margem Operacional Líquida:", color = Color.Gray, fontSize = 11.sp)
                                            Text("${String.format(Locale.US, "%.1f", profitMargin)}%", color = if (profitMargin > 30) AccentGreen else if (profitMargin > 0) AccentAmber else AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (settings.isActiveDeliveryEnabled) {
                                OptimizedRoutePanel(
                                    currentLocation = currentGPSLocation,
                                    activeDeliveryDestination = settings.activeDeliveryDestination,
                                    targetActiveOffer = targetActiveOffer,
                                    rainMultiplier = settings.rainModeMultiplier
                                )
                            }

                            // D3 Interactive Efficiency Chart Card
                            Text(
                                text = "Gráfico de Eficiência (Arraste para Simular)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            


                            // Geolocation speed-based safety lock calculation
                            val isSpeedLocked = currentSpeedKmh > settings.speedLimitKmh
                            val isInteractionBlocked = isSpeedLocked || settings.voiceOnlyMode

                            if (isInteractionBlocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AccentRed.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                        .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(AccentRed.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Aceite Bloqueado",
                                                tint = AccentRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (settings.voiceOnlyMode && !isSpeedLocked) "MODO APENAS VOZ" else "ACEITE BLOQUEADO",
                                                color = AccentRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (settings.voiceOnlyMode && !isSpeedLocked) {
                                                    "O Modo Apenas Voz está habilitado para sua segurança física. Use os comandos de voz do Jarvis para interagir."
                                                } else {
                                                    "Velocidade atual (${String.format(Locale.US, "%.1f", currentSpeedKmh)} km/h) excede o limite seguro de ${settings.speedLimitKmh.toInt()} km/h. Pare o veículo com segurança para interagir."
                                                },
                                                color = TextLight,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Voice commands widget inside the active offer card
                            val voicePulseTransition = rememberInfiniteTransition(label = "VoicePulse")
                            val pulseScale by voicePulseTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1100, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseScale"
                            )
                            val pulseAlpha by voicePulseTransition.animateFloat(
                                initialValue = 0.7f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1100, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isInteractionBlocked) CardSlateBg.copy(alpha = 0.3f)
                                        else if (isVoiceListening) AccentBlue.copy(alpha = 0.12f)
                                        else CardSlateBg.copy(alpha = 0.5f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isInteractionBlocked) Color.Gray.copy(alpha = 0.1f)
                                        else if (isVoiceListening) AccentBlue.copy(alpha = 0.4f)
                                        else Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        if (isVoiceListening) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .scale(pulseScale)
                                                    .scale(pulseScale)
                                                    .alpha(pulseAlpha)
                                                    .background(AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isSpeedLocked) Color.DarkGray.copy(alpha = 0.3f)
                                                    else if (isVoiceListening) AccentBlue.copy(alpha = 0.25f)
                                                    else Color.Gray.copy(alpha = 0.15f),
                                                    RoundedCornerShape(18.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSpeedLocked) Icons.Default.MicOff else if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicOff,
                                                contentDescription = "Microfone",
                                                tint = if (isSpeedLocked) Color.Gray else if (isVoiceListening) AccentBlue else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isSpeedLocked) "Comandos por voz bloqueados" else if (isVoiceListening) "Ouvindo comando..." else "Comandos de voz ativos",
                                            color = if (isSpeedLocked) Color.Gray else if (isVoiceListening) AccentBlue else TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = if (isSpeedLocked) {
                                                "Reduza para < ${settings.speedLimitKmh.toInt()} km/h para liberar"
                                            } else if (isVoiceListening) {
                                                if (voiceRecognizedText.isBlank() || voiceRecognizedText == "Ouvindo comando...") {
                                                    "Fale 'ACEITAR' ou 'PRÓXIMO'..."
                                                } else {
                                                    "Ouvido: \"$voiceRecognizedText\""
                                                }
                                            } else {
                                                "Diga 'Aceitar' ou 'Próximo' para decidir"
                                            },
                                            color = if (isSpeedLocked) Color.Gray else Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!isVoiceListening) {
                                        Button(
                                            onClick = {
                                                val startListeningIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                                    putExtra("START_VOICE_LISTENING_MANUAL", true)
                                                }
                                                context.startService(startListeningIntent)
                                            },
                                            enabled = !isSpeedLocked,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSpeedLocked) Color.DarkGray.copy(alpha = 0.1f) else AccentBlue.copy(alpha = 0.15f),
                                                contentColor = if (isSpeedLocked) Color.Gray else AccentBlue,
                                                disabledContainerColor = Color.DarkGray.copy(alpha = 0.1f),
                                                disabledContentColor = Color.Gray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("activate_voice_button")
                                        ) {
                                            Text("Falar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Manual actions Row with Green Accept and Red Dismiss buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                            putExtra("DISMISS_OFFER_MANUAL", true)
                                        }
                                        context.startService(triggerIntent)
                                    },
                                    enabled = !isInteractionBlocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentRed,
                                        disabledContainerColor = Color.DarkGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Recusar", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                            putExtra("ACCEPT_OFFER_MANUAL", true)
                                        }
                                        context.startService(triggerIntent)
                                    },
                                    enabled = !isInteractionBlocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        disabledContainerColor = Color.DarkGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Aceitar", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                            putExtra("TRIGGER_MAPS_MANUAL", true)
                                        }
                                        context.startService(triggerIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("Abrir Maps", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }


                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // TAB 2: Metrics
            androidx.compose.animation.AnimatedVisibility(visible = currentTab == 2) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val aiRecommendation = remember(totalAnalyzed, avgEarningsPerKm, acceptanceRate, settings, totalEstimatedEarnings, totalAcceptedKm) {
                        val fuelCost = if (settings.motorcycleConsumptionKmPerL > 0.0) {
                            totalAcceptedKm * (settings.fuelPrice / settings.motorcycleConsumptionKmPerL)
                        } else 0.0
                        val rawNet = totalEstimatedEarnings - fuelCost
                        val finalNet = rawNet - settings.fixedCosts
                        
                        val fixedCostsAdvice = if (settings.fixedCosts > 0.0) {
                            " Descontando R$ ${String.format(Locale.US, "%.2f", settings.fixedCosts)} de custos fixos diários e R$ ${String.format(Locale.US, "%.2f", fuelCost)} de gasolina, sua previsão de lucro líquido real agora é de R$ ${String.format(Locale.US, "%.2f", finalNet)}."
                        } else {
                            " Defina seus custos fixos nas configurações para que eu possa projetar seus ganhos líquidos reais exatos."
                        }

                        if (totalAnalyzed == 0) {
                            "Seu Copiloto IA está ativo e pronto! À medida que novas ofertas forem processadas, analisarei seus ganhos, eficiência de combustível/tempo e sugerirei os melhores ajustes operacionais em tempo real." + fixedCostsAdvice
                        } else {
                            val formattedKm = String.format(Locale.US, "%.2f", avgEarningsPerKm)
                            val baseRecommendation = when {
                                avgEarningsPerKm < settings.minValuePerKm -> {
                                    "Métrica crítica: Suas corridas aceitas estão com média de R$ ${formattedKm}/km, abaixo do seu filtro de R$ ${settings.minValuePerKm}/km. Sugiro elevar seu limite por km nas configurações para focar em ofertas de alta rentabilidade."
                                }
                                acceptanceRate < 15.0 -> {
                                    "Seletividade Ultra-Fina: Você aceitou apenas ${acceptanceRate.toInt()}% das ofertas. Se a movimento estiver baixo, tente se deslocar para um Polo de Elite (ex: Shopping Eldorado) ou reduzir ligeiramente o valor mínimo."
                                }
                                acceptanceRate > 60.0 -> {
                                    "Alerta de Desgaste: Sua taxa de aceitação está alta (${acceptanceRate.toInt()}%). Para evitar desgaste da moto e aumentar seu faturamento por hora, aumente seu filtro mínimo de R$/km para recusar viagens medianas."
                                }
                                else -> {
                                    "Excelente aproveitamento operacional! Média de R$ ${formattedKm}/km com ${acceptanceRate.toInt()}% de aceitação. Mantenha as configurações atuais e continue rodando nas regiões quentes indicadas abaixo."
                                }
                            }
                            baseRecommendation + fixedCostsAdvice
                        }
                    }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.6f), AccentGreen.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("ai_copilot_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Copiloto Analítico IA (Offline)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PROJETOR DE GANHOS",
                                color = AccentBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    // Session stat pillars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pillar 1: Total Estimated Session Earnings
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Sessão R$", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "R$ ${String.format(Locale.US, "%.2f", totalEstimatedEarnings)}",
                                    color = AccentGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Pillar 2: Efficiency (Acceptance %)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Taxa Aceite IA", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${acceptanceRate.toInt()}% (${totalAccepted}/${totalAnalyzed})",
                                    color = AccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Pillar 3: Average per Km
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Média R$/km", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "R$ ${String.format(Locale.US, "%.2f", avgEarningsPerKm)}",
                                    color = AccentAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Floating advice bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentBlue.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .border(1.dp, AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RECOMENDAÇÃO DO COPILOTO",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = aiRecommendation,
                                color = TextLight,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Button to speak recommendation using TTS service
                    Button(
                        onClick = {
                            val speakText = "Relatório do Copiloto IA. " + 
                                "Faturamento de hoje: ${totalEstimatedEarnings.toInt()} reais. " +
                                "Aproveitamento: ${acceptanceRate.toInt()} por cento. " +
                                "Média por quilômetro: ${String.format(Locale.US, "%.1f", avgEarningsPerKm)} reais. " +
                                aiRecommendation
                            val startSpeakIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                putExtra("SPEAK_TEXT_MANUAL", true)
                                putExtra("TEXT_TO_SPEAK", speakText)
                            }
                            context.startService(startSpeakIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("ai_copilot_speak_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Ouvir Relatório Falado por Voz",
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD DE RESUMO DE PRODUTIVIDADE GEMINI (IA)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF9D4EDD).copy(alpha = 0.6f), AccentBlue.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("gemini_productivity_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF9D4EDD),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Resumo de Produtividade Gemini (IA)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF9D4EDD).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GEMINI 3.5 FLASH",
                                color = Color(0xFFC77DFF),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    if (geminiSummary == null && !isGeneratingSummary) {
                        Text(
                            text = "O Jarvis está pronto para analisar seu histórico recente do radar e gerar um relatório analítico personalizado em Markdown sobre sua produtividade e os horários mais rentáveis.",
                            color = TextLight.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                isGeneratingSummary = true
                                coroutineScope.launch {
                                    try {
                                        val summary = GeminiManager.generateWeeklyProductivitySummary(historyLogs)
                                        geminiSummary = summary
                                    } catch (e: Exception) {
                                        geminiSummary = "⚠️ Falha ao gerar o resumo: ${e.localizedMessage}"
                                    } finally {
                                        isGeneratingSummary = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("generate_gemini_summary_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Gerar Relatório com Gemini",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (isGeneratingSummary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF9D4EDD),
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "O Jarvis está analisando suas corridas e gerando seu relatório semanal com o Gemini...",
                                color = TextLight,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Display generated Markdown
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            MarkdownText(text = geminiSummary ?: "")
                        }

                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(geminiSummary ?: ""))
                                    android.widget.Toast.makeText(context, "Relatório copiado para a área de transferência!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("copy_gemini_summary_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Copiar Relatório",
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    isGeneratingSummary = true
                                    coroutineScope.launch {
                                        try {
                                            val summary = GeminiManager.generateWeeklyProductivitySummary(historyLogs)
                                            geminiSummary = summary
                                        } catch (e: Exception) {
                                            geminiSummary = "⚠️ Falha ao gerar o resumo: ${e.localizedMessage}"
                                        } finally {
                                            isGeneratingSummary = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("regenerate_gemini_summary_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Recriar Relatório",
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD DE MANUTENÇÃO E TROCA DE ÓLEO
            var localMileageStr by remember(settings.motorcycleMileage) {
                mutableStateOf(if (settings.motorcycleMileage > 0.0) String.format(Locale.US, "%.0f", settings.motorcycleMileage) else "")
            }
            var localNextOilStr by remember(settings.nextOilChangeMileage) {
                mutableStateOf(if (settings.nextOilChangeMileage > 0.0) String.format(Locale.US, "%.0f", settings.nextOilChangeMileage) else "")
            }

            val remainingKm = settings.nextOilChangeMileage - settings.motorcycleMileage
            val isAlertActive = settings.nextOilChangeMileage > 0.0 && remainingKm <= 100.0

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = if (isAlertActive) {
                                listOf(AccentRed.copy(alpha = 0.8f), AccentAmber.copy(alpha = 0.8f))
                            } else {
                                listOf(AccentBlue.copy(alpha = 0.5f), AccentGreen.copy(alpha = 0.5f))
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("oil_change_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlertActive) Icons.Default.Warning else Icons.Default.Build,
                                contentDescription = null,
                                tint = if (isAlertActive) AccentRed else AccentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Manutenção & Troca de Óleo",
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Controle de quilometragem e alertas",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        if (settings.nextOilChangeMileage > 0.0) {
                            val badgeBg = if (isAlertActive) AccentRed.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.15f)
                            val badgeColor = if (isAlertActive) AccentRed else AccentGreen
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (remainingKm <= 0.0) "ATRASADO" else if (isAlertActive) "ALERTA" else "OK",
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    // Alertas Visuais se faltarem 100km
                    if (settings.nextOilChangeMileage > 0.0) {
                        if (remainingKm <= 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "🚨 TROCA DE ÓLEO ATRASADA! Você ultrapassou o limite em ${String.format(Locale.US, "%.0f", -remainingKm)} km. Troque o óleo imediatamente!",
                                        color = TextLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else if (remainingKm <= 100.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "⚠️ ATENÇÃO: Faltam apenas ${String.format(Locale.US, "%.0f", remainingKm)} km para a próxima troca de óleo!",
                                        color = TextLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentGreen.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .border(1.dp, AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Tudo certo! Faltam ${String.format(Locale.US, "%.0f", remainingKm)} km para a próxima troca de óleo.",
                                        color = TextLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Inputs de Quilometragem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = localMileageStr,
                            onValueChange = { newVal ->
                                localMileageStr = newVal.filter { it.isDigit() }
                                val dVal = localMileageStr.toDoubleOrNull() ?: 0.0
                                RadarCoordinator.saveSettings(context, settings.copy(motorcycleMileage = dVal))
                            },
                            label = { Text("KM Atual", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("motorcycle_mileage_input")
                        )

                        OutlinedTextField(
                            value = localNextOilStr,
                            onValueChange = { newVal ->
                                localNextOilStr = newVal.filter { it.isDigit() }
                                val dVal = localNextOilStr.toDoubleOrNull() ?: 0.0
                                RadarCoordinator.saveSettings(context, settings.copy(nextOilChangeMileage = dVal))
                            },
                            label = { Text("Prox. Troca (KM)", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = if (isAlertActive) AccentRed else AccentGreen,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("next_oil_change_input")
                        )
                    }

                    // Atalhos Rápidos para registrar quilometragem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val current = settings.motorcycleMileage
                                val updated = current + 50.0
                                localMileageStr = String.format(Locale.US, "%.0f", updated)
                                RadarCoordinator.saveSettings(context, settings.copy(motorcycleMileage = updated))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ) {
                            Text("+50 km", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val current = settings.motorcycleMileage
                                val updated = current + 100.0
                                localMileageStr = String.format(Locale.US, "%.0f", updated)
                                RadarCoordinator.saveSettings(context, settings.copy(motorcycleMileage = updated))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ) {
                            Text("+100 km", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val current = settings.motorcycleMileage
                                val nextChange = current + 1000.0
                                localNextOilStr = String.format(Locale.US, "%.0f", nextChange)
                                RadarCoordinator.saveSettings(context, settings.copy(nextOilChangeMileage = nextChange))
                                RadarCoordinator.addLog("Jarvis: Registro de troca de óleo efetuado para daqui a 1.000 km (Alvo: ${nextChange.toInt()} km).", com.example.coordinator.LogType.SUCCESS)
                                val speakIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                    putExtra("SPEAK_TEXT_MANUAL", true)
                                    putExtra("TEXT_TO_SPEAK", "Parabéns, Thiago! Troca de óleo registrada. Lembrete configurado para daqui a mil quilômetros!")
                                }
                                context.startService(speakIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(28.dp)
                                .testTag("oil_change_done_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(11.dp))
                                Text("Troquei Hoje", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1.7.5. Lucro Líquido & Meta do Dia
            val acceptedOffersForGoal = historyLogs.filter { it.suggestion.lowercase(java.util.Locale.getDefault()) == "aceitar" }
            val sessionTotalEarnings = acceptedOffersForGoal.sumOf { it.fareValue }
            val sessionTotalKm = acceptedOffersForGoal.sumOf { it.totalDistance }
            
            val fuelCost = if (settings.motorcycleConsumptionKmPerL > 0.0) {
                sessionTotalKm * (settings.fuelPrice / settings.motorcycleConsumptionKmPerL)
            } else 0.0
            
            val netProfit = sessionTotalEarnings - fuelCost
            val dailyGoal = settings.dailyGoalR
            val goalProgress = if (dailyGoal > 0.0) (sessionTotalEarnings / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.5f), AccentGreen.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Lucro Líquido & Combustível",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    // Fuel & Net Profit row with Fixed Costs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Faturamento", color = Color.Gray, fontSize = 10.sp)
                            Text("R$ ${String.format(java.util.Locale.US, "%.2f", sessionTotalEarnings)}", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gasolina", color = Color.Gray, fontSize = 10.sp)
                            Text("- R$ ${String.format(java.util.Locale.US, "%.2f", fuelCost)}", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Custo Fixo", color = Color.Gray, fontSize = 10.sp)
                            Text("- R$ ${String.format(java.util.Locale.US, "%.2f", settings.fixedCosts)}", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LÍQUIDO REAL", color = Color.Gray, fontSize = 10.sp)
                            val finalNetProfit = maxOf(-9999.0, netProfit - settings.fixedCosts)
                            val profitColor = if (finalNetProfit >= 0.0) AccentGreen else AccentRed
                            Text("R$ ${String.format(java.util.Locale.US, "%.2f", finalNetProfit)}", color = profitColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Daily Goal
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Meta do Dia", color = Color.Gray, fontSize = 12.sp)
                            Text("${(goalProgress * 100).toInt()}% de R$ ${String.format(java.util.Locale.US, "%.2f", dailyGoal)}", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (goalProgress >= 1f) AccentGreen else AccentBlue,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        if (goalProgress >= 1f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("🏆 Meta atingida! Jarvis sugere reduzir o ritmo e rodar mais leve.", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        } else if (dailyGoal > 0.0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Faltam R$ ${String.format(java.util.Locale.US, "%.2f", dailyGoal - sessionTotalEarnings)} para bater a meta.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1.8. Relatório Diário de Ganhos
            val dailyReport by viewModel.dailyReport.collectAsState()
            val reportLoading by viewModel.reportLoading.collectAsState()
            val reportError by viewModel.reportError.collectAsState()

            // Auto-fetch report when settings are available or periodically
            LaunchedEffect(settings) {
                viewModel.fetchDailyReport(settings.serverBaseUrl, settings.apiToken)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("daily_earnings_report_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Directions,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Relatório Diário de Ganhos",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.fetchDailyReport(settings.serverBaseUrl, settings.apiToken) },
                            modifier = Modifier.size(28.dp).testTag("refresh_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Atualizar Relatório",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                    // Visualização de ganhos e economias otimizados pela IA


                    Spacer(modifier = Modifier.height(8.dp))

                    if (reportLoading && dailyReport.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Calculando ganhos estimados no servidor...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else if (reportError != null && dailyReport.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Erro ao carregar relatório: ${reportError}",
                                color = AccentRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (dailyReport.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma oferta aceita registrada nos logs.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            dailyReport.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardSlateBg, RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.date,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "R$ ${item.estimatedEarnings}",
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Ofertas: ${item.totalOffersEvaluated} avaliadas",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "${item.totalOffersAccepted} aceitas",
                                                    color = AccentGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "•",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                                Text(
                                                    text = "${item.totalOffersRejected} recusadas",
                                                    color = AccentRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${item.totalDistanceKm} km percorridos",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Tempo: ${item.totalTimeMin} min",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Rend.: R$ ${item.earningsPerKm}/km",
                                            color = AccentAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Média: R$ ${item.averageFareValue}/corrida",
                                            color = AccentBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    item.appBreakdown?.let { breakdown ->
                                        if (breakdown.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                breakdown.forEach { (app, data) ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(CardSlateBg, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "$app: R$ ${data.estimatedEarnings} (${data.offersAccepted})",
                                                            color = Color.LightGray,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1.9. Sniper de Zonas Quentes (Crowdsourced Demand Radar)
            val hotZones by viewModel.hotZones.collectAsState()
            val hotZonesLoading by viewModel.hotZonesLoading.collectAsState()
            val hotZonesError by viewModel.hotZonesError.collectAsState()
            val currentGPSLocation by RadarCoordinator.currentLocation.collectAsState()

            // Fetch hot zones on load or when settings are available or periodically
            LaunchedEffect(settings, currentGPSLocation) {
                val lat = currentGPSLocation?.latitude ?: -23.5505
                val lon = currentGPSLocation?.longitude ?: -46.6333
                viewModel.fetchHotZones(settings.serverBaseUrl, settings.apiToken, lat, lon)
            }

            var selectedHotzoneTab by remember { mutableStateOf(1) }
            var selectedAppFilter by remember { mutableStateOf("Todos") }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("hot_zones_sniper_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Sniper de Zonas Quentes Multi-App",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                if (selectedHotzoneTab == 0) {
                                    val lat = currentGPSLocation?.latitude ?: -23.5505
                                    val lon = currentGPSLocation?.longitude ?: -46.6333
                                    viewModel.fetchHotZones(settings.serverBaseUrl, settings.apiToken, lat, lon)
                                } else {
                                    Toast.makeText(context, "Regiões locais de 2026 já atualizadas!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("refresh_hotzones_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Atualizar Radar",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Dual-tab navigation switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSlateBg, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedHotzoneTab == 0) AccentAmber.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedHotzoneTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tempo Real (Servidor)",
                                color = if (selectedHotzoneTab == 0) AccentAmber else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedHotzoneTab == 1) AccentAmber.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedHotzoneTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Regiões de Elite (2026)",
                                color = if (selectedHotzoneTab == 1) AccentAmber else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (selectedHotzoneTab == 0) {
                        Text(
                            text = "Zonas de alta demanda (iFood, Uber, 99, Keeta) calculadas por faturamento das últimas 2 horas de capturas dos entregadores.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        if (hotZonesLoading && hotZones.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Carregando coordenadas de alta demanda...",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (hotZonesError != null && hotZones.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Erro: ${hotZonesError}",
                                    color = AccentRed,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (hotZones.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma zona quente mapeada no momento.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                hotZones.forEach { zone ->
                                    val distText = if (currentGPSLocation != null) {
                                        val results = FloatArray(1)
                                        android.location.Location.distanceBetween(
                                            currentGPSLocation!!.latitude, currentGPSLocation!!.longitude,
                                            zone.latitude, zone.longitude,
                                            results
                                        )
                                        val km = results[0] / 1000f
                                        String.format("%.1f km", km)
                                    } else {
                                        "SP Centro"
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CardSlateBg, RoundedCornerShape(20.dp))
                                            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            when (zone.predominantApp.lowercase()) {
                                                                "ifood" -> Color(0xFFEA1D2C).copy(alpha = 0.15f)
                                                                "keeta" -> Color(0xFFFACC15).copy(alpha = 0.15f)
                                                                "uber", "uber flash" -> Color(0xFF000000).copy(alpha = 0.3f)
                                                                else -> AccentBlue.copy(alpha = 0.15f)
                                                            },
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = zone.predominantApp,
                                                        color = when (zone.predominantApp.lowercase()) {
                                                            "ifood" -> Color(0xFFEA1D2C)
                                                            "keeta" -> Color(0xFFEAB308)
                                                            "uber", "uber flash" -> Color.White
                                                            else -> AccentBlue
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Text(
                                                    text = zone.address,
                                                    color = TextLight,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = "${zone.offersCount} ord. analisadas",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Dist.: $distText",
                                                    color = AccentBlue,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(0.7f)
                                        ) {
                                            Text(
                                                text = "R$ ${zone.avgValuePerKm}/km",
                                                color = AccentGreen,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Média: R$ ${zone.avgFare}",
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Elite regions based on learned app mechanics in 2026
                        Text(
                            text = "Inteligência local baseada em padrões de faturamento e tempos de espera por app (iFood, Uber, 99). Teste o Radar para calibrar suas regras offline.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        // Filters row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Todos", "iFood", "Uber", "99").forEach { appName ->
                                val isSelected = selectedAppFilter == appName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AccentAmber else Color.Gray.copy(alpha = 0.15f))
                                        .clickable { selectedAppFilter = appName }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appName,
                                        color = if (isSelected) Color.Black else TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        val filteredEliteRegions = if (selectedAppFilter == "Todos") {
                            eliteRegionsList
                        } else {
                            eliteRegionsList.filter { it.predominantApp.equals(selectedAppFilter, ignoreCase = true) }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredEliteRegions.forEach { region ->
                                val distText = if (currentGPSLocation != null) {
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        currentGPSLocation!!.latitude, currentGPSLocation!!.longitude,
                                        region.latitude, region.longitude,
                                        results
                                    )
                                    val km = results[0] / 1000f
                                    String.format("%.1f km", km)
                                } else {
                                    "N/A"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardSlateBg, RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            when (region.predominantApp.lowercase()) {
                                                                "ifood" -> Color(0xFFEA1D2C).copy(alpha = 0.15f)
                                                                "uber" -> Color.White.copy(alpha = 0.15f)
                                                                else -> AccentAmber.copy(alpha = 0.15f)
                                                            },
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = region.predominantApp,
                                                        color = when (region.predominantApp.lowercase()) {
                                                            "ifood" -> Color(0xFFEA1D2C)
                                                            "uber" -> Color.White
                                                            else -> AccentAmber
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Text(
                                                    text = region.name,
                                                    color = TextLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (region.demandLevel) {
                                                            "Crítica" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                            "Extrema" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                            else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                        },
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = region.demandLevel,
                                                    color = when (region.demandLevel) {
                                                        "Crítica" -> Color(0xFFEF4444)
                                                        "Extrema" -> Color(0xFFF59E0B)
                                                        else -> Color(0xFF10B981)
                                                    },
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = "Espera: ~${region.waitTimeText}",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Dist.: $distText",
                                                    color = AccentBlue,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "R$ ${region.avgValuePerKm}/km",
                                                    color = AccentGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "Ticket: R$ ${region.avgFare}",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CardSlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = region.strategy,
                                                color = Color.LightGray,
                                                fontSize = 10.5.sp,
                                                lineHeight = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val mapIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("google.navigation:q=${region.latitude},${region.longitude}"))
                                                    try {
                                                        context.startActivity(mapIntent)
                                                    } catch (e: Exception) {
                                                        val fallbackUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${region.latitude},${region.longitude}")
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f).height(32.dp).testTag("route_elite_region_${region.predominantApp}")
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Directions,
                                                        contentDescription = null,
                                                        tint = AccentBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text("Rota", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (!isServiceActive) {
                                                        Toast.makeText(context, "Ative o coordenador antes!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                                            putExtra("SIMULATE", true)
                                                            putExtra("APP_NAME", region.predominantApp)
                                                            putExtra("FARE_VALUE", region.simFare)
                                                            putExtra("PICKUP_ADDRESS", region.simPickup)
                                                            putExtra("DELIVERY_ADDRESS", region.simDelivery)
                                                            putExtra("DISTANCE_VALUE", region.simDist)
                                                            putExtra("TIME_VALUE", region.simTime)
                                                        }
                                                        context.startService(triggerIntent)
                                                        Toast.makeText(context, "Simulando em ${region.name}...", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f).height(32.dp).testTag("simulate_elite_region_${region.predominantApp}")
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = AccentGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text("Testar", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card de Planejamento de Turnos e Horários Ideais (Jarvis IA)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.5f), AccentRed.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("shifts_planning_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Turnos & Horários Otimizados",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "IA JARVIS",
                                color = AccentRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Análise inteligente das suas corridas recentes para indicar os turnos mais lucrativos e planejar seu dia de trabalho.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Lógica de agrupamento por turnos
                    val acceptedOffers = historyLogs.filter { it.suggestion.lowercase(Locale.getDefault()) == "aceitar" }
                    
                    class ShiftStats(
                        val name: String, 
                        val hoursLabel: String, 
                        val color: Color, 
                        val defaultRate: Float, 
                        var totalEarnings: Float, 
                        var calculatedHours: Float, 
                        val icon: androidx.compose.ui.graphics.vector.ImageVector
                    )
                    
                    val shiftsList = listOf(
                        ShiftStats("Manhã", "06h - 12h", AccentBlue, 24.50f, 0f, 0f, Icons.Default.LightMode),
                        ShiftStats("Almoço", "12h - 15h", AccentGreen, 36.80f, 0f, 0f, Icons.Default.Speed),
                        ShiftStats("Tarde", "15h - 18h", AccentAmber, 28.20f, 0f, 0f, Icons.Default.AccessTime),
                        ShiftStats("Noite", "18h - 00h", AccentRed, 44.50f, 0f, 0f, Icons.Default.DarkMode)
                    )

                    val weekdayEarnings = FloatArray(7) { 0f }
                    val weekdayRidesCount = IntArray(7) { 0 }

                    acceptedOffers.forEach { offer ->
                        val date = java.util.Date(offer.timestamp)
                        val cal = java.util.Calendar.getInstance()
                        cal.time = date
                        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0-indexed (0=Dom, 1=Seg...)
                        val fare = offer.fareValue.toFloat()

                        if (dayOfWeek in 0..6) {
                            weekdayEarnings[dayOfWeek] += fare
                            weekdayRidesCount[dayOfWeek]++
                        }

                        val shiftIdx = when {
                            hour in 6..11 -> 0 // Manhã
                            hour in 12..14 -> 1 // Almoço
                            hour in 15..17 -> 2 // Tarde
                            else -> 3 // Noite
                        }
                        
                        shiftsList[shiftIdx].totalEarnings += fare
                        val rideTimeMin = if (offer.totalTime > 0) offer.totalTime else 22.0
                        shiftsList[shiftIdx].calculatedHours += (rideTimeMin / 60.0).toFloat()
                    }

                    val evaluatedShifts = shiftsList.map { shift ->
                        val rate = if (shift.calculatedHours > 0.2f) {
                            val realRate = shift.totalEarnings / shift.calculatedHours
                            val weight = (shift.calculatedHours / 10f).coerceAtMost(1.0f)
                            (realRate * weight) + (shift.defaultRate * (1f - weight))
                        } else {
                            shift.defaultRate
                        }
                        Pair(shift, rate)
                    }

                    val maxRate = evaluatedShifts.maxOf { it.second }
                    val bestShiftPair = evaluatedShifts.maxByOrNull { it.second } ?: Pair(shiftsList[3], 44.50f)

                    val weekdaysNames = listOf("Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado")
                    var bestDayIndex = 5 // Fallback: Sexta-feira
                    var maxDayEarnings = 0f
                    for (i in 0..6) {
                        if (weekdayEarnings[i] > maxDayEarnings) {
                            maxDayEarnings = weekdayEarnings[i]
                            bestDayIndex = i
                        }
                    }
                    val bestDayName = weekdaysNames[bestDayIndex]

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        evaluatedShifts.forEach { (shift, rate) ->
                            val pct = if (maxRate > 0) rate / maxRate else 0f
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = shift.icon,
                                            contentDescription = null,
                                            tint = shift.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = shift.name,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "(${shift.hoursLabel})",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "R$ ${String.format(Locale.US, "%.2f", rate)}/h",
                                        color = shift.color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = shift.color,
                                    trackColor = Color.White.copy(alpha = 0.04f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Sugestão do Assistente Jarvis",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Thiago, analisando seus faturamentos, o turno de maior rentabilidade média é o da ${bestShiftPair.first.name} (R$ ${String.format(Locale.US, "%.2f", bestShiftPair.second)}/h). O melhor dia histórico de entregas é $bestDayName. Concentre-se nessas janelas para otimizar seus lucros com menor esforço!",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. History logs
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextLight, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Histórico de Análise (${historyLogs.size})",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    if (historyLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Limpar Histórico",
                                tint = AccentRed
                            )
                        }
                    }
                }

                if (historyLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSlateBg, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Nenhuma corrida analisada ainda", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        historyLogs.take(15).forEach { offer ->
                            Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg), shape = RoundedCornerShape(20.dp)) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${offer.appName} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(offer.timestamp))}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        val (color, text) = when (offer.suggestion.lowercase()) {
                                            "aceitar" -> Pair(AccentGreen, "ACEITAR")
                                            "recusar" -> Pair(AccentRed, "RECUSAR")
                                            else -> Pair(AccentAmber, "CONSIDERAR")
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = text,
                                                color = color,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "R$ ${String.format(Locale.US, "%.2f", offer.fareValue)}",
                                                color = AccentGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", offer.totalDistance)} km • ${offer.totalTime.toInt()} min",
                                                color = TextLight,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Moto: ${if (offer.speedKmhAtDecision < 3.0f) "PARADA" else "${offer.speedKmhAtDecision.toInt()} km/h"}",
                                                color = if (offer.speedKmhAtDecision < 3.0f) AccentGreen else AccentRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (offer.isChained) {
                                                Text(
                                                    text = "Modo Encadeado A+B",
                                                    color = AccentBlue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    Text(
                                        text = offer.reason,
                                        color = TextLight.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // TAB 3: Settings
            androidx.compose.animation.AnimatedVisibility(visible = currentTab == 3) {
                var showAsaasPaymentModal by remember { mutableStateOf(false) }

                if (showAsaasPaymentModal) {
                    com.example.ui.components.AsaasPaymentModal(
                        onDismiss = { showAsaasPaymentModal = false },
                        onPaymentSuccess = {
                            showAsaasPaymentModal = false
                            RadarCoordinator.voiceManager?.speak("Licença do Jarvis Pro renovada com sucesso por 30 dias.")
                        }
                    )
                }

                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
            // BANNER DE LICENÇA (Foco em Monetização do APK)
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (showSafetyTip) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3F3700)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Dica", tint = Color(0xFFFFD54F), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dica do Jarvis: Evite Multas", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Recomendamos o uso de intercomunicadores de capacete em vez de fones de ouvido (CTB Art. 252, VI). Mantenha-se seguro e evite infrações enquanto usa o Radar.", color = TextLight, fontSize = 12.sp, lineHeight = 16.sp)
                                }
                                IconButton(onClick = { showSafetyTip = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextLight, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "STATUS DA LICENÇA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentBlue,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "PREMIUM ATIVO (JARVIS PRO)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = { 
                                RadarCoordinator.voiceManager?.speak("Plataforma de renovação de licença acessada.")
                                showAsaasPaymentModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("RENOVAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { 0.8f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = AccentBlue,
                        trackColor = AccentBlue.copy(alpha = 0.1f)
                    )
                    Text(
                        "Expira em 24 dias (12/12/2026)",
                        fontSize = 9.sp,
                        color = TextDim,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // CONTROLE DE AUTONOMIA JARVIS (Foco em APK Autônomo)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentAmber)
                        Column {
                            Text("AUTONOMIA CRÍTICA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Jarvis decide e aceita por você", fontSize = 11.sp, color = TextDim)
                        }
                    }
                    
                    var isJarvisAutonomous by remember { mutableStateOf(settings.isAutoAcceptEnabled) }
                    Switch(
                        checked = isJarvisAutonomous,
                        onCheckedChange = { 
                            isJarvisAutonomous = it
                            RadarCoordinator.saveSettings(context, settings.copy(isAutoAcceptEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentAmber,
                            checkedTrackColor = AccentAmber.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = CardSlateBg
                        )
                    )
                }
            }

            // JARVIS COGNITIVE NEURAL CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF9333EA).copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "MÓDULOS COGNITIVOS IA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Otimizações proativas em tempo real",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF9333EA).copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Toggle 1: aiActiveTrafficReroute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Desvio de Tráfego", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Evita trânsito automaticamente", fontSize = 11.sp, color = TextDim)
                            }
                        }
                        Switch(
                            checked = settings.aiActiveTrafficReroute,
                            onCheckedChange = { 
                                RadarCoordinator.saveSettings(context, settings.copy(aiActiveTrafficReroute = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = CardSlateBg
                            )
                        )
                    }

                    // Toggle 1B: showTrafficDensity (Overlay de Densidade de Tráfego GPS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Densidade de Tráfego em Tempo Real", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Overlay com heatmap e legenda no mapa", fontSize = 11.sp, color = TextDim)
                            }
                        }
                        Switch(
                            checked = settings.showTrafficDensity || settings.showTrafficOverlay,
                            onCheckedChange = { isChecked ->
                                RadarCoordinator.saveSettings(context, settings.copy(showTrafficDensity = isChecked, showTrafficOverlay = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentRed,
                                checkedTrackColor = AccentRed.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = CardSlateBg
                            )
                        )
                    }

                    // Toggle 2: aiActiveFuelSuggest
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Sugestão de Postos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Abastecimento com descontos", fontSize = 11.sp, color = TextDim)
                            }
                        }
                        Switch(
                            checked = settings.aiActiveFuelSuggest,
                            onCheckedChange = { 
                                RadarCoordinator.saveSettings(context, settings.copy(aiActiveFuelSuggest = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentGreen,
                                checkedTrackColor = AccentGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = CardSlateBg
                            )
                        )
                    }

                    // Toggle 3: aiActiveFatigueDetect
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Detector de Fadiga", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Alertas a cada 3h dirigindo", fontSize = 11.sp, color = TextDim)
                            }
                        }
                        Switch(
                            checked = settings.aiActiveFatigueDetect,
                            onCheckedChange = { 
                                RadarCoordinator.saveSettings(context, settings.copy(aiActiveFatigueDetect = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentAmber,
                                checkedTrackColor = AccentAmber.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = CardSlateBg
                            )
                        )
                    }
                }
            }

            GoogleMapsNavigationCard(
                currentLocation = currentGPSLocation,
                currentSpeedKmh = currentSpeedKmh,
                rainMultiplier = settings.rainModeMultiplier
            )

            MultiAppRouteOptimizerCard(
                currentLocation = currentGPSLocation,
                onSetDestination = { address ->
                    com.example.util.MultiAppOrderManager.setNavigationAddress(address)
                }
            )

            // HERO: Jarvis Voice Assistant
            val jarvisStateVal by viewModel.jarvisState.collectAsState()
            val jarvisTransVal by viewModel.jarvisTranscription.collectAsState()
            val jarvisRespVal by viewModel.jarvisResponse.collectAsState()
            val jarvisCatVal by viewModel.jarvisCategory.collectAsState()
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (jarvisStateVal != "IDLE") 3.dp else 1.dp,
                        color = when (jarvisStateVal) {
                            "IDLE" -> AccentBlue.copy(alpha = 0.3f)
                            "GREETING" -> AccentBlue
                            "LISTENING" -> AccentGreen
                            "ANALYZING" -> AccentAmber
                            "SPEAKING_RESPONSE" -> AccentBlue
                            else -> AccentGreen
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Central Jarvis HUD Animation
                    Box(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val dName = FirebaseAuthManager.getCurrentUserEmail().substringBefore("@").takeIf { it.isNotBlank() } ?: "PILOTO"
                        JarvisVoiceHUD(
                            modifier = Modifier.fillMaxSize(), 
                            isActive = jarvisStateVal != "IDLE" || currentRadarState == RadarState.ANALISANDO || currentRadarState == RadarState.SUGERINDO,
                            driverName = dName
                        )
                    }

                    // Status and Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JARVIS IA COPILOTO",
                                color = AccentBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Monitoramento e decisões em tempo real",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        
                        if (jarvisStateVal != "IDLE") {
                            Surface(
                                color = AccentGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = jarvisStateVal,
                                    color = AccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // CONSOLE DE PENSAMENTOS JARVIS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            Text("FLUXO DE PENSAMENTO IA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AccentBlue)
                        }
                        
                        val jarvisLogs = logs.filter { it.message.contains("Jarvis", ignoreCase = true) }.takeLast(5).reversed()
                        if (jarvisLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Aguardando análise de ofertas...", color = Color.Gray.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(jarvisLogs) { log ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(">", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(log.message, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    if (jarvisStateVal != "IDLE") {
                            IconButton(
                                onClick = { viewModel.resetJarvis() },
                                modifier = Modifier.size(36.dp).background(AccentRed.copy(alpha=0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar",
                                    tint = AccentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Dynamic text/state description
                    when (jarvisStateVal) {
                        "IDLE" -> {
                            Text(
                                text = "Jarvis em prontidão. Todos os sistemas monitorados.",
                                color = TextLight.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        "GREETING" -> {
                            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Text("Jarvis está cumprimentando...", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        "LISTENING" -> {
                            val isVoiceActive by (RadarCoordinator.voiceInputManager?.isListening ?: MutableStateFlow(false)).collectAsState()
                            Text(
                                text = if (isVoiceActive) "Fale agora! Jarvis está ouvindo..." else "Ativando escuta...",
                                color = AccentGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        "ANALYZING" -> {
                            CircularProgressIndicator(color = AccentAmber, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Text("Processando o que você disse...", color = AccentAmber, fontSize = 14.sp)
                            if (jarvisTransVal.isNotEmpty()) {
                                Text(
                                    text = "\"$jarvisTransVal\"",
                                    color = TextLight.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        "SPEAKING_RESPONSE", "FINISHED" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val catColor = when (jarvisCatVal) {
                                        "BUG" -> AccentRed
                                        "RULE_UPDATE" -> AccentAmber
                                        "CONVERSATION" -> AccentBlue
                                        else -> AccentGreen
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(catColor.copy(alpha = 0.2f))
                                            .border(1.dp, catColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (jarvisCatVal) {
                                                "BUG" -> "ERRO CRÍTICO"
                                                "RULE_UPDATE" -> "REGRA DO MOTOCICLISTA"
                                                "CONVERSATION" -> "BATE-PAPO"
                                                else -> "SUGESTÃO"
                                            },
                                            color = catColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(if (jarvisCatVal == "CONVERSATION") "Mensagem entregue" else "Feedback interpretado com sucesso", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Você disse: \"$jarvisTransVal\"",
                                    color = TextLight.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )

                                Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Resposta do Jarvis:", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(jarvisRespVal, color = TextLight, fontSize = 13.sp, lineHeight = 18.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.resetJarvis() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("Entendido", color = TextLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

// Jarvis Memories Panel
            val jarvisMemories by RadarCoordinator.jarvisMemories.collectAsState()
            if (jarvisMemories.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regras Aprendidas pelo Jarvis", color = AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        jarvisMemories.take(3).forEach { memory ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp).background(AccentAmber, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(memory, color = TextLight.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                        if (jarvisMemories.size > 3) {
                            Text("+ ${jarvisMemories.size - 3} outras regras ativas", color = Color.Gray, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }


// Global Real-Time Safety Lock Indicator
            val isStopped = speedState == SpeedState.PARADO
            val safetyBg by animateColorAsState(
                targetValue = if (isStopped) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f),
                animationSpec = tween(400)
            )
            val safetyBorderColor by animateColorAsState(
                targetValue = if (isStopped) AccentGreen else AccentRed,
                animationSpec = tween(400)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = safetyBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, safetyBorderColor, RoundedCornerShape(20.dp))
                    .testTag("global_safety_indicator")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(safetyBorderColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, safetyBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isStopped) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = safetyBorderColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isStopped) "STATUS: ACEITE AUTOMÁTICO ATIVO" else "STATUS: ACEITE AUTOMÁTICO BLOQUEADO",
                            color = safetyBorderColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isStopped) 
                                "Moto parou ou velocidade está abaixo do limite seguro. O sistema aceitará boas ofertas automaticamente." 
                            else 
                                "Moto em movimento! Ações automáticas suspensas por segurança para evitar distração no trânsito.",
                            color = TextLight.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            // 1.4.a. 1-Tap Subscriber Express Activation Panel (Piloto Automático Express)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, AccentAmber)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(AccentAmber.copy(alpha = 0.2f), CircleShape)
                                    .border(1.5.dp, AccentAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = "Central de Ativação Express (1-Tap)",
                                    color = TextLight,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (settings.isGhostSequenceEnabled && settings.chainDeliveriesMode && settings.voiceOnlyMode)
                                        "🚀 100% Operacional & Otimizado"
                                    else
                                        "⚠️ Clique abaixo para ligar todas as IAs de uma vez",
                                    color = if (settings.isGhostSequenceEnabled && settings.chainDeliveriesMode && settings.voiceOnlyMode) AccentGreen else AccentAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Feature Checklist Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = if (settings.isGhostSequenceEnabled) AccentGreen.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (settings.isGhostSequenceEnabled) AccentGreen else Color.Gray)
                        ) {
                            Text(
                                text = if (settings.isGhostSequenceEnabled) "👻 Ghost IA: ON" else "👻 Ghost IA: OFF",
                                color = if (settings.isGhostSequenceEnabled) AccentGreen else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = if (settings.chainDeliveriesMode) AccentGreen.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (settings.chainDeliveriesMode) AccentGreen else Color.Gray)
                        ) {
                            Text(
                                text = if (settings.chainDeliveriesMode) "🔗 Encadeadas: ON" else "🔗 Encadeadas: OFF",
                                color = if (settings.chainDeliveriesMode) AccentGreen else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = if (settings.voiceOnlyMode) AccentGreen.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (settings.voiceOnlyMode) AccentGreen else Color.Gray)
                        ) {
                            Text(
                                text = if (settings.voiceOnlyMode) "🎙️ Jarvis Voz: ON" else "🎙️ Jarvis Voz: OFF",
                                color = if (settings.voiceOnlyMode) AccentGreen else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            RadarCoordinator.activateAllSubscriberProFeatures(context)
                            Toast.makeText(context, "🚀 TODAS AS FUNÇÕES PRO ATIVADAS COM SUCESSO!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_1tap_express_activate")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ATIVAR TUDO EM 1-TAP (PILOTO AUTOMÁTICO)",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 1.4.b. Autonomous Admin Self-Healing Engine Panel
            val autoFixCount by com.example.coordinator.RadarCoordinator.autoFixCountToday.collectAsStateWithLifecycle()
            val lastAutoFixAction by com.example.coordinator.RadarCoordinator.lastAutoFixAction.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AccentGreen.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, AccentGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "Administrador Autônomo AI",
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Autocorreção de erros ativa sem programar",
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$autoFixCount Correções Hoje",
                                color = AccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ÚLTIMA AÇÃO AUTÔNOMA:",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastAutoFixAction,
                                color = TextLight.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                com.example.coordinator.RadarCoordinator.triggerManualSelfHealing()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Black
                                )
                                Text(
                                    text = "Diagnóstico 1-Tap",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 1.5. Accessibility Service Panel (Leitura Automática)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Leitura de Tela Automática",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Leitura em tempo real ativa" else "Inativa. Toque para configurar",
                                color = if (isAccessibilityEnabled) AccentGreen else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Não foi possível abrir as configurações", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) AccentBlue.copy(alpha = 0.2f) else AccentGreen
                            ),
                            border = if (isAccessibilityEnabled) androidx.compose.foundation.BorderStroke(1.dp, AccentBlue) else null,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isAccessibilityEnabled) AccentBlue else Color.White
                                )
                                Text(
                                    text = if (isAccessibilityEnabled) "Configurado" else "Ativar",
                                    color = if (isAccessibilityEnabled) AccentBlue else Color.White
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    
                    Text(
                        text = "🔒 Segurança & Privacidade:\nEste serviço funciona de forma local e lê SOMENTE os dados dos aplicativos de entrega suportados (iFood, Uber, 99, Rappi, Lalamove). Suas informações pessoais de outros aplicativos nunca são acessadas ou coletadas.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // 1.5.b. Notification Listener Panel (Leitor do WhatsApp)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acesso a Notificações (WhatsApp)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isNotificationListenerEnabled) "Leitor de mensagens ativo" else "Inativa. Toque para configurar",
                                color = if (isNotificationListenerEnabled) AccentGreen else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Não foi possível abrir as configurações", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNotificationListenerEnabled) AccentBlue.copy(alpha = 0.2f) else AccentGreen
                            ),
                            border = if (isNotificationListenerEnabled) androidx.compose.foundation.BorderStroke(1.dp, AccentBlue) else null,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNotificationListenerEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isNotificationListenerEnabled) AccentBlue else Color.White
                                )
                                Text(
                                    text = if (isNotificationListenerEnabled) "Configurado" else "Ativar",
                                    color = if (isNotificationListenerEnabled) AccentBlue else Color.White
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    
                    Text(
                        text = "🎧 Jarvis WhatsApp:\nEste recurso lê notificações recebidas do WhatsApp para mantê-lo focado no trânsito. Permite também que você envie respostas por voz ou texto através do painel de controle.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // 🚨 CONFIGURAÇÃO DE EMERGÊNCIA (S.O.S)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("sos_config_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Configurações de Emergência (S.O.S)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Cadastre os números de telefone de seus contatos de confiança e personalize a mensagem de pânico que será transmitida automaticamente ao ativar o botão SOS.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    var localEmergencyContacts by remember { mutableStateOf(settings.emergencyContacts) }
                    var localEmergencyMessage by remember { mutableStateOf(settings.emergencyMessage) }

                    OutlinedTextField(
                        value = localEmergencyContacts,
                        onValueChange = { localEmergencyContacts = it },
                        label = { Text("Contatos de Emergência (ex: 190, +5511999999999)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = TextLight.copy(alpha = 0.3f),
                            focusedLabelColor = AccentRed,
                            unfocusedLabelColor = TextDim
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sos_config_contacts_input")
                    )

                    OutlinedTextField(
                        value = localEmergencyMessage,
                        onValueChange = { localEmergencyMessage = it },
                        label = { Text("Mensagem de Emergência") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = TextLight.copy(alpha = 0.3f),
                            focusedLabelColor = AccentRed,
                            unfocusedLabelColor = TextDim
                        ),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("sos_config_message_input")
                    )

                    Text(
                        text = "Dica: Use as tags {lat} e {lon} na mensagem para anexar automaticamente sua localização GPS em tempo real.",
                        color = AccentAmber,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )

                    Button(
                        onClick = {
                            val updated = settings.copy(
                                emergencyContacts = localEmergencyContacts,
                                emergencyMessage = localEmergencyMessage
                            )
                            RadarCoordinator.saveSettings(context, updated)
                            Toast.makeText(context, "Configurações SOS salvas com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("sos_config_save_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Salvar Configurações SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Navigation App Selection
            Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aplicativo de Navegação Padrão",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Escolha qual aplicativo usar para iniciar a rota automaticamente após aceitar uma corrida VIP ou iniciar uma entrega.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    var localNavApp by remember { mutableStateOf(settings.defaultNavigationApp) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                localNavApp = "waze"
                                RadarCoordinator.saveSettings(context, settings.copy(defaultNavigationApp = "waze"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (localNavApp == "waze") AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (localNavApp == "waze") AccentBlue else TextDim
                            ),
                            border = BorderStroke(1.dp, if (localNavApp == "waze") AccentBlue else TextDim),
                            modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("btn_select_waze")
                        ) {
                            Text("Waze", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                localNavApp = "google_maps"
                                RadarCoordinator.saveSettings(context, settings.copy(defaultNavigationApp = "google_maps"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (localNavApp == "google_maps") AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (localNavApp == "google_maps") AccentBlue else TextDim
                            ),
                            border = BorderStroke(1.dp, if (localNavApp == "google_maps") AccentBlue else TextDim),
                            modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("btn_select_maps")
                        ) {
                            Text("Google Maps", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Monitored Platforms Card (Plataformas Monitoradas)
            Card(colors = CardDefaults.cardColors(containerColor = CardSlateBg), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plataformas Ativas para Monitoramento",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRONTO",
                                color = AccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "O Radar analisa exclusivamente as telas dos seguintes aplicativos parceiros para extrair endereços e calcular a rentabilidade de forma segura:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Platform 1: iFood
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(CardSlateBg, RoundedCornerShape(20.dp))
                                .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Text(
                                        text = "iFood",
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "Rastreando Ativo",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Endereço, Coleta e R$/km",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Platform 2: Uber / UberEats
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(CardSlateBg, RoundedCornerShape(20.dp))
                                .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AccentBlue)
                                    )
                                    Text(
                                        text = "Uber / 99",
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "Rastreando Ativo",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Rotas, Tarifas e KM",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }


            }

            // Geofence / Cerca Virtual Advanced Config
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { showGeofenceModal = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Cerca Virtual Avançada", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Desenhe raios e gerencie alertas por voz", fontSize = 12.sp, color = TextDim)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextDim)
                }
            }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }





            // Overlays
            val isOverlayActive = showProfilePanel || showConfigPanel || showGeofenceModal
            if (isOverlayActive) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { showProfilePanel = false; showConfigPanel = false; showGeofenceModal = false }
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {

            // User Profile Panel
            AnimatedVisibility(visible = showProfilePanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).verticalScroll(rememberScrollState())
                        .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                                Text("Perfil do Entregador", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            IconButton(onClick = { showProfilePanel = false }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Salvar e Fechar", tint = AccentBlue)
                            }
                        }

                        val currentUserState = FirebaseAuthManager.currentUser.collectAsState().value

                        if (currentUserState == null) {
                            // Not authenticated prompt
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Acesso Limitado",
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Faça login ou crie uma conta na seção de configurações para desbloquear e gerenciar seu perfil com persistência em nuvem.",
                                    color = TextLight.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Button(
                                    onClick = {
                                        showConfigPanel = true
                                        showProfilePanel = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Text("Ir para Configurações", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Profile Editor
                            var fullName by remember(userProfile) { mutableStateOf(userProfile.fullName) }
                            var phoneNumber by remember(userProfile) { mutableStateOf(userProfile.phoneNumber) }
                            var vehiclePlate by remember(userProfile) { mutableStateOf(userProfile.vehiclePlate) }
                            
                            var notifyOnAutoReject by remember(userProfile) { mutableStateOf(userProfile.notifyOnAutoReject) }
                            var audioAlertEnabled by remember(userProfile) { mutableStateOf(userProfile.audioAlertEnabled) }
                            var voiceCommandsEnabled by remember(userProfile) { mutableStateOf(userProfile.voiceCommandsEnabled) }
                            var vibrateOnNewOffer by remember(userProfile) { mutableStateOf(userProfile.vibrateOnNewOffer) }

                            var emergencyContactName by remember(userProfile) { mutableStateOf(userProfile.emergencyContactName) }
                            var emergencyContactPhone by remember(userProfile) { mutableStateOf(userProfile.emergencyContactPhone) }

                            var isSaving by remember { mutableStateOf(false) }
                            var profileError by remember { mutableStateOf<String?>(null) }
                            var profileSuccess by remember { mutableStateOf<String?>(null) }

                            DisposableEffect(showProfilePanel, currentUserState) {
                                var registration: com.google.firebase.firestore.ListenerRegistration? = null
                                if (showProfilePanel) {
                                    registration = com.example.data.FirestoreManager.listenToErrorLogs { logs ->
                                        firebaseErrorLogs = logs
                                    }
                                }
                                onDispose {
                                    registration?.remove()
                                }
                            }

                            // Security settings state
                            var showChangePassword by remember { mutableStateOf(false) }
                            var newPassword by remember { mutableStateOf("") }
                            var passwordSuccessMsg by remember { mutableStateOf<String?>(null) }
                            var passwordErrorMsg by remember { mutableStateOf<String?>(null) }

                            Text(
                                text = "Informações Básicas",
                                color = AccentBlue.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Nome Completo") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("profile_fullname_input")
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Telefone / WhatsApp") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                            )

                            OutlinedTextField(
                                value = vehiclePlate,
                                onValueChange = { vehiclePlate = it },
                                label = { Text("Placa do Veículo") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("profile_plate_input")
                            )

                            HorizontalDivider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

                            Text(
                                text = "Contato de Emergência (SOS)",
                                color = AccentRed.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = emergencyContactName,
                                onValueChange = { emergencyContactName = it },
                                label = { Text("Nome do Contato") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentRed,
                                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("profile_emergency_name_input")
                            )

                            OutlinedTextField(
                                value = emergencyContactPhone,
                                onValueChange = { emergencyContactPhone = it },
                                label = { Text("Telefone / WhatsApp do Contato") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = AccentRed,
                                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("profile_emergency_phone_input")
                            )

                            HorizontalDivider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

                            Text(
                                text = "Preferências de Notificação & Alertas",
                                color = AccentBlue.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            // Switches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Alertas Sonoros", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Emitir sons de aviso em novas rotas", color = TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = audioAlertEnabled,
                                    onCheckedChange = { audioAlertEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentBlue,
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("switch_audio_alert")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Comandos de Voz", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Habilitar microfone para responder por voz", color = TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = voiceCommandsEnabled,
                                    onCheckedChange = { voiceCommandsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentBlue,
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("switch_voice_commands")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Vibrar ao receber oferta", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Vibrar o aparelho para chamar atenção", color = TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = vibrateOnNewOffer,
                                    onCheckedChange = { vibrateOnNewOffer = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentBlue,
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("switch_vibrate_offer")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Aviso de Rejeição Automática", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Notificar por voz e texto ao recusar corrida ruim", color = TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = notifyOnAutoReject,
                                    onCheckedChange = { notifyOnAutoReject = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AccentBlue,
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("switch_notify_reject")
                                )
                            }

                            HorizontalDivider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

                            Text(
                                text = "Configurações de Segurança",
                                color = AccentBlue.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Conta Ativa", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(currentUserState.email ?: "", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showChangePassword = !showChangePassword },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("btn_toggle_change_password")
                                ) {
                                    Text("Alterar Senha", fontSize = 11.sp)
                                }
                            }

                            if (showChangePassword) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { 
                                            newPassword = it
                                            passwordErrorMsg = null
                                            passwordSuccessMsg = null
                                        },
                                        label = { Text("Nova Senha") },
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextLight,
                                            unfocusedTextColor = TextLight,
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("profile_new_password_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (newPassword.length < 6) {
                                                passwordErrorMsg = "A senha deve conter pelo menos 6 caracteres."
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                try {
                                                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                                    if (firebaseUser != null) {
                                                        firebaseUser.updatePassword(newPassword).awaitTask()
                                                        passwordSuccessMsg = "Senha alterada com sucesso!"
                                                        newPassword = ""
                                                    } else {
                                                        passwordErrorMsg = "Usuário não autenticado."
                                                    }
                                                } catch (e: Exception) {
                                                    passwordErrorMsg = "Erro: ${e.message}"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_save_new_password")
                                    ) {
                                        Text("Confirmar Nova Senha", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (passwordErrorMsg != null) {
                                        Text(passwordErrorMsg ?: "", color = AccentRed, fontSize = 11.sp)
                                    }
                                    if (passwordSuccessMsg != null) {
                                        Text(passwordSuccessMsg ?: "", color = AccentGreen, fontSize = 11.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

                            // Save status/error messages
                            if (profileError != null) {
                                Text(profileError ?: "", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            if (profileSuccess != null) {
                                Text(profileSuccess ?: "", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.loadUserProfile()
                                            profileSuccess = "Perfil carregado da nuvem!"
                                            profileError = null
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                                    border = BorderStroke(1.dp, AccentBlue),
                                    modifier = Modifier.weight(1f).testTag("profile_sync_button")
                                ) {
                                    Text("Sincronizar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        isSaving = true
                                        profileError = null
                                        profileSuccess = null
                                        coroutineScope.launch {
                                            try {
                                                val updatedProfile = com.example.data.UserProfile(
                                                    fullName = fullName.trim(),
                                                    phoneNumber = phoneNumber.trim(),
                                                    vehiclePlate = vehiclePlate.trim().uppercase(),
                                                    notifyOnAutoReject = notifyOnAutoReject,
                                                    audioAlertEnabled = audioAlertEnabled,
                                                    voiceCommandsEnabled = voiceCommandsEnabled,
                                                    vibrateOnNewOffer = vibrateOnNewOffer,
                                                    emergencyContactName = emergencyContactName.trim(),
                                                    emergencyContactPhone = emergencyContactPhone.trim()
                                                )
                                                viewModel.saveUserProfile(updatedProfile)
                                                profileSuccess = "Perfil atualizado e salvo no Firebase!"
                                            } catch (e: Exception) {
                                                profileError = "Falha ao salvar: ${e.message}"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                    enabled = !isSaving,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    modifier = Modifier.weight(1.2f).testTag("profile_save_button")
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextLight, strokeWidth = 2.dp)
                                    } else {
                                        Text("Salvar Perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Estabilidade em Tempo Real",
                                        color = TextLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AccentGreen, shape = androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Text(
                                        text = "Conectado",
                                        color = AccentGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Monitore falhas de conexão ou geolocalização capturadas no Firebase.",
                                color = TextLight.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )

                            // Simulated Error injection buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        com.example.data.FirestoreManager.logErrorToFirebase(
                                            type = "CONNECTION_ERROR",
                                            message = "Falha simulada de conexão com a API de análise de ofertas."
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(30.dp)
                                ) {
                                    Text("Simular Conexão", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        com.example.data.FirestoreManager.logErrorToFirebase(
                                            type = "GEOLOCATION_ERROR",
                                            message = "Falha simulada na aquisição de coordenadas de GPS."
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(30.dp)
                                ) {
                                    Text("Simular GPS", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Logs list
                            if (firebaseErrorLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tudo operando normalmente! Sem falhas registradas.",
                                        color = AccentGreen.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    firebaseErrorLogs.take(5).forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Badges
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (log.type == "CONNECTION_ERROR") AccentBlue.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (log.type == "CONNECTION_ERROR") "CONEXÃO" else "GPS",
                                                    color = if (log.type == "CONNECTION_ERROR") AccentBlue else AccentRed,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.message,
                                                    color = TextLight,
                                                    fontSize = 10.sp,
                                                    maxLines = 2
                                                )
                                                Text(
                                                    text = "${log.deviceModel} • ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))}",
                                                    color = TextLight.copy(alpha = 0.4f),
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Config Panel

            // Geofence Modal
            AnimatedVisibility(visible = showGeofenceModal) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).verticalScroll(rememberScrollState())
                        .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                                Text("Cerca Virtual Avançada", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            IconButton(onClick = { showGeofenceModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = AccentBlue)
                            }
                        }
                        
                        Text(
                            text = "Configure zonas de risco, áreas de bônus ou locais restritos. O Jarvis avisará por voz ao se aproximar.",
                            color = TextLight.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        var localZones by remember(settings) { mutableStateOf(settings.geofenceZones) }
                        
                        // List of existing zones
                        if (localZones.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nenhuma zona configurada.", color = TextDim, fontSize = 14.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                localZones.forEachIndexed { index, zone ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (zone.isDangerZone) AccentRed.copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (zone.isDangerZone) AccentRed.copy(alpha = 0.3f) else AccentGreen.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(zone.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                Switch(
                                                    checked = zone.active,
                                                    onCheckedChange = { checked ->
                                                        val updated = localZones.toMutableList()
                                                        updated[index] = zone.copy(active = checked)
                                                        localZones = updated
                                                    }
                                                )
                                            }
                                            Text("Raio: ${zone.radiusMeters.toInt()}m - Lat: ${String.format("%.4f", zone.latitude)}, Lon: ${String.format("%.4f", zone.longitude)}", color = TextDim, fontSize = 12.sp)
                                            if (zone.customVoiceAlert.isNotBlank()) {
                                                Text("🗣️ Jarvis: \"${zone.customVoiceAlert}\"", color = AccentBlue, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                                            }
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = {
                                                    val updated = localZones.toMutableList()
                                                    updated.removeAt(index)
                                                    localZones = updated
                                                }) {
                                                    Text("Remover", color = AccentRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        // Add new zone form
                        var newName by remember { mutableStateOf("") }
                        var newLat by remember { mutableStateOf("") }
                        var newLon by remember { mutableStateOf("") }
                        var newRadius by remember { mutableStateOf("1000") }
                        var newIsDanger by remember { mutableStateOf(true) }
                        var newVoiceAlert by remember { mutableStateOf("") }
                        
                        Text("Adicionar Nova Zona", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nome da Zona (ex: Cracolândia, Centro)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newLat,
                                onValueChange = { newLat = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = newLon,
                                onValueChange = { newLon = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                        
                        OutlinedTextField(
                            value = newRadius,
                            onValueChange = { newRadius = it },
                            label = { Text("Raio (metros)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = newIsDanger, onCheckedChange = { newIsDanger = it })
                            Text("Zona de Risco (Bloquear ofertas dentro do raio)", color = Color.White, fontSize = 14.sp)
                        }
                        
                        OutlinedTextField(
                            value = newVoiceAlert,
                            onValueChange = { newVoiceAlert = it },
                            label = { Text("Alerta de Voz Personalizado (Jarvis)") },
                            placeholder = { Text("Ex: Atenção, entrando em área de risco.") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Button(
                            onClick = {
                                val lat = newLat.toDoubleOrNull()
                                val lon = newLon.toDoubleOrNull()
                                val rad = newRadius.toFloatOrNull()
                                if (lat != null && lon != null && rad != null && newName.isNotBlank()) {
                                    val zone = com.example.coordinator.GeofenceZone(
                                        name = newName,
                                        latitude = lat,
                                        longitude = lon,
                                        radiusMeters = rad,
                                        isDangerZone = newIsDanger,
                                        customVoiceAlert = newVoiceAlert
                                    )
                                    val updated = localZones.toMutableList()
                                    updated.add(zone)
                                    localZones = updated
                                    
                                    // Reset form
                                    newName = ""
                                    newLat = ""
                                    newLon = ""
                                    newRadius = "1000"
                                    newVoiceAlert = ""
                                } else {
                                    Toast.makeText(context, "Preencha os campos corretamente", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adicionar Zona", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val updatedSettings = settings.copy(geofenceZones = localZones)
                                RadarCoordinator.saveSettings(context, updatedSettings)
                                Toast.makeText(context, "Zonas salvas com sucesso!", Toast.LENGTH_SHORT).show()
                                showGeofenceModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Salvar Configurações", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }


            AnimatedVisibility(visible = showConfigPanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).verticalScroll(rememberScrollState())
                        .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Configurações do Servidor", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { showConfigPanel = false }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Salvar", tint = AccentBlue)
                            }
                        }

                        Button(
                            onClick = {
                                RadarCoordinator.syncWithCloud(context)
                                android.widget.Toast.makeText(context, "Sincronização iniciada...", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("sync_cloud_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizar com a Nuvem", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAdminLoginModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                        ) {
                            Text("Acesso Administrador", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }

                        var vehicleType by remember(settings) { mutableStateOf(settings.vehicleType) }
                        var useLocalGemini by remember { mutableStateOf(settings.useLocalGemini) }
                        var geminiApiKey by remember { mutableStateOf(settings.geminiApiKey) }
                        var serverUrl by remember { mutableStateOf(settings.serverBaseUrl) }
                        var token by remember { mutableStateOf(settings.apiToken) }
                        var mockGpsEnabled by remember { mutableStateOf(settings.forceMockSpeed) }
                        var minValKm by remember { mutableStateOf(settings.minValuePerKm.toString()) }
                        var minFare by remember { mutableStateOf(settings.minFareValue.toString()) }
                        var fuelPrice by remember { mutableStateOf(settings.fuelPrice.toString()) }
                        var motorcycleConsumption by remember { mutableStateOf(settings.motorcycleConsumptionKmPerL.toString()) }
                        var dailyGoal by remember { mutableStateOf(settings.dailyGoalR.toString()) }
                        var fixedCosts by remember { mutableStateOf(settings.fixedCosts.toString()) }
                        var riskZones by remember { mutableStateOf(settings.riskZonesKeywords) }
                        var rejectSupermarkets by remember { mutableStateOf(settings.rejectSupermarkets) }
                        var avoidStoreKeywords by remember { mutableStateOf(settings.avoidStoreKeywords) }
                        var minProfitPerHour by remember { mutableStateOf(settings.minProfitPerHour.toString()) }
                        var rainModeMultiplier by remember { mutableStateOf(settings.rainModeMultiplier.toString()) }
                        var maxDrops by remember { mutableStateOf(settings.maxDrops.toString()) }
                        var headingHomeMode by remember { mutableStateOf(settings.headingHomeMode) }
                        var homeAddress by remember { mutableStateOf(settings.homeAddress) }
                        var autoAcceptPremium by remember { mutableStateOf(settings.autoAcceptPremium) }
                        var autoAcceptMinPerKm by remember { mutableStateOf(settings.autoAcceptMinPerKm.toString()) }
                        var chainDeliveriesMode by remember { mutableStateOf(settings.chainDeliveriesMode) }
                        var voiceOnlyMode by remember { mutableStateOf(settings.voiceOnlyMode) }
                        var preferredReturnNeighborhoods by remember { mutableStateOf(settings.preferredReturnNeighborhoods) }
                        var jarvisOverlayMode by remember { mutableStateOf(settings.jarvisOverlayMode) }
                        var jarvisVoiceOverlayMode by remember { mutableStateOf(settings.jarvisOverlayMode) } // alias
                        var jarvisVoiceEngine by remember { mutableStateOf(settings.jarvisVoiceEngine) }
                        var jarvisVoiceTone by remember { mutableStateOf(settings.jarvisVoiceTone) }
                        var jarvisVoiceStyle by remember { mutableStateOf(settings.jarvisVoiceStyle) }
                        var elevenLabsApiKey by remember { mutableStateOf(settings.elevenLabsApiKey) }
                        var elevenLabsVoiceId by remember { mutableStateOf(settings.elevenLabsVoiceId) }
                        var elevenLabsModelId by remember { mutableStateOf(settings.elevenLabsModelId) }
                        var elevenLabsStability by remember { mutableStateOf(settings.elevenLabsStability) }
                        var elevenLabsSimilarityBoost by remember { mutableStateOf(settings.elevenLabsSimilarityBoost) }
                        var elevenLabsStyle by remember { mutableStateOf(settings.elevenLabsStyle) }
                        var elevenLabsSpeakerBoost by remember { mutableStateOf(settings.elevenLabsSpeakerBoost) }
                        var openAiApiKey by remember { mutableStateOf(settings.openAiApiKey) }
                        var openAiVoice by remember { mutableStateOf(settings.openAiVoice) }
                        var openAiModel by remember { mutableStateOf(settings.openAiModel) }
                        var cliqueSuperVeloz by remember { mutableStateOf(settings.cliqueSuperVeloz) }
                        var antiDeteccaoMilitar by remember { mutableStateOf(settings.antiDeteccaoMilitar) }
                        var camuflagemOverlay by remember { mutableStateOf(settings.camuflagemOverlay) }
                        var isAutoRejectEnabled by remember { mutableStateOf(settings.isAutoRejectEnabled) }

                        var autoRejectMinFare by remember { mutableStateOf(settings.autoRejectMinFare.toString()) }
                        var speedLimitKmh by remember { mutableStateOf(settings.speedLimitKmh.toString()) }
                        var filterByTimeEnabled by remember { mutableStateOf(settings.filterByTimeEnabled) }
                        var filterStartTime by remember { mutableStateOf(settings.filterStartTime) }
                        var filterEndTime by remember { mutableStateOf(settings.filterEndTime) }
                        var highValueAlertTone by remember { mutableStateOf(settings.highValueAlertTone) }
                        var notifyOnTrafficChange by remember { mutableStateOf(settings.notifyOnTrafficChange) }
                        var voiceCmdAccept by remember { mutableStateOf(settings.voiceCmdAccept) }
                        var voiceCmdReject by remember { mutableStateOf(settings.voiceCmdReject) }
                        var voiceCmdSupport by remember { mutableStateOf(settings.voiceCmdSupport) }
                        var voiceCmdVip by remember { mutableStateOf(settings.voiceCmdVip) }
                        var quickReply1Cmd by remember { mutableStateOf(settings.quickReply1Cmd) }
                        var quickReply1Text by remember { mutableStateOf(settings.quickReply1Text) }
                        var quickReply2Cmd by remember { mutableStateOf(settings.quickReply2Cmd) }
                        var quickReply2Text by remember { mutableStateOf(settings.quickReply2Text) }
                        var quickReply3Cmd by remember { mutableStateOf(settings.quickReply3Cmd) }
                        var quickReply3Text by remember { mutableStateOf(settings.quickReply3Text) }

                        // --- SEÇÃO DE AUTENTICAÇÃO FIREBASE ---
                        com.example.ui.components.FirebaseAccountSection(settings = settings)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Usar IA Local (Sem Servidor)", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = useLocalGemini,
                                onCheckedChange = {
                                    useLocalGemini = it
                                    RadarCoordinator.saveSettings(context, settings.copy(useLocalGemini = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        if (useLocalGemini) {
                            OutlinedTextField(
                                value = geminiApiKey,
                                onValueChange = {
                                    geminiApiKey = it
                                    RadarCoordinator.saveSettings(context, settings.copy(geminiApiKey = it))
                                },
                                label = { Text("Chave API do Gemini") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_gemini_api_key")
                            )
                        } else {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = {
                                    serverUrl = it
                                    RadarCoordinator.saveSettings(context, settings.copy(serverBaseUrl = it))
                                },
                                label = { Text("Base URL do Backend Flask") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_server_url")
                            )

                            OutlinedTextField(
                                value = token,
                                onValueChange = {
                                    token = it
                                    RadarCoordinator.saveSettings(context, settings.copy(apiToken = it))
                                },
                                label = { Text("X-API-Token") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_api_token")
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minValKm,
                                onValueChange = {
                                    minValKm = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(minValuePerKm = d))
                                    }
                                },
                                label = { Text("Mínimo R$/km") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minFare,
                                onValueChange = {
                                    minFare = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(minFareValue = d))
                                    }
                                },
                                label = { Text("Mínimo Corrida") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var maxPickupStr by remember { mutableStateOf(settings.maxPickupDistanceKm.toString()) }
                            var maxTotalStr by remember { mutableStateOf(settings.maxTotalDistanceKm.toString()) }

                            OutlinedTextField(
                                value = maxPickupStr,
                                onValueChange = {
                                    maxPickupStr = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(maxPickupDistanceKm = d))
                                    }
                                },
                                label = { Text("Raio Coleta Máx (km)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_max_pickup_distance")
                            )
                            OutlinedTextField(
                                value = maxTotalStr,
                                onValueChange = {
                                    maxTotalStr = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(maxTotalDistanceKm = d))
                                    }
                                },
                                label = { Text("Distância Total Máx (km)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_max_total_distance")
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                        Text("Módulo de Consumo e Autonomia do Veículo", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val vehicles = listOf(
                                Triple("MOTO", "Moto", "🛵"),
                                Triple("CARRO", "Carro", "🚗"),
                                Triple("CARRO_GNV", "GNV", "🔥"),
                                Triple("ELETRICO", "Elet.", "⚡")
                            )
                            vehicles.forEach { (type, label, emoji) ->
                                val selected = vehicleType == type
                                Button(
                                    onClick = {
                                        vehicleType = type
                                        val (defaultCons, defaultPrice) = when (type) {
                                            "MOTO" -> Pair(38.0, 5.80)
                                            "CARRO" -> Pair(11.0, 5.80)
                                            "CARRO_GNV" -> Pair(14.0, 4.50)
                                            else -> Pair(7.0, 0.80) // ELETRICO
                                        }
                                        motorcycleConsumption = defaultCons.toString()
                                        fuelPrice = defaultPrice.toString()
                                        RadarCoordinator.saveSettings(context, settings.copy(
                                            vehicleType = type,
                                            motorcycleConsumptionKmPerL = defaultCons,
                                            fuelPrice = defaultPrice
                                        ))
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) AccentBlue else CardSlateBg,
                                        contentColor = if (selected) Color.White else TextLight
                                    ),
                                    border = BorderStroke(1.dp, if (selected) AccentBlue else Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text("$emoji $label", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val fuelLabel = when (vehicleType) {
                                "MOTO" -> "Preço Gasolina (R$)"
                                "CARRO" -> "Preço Gasolina (R$)"
                                "CARRO_GNV" -> "Preço GNV (R$/m³)"
                                "ELETRICO" -> "Custo Energia (R$/kWh)"
                                else -> "Preço Combustível"
                            }
                            val consLabel = when (vehicleType) {
                                "MOTO" -> "Autonomia (km/L)"
                                "CARRO" -> "Autonomia (km/L)"
                                "CARRO_GNV" -> "Autonomia (km/m³)"
                                "ELETRICO" -> "Autonomia (km/kWh)"
                                else -> "Autonomia"
                            }
                            OutlinedTextField(
                                value = fuelPrice,
                                onValueChange = {
                                    fuelPrice = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(fuelPrice = d))
                                    }
                                },
                                label = { Text(fuelLabel, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_fuel_price")
                            )
                            OutlinedTextField(
                                value = motorcycleConsumption,
                                onValueChange = {
                                    motorcycleConsumption = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(motorcycleConsumptionKmPerL = d))
                                    }
                                },
                                label = { Text(consLabel, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_motorcycle_consumption")
                            )
                        }

                        val priceVal = fuelPrice.toDoubleOrNull() ?: 0.0
                        val consVal = motorcycleConsumption.toDoubleOrNull() ?: 0.0
                        val kmCost = if (consVal > 0.0) priceVal / consVal else 0.0
                        if (kmCost > 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentBlue.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                    .border(1.dp, AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Custo Base de Combustível:",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "R$ ${String.format(Locale.US, "%.3f", kmCost)} / km",
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = dailyGoal,
                            onValueChange = {
                                dailyGoal = it
                                it.toDoubleOrNull()?.let { d ->
                                    RadarCoordinator.saveSettings(context, settings.copy(dailyGoalR = d))
                                }
                            },
                            label = { Text("Meta Financeira do Dia (R$)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("settings_daily_goal")
                        )

                        OutlinedTextField(
                            value = fixedCosts,
                            onValueChange = {
                                fixedCosts = it
                                it.toDoubleOrNull()?.let { d ->
                                    RadarCoordinator.saveSettings(context, settings.copy(fixedCosts = d))
                                }
                            },
                            label = { Text("Custos Fixos Diários (R$ - ex: aluguel de moto)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth().testTag("settings_fixed_costs")
                        )

                        OutlinedTextField(
                            value = riskZones,
                            onValueChange = {
                                riskZones = it
                                RadarCoordinator.saveSettings(context, settings.copy(riskZonesKeywords = it))
                            },
                            label = { Text("Zonas de Risco Omitidas (Separadas por vírgula)") },
                            placeholder = { Text("Heliópolis, Capão Redondo, Cracolândia") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_risk_zones")
                        )

                        OutlinedTextField(
                            value = avoidStoreKeywords,
                            onValueChange = {
                                avoidStoreKeywords = it
                                RadarCoordinator.saveSettings(context, settings.copy(avoidStoreKeywords = it))
                            },
                            label = { Text("Lojas para Evitar (Nomes separados por vírgula)") },
                            placeholder = { Text("McDonalds, Carrefour, Assaí") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_avoid_stores")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rejeitar Mercado / Atacado", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Evitar entregas pesadas ou demoradas", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = rejectSupermarkets,
                                onCheckedChange = {
                                    rejectSupermarkets = it
                                    RadarCoordinator.saveSettings(context, settings.copy(rejectSupermarkets = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentRed, checkedTrackColor = AccentRed.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Filtrar por Horário (Pico)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Apenas receber ofertas no horário configurado", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = filterByTimeEnabled,
                                onCheckedChange = {
                                    filterByTimeEnabled = it
                                    RadarCoordinator.saveSettings(context, settings.copy(filterByTimeEnabled = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        if (filterByTimeEnabled) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = filterStartTime,
                                    onValueChange = {
                                        filterStartTime = it
                                        RadarCoordinator.saveSettings(context, settings.copy(filterStartTime = it))
                                    },
                                    label = { Text("Hora Início (HH:MM)") },
                                    placeholder = { Text("18:00") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = filterEndTime,
                                    onValueChange = {
                                        filterEndTime = it
                                        RadarCoordinator.saveSettings(context, settings.copy(filterEndTime = it))
                                    },
                                    label = { Text("Hora Fim (HH:MM)") },
                                    placeholder = { Text("22:00") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo HyperDrive (Aceite SuperVeloz)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Aceite instantâneo (~100ms) para vencer a concorrência", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = cliqueSuperVeloz,
                                onCheckedChange = {
                                    cliqueSuperVeloz = it
                                    RadarCoordinator.saveSettings(context, settings.copy(cliqueSuperVeloz = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anti-Detecção Militar (Stealth Engine)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Jitter cinético, fadiga biológica, adrenalina simulada, mapa pan, arrastos curvos e toques micro-hesitantes (Impossível detectar).", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = antiDeteccaoMilitar,
                                onCheckedChange = {
                                    antiDeteccaoMilitar = it
                                    RadarCoordinator.saveSettings(context, settings.copy(antiDeteccaoMilitar = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Ghost Invisible HUD", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Oculta o overlay visual sobre Uber/99 para 0% rastro", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = camuflagemOverlay,
                                onCheckedChange = {
                                    camuflagemOverlay = it
                                    RadarCoordinator.saveSettings(context, settings.copy(camuflagemOverlay = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxDrops,
                                onValueChange = {
                                    maxDrops = it
                                    it.toIntOrNull()?.let { i ->
                                        RadarCoordinator.saveSettings(context, settings.copy(maxDrops = i))
                                    }
                                },
                                label = { Text("Max. Paradas (Drops)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_max_drops")
                            )
                            OutlinedTextField(
                                value = rainModeMultiplier,
                                onValueChange = {
                                    rainModeMultiplier = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(rainModeMultiplier = d))
                                    }
                                },
                                label = { Text("Multiplicador de Chuva") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.weight(1f).testTag("settings_rain_mode")
                            )
                        }
                        
                        OutlinedTextField(
                            value = minProfitPerHour,
                            onValueChange = {
                                minProfitPerHour = it
                                it.toDoubleOrNull()?.let { d ->
                                    RadarCoordinator.saveSettings(context, settings.copy(minProfitPerHour = d))
                                }
                            },
                            label = { Text("Lucro Mínimo Desejado por Hora (R$/h)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("settings_min_profit_per_hour")
                        )

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                        
                        // NOVIDADE: MODO VOLTANDO PARA CASA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo 'Voltando para Casa'", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Filtra corridas apenas em direção ao endereço definido", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = headingHomeMode,
                                onCheckedChange = {
                                    headingHomeMode = it
                                    RadarCoordinator.saveSettings(context, settings.copy(headingHomeMode = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }
                        if (headingHomeMode) {
                            OutlinedTextField(
                                value = homeAddress,
                                onValueChange = {
                                    homeAddress = it
                                    RadarCoordinator.saveSettings(context, settings.copy(homeAddress = it))
                                },
                                label = { Text("Endereço de Casa / Destino") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("settings_home_address")
                            )
                        }
                        
                        // NOVIDADE: AUTO ACCEPT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aceitar Automaticamente (Premium)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Aceita a corrida sozinho se o valor/km for excelente", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = autoAcceptPremium,
                                onCheckedChange = {
                                    autoAcceptPremium = it
                                    RadarCoordinator.saveSettings(context, settings.copy(autoAcceptPremium = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.5f))
                            )
                        }
                        if (autoAcceptPremium) {
                            OutlinedTextField(
                                value = autoAcceptMinPerKm,
                                onValueChange = {
                                    autoAcceptMinPerKm = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(autoAcceptMinPerKm = d))
                                    }
                                },
                                label = { Text("Auto-Aceite a partir de (R$/km)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().testTag("settings_auto_accept_min")
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        // NOVIDADE: ALERTA PROATIVO DE TRÂNSITO
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Alerta Sonoro de Trânsito (Jarvis)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Avisa por voz quando o trânsito da rota passar de fluido para intenso", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = notifyOnTrafficChange,
                                onCheckedChange = {
                                    notifyOnTrafficChange = it
                                    RadarCoordinator.saveSettings(context, settings.copy(notifyOnTrafficChange = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(alpha = 0.5f))
                            )
                        }
                        
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        // SEÇÃO: ALERTAS SONOROS VIP
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Tom de Alerta VIP (Ofertas Excelentes)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Escolha o sinal sonoro preferido para ofertas de alto valor e teste abaixo", color = Color.Gray, fontSize = 10.sp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val tonesList = listOf(
                                    "bell" to "Sino Clássico",
                                    "beep" to "Bipe Rápido",
                                    "sonar" to "Sonar Profundo"
                                )
                                tonesList.forEach { (toneKey, toneName) ->
                                    Button(
                                        onClick = {
                                            highValueAlertTone = toneKey
                                            RadarCoordinator.saveSettings(context, settings.copy(highValueAlertTone = toneKey))
                                            RadarCoordinator.voiceManager?.playVipAlert(toneKey)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (highValueAlertTone == toneKey) AccentBlue else Color.Gray.copy(alpha = 0.2f),
                                            contentColor = if (highValueAlertTone == toneKey) Color.Black else TextLight
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp).testTag("tone_btn_$toneKey"),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(toneName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    RadarCoordinator.voiceManager?.playVipAlert(highValueAlertTone)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("test_sound_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Testar Som",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testar Alerta Sonoro", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        // SEÇÃO: COMANDOS DE VOZ PERSONALIZADOS
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Frases de Comandos de Voz", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Grave ou digite as frases que você falará para acionar as ações com as mãos livres no trânsito.", color = Color.Gray, fontSize = 10.sp)
                            
                            OutlinedTextField(
                                value = voiceCmdAccept,
                                onValueChange = {
                                    voiceCmdAccept = it
                                    RadarCoordinator.saveSettings(context, settings.copy(voiceCmdAccept = it))
                                },
                                label = { Text("Comando: Aceitar Corrida", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("voice_cmd_accept_field")
                            )

                            OutlinedTextField(
                                value = voiceCmdReject,
                                onValueChange = {
                                    voiceCmdReject = it
                                    RadarCoordinator.saveSettings(context, settings.copy(voiceCmdReject = it))
                                },
                                label = { Text("Comando: Recusar Corrida", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("voice_cmd_reject_field")
                            )

                            OutlinedTextField(
                                value = voiceCmdSupport,
                                onValueChange = {
                                    voiceCmdSupport = it
                                    RadarCoordinator.saveSettings(context, settings.copy(voiceCmdSupport = it))
                                },
                                label = { Text("Comando: Chamar Suporte", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("voice_cmd_support_field")
                            )

                            OutlinedTextField(
                                value = voiceCmdVip,
                                onValueChange = {
                                    voiceCmdVip = it
                                    RadarCoordinator.saveSettings(context, settings.copy(voiceCmdVip = it))
                                },
                                label = { Text("Comando: Aceitar Corrida VIP", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier.fillMaxWidth().testTag("voice_cmd_vip_field")
                            )
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                            Text("Respostas Rápidas (Chat)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Diga o comando e o Jarvis digitará o texto no chat do iFood.", color = Color.Gray, fontSize = 10.sp)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply1Cmd,
                                    onValueChange = {
                                        quickReply1Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply1Cmd = it))
                                    },
                                    label = { Text("Cmd 1", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply1Text,
                                    onValueChange = {
                                        quickReply1Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply1Text = it))
                                    },
                                    label = { Text("Texto 1", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply2Cmd,
                                    onValueChange = {
                                        quickReply2Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply2Cmd = it))
                                    },
                                    label = { Text("Cmd 2", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply2Text,
                                    onValueChange = {
                                        quickReply2Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply2Text = it))
                                    },
                                    label = { Text("Texto 2", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply3Cmd,
                                    onValueChange = {
                                        quickReply3Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply3Cmd = it))
                                    },
                                    label = { Text("Cmd 3", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply3Text,
                                    onValueChange = {
                                        quickReply3Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply3Text = it))
                                    },
                                    label = { Text("Texto 3", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }


                            // Voice match testing area
                            var isTestingVoice by remember { mutableStateOf(false) }
                            var voiceTestResult by remember { mutableStateOf("") }
                            var voiceMatchedCommand by remember { mutableStateOf("") }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Área de Teste de Fala", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (isTestingVoice) {
                                        Text("Ouvindo... Fale o comando agora.", color = AccentGreen, fontSize = 11.sp)
                                    } else {
                                        Text("Toque no botão abaixo para testar o reconhecimento das suas frases.", color = Color.Gray, fontSize = 10.sp)
                                    }
                                    
                                    if (voiceTestResult.isNotEmpty()) {
                                        Text("Você disse: \"$voiceTestResult\"", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (voiceMatchedCommand.isNotEmpty()) {
                                            Text("Ação identificada: $voiceMatchedCommand ✅", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Nenhuma ação correspondente identificada.", color = Color.Red, fontSize = 11.sp)
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (isTestingVoice) {
                                                RadarCoordinator.voiceInputManager?.stopListening()
                                                isTestingVoice = false
                                            } else {
                                                voiceTestResult = ""
                                                voiceMatchedCommand = ""
                                                isTestingVoice = true
                                                RadarCoordinator.voiceInputManager?.startListening { spoken ->
                                                    isTestingVoice = false
                                                    voiceTestResult = spoken
                                                    val cleanSpoken = spoken.lowercase(Locale.getDefault()).trim()
                                                    val customAcc = voiceCmdAccept.lowercase(Locale.getDefault()).trim()
                                                    val customRej = voiceCmdReject.lowercase(Locale.getDefault()).trim()
                                                    val customSup = voiceCmdSupport.lowercase(Locale.getDefault()).trim()
                                                    val customV = voiceCmdVip.lowercase(Locale.getDefault()).trim()

                                                    voiceMatchedCommand = when {
                                                        customAcc.isNotEmpty() && cleanSpoken.contains(customAcc) -> "Aceitar Corrida"
                                                        customRej.isNotEmpty() && cleanSpoken.contains(customRej) -> "Recusar Corrida"
                                                        customSup.isNotEmpty() && cleanSpoken.contains(customSup) -> "Chamar Suporte"
                                                        customV.isNotEmpty() && cleanSpoken.contains(customV) -> "Aceitar Corrida VIP"
                                                        cleanSpoken.contains("aceitar") -> "Aceitar Corrida"
                                                        cleanSpoken.contains("recusar") -> "Recusar Corrida"
                                                        else -> ""
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTestingVoice) Color.Red else AccentBlue,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("btn_test_voice_recognition"),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (isTestingVoice) "Parar de Ouvir" else "Gravar/Testar Minha Voz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                        
                        // NOVIDADE: MODO CASADINHA (EMENDA)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Emenda Inteligente (Casadinha)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Jarvis busca corridas começando perto do destino final da sua corrida atual", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = chainDeliveriesMode,
                                onCheckedChange = {
                                    chainDeliveriesMode = it
                                    RadarCoordinator.saveSettings(context, settings.copy(chainDeliveriesMode = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.5f))
                            )
                        }
                        if (chainDeliveriesMode) {
                            OutlinedTextField(
                                value = preferredReturnNeighborhoods,
                                onValueChange = {
                                    preferredReturnNeighborhoods = it
                                    RadarCoordinator.saveSettings(context, settings.copy(preferredReturnNeighborhoods = it))
                                },
                                label = { Text("Bairros preferidos (Ex: Pinheiros, Centro)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().testTag("settings_preferred_neighborhoods")
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Apenas Voz (Jarvis)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Mantém a tela bloqueada por segurança e opera exclusivamente via comandos de voz do Jarvis", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = voiceOnlyMode,
                                onCheckedChange = {
                                    voiceOnlyMode = it
                                    RadarCoordinator.saveSettings(context, settings.copy(voiceOnlyMode = it))
                                    if (it) {
                                        RadarCoordinator.voiceManager?.speak("Modo apenas voz ativado com sucesso. Dirija com cuidado!")
                                    } else {
                                        RadarCoordinator.voiceManager?.speak("Modo apenas voz desativado.")
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("settings_voice_only_mode_toggle")
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        Button(
                            onClick = { showVoiceConfigModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_open_voice_config_modal")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ajustar Voz e Tom (TTS) do Jarvis",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Tom de Voz do Jarvis", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("FORMAL", "AMIGÁVEL", "DIRETO").forEach { tone ->
                                    val isSelected = jarvisVoiceTone == tone
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentBlue else Color.Transparent)
                                            .border(1.dp, if (isSelected) AccentBlue else Color.Gray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                jarvisVoiceTone = tone
                                                RadarCoordinator.saveSettings(context, settings.copy(jarvisVoiceTone = tone))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tone,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Motor de Síntese de Voz (Voice Engine)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Selecione o motor para a voz ultra realista", color = Color.Gray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "LOCAL" to "Local TTS 📱",
                                    "OPENAI" to "OpenAI TTS 🧠"
                                ).forEach { (engine, label) ->
                                    val isSelected = jarvisVoiceEngine == engine
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentGreen else Color.Transparent)
                                            .border(1.dp, if (isSelected) AccentGreen else Color.Gray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                jarvisVoiceEngine = engine
                                                RadarCoordinator.saveSettings(context, settings.copy(jarvisVoiceEngine = engine))
                                                RadarCoordinator.voiceManager?.speak("Motor de voz alterado com sucesso!")
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Estilo da Voz Assistiva (Jarvis TTS)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "PADRAO" to "Jarvis",
                                    "MASCULINA" to "Masc",
                                    "FEMININA" to "Fem",
                                    "ACELERADA" to "⚡ Fast"
                                ).forEach { (style, label) ->
                                    val isSelected = jarvisVoiceStyle == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentGreen else Color.Transparent)
                                            .border(1.dp, if (isSelected) AccentGreen else Color.Gray, RoundedCornerShape(8.dp))
                                            .clickable {
                                                jarvisVoiceStyle = style
                                                RadarCoordinator.saveSettings(context, settings.copy(jarvisVoiceStyle = style))
                                                // Test speak instantly so the user can hear the pitch/rate difference!
                                                RadarCoordinator.voiceManager?.speak("Voz configurada com sucesso!")
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (jarvisVoiceEngine == "NEURAL") {
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF141A24), shape = RoundedCornerShape(12.dp)).padding(12.dp)) {
                                Text("SINTETIZADOR ELEVENLABS (VOZ REALISTA 100%)", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Configure parâmetros avançados para a voz neural do Jarvis", color = Color.Gray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Chave ElevenLabs", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = elevenLabsApiKey,
                                onValueChange = {
                                    elevenLabsApiKey = it
                                    RadarCoordinator.saveSettings(context, settings.copy(elevenLabsApiKey = it))
                                },
                                placeholder = { Text("Cole sua API Key...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().testTag("settings_elevenlabs_api_key")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Preset de Voz PT-BR Ultra Realista 🗣️", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val presets = listOf(
                                    "ErXwobaY60C9iAWzCgEh" to "Antoni\n(Amigável/Conversacional)",
                                    "pNInz6obpg7AN6ZbeS31" to "Adam\n(Grave/Narrativo)",
                                    "21m00Tcm4TlvDq8ikWAM" to "Rachel\n(Feminino Suave)",
                                    "VR6AewrXP67pIn9N9rU2" to "Jarvis\n(Padrão)"
                                )
                                presets.forEach { (voiceKey, label) ->
                                    val isSelected = elevenLabsVoiceId == voiceKey
                                    Button(
                                        onClick = {
                                            elevenLabsVoiceId = voiceKey
                                            var stability = 0.40f
                                            var similarity = 0.75f
                                            var style = 0.20f
                                            var model = "eleven_turbo_v2_5"
                                            
                                            if (voiceKey == "pNInz6obpg7AN6ZbeS31") {
                                                stability = 0.35f
                                                similarity = 0.75f
                                                style = 0.25f
                                            } else if (voiceKey == "ErXwobaY60C9iAWzCgEh") {
                                                stability = 0.40f
                                                similarity = 0.80f
                                                style = 0.15f
                                                model = "eleven_multilingual_v2"
                                            } else if (voiceKey == "21m00Tcm4TlvDq8ikWAM") {
                                                stability = 0.45f
                                                similarity = 0.75f
                                                style = 0.10f
                                                model = "eleven_multilingual_v2"
                                            } else if (voiceKey == "VR6AewrXP67pIn9N9rU2") {
                                                stability = 0.50f
                                                similarity = 0.75f
                                                style = 0.0f
                                                model = "eleven_multilingual_v2"
                                            }
                                            
                                            elevenLabsStability = stability
                                            elevenLabsSimilarityBoost = similarity
                                            elevenLabsStyle = style
                                            elevenLabsModelId = model
                                            elevenLabsSpeakerBoost = true
                                            
                                            RadarCoordinator.saveSettings(context, settings.copy(
                                                elevenLabsVoiceId = voiceKey,
                                                elevenLabsStability = stability,
                                                elevenLabsSimilarityBoost = similarity,
                                                elevenLabsStyle = style,
                                                elevenLabsModelId = model,
                                                elevenLabsSpeakerBoost = true
                                            ))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AccentGreen else Color(0xFF1E2633)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Voice ID (Clonagem de Voz)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = elevenLabsVoiceId,
                                onValueChange = {
                                    elevenLabsVoiceId = it
                                    RadarCoordinator.saveSettings(context, settings.copy(elevenLabsVoiceId = it))
                                },
                                placeholder = { Text("VR6AewrXP67pIn9N9rU2 (Jarvis default)", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().testTag("settings_elevenlabs_voice_id")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Modelo de Voz Neural", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val models = listOf(
                                    "eleven_multilingual_v2" to "Multilingual v2\n(Alta Clareza)",
                                    "eleven_turbo_v2_5" to "Turbo v2.5\n(Super Realista)",
                                    "eleven_flash_v2_5" to "Flash v2.5\n(Ultra Rápido)"
                                )
                                models.forEach { (modelKey, label) ->
                                    val isSelected = elevenLabsModelId == modelKey
                                    Button(
                                        onClick = {
                                            elevenLabsModelId = modelKey
                                            RadarCoordinator.saveSettings(context, settings.copy(elevenLabsModelId = modelKey))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AccentGreen else Color(0xFF1E2633)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // STABILITY SLIDER
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estabilidade: ${(elevenLabsStability * 100).toInt()}%", color = TextLight, fontSize = 10.sp)
                                Text(if (elevenLabsStability < 0.4f) "Mais Emocional" else if (elevenLabsStability > 0.7f) "Mais Firme" else "Equilibrado", color = Color.Gray, fontSize = 9.sp)
                            }
                            Slider(
                                value = elevenLabsStability,
                                onValueChange = {
                                    elevenLabsStability = it
                                    RadarCoordinator.saveSettings(context, settings.copy(elevenLabsStability = it))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                            )

                            // SIMILARITY BOOST SLIDER
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Clareza: ${(elevenLabsSimilarityBoost * 100).toInt()}%", color = TextLight, fontSize = 10.sp)
                                Text(if (elevenLabsSimilarityBoost > 0.8f) "Fidelidade Máxima" else "Variação Natural", color = Color.Gray, fontSize = 9.sp)
                            }
                            Slider(
                                value = elevenLabsSimilarityBoost,
                                onValueChange = {
                                    elevenLabsSimilarityBoost = it
                                    RadarCoordinator.saveSettings(context, settings.copy(elevenLabsSimilarityBoost = it))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                            )

                            // STYLE EXAGGERATION SLIDER
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exagero de Estilo: ${(elevenLabsStyle * 100).toInt()}%", color = TextLight, fontSize = 10.sp)
                                Text(if (elevenLabsStyle > 0.6f) "Altamente Expressivo" else "Tom Sóbrio", color = Color.Gray, fontSize = 9.sp)
                            }
                            Slider(
                                value = elevenLabsStyle,
                                onValueChange = {
                                    elevenLabsStyle = it
                                    RadarCoordinator.saveSettings(context, settings.copy(elevenLabsStyle = it))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // SPEAKER BOOST
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Amplificador de Falante (Speaker Boost)", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Melhora a clareza geral da voz artificial", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = elevenLabsSpeakerBoost,
                                    onCheckedChange = {
                                        elevenLabsSpeakerBoost = it
                                        RadarCoordinator.saveSettings(context, settings.copy(elevenLabsSpeakerBoost = it))
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.5f))
                                )
                            }
                        }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF141A24), shape = RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text("SINTETIZADOR OPENAI TTS (VOZ ULTRA-REALISTA)", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Excelente alternativa de alta performance com vozes naturais", color = Color.Gray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Chave OpenAI (API Key)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = openAiApiKey,
                                onValueChange = {
                                    openAiApiKey = it
                                    RadarCoordinator.saveSettings(context, settings.copy(openAiApiKey = it))
                                },
                                placeholder = { Text("sk-...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().testTag("settings_openai_api_key")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Voz Humana (PT-BR Nativo & Multilíngue)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val voices = listOf(
                                    "alloy" to "Alloy\n(Neutro)",
                                    "nova" to "Nova\n(Feminino BR)",
                                    "shimmer" to "Shimmer\n(Feminino)",
                                    "onyx" to "Onyx\n(Grave)"
                                )
                                voices.forEach { (voiceKey, label) ->
                                    val isSelected = openAiVoice == voiceKey
                                    Button(
                                        onClick = {
                                            openAiVoice = voiceKey
                                            RadarCoordinator.saveSettings(context, settings.copy(openAiVoice = voiceKey))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AccentGreen else Color(0xFF1E2633)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // NOVIDADE: MODO HUD JARVIS (SOBREPOSIÇÃO)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("HUD Jarvis (Sobreposição)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Onda de voz aparece sobre outros apps (requer permissão)", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = jarvisOverlayMode,
                                onCheckedChange = {
                                    jarvisOverlayMode = it
                                    RadarCoordinator.saveSettings(context, settings.copy(jarvisOverlayMode = it))
                                    if (it && !android.provider.Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                VoiceFilterSettingsSection(settings, context)
                                Text("Auto-Rejeitar Corridas de Baixo Valor", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Recusa automaticamente se o valor for menor que o limite definido", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = isAutoRejectEnabled,
                                onCheckedChange = {
                                    isAutoRejectEnabled = it
                                    RadarCoordinator.saveSettings(context, settings.copy(isAutoRejectEnabled = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentRed, checkedTrackColor = AccentRed.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("settings_auto_reject_switch")
                            )
                        }

                        if (isAutoRejectEnabled) {
                            OutlinedTextField(
                                value = autoRejectMinFare,
                                onValueChange = {
                                    autoRejectMinFare = it
                                    it.toDoubleOrNull()?.let { d ->
                                        RadarCoordinator.saveSettings(context, settings.copy(autoRejectMinFare = d))
                                    }
                                },
                                label = { Text("Valor Mínimo para Auto-Rejeitar (R$)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_auto_reject_min_fare_input")
                            )
                        }

                        OutlinedTextField(
                            value = speedLimitKmh,
                            onValueChange = {
                                speedLimitKmh = it
                                it.toFloatOrNull()?.let { f ->
                                    RadarCoordinator.saveSettings(context, settings.copy(speedLimitKmh = f))
                                }
                            },
                            label = { Text("Limite de Velocidade para Trava (km/h)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_speed_limit_input")
                        )

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Modo Noturno (Tema Escuro)", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = settings.isDarkMode,
                                onCheckedChange = {
                                    RadarCoordinator.saveSettings(context, settings.copy(isDarkMode = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("settings_theme_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Modo Simulação Avançada (Treinamento)", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = mockGpsEnabled,
                                onCheckedChange = {
                                    mockGpsEnabled = it
                                    RadarCoordinator.saveSettings(context, settings.copy(forceMockSpeed = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        // --- JARVIS OTA UPDATE & REMOTE RULES SYNC PANEL ---
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AccentGreen, CircleShape)
                                    )
                                    Text(
                                        text = "Cérebro Jarvis OTA Ativo",
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "Todas as melhorias e critérios que você solicita no chat do Google AI Studio são programadas pelo agente de IA e carregadas automaticamente no app.",
                                    color = TextLight.copy(alpha = 0.8f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val updateInfo by viewModel.appUpdateInfo.collectAsState()
                                    Text(
                                        text = if (updateInfo != null) "Atualização disponível!" else "Versão do app em conformidade",
                                        color = if (updateInfo != null) AccentAmber else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = { viewModel.checkUpdateAndConfig() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Sincronizar Agora", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        if (showUpdateModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = AccentGreen)
                            Text("Atualização Disponível!", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 20.sp)
                        }
                        
                        Text("Uma nova versão do Jarvis (v3.0.1) está disponível.", fontSize = 14.sp, color = TextLight)
                        Text("• Novo Modo Deus implementado\n• Precisão de IA aumentada em 40%\n• Correção de bugs de bateria", fontSize = 12.sp, color = Color.Gray)
                        Text("Deseja baixar e instalar agora?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.setShowUpdateModal(false) }) {
                                Text("Mais tarde", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setShowUpdateModal(false)
                                    android.widget.Toast.makeText(context, "Atualização aplicada.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = CardSlateBg)
                            ) {
                                Text("Atualizar Agora")
                            }
                        }
                    }
                }
            }
        }

        if (showConfetti) {
            com.example.ui.components.ConfettiAnimation(onAnimationEnd = { showConfetti = false })
        }
    }
}
}
}
}

@Composable
fun D3InteractiveEfficiencyChart(
    activeOffer: com.example.coordinator.ActiveOffer,
    settings: com.example.coordinator.RadarSettings,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.D3InteractiveEfficiencyChart(activeOffer, settings, modifier)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun RadarLogConsole(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("radar_log_console")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Console de Monitoramento",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Limpar Console",
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Text(
                text = "Histórico em tempo real (limpeza automática após 30 min) de varreduras dos filtros, status do GPS e processamento inteligente do Radar:",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Nenhum evento registrado no console.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
                            val (badgeText, prefix, colorText) = when(log.type) {
                                LogType.SUCCESS -> Triple(AccentGreen, "[OK]", AccentGreen)
                                LogType.WARNING -> Triple(AccentAmber, "[WARN]", AccentAmber)
                                LogType.ALERT -> Triple(AccentRed, "[ALERT]", AccentRed)
                                LogType.DEBUG -> Triple(Color.LightGray, "[DEBUG]", Color.LightGray)
                                LogType.INFO -> Triple(AccentBlue, "[INFO]", Color.White)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = timeStr,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Text(
                                    text = prefix,
                                    color = badgeText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Text(
                                    text = log.message,
                                    color = colorText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "${context.packageName}/com.example.service.RadarAccessibilityService"
    val enabledServicesSetting = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServicesSetting.split(':').any { it.equals(expectedComponentName, ignoreCase = true) }
}

fun isNotificationListenerServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat != null && flat.contains(pkgName)
}

@Composable
fun EarningsAndSavingsOptimizationChart(
    dailyReport: List<com.example.api.DailyReportItem>,
    settings: com.example.coordinator.RadarSettings,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.EarningsAndSavingsOptimizationChart(dailyReport, settings, modifier)
}

@Composable
fun OptimizedRoutePanel(
    currentLocation: android.location.Location?,
    activeDeliveryDestination: String,
    targetActiveOffer: com.example.coordinator.ActiveOffer,
    rainMultiplier: Double = 1.0
) {
    com.example.ui.components.OptimizedRoutePanel(currentLocation, activeDeliveryDestination, targetActiveOffer, rainMultiplier)
}

@Composable
fun GoogleMapsNavigationCard(
    currentLocation: android.location.Location?,
    currentSpeedKmh: Float,
    rainMultiplier: Double = 1.0
) {
    com.example.ui.components.GoogleMapsNavigationCard(currentLocation, currentSpeedKmh, rainMultiplier)
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("###")) {
                val headerText = trimmed.removePrefix("###").trim()
                Text(
                    text = headerText,
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            } else if (trimmed.startsWith("##")) {
                val headerText = trimmed.removePrefix("##").trim()
                Text(
                    text = headerText,
                    color = Color(0xFF9D4EDD), // Royal Purple
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            } else if (trimmed.startsWith("#")) {
                val headerText = trimmed.removePrefix("#").trim()
                Text(
                    text = headerText,
                    color = AccentBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                val bulletText = trimmed.substring(1).trim()
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = parseBoldMarkdown(bulletText),
                        color = TextLight,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = parseBoldMarkdown(trimmed),
                    color = TextLight,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val parts = text.split("**")
    var isBold = false
    parts.forEach { part ->
        if (isBold) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
            builder.append(part)
            builder.pop()
        } else {
            builder.append(part)
        }
        isBold = !isBold
    }
    return builder.toAnnotatedString()
}

@Composable
fun MultiAppRouteOptimizerCard(
    currentLocation: Location?,
    onSetDestination: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val activeOrders by com.example.util.MultiAppOrderManager.activeOrders.collectAsState()
    val optimizedRoute by com.example.util.MultiAppOrderManager.optimizedRoute.collectAsState()
    
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("multi_app_route_optimizer_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Roteiro Multi-App Otimizado",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                // Active count badge
                if (activeOrders.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activeOrders.size} Apps Ativos",
                            color = AccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Text(
                text = "O Jarvis agrupa coletas e entregas do iFood, Uber e 99 para economizar combustível, reduzir quilômetros rodados e minimizar a espera do cliente.",
                color = TextDim,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            val settings by com.example.coordinator.RadarCoordinator.settings.collectAsState()
            
            // Toggle row for Sincronização de Rota Inteligente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Sincronização de Rota Inteligente",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Auto-otimização para menor consumo",
                            color = TextDim,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Switch(
                    checked = settings.smartSequenceEnabled,
                    onCheckedChange = { enabled ->
                        val updated = settings.copy(smartSequenceEnabled = enabled)
                        com.example.coordinator.RadarCoordinator.updateSettings(updated)
                        com.example.data.FirestoreManager.saveSettings(updated)
                        
                        // Recalcular para atualizar imediatamente
                        coroutineScope.launch {
                            com.example.util.MultiAppOrderManager.recalculateRoute()
                        }
                        
                        if (enabled) {
                            RadarCoordinator.voiceManager?.speak("Sincronização de rota inteligente ativada, Thiago. Otimizando sequência de entregas para economia máxima de combustível.")
                            RadarCoordinator.addLog("Jarvis: Sincronização de Rota Inteligente ATIVADA.", com.example.coordinator.LogType.SUCCESS)
                        } else {
                            RadarCoordinator.voiceManager?.speak("Sincronização de rota desativada. Seguindo sequência padrão de coletas e entregas.")
                            RadarCoordinator.addLog("Jarvis: Sincronização de Rota Inteligente DESATIVADA.", com.example.coordinator.LogType.INFO)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = AccentGreen.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Simulator Quick Actions (No Dead Ends!)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Simular Entrada de Pedidos (Multi-App):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val lat = currentLocation?.latitude ?: -23.5505
                            val lng = currentLocation?.longitude ?: -46.6333
                            val order = com.example.util.ActiveOrder(
                                id = "mock_ifood_" + System.currentTimeMillis(),
                                appName = "iFood",
                                fare = 18.50,
                                pickupAddress = "McDonald's Paulista, 1200",
                                deliveryAddress = "Alameda Lorena, 800 - Jardim Paulista",
                                pickupLat = lat + 0.003,
                                pickupLng = lng - 0.002,
                                deliveryLat = lat + 0.008,
                                deliveryLng = lng - 0.004
                            )
                            com.example.util.MultiAppOrderManager.addOrder(order)
                            RadarCoordinator.voiceManager?.speak("Novo pedido iFood adicionado ao agrupador. Recalculando rota ideal.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA1D2C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("+ iFood", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val lat = currentLocation?.latitude ?: -23.5505
                            val lng = currentLocation?.longitude ?: -46.6333
                            val order = com.example.util.ActiveOrder(
                                id = "mock_uber_" + System.currentTimeMillis(),
                                appName = "Uber",
                                fare = 24.90,
                                pickupAddress = "Shopping Cidade São Paulo",
                                deliveryAddress = "Rua Augusta, 1500 - Consolação",
                                pickupLat = lat + 0.001,
                                pickupLng = lng + 0.002,
                                deliveryLat = lat + 0.005,
                                deliveryLng = lng + 0.001
                            )
                            com.example.util.MultiAppOrderManager.addOrder(order)
                            RadarCoordinator.voiceManager?.speak("Corrida Uber adicionada ao roteiro inteligente. Otimizando sequência.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("+ Uber", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val lat = currentLocation?.latitude ?: -23.5505
                            val lng = currentLocation?.longitude ?: -46.6333
                            val order = com.example.util.ActiveOrder(
                                id = "mock_99_" + System.currentTimeMillis(),
                                appName = "99",
                                fare = 15.20,
                                pickupAddress = "Metrô Trianon-Masp, Av Paulista",
                                deliveryAddress = "Rua Bela Cintra, 1100 - Consolação",
                                pickupLat = lat + 0.002,
                                pickupLng = lng - 0.001,
                                deliveryLat = lat + 0.006,
                                deliveryLng = lng - 0.003
                            )
                            com.example.util.MultiAppOrderManager.addOrder(order)
                            RadarCoordinator.voiceManager?.speak("Corrida 99 adicionada. Jarvis organizando ordem ideal de entrega.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5200)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("+ 99", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Gatilhos da Inteligência Artificial (Jarvis):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.coordinator.RadarCoordinator.updateTrafficDetour(true, 12, "Congestionamento pesado à frente na rota principal.")
                            RadarCoordinator.voiceManager?.speak("Atenção Thiago, detectei tráfego intenso à frente pelo sensor do Google Maps. Jarvis sugere rota de desvio automático para economizar 12 minutos.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Tráfego IA", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            com.example.coordinator.RadarCoordinator.updateFuelSuggestion(true)
                            RadarCoordinator.voiceManager?.speak("Thiago, identifiquei um Posto Ipiranga a 800 metros à frente com preço promocional de combustível para usuários do Radar. Gostaria de adicionar uma rota rápida de abastecimento?")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Gasolina IA", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            com.example.coordinator.RadarCoordinator.updateFatigueAlert(true)
                            RadarCoordinator.voiceManager?.speak("Thiago! Detectei que você já está pilotando há 3 horas seguidas sem pausas. Para sua segurança física e prevenção de acidentes, Jarvis sugere que você faça uma pausa de 15 minutos para descansar, tomar uma água ou um café.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Fadiga IA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            
            if (activeOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = TextDim.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Nenhum pedido ativo no agrupador.\nUse os botões acima para simular e testar!",
                            color = TextDim,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                // Show Route Metrics Summary
                val lat = currentLocation?.latitude ?: -23.5505
                val lng = currentLocation?.longitude ?: -46.6333
                
                // Let's build a list of stops for the optimizer metrics calculation
                val stopsList = mutableListOf<com.example.util.StopPoint>()
                activeOrders.forEach { order ->
                    if (order.status == com.example.util.OrderStatus.PICKING_UP) {
                        stopsList.add(com.example.util.StopPoint(
                            id = "${order.id}_p",
                            address = order.pickupAddress,
                            latitude = order.pickupLat,
                            longitude = order.pickupLng,
                            type = com.example.util.StopType.PICKUP,
                            orderId = order.id,
                            baseValue = order.fare
                        ))
                    }
                    if (order.status != com.example.util.OrderStatus.COMPLETED && order.status != com.example.util.OrderStatus.CANCELLED) {
                        stopsList.add(com.example.util.StopPoint(
                            id = "${order.id}_d",
                            address = order.deliveryAddress,
                            latitude = order.deliveryLat,
                            longitude = order.deliveryLng,
                            type = com.example.util.StopType.DELIVERY,
                            orderId = order.id,
                            baseValue = order.fare
                        ))
                    }
                }
                
                val metrics = com.example.util.RouteOptimizer.calculateRouteMetrics(lat, lng, stopsList)
                
                if (!settings.smartSequenceEnabled && metrics.timeSavedMinutes > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Desperdício de Combustível!",
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Ative a Sincronização de Rota para poupar ${metrics.timeSavedMinutes} min e ~${String.format(Locale.getDefault(), "%.1f", metrics.totalDistanceKm * 0.05)}L de gasolina.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                            Button(
                                onClick = {
                                    val updated = settings.copy(smartSequenceEnabled = true)
                                    com.example.coordinator.RadarCoordinator.updateSettings(updated)
                                    com.example.data.FirestoreManager.saveSettings(updated)
                                    coroutineScope.launch {
                                        com.example.util.MultiAppOrderManager.recalculateRoute()
                                    }
                                    RadarCoordinator.voiceManager?.speak("Sincronização ativada, Thiago. Roteiro reordenado para máxima economia de combustível.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Otimizar", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Km Total", color = TextDim, fontSize = 9.sp)
                        Text(String.format(Locale.getDefault(), "%.1f km", metrics.totalDistanceKm), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Tempo de Rota", color = TextDim, fontSize = 9.sp)
                        Text("${metrics.totalTimeMinutes} min", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Tempo Salvo", color = TextDim, fontSize = 9.sp)
                        Text("-${metrics.timeSavedMinutes} min", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                // Timeline of Optimized Sequence
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    optimizedRoute.forEachIndexed { index, stop ->
                        val correspondingOrder = activeOrders.find { it.id == stop.orderId }
                        val appName = correspondingOrder?.appName ?: "App"
                        val badgeColor = when (appName.lowercase()) {
                            "ifood" -> Color(0xFFEA1D2C)
                            "uber" -> Color.Black
                            "99" -> Color(0xFFFF5200)
                            else -> AccentBlue
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Timeline Step circle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(badgeColor.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // App badge label
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = appName.uppercase(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Stop type label
                                    val isPickup = stop.type == com.example.util.StopType.PICKUP
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                (if (isPickup) AccentBlue else AccentGreen).copy(alpha = 0.15f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, if (isPickup) AccentBlue else AccentGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPickup) "COLETA" else "ENTREGA",
                                            color = if (isPickup) AccentBlue else AccentGreen,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // ETA Text
                                    Text(
                                        text = "ETA: ${com.example.util.RouteOptimizer.formatEta(stop.estimatedArrival)}",
                                        color = AccentAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = stop.address,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            // Actions column
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onSetDestination(stop.address)
                                        RadarCoordinator.voiceManager?.speak("Traçando navegação para o ponto ${index + 1} da rota: ${stop.address.split(",").first()}")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Ir", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        val nextStatus = if (stop.type == com.example.util.StopType.PICKUP) {
                                            com.example.util.OrderStatus.PICKED_UP
                                        } else {
                                            com.example.util.OrderStatus.COMPLETED
                                        }
                                        com.example.util.MultiAppOrderManager.updateOrderStatus(stop.orderId, nextStatus)
                                        
                                        val actionWord = if (stop.type == com.example.util.StopType.PICKUP) "Coleta realizada" else "Entrega finalizada"
                                        RadarCoordinator.voiceManager?.speak("$actionWord com sucesso!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Ok", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Clear button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            activeOrders.forEach {
                                com.example.util.MultiAppOrderManager.removeOrder(it.id)
                            }
                            RadarCoordinator.voiceManager?.speak("Roteiro multi-app reiniciado.")
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Limpar Roteiro", color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}



@Composable
fun VoiceFilterSettingsSection(
    settings: com.example.coordinator.RadarSettings,
    context: android.content.Context
) {
    var voiceFilterEnabled by remember(settings.voiceFilterEnabled) { mutableStateOf(settings.voiceFilterEnabled) }
    var voiceFilterMinFare by remember(settings.voiceFilterMinFare) { mutableStateOf(settings.voiceFilterMinFare.toString()) }
    var voiceFilterMaxDistance by remember(settings.voiceFilterMaxDistance) { mutableStateOf(settings.voiceFilterMaxDistance.toString()) }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Filtro Seletivo de Voz (Jarvis)", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Jarvis anunciará apenas ofertas que atingirem esses limites", color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = voiceFilterEnabled,
            onCheckedChange = { 
                voiceFilterEnabled = it
                com.example.coordinator.RadarCoordinator.saveSettings(context, settings.copy(voiceFilterEnabled = it))
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3A86FF), checkedTrackColor = Color(0xFF3A86FF).copy(alpha = 0.5f))
        )
    }
    
    if (voiceFilterEnabled) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = voiceFilterMinFare,
                onValueChange = { 
                    voiceFilterMinFare = it 
                    it.toDoubleOrNull()?.let { d ->
                        com.example.coordinator.RadarCoordinator.saveSettings(context, settings.copy(voiceFilterMinFare = d))
                    }
                },
                label = { Text("Valor Mínimo (R$)", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFFE2E8F0), unfocusedTextColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF3A86FF)),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = voiceFilterMaxDistance,
                onValueChange = { 
                    voiceFilterMaxDistance = it 
                    it.toDoubleOrNull()?.let { d ->
                        com.example.coordinator.RadarCoordinator.saveSettings(context, settings.copy(voiceFilterMaxDistance = d))
                    }
                },
                label = { Text("Distância Máxima (km)", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFFE2E8F0), unfocusedTextColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF3A86FF)),
                modifier = Modifier.weight(1f)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}
