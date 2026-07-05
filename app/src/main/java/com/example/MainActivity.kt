package com.example

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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coordinator.ActiveOffer
import com.example.coordinator.LogEntry
import com.example.coordinator.LogType
import com.example.coordinator.RadarCoordinator
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
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)

val CardSlateBg: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val TextLight: Color
    get() = if (RadarCoordinator.settings.value.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val AccentGreen = Color(0xFF10B981)
val AccentRed = Color(0xFFEF4444)
val AccentAmber = Color(0xFFF59E0B)
val AccentBlue = Color(0xFF3B82F6)

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

class MainViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(context)
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

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    // Multi-permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (fineGranted || coarseGranted) {
            Toast.makeText(this, "Permissão de localização concedida!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Localização necessária para monitorar velocidade real.", Toast.LENGTH_LONG).show()
        }

        if (!notificationGranted) {
            Toast.makeText(this, "Notificações necessárias para rodar o serviço em segundo plano.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize coordinator
        RadarCoordinator.init(this)
        viewModel = MainViewModel(this)

        enableEdgeToEdge()

        // Request permissions on startup
        checkAndRequestPermissions()

        setContent {
            val settings by RadarCoordinator.settings.collectAsState()
            MyApplicationTheme(darkTheme = settings.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSlateBg
                ) {
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Collect variables from Coordinator
    val serviceState by remember { mutableStateOf(RadarCoordinatorService.isServiceRunning) }
    var isServiceActive by remember { mutableStateOf(RadarCoordinatorService.isServiceRunning) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    
    // Periodically sync service and accessibility status
    LaunchedEffect(Unit) {
        while (true) {
            isServiceActive = RadarCoordinatorService.isServiceRunning
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    val currentRadarState by RadarCoordinator.currentState.collectAsState()
    val speedState by RadarCoordinator.speedState.collectAsState()
    val currentSpeedKmh by RadarCoordinator.currentSpeedKmh.collectAsState()
    val settings by RadarCoordinator.settings.collectAsState()
    val activeOffer by RadarCoordinator.activeOffer.collectAsState()
    val lastDecision by RadarCoordinator.lastDecision.collectAsState()
    val lastReason by RadarCoordinator.lastReason.collectAsState()
    val historyLogs by viewModel.historyOffers.collectAsState()
    val logs by RadarCoordinator.logs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val deliveryActive by RadarCoordinator.deliveryActive.collectAsState()
    val deliveryStartTimestamp by RadarCoordinator.deliveryStartTimestamp.collectAsState()
    val deliveryAccumulatedDistanceMeters by RadarCoordinator.deliveryAccumulatedDistanceMeters.collectAsState()
    val deliveryFare by RadarCoordinator.deliveryFare.collectAsState()
    val deliveryAppName by RadarCoordinator.deliveryAppName.collectAsState()
    val deliveryEstimatedDistanceKm by RadarCoordinator.deliveryEstimatedDistanceKm.collectAsState()
    val deliveryEstimatedTimeMin by RadarCoordinator.deliveryEstimatedTimeMin.collectAsState()
    val deliveryCompletedCount by RadarCoordinator.deliveryCompletedCount.collectAsState()
    val deliveryTotalEarnings by RadarCoordinator.deliveryTotalEarnings.collectAsState()
    val deliveryTotalDistanceKm by RadarCoordinator.deliveryTotalDistanceKm.collectAsState()
    val deliveryTotalTimeMinutes by RadarCoordinator.deliveryTotalTimeMinutes.collectAsState()
    val blockedByGeofenceCount by RadarCoordinator.blockedByGeofenceCount.collectAsState()
    val currentGPSLocation by RadarCoordinator.currentLocation.collectAsState()
    val sosActive by RadarCoordinator.sosActive.collectAsState()

    val isVoiceListening by (RadarCoordinator.voiceInputManager?.isListening ?: MutableStateFlow(false)).collectAsState()
    val voiceRecognizedText by (RadarCoordinator.voiceInputManager?.recognizedText ?: MutableStateFlow("")).collectAsState()

    // Form states for customizable simulation
    var simAppName by remember { mutableStateOf("iFood") }
    var simFareValue by remember { mutableStateOf("18.50") }
    var simDistance by remember { mutableStateOf("3.0") }
    var simPickup by remember { mutableStateOf("McDonalds - Shopping") }
    var simDelivery by remember { mutableStateOf("Rua das Flores, 123") }

    // Toggle states for panels
    var showCustomSimPanel by remember { mutableStateOf(false) }
    var showConfigPanel by remember { mutableStateOf(false) }
    var showProfilePanel by remember { mutableStateOf(false) }
    var firebaseErrorLogs by remember { mutableStateOf<List<com.example.data.AppErrorLog>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) AccentGreen else AccentRed)
                        )
                        Text(
                            text = "Radar Delivery AI",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    val isStopped = speedState == SpeedState.PARADO
                    val actionPillColor by animateColorAsState(
                        targetValue = if (isStopped) AccentGreen else AccentRed,
                        animationSpec = tween(400)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(actionPillColor.copy(alpha = 0.15f))
                            .border(1.dp, actionPillColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isStopped) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = actionPillColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isStopped) "SEGURO" else "BLOQUEADO",
                            color = actionPillColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            RadarCoordinator.saveSettings(context, settings.copy(isDarkMode = !settings.isDarkMode))
                        },
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Alternar Tema",
                            tint = if (settings.isDarkMode) AccentAmber else AccentBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            showProfilePanel = !showProfilePanel
                            showConfigPanel = false
                        },
                        modifier = Modifier.testTag("toggle_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = if (showProfilePanel) AccentBlue else TextLight
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            showConfigPanel = !showConfigPanel
                            showProfilePanel = false
                        },
                        modifier = Modifier.testTag("toggle_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = TextLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSlateBg
                )
            )
        },
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, safetyBorderColor, RoundedCornerShape(12.dp))
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

            // 1. Service Controls Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Coordenador Central",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isServiceActive) "Roda em segundo plano" else "Inativo. Ligue para monitorar",
                            color = if (isServiceActive) AccentGreen else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = {
                            if (isServiceActive) {
                                RadarCoordinatorService.stopService(context)
                                isServiceActive = false
                                Toast.makeText(context, "Radar Desativado", Toast.LENGTH_SHORT).show()
                            } else {
                                RadarCoordinatorService.startService(context)
                                isServiceActive = true
                                Toast.makeText(context, "Radar Ativado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceActive) AccentRed else AccentGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("toggle_service_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isServiceActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isServiceActive) "Parar" else "Ativar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 1.5. Accessibility Service Panel (Leitura Automática)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                            shape = RoundedCornerShape(12.dp)
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
                    
                    Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    
                    Text(
                        text = "🔒 Segurança & Privacidade:\nEste serviço funciona de forma local e lê SOMENTE os dados dos aplicativos de entrega suportados (iFood, Uber, 99, Rappi, Lalamove). Suas informações pessoais de outros aplicativos nunca são acessadas ou coletadas.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // Monitored Platforms Card (Plataformas Monitoradas)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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

            // 2. Speed safety lock and Hysteresis indicators
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
                    Text(
                        text = "Trava de Segurança (Velocidade)",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current Speed display
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", currentSpeedKmh),
                                    color = TextLight,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "km/h",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Locked status display
                        val isStopped = speedState == SpeedState.PARADO
                        val safetyBg by animateColorAsState(
                            targetValue = if (isStopped) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                            animationSpec = tween(400)
                        )
                        val safetyBorder by animateColorAsState(
                            targetValue = if (isStopped) AccentGreen else AccentRed,
                            animationSpec = tween(400)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(74.dp)
                                .background(safetyBg, RoundedCornerShape(12.dp))
                                .border(1.dp, safetyBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isStopped) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isStopped) AccentGreen else AccentRed,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isStopped) "MOTO PARADA" else "ANDANDO",
                                        color = if (isStopped) AccentGreen else AccentRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isStopped) "Ações Liberadas" else "Trava Ativa (Somente Voz)",
                                        color = TextLight.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Colored indicator bar for safety lock status
                    val isLocked = currentSpeedKmh > settings.speedLimitKmh
                    val speedPercentage = (currentSpeedKmh / settings.speedLimitKmh).coerceIn(0f, 1f)
                    
                    val barColor by animateColorAsState(
                        targetValue = if (isLocked) AccentRed else AccentGreen,
                        animationSpec = tween(400)
                    )
                    
                    // Animate pulsing alpha when locked to attract attention
                    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                            .padding(12.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isLocked) AccentRed.copy(alpha = pulseAlpha) else AccentGreen)
                                )
                                Text(
                                    text = if (isLocked) "TRAVA DE SEGURANÇA ATIVA" else "TRAVA DE SEGURANÇA INATIVA",
                                    color = if (isLocked) AccentRed else AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("safety_lock_status_label")
                                )
                            }
                            Text(
                                text = "${(speedPercentage * 100).toInt()}% do limite",
                                color = if (isLocked) AccentRed else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Linear progress bar for speed vs limit
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = speedPercentage)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = if (isLocked) {
                                                listOf(AccentRed.copy(alpha = 0.8f), AccentRed)
                                            } else {
                                                listOf(AccentGreen.copy(alpha = 0.6f), AccentGreen)
                                            }
                                        )
                                    )
                            )
                        }
                        
                        Text(
                            text = if (isLocked) {
                                "Interface de toque bloqueada por excesso de velocidade. Reduza a velocidade abaixo de ${settings.speedLimitKmh.toInt()} km/h."
                            } else {
                                "Velocidade sob controle. Toques na interface e comandos por voz liberados com segurança."
                            },
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    // Simulated Speed override slider
                    if (settings.forceMockSpeed) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Simulador de Velocidade (Mock GPS)",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${currentSpeedKmh.toInt()} km/h",
                                    color = AccentAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = currentSpeedKmh,
                                onValueChange = {
                                    RadarCoordinator.updateSpeed(it)
                                    RadarCoordinator.saveSettings(context, settings.copy(mockSpeedKmh = it))
                                },
                                valueRange = 0f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentAmber,
                                    activeTrackColor = AccentAmber,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.testTag("speed_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0 km/h (Parado)", color = Color.Gray, fontSize = 10.sp)
                                Text("8 km/h (Andando)", color = Color.Gray, fontSize = 10.sp)
                                Text("40 km/h", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 3. State Machine Tracker
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Máquina de Estados (O Cérebro)",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Current state indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompassCalibration,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Estado Atual:",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = when (currentRadarState) {
                                        RadarState.OUVINDO -> "OUVINDO (Aguardando nova corrida)"
                                        RadarState.OFERTA_LIDA -> "OFERTA_LIDA (Dados extraídos)"
                                        RadarState.ANALISANDO -> "ANALISANDO (Chamando backend)"
                                        RadarState.SUGERINDO -> "SUGERINDO (Falando por voz)"
                                        RadarState.AGUARDANDO_ACAO -> "AGUARDANDO_AÇÃO (Parado vs Andando)"
                                        RadarState.ACEITANDO -> "ACEITANDO (Clique automático seguro)"
                                        RadarState.NAVEGANDO -> "NAVEGANDO (Abrindo Google Maps)"
                                    },
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Horizontal state sequence visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val states = listOf(RadarState.OUVINDO, RadarState.ANALISANDO, RadarState.SUGERINDO, RadarState.AGUARDANDO_ACAO, RadarState.NAVEGANDO)
                        states.forEachIndexed { index, state ->
                            val isActive = currentRadarState == state
                            val stepColor = if (isActive) AccentBlue else Color.DarkGray
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(stepColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (state) {
                                        RadarState.OUVINDO -> "Ouvir"
                                        RadarState.ANALISANDO -> "Análise"
                                        RadarState.SUGERINDO -> "Voz"
                                        RadarState.AGUARDANDO_ACAO -> "Ação"
                                        RadarState.NAVEGANDO -> "Rota"
                                        else -> ""
                                    },
                                    color = if (isActive) TextLight else Color.Gray,
                                    fontSize = 9.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 3.5. Visual Console / Log View
            RadarLogConsole(
                logs = logs,
                onClear = { RadarCoordinator.clearLogs() }
            )

            // 4. Offer simulation section
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
                    Text(
                        text = "Simulador de Ofertas de Corrida",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Presets
                    Text("Escolha uma corrida mockada para testar o fluxo:", color = Color.Gray, fontSize = 12.sp)

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
                                    // Use standard compact pixel base64 representable of an iFood screenshot
                                    val fakeImageBase64 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
                                    // Trigger simulation
                                    Toast.makeText(context, "Simulando Corrida Excelente...", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(context, RadarCoordinatorService::class.java).apply {
                                        action = "PROCESS_SIMULATION"
                                    }
                                    // Since we have direct access to our service class, we can call it!
                                    // Let's implement an direct trigger:
                                    coroutineScope.launch {
                                        val mockSvc = context as? MainActivity
                                        // Trigger directly on the running instance if possible
                                    }
                                    // Or since it's a singleton pattern, we can trigger via binding or static trigger!
                                    // Let's call the public service function! To avoid binding complications,
                                    // we can directly launch it from our scope!
                                    coroutineScope.launch {
                                        val serviceInstance = com.example.service.RadarCoordinatorService()
                                        // Wait, the safest way is to launch a coroutine that calls processNewOffer in the service scope or emulator!
                                        // Let's create an elegant helper to invoke it:
                                        com.example.service.RadarCoordinatorService.isServiceRunning.let {
                                            // The service is running, so we can trigger the offer simulation!
                                            // Let's start the service with arguments or just trigger processNewOffer!
                                            // Since the service is running, let's trigger it directly.
                                            // Since it's a background service, let's make an helper or send broadcast!
                                            // Wait, a clean static broadcast or just using direct method invocation is great!
                                            // Let's do a trick: we can start the service with intent extras!
                                            val triggerIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                                                putExtra("SIMULATE", true)
                                                putExtra("APP_NAME", "iFood")
                                                putExtra("FARE_VALUE", 18.50)
                                                putExtra("PICKUP_ADDRESS", "Av. Paulista, 1500 - Shopping Cidade São Paulo")
                                                putExtra("DELIVERY_ADDRESS", "Av. Rebouças, 2500 - Pinheiros")
                                            }
                                            context.startService(triggerIntent)
                                            // Let's update our service code to handle this intent! That is extremely clean!
                                            // Wait, let's write an edit for the service to check intent extras in onStartCommand.
                                            // Let's do that!
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Desvio Muito Longo (Sugere Recusar)", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("R$ 12,00 | 10,0 km | R$ 1.20/km (Distância alta)", color = TextLight.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }

                    // Chained delivery (A+B) settings
                    Divider(color = Color.DarkGray, thickness = 1.dp)

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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Injetar Corrida Personalizada", fontWeight = FontWeight.Bold)
                            }
                        }
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
                            val (decisionColor, decisionTitle, decisionDesc) = when (decisionText) {
                                "aceitar" -> Triple(AccentGreen, "SUGESTÃO: ACEITAR", "Corrida altamente rentável")
                                "recusar" -> Triple(AccentRed, "SUGESTÃO: RECUSAR", "Corrida com baixo rendimento ou desvio longo")
                                else -> Triple(AccentAmber, "SUGESTÃO: CONSIDERAR", "Abaixo do ideal, decida com cuidado")
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(decisionColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .border(1.dp, decisionColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                    .background(DarkSlateBg, RoundedCornerShape(12.dp))
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

                            // D3 Interactive Efficiency Chart Card
                            Text(
                                text = "Gráfico de Eficiência (Arraste para Simular)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            D3InteractiveEfficiencyChart(
                                activeOffer = targetActiveOffer,
                                settings = settings,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Geolocation speed-based safety lock calculation
                            val isSpeedLocked = currentSpeedKmh > settings.speedLimitKmh

                            if (isSpeedLocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AccentRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                                text = "ACEITE BLOQUEADO",
                                                color = AccentRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Velocidade atual (${String.format(Locale.US, "%.1f", currentSpeedKmh)} km/h) excede o limite seguro de ${settings.speedLimitKmh.toInt()} km/h. Pare o veículo com segurança para interagir.",
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
                                        if (isSpeedLocked) CardSlateBg.copy(alpha = 0.3f)
                                        else if (isVoiceListening) AccentBlue.copy(alpha = 0.12f)
                                        else CardSlateBg.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSpeedLocked) Color.Gray.copy(alpha = 0.1f)
                                        else if (isVoiceListening) AccentBlue.copy(alpha = 0.4f)
                                        else Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
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
                                    enabled = !isSpeedLocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentRed,
                                        disabledContainerColor = Color.DarkGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
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
                                    enabled = !isSpeedLocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        disabledContainerColor = Color.DarkGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
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
                                    shape = RoundedCornerShape(10.dp)
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

            // User Profile Panel
            AnimatedVisibility(visible = showProfilePanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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
                                if (showProfilePanel && currentUserState != null) {
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

                            Divider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

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

                            Divider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

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

                            Divider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

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

                            Divider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

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

                            Divider(color = AccentBlue.copy(alpha = 0.15f), thickness = 1.dp)

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
            AnimatedVisibility(visible = showConfigPanel) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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

                        var useLocalGemini by remember { mutableStateOf(settings.useLocalGemini) }
                        var geminiApiKey by remember { mutableStateOf(settings.geminiApiKey) }
                        var useHermesAgent by remember { mutableStateOf(settings.useHermesAgent) }
                        var hermesBaseUrl by remember { mutableStateOf(settings.hermesBaseUrl) }
                        var hermesApiKey by remember { mutableStateOf(settings.hermesApiKey) }
                        var serverUrl by remember { mutableStateOf(settings.serverBaseUrl) }
                        var token by remember { mutableStateOf(settings.apiToken) }
                        var mockGpsEnabled by remember { mutableStateOf(settings.forceMockSpeed) }
                        var minValKm by remember { mutableStateOf(settings.minValuePerKm.toString()) }
                        var minFare by remember { mutableStateOf(settings.minFareValue.toString()) }
                        var riskZones by remember { mutableStateOf(settings.riskZonesKeywords) }
                        var isAutoRejectEnabled by remember { mutableStateOf(settings.isAutoRejectEnabled) }
                        var autoRejectMinFare by remember { mutableStateOf(settings.autoRejectMinFare.toString()) }
                        var speedLimitKmh by remember { mutableStateOf(settings.speedLimitKmh.toString()) }

                        // --- SEÇÃO DE AUTENTICAÇÃO FIREBASE ---
                        val currentUser by FirebaseAuthManager.currentUser.collectAsState()
                        var authEmail by remember { mutableStateOf("") }
                        var authPassword by remember { mutableStateOf("") }
                        var authLoading by remember { mutableStateOf(false) }
                        var authError by remember { mutableStateOf<String?>(null) }
                        var authSuccessMsg by remember { mutableStateOf<String?>(null) }
                        val coroutineScope = rememberCoroutineScope()

                        Divider(color = AccentBlue.copy(alpha = 0.2f), thickness = 1.dp)

                        Text(
                            text = "Conta do Entregador",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (currentUser == null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Faça login para salvar seus ganhos, histórico de ofertas e preferências de forma permanente vinculados à sua conta pessoal.",
                                    color = TextLight.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )

                                OutlinedTextField(
                                    value = authEmail,
                                    onValueChange = { 
                                        authEmail = it
                                        authError = null
                                    },
                                    label = { Text("E-mail do Entregador") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextLight, 
                                        unfocusedTextColor = TextLight, 
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                                )

                                OutlinedTextField(
                                    value = authPassword,
                                    onValueChange = { 
                                        authPassword = it
                                        authError = null
                                    },
                                    label = { Text("Senha") },
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextLight, 
                                        unfocusedTextColor = TextLight, 
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                                )

                                if (authError != null) {
                                    Text(
                                        text = authError ?: "",
                                        color = AccentRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (authEmail.isBlank() || authPassword.isBlank()) {
                                                authError = "Por favor, preencha o e-mail e a senha."
                                                return@Button
                                            }
                                            authLoading = true
                                            authError = null
                                            authSuccessMsg = null
                                            coroutineScope.launch {
                                                val res = FirebaseAuthManager.loginWithEmail(authEmail.trim(), authPassword)
                                                authLoading = false
                                                if (res.isSuccess) {
                                                    authSuccessMsg = "Login realizado com sucesso!"
                                                    authEmail = ""
                                                    authPassword = ""
                                                } else {
                                                    authError = "Erro ao entrar: ${res.exceptionOrNull()?.message}"
                                                }
                                            }
                                        },
                                        enabled = !authLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        modifier = Modifier.weight(1f).testTag("auth_login_button")
                                    ) {
                                        if (authLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextLight, strokeWidth = 2.dp)
                                        } else {
                                            Text("Entrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (authEmail.isBlank() || authPassword.isBlank()) {
                                                authError = "Por favor, preencha o e-mail e a senha."
                                                return@OutlinedButton
                                            }
                                            if (authPassword.length < 6) {
                                                authError = "A senha deve conter pelo menos 6 caracteres."
                                                return@OutlinedButton
                                            }
                                            authLoading = true
                                            authError = null
                                            authSuccessMsg = null
                                            coroutineScope.launch {
                                                val res = FirebaseAuthManager.signUpWithEmail(authEmail.trim(), authPassword)
                                                authLoading = false
                                                if (res.isSuccess) {
                                                    authSuccessMsg = "Conta criada e conectada!"
                                                    authEmail = ""
                                                    authPassword = ""
                                                } else {
                                                    authError = "Erro ao cadastrar: ${res.exceptionOrNull()?.message}"
                                                }
                                            }
                                        },
                                        enabled = !authLoading,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                                        border = BorderStroke(1.dp, AccentBlue),
                                        modifier = Modifier.weight(1f).testTag("auth_signup_button")
                                    ) {
                                        Text("Criar Conta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Conectado como:",
                                            color = TextLight.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = currentUser?.email ?: "",
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            FirebaseAuthManager.logout()
                                            authSuccessMsg = "Sessão encerrada."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f)),
                                        modifier = Modifier.testTag("auth_logout_button")
                                    ) {
                                        Text("Sair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "✓ Ganhos, preferências de rotas e históricos vinculados e sincronizados na nuvem.",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (authSuccessMsg != null) {
                            Text(
                                text = authSuccessMsg ?: "",
                                color = AccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Divider(color = AccentBlue.copy(alpha = 0.2f), thickness = 1.dp)

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

                        Divider(color = AccentBlue.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Usar Hermes Agent (Nous Research)", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = useHermesAgent,
                                onCheckedChange = {
                                    useHermesAgent = it
                                    RadarCoordinator.saveSettings(context, settings.copy(useHermesAgent = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }

                        if (useHermesAgent) {
                            OutlinedTextField(
                                value = hermesBaseUrl,
                                onValueChange = {
                                    hermesBaseUrl = it
                                    RadarCoordinator.saveSettings(context, settings.copy(hermesBaseUrl = it))
                                },
                                label = { Text("Hermes API Base URL") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = hermesApiKey,
                                onValueChange = {
                                    hermesApiKey = it
                                    RadarCoordinator.saveSettings(context, settings.copy(hermesApiKey = it))
                                },
                                label = { Text("Hermes API Key") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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

                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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

                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

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
                            Text("Forçar Simulador de Velocidade (Mock)", color = TextLight, fontSize = 12.sp)
                            Switch(
                                checked = mockGpsEnabled,
                                onCheckedChange = {
                                    mockGpsEnabled = it
                                    RadarCoordinator.saveSettings(context, settings.copy(forceMockSpeed = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // 1.75. Copiloto IA Offline - Estatísticas e Recomendações Operacionais
            val totalAnalyzed = historyLogs.size
            val totalAccepted = historyLogs.count { it.suggestion.lowercase() == "aceitar" }
            val totalRejected = historyLogs.count { it.suggestion.lowercase() == "recusar" }
            val totalEstimatedEarnings = historyLogs.filter { it.suggestion.lowercase() == "aceitar" }.sumOf { it.fareValue }
            val totalAcceptedKm = historyLogs.filter { it.suggestion.lowercase() == "aceitar" }.sumOf { it.totalDistance }
            
            val avgEarningsPerKm = if (totalAcceptedKm > 0.0) totalEstimatedEarnings / totalAcceptedKm else 0.0
            val acceptanceRate = if (totalAnalyzed > 0) (totalAccepted.toDouble() / totalAnalyzed.toDouble()) * 100.0 else 0.0

            val aiRecommendation = remember(totalAnalyzed, avgEarningsPerKm, acceptanceRate, settings) {
                if (totalAnalyzed == 0) {
                    "Seu Copiloto IA está ativo e pronto! À medida que novas ofertas forem processadas, analisarei seus ganhos, eficiência de combustível/tempo e sugerirei os melhores ajustes operacionais em tempo real."
                } else {
                    val formattedKm = String.format(Locale.US, "%.2f", avgEarningsPerKm)
                    when {
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

                    Divider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    // Session stat pillars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pillar 1: Total Estimated Session Earnings
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(10.dp),
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
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(10.dp),
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
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(10.dp),
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
                            .background(AccentBlue.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                                imageVector = Icons.Default.VolumeUp,
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

            // 1.75. Painel de Controle de Raio Geográfico (GPS Geofence Control)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.5f), AccentAmber.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("geofence_control_card")
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
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Controle de Raio Geográfico (GPS)",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Circular badge for blocked count
                        Box(
                            modifier = Modifier
                                .background(
                                    if (blockedByGeofenceCount > 0) AccentRed.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (blockedByGeofenceCount > 0) AccentRed.copy(alpha = 0.4f) else AccentBlue.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$blockedByGeofenceCount Bloqueadas",
                                color = if (blockedByGeofenceCount > 0) AccentRed else AccentBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    // Real-Time GPS Coordinates Display
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Geolocalização Atual do Entregador", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    val latVal = currentGPSLocation?.latitude ?: -23.550520
                                    val lonVal = currentGPSLocation?.longitude ?: -46.633308
                                    Text(
                                        text = String.format(Locale.US, "Lat: %.6f | Lon: %.6f", latVal, lonVal),
                                        color = TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(AccentGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("GPS CONECTADO", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Simulation locations for visual testing (critical for AI Studio!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val loc = Location("mock").apply {
                                            latitude = -23.550520 // Centro (Av. Paulista / Sé)
                                            longitude = -46.633308
                                            speed = 0f
                                            accuracy = 5f
                                            time = System.currentTimeMillis()
                                        }
                                        RadarCoordinator.updateLocation(loc)
                                        RadarCoordinator.addLog("Simulação GPS: Localização alterada para Centro (São Paulo)", LogType.INFO)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.1f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .testTag("simulate_gps_center_button")
                                ) {
                                    Text("Simular Centro", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val loc = Location("mock").apply {
                                            latitude = -23.5855 // Sul (Parque Ibirapuera)
                                            longitude = -46.6620
                                            speed = 0f
                                            accuracy = 5f
                                            time = System.currentTimeMillis()
                                        }
                                        RadarCoordinator.updateLocation(loc)
                                        RadarCoordinator.addLog("Simulação GPS: Localização alterada para Zona Sul (Ibirapuera)", LogType.INFO)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.1f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .testTag("simulate_gps_south_button")
                                ) {
                                    Text("Simular Zona Sul", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Stepper Controls for Distance Radiuses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Max Pickup Radius Control (Raio Máximo de Coleta)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Raio de Coleta Máximo",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                val pickupKm = settings.maxPickupDistanceKm
                                Text(
                                    text = if (pickupKm <= 0.0) "Sem Limite" else "${String.format(Locale.US, "%.1f", pickupKm)} km",
                                    color = if (pickupKm <= 0.0) AccentBlue else AccentAmber,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newVal = (pickupKm - 0.5).coerceAtLeast(0.0)
                                            RadarCoordinator.saveSettings(context, settings.copy(maxPickupDistanceKm = newVal))
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CardSlateBg, CircleShape)
                                            .testTag("decrease_pickup_radius_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Diminuir Coleta",
                                            tint = TextLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val newVal = if (pickupKm <= 0.0) 0.5 else pickupKm + 0.5
                                            RadarCoordinator.saveSettings(context, settings.copy(maxPickupDistanceKm = newVal.coerceAtMost(25.0)))
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CardSlateBg, CircleShape)
                                            .testTag("increase_pickup_radius_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Aumentar Coleta",
                                            tint = TextLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Max Total Distance Control (Distância Total Máxima)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Distância Total Máxima",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val totalKm = settings.maxTotalDistanceKm
                                Text(
                                    text = if (totalKm <= 0.0) "Sem Limite" else "${String.format(Locale.US, "%.1f", totalKm)} km",
                                    color = if (totalKm <= 0.0) AccentBlue else AccentAmber,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newVal = (totalKm - 1.0).coerceAtLeast(0.0)
                                            RadarCoordinator.saveSettings(context, settings.copy(maxTotalDistanceKm = newVal))
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CardSlateBg, CircleShape)
                                            .testTag("decrease_total_distance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Diminuir Distância Total",
                                            tint = TextLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val newVal = if (totalKm <= 0.0) 1.0 else totalKm + 1.0
                                            RadarCoordinator.saveSettings(context, settings.copy(maxTotalDistanceKm = newVal.coerceAtMost(50.0)))
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CardSlateBg, CircleShape)
                                            .testTag("increase_total_distance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Aumentar Distância Total",
                                            tint = TextLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Explanatory info & Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtros ativados atuam automaticamente rejeitando corridas fora da zona permitida.",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1.5f),
                            lineHeight = 11.sp
                        )
                        
                        TextButton(
                            onClick = {
                                RadarCoordinator.saveSettings(context, settings.copy(maxPickupDistanceKm = 0.0, maxTotalDistanceKm = 0.0))
                            },
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("reset_geofence_button")
                        ) {
                            Text("Limpar", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1.78. Dashboard de Métricas & Telemetria em Tempo Real (Ganho Real vs Tempo Investido)
            // State for real-time ticking clock
            var currentTickTime by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(deliveryActive) {
                if (deliveryActive) {
                    while (true) {
                        currentTickTime = System.currentTimeMillis()
                        kotlinx.coroutines.delay(1000L)
                    }
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
                            colors = listOf(AccentGreen.copy(alpha = 0.5f), AccentBlue.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("telemetry_dashboard_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
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
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Métricas & Telemetria Real",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        if (deliveryActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Text(
                                    text = "GPS ATIVO",
                                    color = Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "MODO ESCUTA",
                                color = AccentBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.15f), thickness = 0.5.dp)

                    if (deliveryActive) {
                        // 1. Active Delivery Telemetry Dashboard
                        val elapsedMs = if (deliveryStartTimestamp > 0L) (currentTickTime - deliveryStartTimestamp).coerceAtLeast(0L) else 0L
                        val seconds = (elapsedMs / 1000) % 60
                        val minutes = (elapsedMs / 60000) % 60
                        val hoursNum = (elapsedMs / 3600000)
                        val formattedTime = String.format(Locale.US, "%02d:%02d:%02d", hoursNum, minutes, seconds)
                        
                        val elapsedHours = elapsedMs.toDouble() / 3600000.0
                        val actualDistanceKm = deliveryAccumulatedDistanceMeters / 1000.0
                        
                        // Profit calculations
                        val estHours = if (deliveryEstimatedTimeMin > 0.0) deliveryEstimatedTimeMin / 60.0 else 0.25
                        val estProfitPerHour = deliveryFare / estHours
                        val estProfitPerKm = if (deliveryEstimatedDistanceKm > 0.0) deliveryFare / deliveryEstimatedDistanceKm else 2.0
                        
                        val realProfitPerHour = if (elapsedHours > 0.0) deliveryFare / elapsedHours else estProfitPerHour
                        val realProfitPerKm = if (actualDistanceKm > 0.0) deliveryFare / actualDistanceKm else estProfitPerKm
                        
                        val hourDiffPercent = if (estProfitPerHour > 0.0) ((realProfitPerHour - estProfitPerHour) / estProfitPerHour) * 100.0 else 0.0
                        
                        // Performance status
                        val (statusText, statusColor, statusBg) = when {
                            realProfitPerHour >= estProfitPerHour -> Triple("DESEMPENHO EXCELENTE (+${hourDiffPercent.toInt()}% por hora)", AccentGreen, AccentGreen.copy(alpha = 0.1f))
                            realProfitPerHour >= estProfitPerHour * 0.8 -> Triple("RITMO ESTÁVEL (Dentro do estimado)", AccentAmber, AccentAmber.copy(alpha = 0.1f))
                            else -> Triple("ALERTA DE ATRASO (${hourDiffPercent.toInt()}% por hora)", Color.Red, Color.Red.copy(alpha = 0.1f))
                        }

                        // Info Bar (App and Destination)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Entrega em Curso", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(deliveryAppName, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor da Corrida", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("R$ ${String.format(Locale.US, "%.2f", deliveryFare)}", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Real-time variables tracking
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column 1: Time elapsed
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                                        Text("Tempo", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(formattedTime, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Est: ${deliveryEstimatedTimeMin.toInt()} min", color = Color.Gray, fontSize = 8.sp)
                                }
                            }

                            // Column 2: Actual Distance
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                                        Text("Distância", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${String.format(Locale.US, "%.2f", actualDistanceKm)} km", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Est: ${String.format(Locale.US, "%.1f", deliveryEstimatedDistanceKm)} km", color = Color.Gray, fontSize = 8.sp)
                                }
                            }
                        }

                        // Comparing Gains/Time & Gains/Distance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Profit per Km
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("R$ / Quilômetro", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "R$ ${String.format(Locale.US, "%.2f", realProfitPerKm)}/km",
                                        color = AccentGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Prometido: R$ ${String.format(Locale.US, "%.2f", estProfitPerKm)}",
                                        color = Color.Gray,
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            // Profit per Hour
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("R$ / Hora (Real)", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "R$ ${String.format(Locale.US, "%.1f", realProfitPerHour)}/h",
                                        color = AccentBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Estimado: R$ ${String.format(Locale.US, "%.1f", estProfitPerHour)}",
                                        color = Color.Gray,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        // Traffic / Performance Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(statusBg, RoundedCornerShape(8.dp))
                                .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Interactive Control & Simulation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Simulate GPS Movement Button (critical for testing in AI Studio!)
                            Button(
                                onClick = {
                                    RadarCoordinator.simulateGpsMovement(500.0) // moto move 500m
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(36.dp)
                                    .testTag("simulate_gps_movement_button")
                            ) {
                                Text("+500m GPS", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Manual Complete Button
                            Button(
                                onClick = {
                                    val updatedSettings = settings.copy(
                                        isActiveDeliveryEnabled = false,
                                        activeDeliveryDestination = ""
                                    )
                                    RadarCoordinator.saveSettings(context, updatedSettings)
                                    RadarCoordinator.completeActiveDelivery()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("manual_complete_delivery_button")
                            ) {
                                Text("Finalizar", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Cancel Button
                            Button(
                                onClick = {
                                    val updatedSettings = settings.copy(
                                        isActiveDeliveryEnabled = false,
                                        activeDeliveryDestination = ""
                                    )
                                    RadarCoordinator.saveSettings(context, updatedSettings)
                                    RadarCoordinator.cancelActiveDelivery()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("manual_cancel_delivery_button")
                            ) {
                                Text("Cancelar", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // 2. Cumulative Dashboard & Session Productivity
                        val avgRealSessionHour = if (deliveryTotalTimeMinutes > 0.0) (deliveryTotalEarnings / (deliveryTotalTimeMinutes / 60.0)) else 0.0
                        val avgRealSessionKm = if (deliveryTotalDistanceKm > 0.0) (deliveryTotalEarnings / deliveryTotalDistanceKm) else 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column 1: Total Earnings Session
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Faturamento Real", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("R$ ${String.format(Locale.US, "%.2f", deliveryTotalEarnings)}", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${deliveryCompletedCount} entregas", color = Color.Gray, fontSize = 8.sp)
                                }
                            }

                            // Column 2: Avg R$/km
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("R$ / Km Real Médio", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("R$ ${String.format(Locale.US, "%.2f", avgRealSessionKm)}/km", color = AccentAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${String.format(Locale.US, "%.1f", deliveryTotalDistanceKm)} km rodados", color = Color.Gray, fontSize = 8.sp)
                                }
                            }

                            // Column 3: Avg R$/h
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("R$ / Hora Real Médio", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("R$ ${String.format(Locale.US, "%.1f", avgRealSessionHour)}/h", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${deliveryTotalTimeMinutes.toInt()} min investidos", color = Color.Gray, fontSize = 8.sp)
                                }
                            }
                        }

                        // Selectivity target gauge vs filter min value per km
                        val percentValueKm = if (settings.minValuePerKm > 0.0) (avgRealSessionKm / settings.minValuePerKm) * 100.0 else 0.0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aproveitamento vs Filtro Mínimo (R$ ${String.format(Locale.US, "%.2f", settings.minValuePerKm)}/km)",
                                        color = TextLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${percentValueKm.toInt()}%",
                                        color = if (percentValueKm >= 100.0) AccentGreen else AccentAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Progress bar indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(CardSlateBg, RoundedCornerShape(3.dp))
                                ) {
                                    val progressFraction = (avgRealSessionKm / settings.minValuePerKm).coerceIn(0.0, 1.5).toFloat() / 1.5f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressFraction)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(AccentBlue, if (percentValueKm >= 100.0) AccentGreen else AccentAmber)
                                                ),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                                
                                Text(
                                    text = if (percentValueKm >= 100.0) {
                                        "Seu faturamento real/km supera seu filtro mínimo! Suas decisões de aceitação estão gerando lucro líquido otimizado."
                                    } else {
                                        "Seu faturamento real/km está abaixo do filtro desejado de R$ ${settings.minValuePerKm}/km. Ative filtros mais rígidos ou rode em regiões quentes."
                                    },
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
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

                    Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                    // Visualização de ganhos e economias otimizados pela IA
                    EarningsAndSavingsOptimizationChart(
                        dailyReport = dailyReport,
                        settings = settings,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

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
                                        .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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

                                    Divider(color = Color.Gray.copy(alpha = 0.1f), thickness = 0.5.dp)

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
                            .background(DarkSlateBg, RoundedCornerShape(8.dp))
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

                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

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
                                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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

                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

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
                                        .background(DarkSlateBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                                                        RoundedCornerShape(12.dp)
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
                                                    Text("Navegar Rota", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                                    Text("Testar Radar", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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

            // 7. History logs
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
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
                            .background(CardSlateBg, RoundedCornerShape(12.dp))
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
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
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

                                    Divider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)

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
        }
    }
}

@Composable
fun D3InteractiveEfficiencyChart(
    activeOffer: com.example.coordinator.ActiveOffer,
    settings: com.example.coordinator.RadarSettings,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current

    val offerDistance = activeOffer.totalDistance
    val offerFare = activeOffer.fareValue

    val maxX = maxOf(offerDistance * 1.4, 15.0)
    val maxY = maxOf(offerFare * 1.4, 40.0)

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val gridColor = Color.Gray.copy(alpha = 0.15f)
    val fontColor = Color.Gray
    val thresholdLineColor = AccentAmber
    val minFareLineColor = Color.Magenta.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(DarkSlateBg, RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeOffer) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        touchPoint = down.position
                        isInteracting = true
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                val pointer = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                touchPoint = pointer.position
                                pointer.consume()
                            } else {
                                touchPoint = null
                                isInteracting = false
                                break
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val marginLeft = with(density) { 48.dp.toPx() }
            val marginBottom = with(density) { 36.dp.toPx() }
            val marginRight = with(density) { 16.dp.toPx() }
            val marginTop = with(density) { 16.dp.toPx() }

            val plotWidth = canvasWidth - marginLeft - marginRight
            val plotHeight = canvasHeight - marginTop - marginBottom

            if (plotWidth > 0f && plotHeight > 0f) {
                fun getPixelX(x: Double): Float = marginLeft + ((x / maxX) * plotWidth).toFloat()
                fun getPixelY(y: Double): Float = marginTop + plotHeight - ((y / maxY) * plotHeight).toFloat()

                val acceptPath = Path().apply {
                    val intersectX = if (settings.minValuePerKm > 0.0) settings.minFareValue / settings.minValuePerKm else 0.0
                    moveTo(getPixelX(intersectX), getPixelY(settings.minFareValue))
                    lineTo(getPixelX(maxX), getPixelY(settings.minFareValue))
                    lineTo(getPixelX(maxX), getPixelY(maxY))

                    val topEfficiencyY = maxX * settings.minValuePerKm
                    if (topEfficiencyY > maxY) {
                        val boundaryX = maxY / settings.minValuePerKm
                        lineTo(getPixelX(boundaryX), getPixelY(maxY))
                    } else {
                        lineTo(getPixelX(maxX), getPixelY(topEfficiencyY))
                    }
                    close()
                }

                drawPath(
                    path = acceptPath,
                    color = AccentGreen.copy(alpha = 0.06f)
                )

                val yStep = if (maxY > 50) 15.0 else 10.0
                var yVal = 0.0
                while (yVal <= maxY) {
                    val py = getPixelY(yVal)
                    drawLine(
                        color = gridColor,
                        start = Offset(marginLeft, py),
                        end = Offset(canvasWidth - marginRight, py),
                        strokeWidth = 1f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "R$ ${yVal.toInt()}",
                        style = TextStyle(color = fontColor, fontSize = 9.sp),
                        topLeft = Offset(8f, py - 12f)
                    )
                    yVal += yStep
                }

                val xStep = if (maxX > 20) 5.0 else 2.0
                var xVal = 0.0
                while (xVal <= maxX) {
                    val px = getPixelX(xVal)
                    drawLine(
                        color = gridColor,
                        start = Offset(px, marginTop),
                        end = Offset(px, marginTop + plotHeight),
                        strokeWidth = 1f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${xVal.toInt()}km",
                        style = TextStyle(color = fontColor, fontSize = 9.sp),
                        topLeft = Offset(px - 20f, marginTop + plotHeight + 4f)
                    )
                    xVal += xStep
                }

                val slopeEndX = minOf(maxX, if (settings.minValuePerKm > 0.0) maxY / settings.minValuePerKm else maxX)
                val slopeEndY = slopeEndX * settings.minValuePerKm
                drawLine(
                    color = thresholdLineColor.copy(alpha = 0.8f),
                    start = Offset(getPixelX(0.0), getPixelY(0.0)),
                    end = Offset(getPixelX(slopeEndX), getPixelY(slopeEndY)),
                    strokeWidth = 3f
                )

                if (settings.minFareValue > 0.0 && settings.minFareValue <= maxY) {
                    val pyMinFare = getPixelY(settings.minFareValue)
                    drawLine(
                        color = minFareLineColor,
                        start = Offset(marginLeft, pyMinFare),
                        end = Offset(canvasWidth - marginRight, pyMinFare),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val currentOfferPx = getPixelX(offerDistance)
                val currentOfferPy = getPixelY(offerFare)

                drawCircle(
                    color = AccentBlue.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(currentOfferPx, currentOfferPy)
                )
                drawCircle(
                    color = AccentBlue,
                    radius = 5f,
                    center = Offset(currentOfferPx, currentOfferPy)
                )

                if (isInteracting) {
                    touchPoint?.let { pos ->
                        if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight &&
                            pos.y >= marginTop && pos.y <= marginTop + plotHeight) {

                            drawLine(
                                color = AccentBlue.copy(alpha = 0.4f),
                                start = Offset(pos.x, marginTop),
                                end = Offset(pos.x, marginTop + plotHeight),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            drawLine(
                                color = AccentBlue.copy(alpha = 0.4f),
                                start = Offset(marginLeft, pos.y),
                                end = Offset(canvasWidth - marginRight, pos.y),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )

                            val touchX = ((pos.x - marginLeft) / plotWidth) * maxX
                            val touchY = ((marginTop + plotHeight - pos.y) / plotHeight) * maxY

                            val touchEfficiency = if (touchX > 0.0) touchY / touchX else 0.0
                            val isTouchPointAcceptable = touchY >= settings.minFareValue && touchEfficiency >= settings.minValuePerKm

                            val tooltipText = String.format(Locale.US, "%.1f km | R$ %.2f\n%.2f R$/km\n%s",
                                touchX, touchY, touchEfficiency, if (isTouchPointAcceptable) "ACEITÁVEL" else "RECUSADO")

                            val textLayoutResult = textMeasurer.measure(
                                text = tooltipText,
                                style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )

                            val tooltipWidth = textLayoutResult.size.width + 16f
                            val tooltipHeight = textLayoutResult.size.height + 12f

                            var tooltipX = pos.x - tooltipWidth / 2f
                            var tooltipY = pos.y - tooltipHeight - 16f

                            tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)
                            tooltipY = tooltipY.coerceIn(marginTop, marginTop + plotHeight - tooltipHeight)

                            // Draw background fill first
                            drawRoundRect(
                                color = DarkSlateBg.copy(alpha = 0.9f),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                            // Draw border stroke second
                            drawRoundRect(
                                color = if (isTouchPointAcceptable) AccentGreen else AccentRed,
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                                style = Stroke(width = 2f)
                            )

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(tooltipX + 8f, tooltipY + 6f)
                            )
                        }
                    }
                }
            }
        }
    }
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

data class ChartDayData(
    val dateLabel: String,
    val earnings: Double,
    val savings: Double,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val totalKm: Double
)

data class ForecastDayData(
    val dateLabel: String,
    val baselineEarnings: Double,
    val potentialEarnings: Double,
    val percentageGain: Double,
    val recommendedRoutesCount: Int
)

@Composable
fun EarningsAndSavingsOptimizationChart(
    dailyReport: List<com.example.api.DailyReportItem>,
    settings: com.example.coordinator.RadarSettings,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current

    var selectedViewTab by remember { mutableStateOf(0) } // 0 = Histórico, 1 = Previsão IA

    val chartItems = remember(dailyReport) {
        if (dailyReport.isNotEmpty()) {
            dailyReport.map { item ->
                val dateParts = item.date.split("/")
                val shortDate = if (dateParts.size >= 2) "${dateParts[0]}/${dateParts[1]}" else item.date
                val calculatedSavings = (item.totalOffersRejected * 2.50) + (item.totalOffersAccepted * 4.50)
                ChartDayData(
                    dateLabel = shortDate,
                    earnings = item.estimatedEarnings,
                    savings = calculatedSavings,
                    acceptedCount = item.totalOffersAccepted,
                    rejectedCount = item.totalOffersRejected,
                    totalKm = item.totalDistanceKm
                )
            }.reversed()
        } else {
            listOf(
                ChartDayData("Seg", 120.0, 32.50, 8, 12, 48.0),
                ChartDayData("Ter", 145.5, 41.20, 10, 15, 54.0),
                ChartDayData("Qua", 110.0, 28.00, 7, 10, 42.0),
                ChartDayData("Qui", 168.0, 49.80, 11, 18, 62.0),
                ChartDayData("Sex", 195.0, 58.60, 13, 22, 75.0),
                ChartDayData("Sáb", 240.0, 74.30, 16, 25, 92.0),
                ChartDayData("Dom", 185.0, 52.00, 12, 17, 70.0)
            )
        }
    }

    // Dynamic Route Optimization Efficiency
    val routeOptimizationEfficiency = remember(settings) {
        val baseEff = 0.12 // Base AI optimization (12%)
        val filterBonus = ((settings.minValuePerKm - 1.5) * 0.08).coerceIn(0.0, 0.20) // Filter reward (up to 20%)
        val autoRejectBonus = if (settings.isAutoRejectEnabled) 0.10 else 0.02 // Auto reject benefit (up to 10%)
        val distanceBonus = if (settings.maxPickupDistanceKm <= 4.0) 0.04 else 0.01 // short pickup filter (up to 4%)
        baseEff + filterBonus + autoRejectBonus + distanceBonus
    }

    // Projected forecast items (Seasonal Weekday-based Projection)
    val forecastItems = remember(chartItems, routeOptimizationEfficiency) {
        val avgHistoricalEarnings = if (chartItems.isNotEmpty()) {
            chartItems.map { it.earnings }.average()
        } else {
            160.0
        }
        val baseMean = if (avgHistoricalEarnings.isNaN() || avgHistoricalEarnings < 20.0) 160.0 else avgHistoricalEarnings

        val futureLabels = listOf("Seg+", "Ter+", "Qua+", "Qui+", "Sex+", "Sáb+", "Dom+")
        futureLabels.mapIndexed { idx, label ->
            val dayOfWeekFactor = when (idx) {
                4 -> 1.15 // Sexta-feira aquecida
                5 -> 1.35 // Sábado de pico
                6 -> 1.20 // Domingo de alta demanda
                else -> 0.90 // Dias de semana normais
            }
            val dailyBaseline = baseMean * dayOfWeekFactor
            val dailyPotential = dailyBaseline * (1.0 + routeOptimizationEfficiency)
            
            ForecastDayData(
                dateLabel = label,
                baselineEarnings = dailyBaseline,
                potentialEarnings = dailyPotential,
                percentageGain = routeOptimizationEfficiency * 100.0,
                recommendedRoutesCount = (8 + (dayOfWeekFactor * 6).toInt()).coerceAtLeast(5)
            )
        }
    }

    val maxEarnings = chartItems.maxOfOrNull { it.earnings } ?: 100.0
    val maxSavings = chartItems.maxOfOrNull { it.savings } ?: 50.0
    val maxValHist = maxOf(maxEarnings, maxSavings)
    val maxYHist = maxOf(maxValHist * 1.15, 100.0)

    val maxForecastPot = forecastItems.maxOfOrNull { it.potentialEarnings } ?: 100.0
    val maxForecastBase = forecastItems.maxOfOrNull { it.baselineEarnings } ?: 100.0
    val maxValForecast = maxOf(maxForecastPot, maxForecastBase)
    val maxYForecast = maxOf(maxValForecast * 1.15, 100.0)

    val maxY = if (selectedViewTab == 0) maxYHist else maxYForecast

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    val isDemoMode = dailyReport.isEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSlateBg, RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row with custom premium capsules switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PILOTO DE INTELIGÊNCIA ARTIFICIAL",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (selectedViewTab == 0) "Histórico Otimizado de Operação" else "Previsão & Prospecção Futura",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Custom capsule switcher
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Histórico", "Previsão").forEachIndexed { idx, title ->
                        val isSelected = selectedViewTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
                                .border(1.dp, if (isSelected) AccentBlue.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { selectedViewTab = idx }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) TextLight else Color.Gray,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // High-fidelity Dynamic KPI Metrics Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedViewTab == 0) {
                    val totalEarnings = chartItems.sumOf { it.earnings }
                    val totalSavings = chartItems.sumOf { it.savings }
                    val acceptedTotal = chartItems.sumOf { it.acceptedCount }
                    val rejectedTotal = chartItems.sumOf { it.rejectedCount }
                    val totalOffers = acceptedTotal + rejectedTotal
                    val efficiencyPercent = if (totalOffers > 0) (rejectedTotal.toDouble() / totalOffers * 100.0).coerceAtMost(85.0) else 45.0

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Faturamento", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalEarnings), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Poupado (IA)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalSavings), color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Filtro Eficaz", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.1f%%", efficiencyPercent), color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val totalForecasted = forecastItems.sumOf { it.potentialEarnings }
                    val totalBaseline = forecastItems.sumOf { it.baselineEarnings }
                    val totalIncremental = totalForecasted - totalBaseline
                    val efficiencyPercent = routeOptimizationEfficiency * 100.0

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Otimização de Rota", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "+%.1f%%", efficiencyPercent), color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Projeção (7 Dias)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalForecasted), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Lucro Extra IA", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "+R$ %.2f", totalIncremental), color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Color legend row based on tab selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedViewTab == 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentGreen, RoundedCornerShape(1.5.dp)))
                        Text("Ganhos Reais", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentBlue, RoundedCornerShape(4.dp)))
                        Text("Economia IA", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentAmber, RoundedCornerShape(4.dp)))
                        Text("Potencial Otimizado", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
                        Text("Baseline (Sem IA)", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                }
            }

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedViewTab, chartItems, forecastItems) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                touchPoint = down.position
                                isInteracting = true
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (anyPressed) {
                                        val pointer = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                        touchPoint = pointer.position
                                        pointer.consume()
                                    } else {
                                        touchPoint = null
                                        isInteracting = false
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val marginLeft = with(density) { 42.dp.toPx() }
                    val marginBottom = with(density) { 20.dp.toPx() }
                    val marginRight = with(density) { 8.dp.toPx() }
                    val marginTop = with(density) { 8.dp.toPx() }

                    val plotWidth = canvasWidth - marginLeft - marginRight
                    val plotHeight = canvasHeight - marginTop - marginBottom

                    if (plotWidth > 0f && plotHeight > 0f) {
                        fun getPixelY(value: Double): Float =
                            marginTop + plotHeight - ((value / maxY) * plotHeight).toFloat()

                        // Draw horizontal grid lines & Y labels
                        val gridLineCount = 4
                        for (i in 0..gridLineCount) {
                            val ratio = i.toFloat() / gridLineCount
                            val yVal = ratio * maxY
                            val py = getPixelY(yVal)

                            drawLine(
                                color = Color.Gray.copy(alpha = 0.1f),
                                start = Offset(marginLeft, py),
                                end = Offset(canvasWidth - marginRight, py),
                                strokeWidth = 1f
                            )

                            drawText(
                                textMeasurer = textMeasurer,
                                text = "R$ ${yVal.toInt()}",
                                style = TextStyle(color = Color.Gray, fontSize = 8.sp),
                                topLeft = Offset(4f, py - 6.dp.toPx())
                            )
                        }

                        if (selectedViewTab == 0) {
                            // RENDER HISTORICAL VIEW
                            val colWidth = plotWidth / chartItems.size
                            val savingsPoints = mutableListOf<Offset>()

                            chartItems.forEachIndexed { idx, day ->
                                val centerX = marginLeft + (idx * colWidth) + (colWidth / 2f)

                                // Earnings Column
                                val barWidth = colWidth * 0.45f
                                val barHeight = ((day.earnings / maxY) * plotHeight).toFloat()
                                val barTop = marginTop + plotHeight - barHeight

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            AccentGreen,
                                            AccentGreen.copy(alpha = 0.2f)
                                        )
                                    ),
                                    topLeft = Offset(centerX - barWidth / 2f, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )

                                val pySavings = getPixelY(day.savings)
                                savingsPoints.add(Offset(centerX, pySavings))

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = day.dateLabel,
                                    style = TextStyle(color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    topLeft = Offset(centerX - 12.dp.toPx(), marginTop + plotHeight + 4.dp.toPx())
                                )
                            }

                            // Savings Line (Smooth bezier)
                            if (savingsPoints.isNotEmpty()) {
                                val linePath = Path().apply {
                                    moveTo(savingsPoints[0].x, savingsPoints[0].y)
                                    for (i in 1 until savingsPoints.size) {
                                        val prev = savingsPoints[i - 1]
                                        val curr = savingsPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePath,
                                    color = AccentBlue.copy(alpha = 0.7f),
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                savingsPoints.forEach { point ->
                                    drawCircle(
                                        color = DarkSlateBg,
                                        radius = 4.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = AccentBlue,
                                        radius = 2.5.dp.toPx(),
                                        center = point
                                    )
                                }
                            }

                            // Interactive tooltip
                            if (isInteracting) {
                                touchPoint?.let { pos ->
                                    if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight) {
                                        val hoveredIdx = (((pos.x - marginLeft) / plotWidth) * chartItems.size)
                                            .toInt()
                                            .coerceIn(0, chartItems.size - 1)

                                        val dayData = chartItems[hoveredIdx]
                                        val cursorX = marginLeft + (hoveredIdx * colWidth) + (colWidth / 2f)

                                        drawLine(
                                            color = AccentBlue.copy(alpha = 0.4f),
                                            start = Offset(cursorX, marginTop),
                                            end = Offset(cursorX, marginTop + plotHeight),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )

                                        val tooltipText = String.format(
                                            Locale.US,
                                            "%s\nGanhos: R$ %.2f\nSalvo IA: R$ %.2f\nRotas: %da / %dr\nDist.: %.1f km",
                                            dayData.dateLabel,
                                            dayData.earnings,
                                            dayData.savings,
                                            dayData.acceptedCount,
                                            dayData.rejectedCount,
                                            dayData.totalKm
                                        )

                                        val textLayoutResult = textMeasurer.measure(
                                            text = tooltipText,
                                            style = TextStyle(color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                                        )

                                        val tooltipWidth = textLayoutResult.size.width + 12f
                                        val tooltipHeight = textLayoutResult.size.height + 10f

                                        var tooltipX = cursorX - tooltipWidth / 2f
                                        val tooltipY = marginTop + 6f

                                        tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)

                                        drawRoundRect(
                                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                        drawRoundRect(
                                            color = AccentBlue,
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )

                                        drawText(
                                            textLayoutResult = textLayoutResult,
                                            topLeft = Offset(tooltipX + 6f, tooltipY + 5f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // RENDER FORECAST VIEW (Amber themed glowing predictive area chart)
                            val colWidth = plotWidth / forecastItems.size
                            val baselinePoints = mutableListOf<Offset>()
                            val potentialPoints = mutableListOf<Offset>()

                            forecastItems.forEachIndexed { idx, day ->
                                val centerX = marginLeft + (idx * colWidth) + (colWidth / 2f)

                                val pyBase = getPixelY(day.baselineEarnings)
                                val pyPot = getPixelY(day.potentialEarnings)

                                baselinePoints.add(Offset(centerX, pyBase))
                                potentialPoints.add(Offset(centerX, pyPot))

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = day.dateLabel,
                                    style = TextStyle(color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    topLeft = Offset(centerX - 12.dp.toPx(), marginTop + plotHeight + 4.dp.toPx())
                                )
                            }

                            // Draw Potential Area Gradient (Glowing forecast fill)
                            if (potentialPoints.isNotEmpty()) {
                                val areaPath = Path().apply {
                                    moveTo(potentialPoints[0].x, potentialPoints[0].y)
                                    for (i in 1 until potentialPoints.size) {
                                        val prev = potentialPoints[i - 1]
                                        val curr = potentialPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                    lineTo(potentialPoints.last().x, marginTop + plotHeight)
                                    lineTo(potentialPoints.first().x, marginTop + plotHeight)
                                    close()
                                }

                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            AccentAmber.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )

                                // Draw Potential curve
                                val linePathPot = Path().apply {
                                    moveTo(potentialPoints[0].x, potentialPoints[0].y)
                                    for (i in 1 until potentialPoints.size) {
                                        val prev = potentialPoints[i - 1]
                                        val curr = potentialPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePathPot,
                                    color = AccentAmber,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Draw Baseline dotted line
                                val linePathBase = Path().apply {
                                    moveTo(baselinePoints[0].x, baselinePoints[0].y)
                                    for (i in 1 until baselinePoints.size) {
                                        val prev = baselinePoints[i - 1]
                                        val curr = baselinePoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePathBase,
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                )

                                // Circles for potential points
                                potentialPoints.forEach { point ->
                                    drawCircle(
                                        color = DarkSlateBg,
                                        radius = 4.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = AccentAmber,
                                        radius = 2.5.dp.toPx(),
                                        center = point
                                    )
                                }
                            }

                            // Interactive tooltip for Forecast mode
                            if (isInteracting) {
                                touchPoint?.let { pos ->
                                    if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight) {
                                        val hoveredIdx = (((pos.x - marginLeft) / plotWidth) * forecastItems.size)
                                            .toInt()
                                            .coerceIn(0, forecastItems.size - 1)

                                        val dayData = forecastItems[hoveredIdx]
                                        val cursorX = marginLeft + (hoveredIdx * colWidth) + (colWidth / 2f)

                                        // Draw vertical dash cursor
                                        drawLine(
                                            color = AccentAmber.copy(alpha = 0.4f),
                                            start = Offset(cursorX, marginTop),
                                            end = Offset(cursorX, marginTop + plotHeight),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )

                                        val tooltipText = String.format(
                                            Locale.US,
                                            "Previsão: %s\nPotencial: R$ %.2f\nSem IA (Base): R$ %.2f\nLucro Extra: +R$ %.2f\nEficiência: +%.1f%%\nRotas IA: %d",
                                            dayData.dateLabel,
                                            dayData.potentialEarnings,
                                            dayData.baselineEarnings,
                                            dayData.potentialEarnings - dayData.baselineEarnings,
                                            dayData.percentageGain,
                                            dayData.recommendedRoutesCount
                                        )

                                        val textLayoutResult = textMeasurer.measure(
                                            text = tooltipText,
                                            style = TextStyle(color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                                        )

                                        val tooltipWidth = textLayoutResult.size.width + 12f
                                        val tooltipHeight = textLayoutResult.size.height + 10f

                                        var tooltipX = cursorX - tooltipWidth / 2f
                                        val tooltipY = marginTop + 6f

                                        tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)

                                        drawRoundRect(
                                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                        drawRoundRect(
                                            color = AccentAmber,
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )

                                        drawText(
                                            textLayoutResult = textLayoutResult,
                                            topLeft = Offset(tooltipX + 6f, tooltipY + 5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = if (selectedViewTab == 0) "💡 Arraste no gráfico para ver detalhes do dia" else "🔮 Prospecção baseada nas suas regras de km, filtros e auto-rejeição ativos",
                color = Color.Gray,
                fontSize = 8.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
