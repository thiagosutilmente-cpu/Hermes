package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val DarkBackground = Color(0xFF0A0A0F)
private val CardSurface = Color(0xFF13131F)
private val NeonGreen = Color(0xFF00FF88)
private val TextMuted = Color(0xFF8E8EA0)
private val CyberCyan = Color(0xFF00D2FF)
private val DangerRed = Color(0xFFFF4757)

/**
 * Tela principal de listagem de ofertas de entrega.
 * Integra reconhecimento de voz via [SpeechRecognizer] para controle hands-free (Aceitar / Recusar).
 *
 * @param initialOffers Lista inicial de ofertas de entrega.
 * @param onAcceptOffer Callback acionado ao aceitar uma oferta.
 * @param onDeclineOffer Callback acionado ao recusar uma oferta.
 * @param modifier Modificador de layout.
 */
@Composable
fun OfferListScreen(
    initialOffers: List<DeliveryOffer> = getSampleDeliveryOffers(),
    onAcceptOffer: (DeliveryOffer) -> Unit = {},
    onDeclineOffer: (DeliveryOffer) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance(context) }
    val gpsLocation by locationManager.currentLocation.collectAsState()

    var offers by remember { mutableStateOf(initialOffers) }
    val savedCriteria = remember { FilterPreferencesManager.loadCriteria(context) }
    var minPriceFilter by remember { mutableFloatStateOf(savedCriteria.minValue.toFloat()) }
    var maxRadiusKm by remember { mutableFloatStateOf(savedCriteria.maxDistanceKm.toFloat()) }
    var isGeoRadiusEnabled by remember { mutableStateOf(savedCriteria.isGeoRadiusFilterActive) }
    var selectedHotspot by remember { mutableStateOf("Todos") }

    val driverLat = gpsLocation.latitude
    val driverLng = gpsLocation.longitude

    // Estados e controle de voz mãos-livres (Google Speech-to-Text)
    var lastVoiceCommand by remember { mutableStateOf<String?>(null) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var speechManager: HandsFreeSpeechManager? by remember { mutableStateOf(null) }

    val filteredOffers by remember(offers, minPriceFilter, maxRadiusKm, isGeoRadiusEnabled, selectedHotspot, driverLat, driverLng) {
        derivedStateOf {
            offers.filter { offer ->
                val passesValue = offer.valor >= minPriceFilter
                val distKm = offer.distanceTo(driverLat, driverLng)
                val passesRadius = !isGeoRadiusEnabled || distKm <= maxRadiusKm
                val passesHotspot = selectedHotspot == "Todos" || offer.poloGastronomico.contains(selectedHotspot, ignoreCase = true)
                passesValue && passesRadius && passesHotspot
            }
        }
    }

    val hotspotsList = listOf("Todos", "Paulista", "Jardins", "Pinheiros", "Faria Lima", "Moema")

    // Motor de Voz Neural para feedback por áudio no capacete (Text-to-Speech)
    var isTtsSpeaking by remember { mutableStateOf(false) }
    val neuralVoice = remember {
        NeuralVoiceManager(context).apply {
            onSpeechStarted = {
                isTtsSpeaking = true
                speechManager?.pauseTemporarilyForTts()
            }
            onSpeechFinished = {
                isTtsSpeaking = false
                speechManager?.resumeAfterTts()
            }
        }
    }

    fun handleAcceptTopOffer() {
        val targetOffer = filteredOffers.firstOrNull()
        if (targetOffer != null) {
            onAcceptOffer(targetOffer)
            offers = offers.filter { it.id != targetOffer.id }
            HapticFeedbackHelper.vibrateAccept(context)
            neuralVoice.announceAccept(targetOffer.nomeRestaurante, targetOffer.valor)
            Toast.makeText(context, "✅ Aceito por voz: ${targetOffer.nomeRestaurante} (R$ ${String.format(Locale.GERMANY, "%.2f", targetOffer.valor)})", Toast.LENGTH_SHORT).show()
        } else {
            neuralVoice.speak("Nenhuma oferta ativa no radar para aceitar.")
            Toast.makeText(context, "Nenhuma oferta no radar para aceitar", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDeclineTopOffer() {
        val targetOffer = filteredOffers.firstOrNull()
        if (targetOffer != null) {
            onDeclineOffer(targetOffer)
            offers = offers.filter { it.id != targetOffer.id }
            HapticFeedbackHelper.vibrateDecline(context)
            neuralVoice.announceDecline()
            Toast.makeText(context, "❌ Recusado por voz: ${targetOffer.nomeRestaurante}", Toast.LENGTH_SHORT).show()
        } else {
            neuralVoice.speak("Nenhuma oferta ativa para recusar.")
            Toast.makeText(context, "Nenhuma oferta no radar para recusar", Toast.LENGTH_SHORT).show()
        }
    }

    // Lançador de permissão do microfone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            speechManager?.startListening()
            neuralVoice.speak("Comandos viva-voz ativados. Diga aceitar ou recusar sem soltar o guidão.")
            Toast.makeText(context, "Google Speech-to-Text ativado! Diga 'Aceitar' ou 'Recusar'", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para comandos por voz", Toast.LENGTH_SHORT).show()
        }
    }

    // Inicialização do HandsFreeSpeechManager com API Google Speech-to-Text e LocationManager
    DisposableEffect(context) {
        val manager = HandsFreeSpeechManager(context) { command, spokenText ->
            lastVoiceCommand = spokenText
            HapticFeedbackHelper.vibrateVoiceCommandRecognized(context)
            when (command) {
                VoiceActionCommand.ACCEPT,
                VoiceActionCommand.ACCEPT_IFOOD,
                VoiceActionCommand.ACCEPT_RAPPI,
                VoiceActionCommand.ACCEPT_UBER,
                VoiceActionCommand.ACCEPT_99 -> {
                    handleAcceptTopOffer()
                }
                VoiceActionCommand.DECLINE,
                VoiceActionCommand.DECLINE_IFOOD,
                VoiceActionCommand.DECLINE_RAPPI,
                VoiceActionCommand.DECLINE_UBER,
                VoiceActionCommand.DECLINE_99 -> {
                    handleDeclineTopOffer()
                }
                VoiceActionCommand.READ_OFFER -> {
                    val target = filteredOffers.firstOrNull()
                    if (target != null) {
                        neuralVoice.announceDrivingHandsFreeOffer(
                            appName = target.appOrigem,
                            restaurant = target.nomeRestaurante,
                            value = target.valor,
                            distanceKm = target.distanceTo(driverLat, driverLng),
                            gainPerKm = target.ganhoPorKm
                        )
                    } else {
                        neuralVoice.speak("Nenhuma oferta disponível nos filtros atuais.")
                    }
                }
                VoiceActionCommand.FILTER_RAIN_PRESET -> {
                    minPriceFilter = 25f
                    maxRadiusKm = 4.0f
                    isGeoRadiusEnabled = true
                    neuralVoice.speak("Filtro de Chuva ativado. Piso mínimo de 25 reais e raio de 4 quilômetros.")
                }
                VoiceActionCommand.FILTER_SHORT_PRESET -> {
                    maxRadiusKm = 3.0f
                    isGeoRadiusEnabled = true
                    neuralVoice.speak("Filtro de Corridas Curtas ativado. Raio máximo de 3 quilômetros.")
                }
                VoiceActionCommand.FILTER_MIN_15 -> {
                    minPriceFilter = 15f
                    neuralVoice.speak("Piso mínimo alterado para 15 reais.")
                }
                VoiceActionCommand.FILTER_MIN_20 -> {
                    minPriceFilter = 20f
                    neuralVoice.speak("Piso mínimo alterado para 20 reais.")
                }
                VoiceActionCommand.FILTER_MIN_30 -> {
                    minPriceFilter = 30f
                    neuralVoice.speak("Piso mínimo alterado para 30 reais.")
                }
                VoiceActionCommand.FILTER_RESET -> {
                    minPriceFilter = 0f
                    maxRadiusKm = 6.0f
                    isGeoRadiusEnabled = true
                    selectedHotspot = "Todos"
                    neuralVoice.speak("Filtros redefinidos para o padrão.")
                }
                VoiceActionCommand.HELP -> {
                    neuralVoice.announceHelp()
                }
                VoiceActionCommand.CLOSE_SCREEN -> {
                    neuralVoice.speak("Voltando ao cockpit.")
                    onNavigateBack?.invoke()
                }
                else -> {}
            }
        }
        speechManager = manager

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            locationManager.startRealtimeLocationUpdates()
        }

        if (hasMicPermission) {
            manager.startListening()
        }

        onDispose {
            manager.destroy()
            locationManager.stopLocationUpdates()
            neuralVoice.shutdown()
        }
    }

    val rawVoiceState = speechManager?.state?.collectAsState()?.value
        ?: VoiceCommandState(isPermissionGranted = hasMicPermission)
    val currentVoiceState = remember(rawVoiceState, hasMicPermission) {
        rawVoiceState.copy(isPermissionGranted = hasMicPermission)
    }

    // Telemetria do Sensor de Velocidade Fused Location do LocationService
    val locationSpeedState by LocationService.globalLocationState.collectAsState()
    val isSpeedLocked = locationSpeedState.isSafetyLockActive || locationSpeedState.currentSpeedKmh > 10.0

    // Animação de pulso para quando o microfone estiver ouvindo
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
        // Cabeçalho da Tela com Botões de Voz e Atualização
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardSurface)
                        .testTag("btn_back_to_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar ao Cockpit",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RADAR DE OFERTAS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${filteredOffers.size} de ${offers.size} ofertas (Raio: ${if (isGeoRadiusEnabled) "≤ ${String.format(Locale.GERMANY, "%.1f", maxRadiusKm)}km" else "Livre"} • Piso: ≥ R$ ${String.format(Locale.GERMANY, "%.2f", minPriceFilter.toDouble())})",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botão de Reconhecimento de Voz
                IconButton(
                    onClick = {
                        if (currentVoiceState.isListening) {
                            speechManager?.stopListening()
                        } else {
                            if (hasMicPermission) {
                                speechManager?.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .scale(if (currentVoiceState.isListening) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(if (currentVoiceState.isListening) NeonGreen else CardSurface)
                        .border(
                            1.dp,
                            if (currentVoiceState.isListening) NeonGreen else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .testTag("btn_voice_recognition")
                ) {
                    Icon(
                        imageVector = if (currentVoiceState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (currentVoiceState.isListening) "Ouvindo comandos de voz" else "Ativar comandos de voz",
                        tint = if (currentVoiceState.isListening) DarkBackground else CyberCyan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botão de Refresh
                IconButton(
                    onClick = {
                        offers = getSampleDeliveryOffers()
                        minPriceFilter = 0f
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardSurface)
                        .testTag("btn_refresh_offer_list")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recarregar ofertas",
                        tint = NeonGreen
                    )
                }
            }
        }

        // Banner Viva-Voz do Google Speech-to-Text (Mãos no Guidão)
        VoiceCommandLiveBanner(
            voiceState = currentVoiceState,
            isSpeaking = isTtsSpeaking,
            onToggleListening = {
                if (currentVoiceState.isListening) {
                    speechManager?.stopListening()
                } else {
                    speechManager?.startListening()
                }
            },
            onRequestMicPermission = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onSimulateCommand = { simulatedText ->
                speechManager?.simulateVoiceCommand(simulatedText)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // PAINEL DE CONFIGURAÇÕES DE PREFERÊNCIAS DE OFERTAS (SETTINGS PANEL)
        DeliveryOfferSettingsPanel(
            currentMinValue = minPriceFilter.toDouble(),
            currentMaxRadiusKm = maxRadiusKm.toDouble(),
            onMinValueChange = { minPriceFilter = it.toFloat() },
            onMaxRadiusKmChange = { 
                maxRadiusKm = it.toFloat()
                isGeoRadiusEnabled = true
            },
            isRadiusLimitEnabled = isGeoRadiusEnabled,
            onToggleRadiusLimit = { isGeoRadiusEnabled = it },
            matchingOffersCount = filteredOffers.size,
            totalOffersCount = offers.size,
            onSavePreferences = {
                val currentCriteria = FilterPreferencesManager.loadCriteria(context)
                val updated = currentCriteria.copy(
                    minValue = minPriceFilter.toDouble(),
                    maxDistanceKm = maxRadiusKm.toDouble(),
                    maxPickupRadiusKm = maxRadiusKm.toDouble(),
                    isGeoRadiusFilterActive = isGeoRadiusEnabled
                )
                FilterPreferencesManager.saveCriteria(context, updated)
            },
            onResetDefaults = {
                minPriceFilter = 0f
                maxRadiusKm = 6.0f
                isGeoRadiusEnabled = true
                selectedHotspot = "Todos"
            },
            isCollapsible = true,
            initiallyExpanded = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card Complementar: Status GPS em Tempo Real & Filtro de Polo Gastronômico
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {

            // Indicador de Status do GPS em tempo real
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (gpsLocation.accuracy <= 50f) NeonGreen else CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS Ativo: ${String.format(Locale.US, "%.4f, %.4f", driverLat, driverLng)}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "±${gpsLocation.accuracy.toInt()}m",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SEÇÃO 3: POLO GASTRONÔMICO
            Text(
                text = "Polo Gastronômico:",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hotspotsList.forEach { hotspot ->
                    val isSelected = (selectedHotspot == hotspot)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedHotspot = hotspot },
                        label = {
                            Text(
                                text = hotspot,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkBackground,
                            labelColor = TextMuted,
                            selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                            selectedLabelColor = NeonGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("chip_hotspot_${hotspot.lowercase(Locale.ROOT)}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conteúdo: LazyColumn com scroll eficiente ou Empty State
        if (filteredOffers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎯", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (offers.isEmpty()) "Nenhuma oferta disponível" else "Nenhuma oferta com valor ≥ R$ ${String.format(Locale.GERMANY, "%.2f", minPriceFilter.toDouble())}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (offers.isEmpty()) "Toque no botão de atualizar acima para buscar novas entregas no radar." else "Tente reduzir o filtro de valor mínimo para exibir mais oportunidades.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lazy_delivery_offer_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(
                    items = filteredOffers,
                    key = { it.id }
                ) { offer ->
                    val dynamicDistance = offer.distanceTo(driverLat, driverLng)
                    val displayOffer = offer.copy(distancia = dynamicDistance)
                    DeliveryOfferCard(
                        offer = displayOffer,
                        onAccept = { accepted ->
                            onAcceptOffer(accepted)
                            offers = offers.filter { it.id != accepted.id }
                        },
                        onDecline = { declined ->
                            onDeclineOffer(declined)
                            offers = offers.filter { it.id != declined.id }
                        }
                    )
                }
            }
        }

        // Bloqueio Integral da Interface do OfferListScreen por Velocidade (> 10 km/h)
        SpeedSafetyLockOverlay(
            isLocked = isSpeedLocked,
            currentSpeedKmh = locationSpeedState.currentSpeedKmh,
            speedThresholdKmh = 10.0,
            isListeningVoice = currentVoiceState.isListening,
            lastVoiceCommand = currentVoiceState.lastRecognizedText,
            rmsDb = currentVoiceState.rmsDb,
            pendingOffer = filteredOffers.firstOrNull(),
            onSimulateVoiceCommand = { speechManager?.simulateVoiceCommand(it) },
            onSetSimulatedSpeed = { speed ->
                LocationService.updateSimulatedSpeed(speed, context)
            },
            onAcceptCurrentOffer = { handleAcceptTopOffer() },
            onDeclineCurrentOffer = { handleDeclineTopOffer() }
        )
    }
}

/**
 * Fornece dados de exemplo para preenchimento da lista e visualização de previews.
 */
fun getSampleDeliveryOffers(): List<DeliveryOffer> = listOf(
    DeliveryOffer(
        id = "stk_01",
        nomeRestaurante = "Burger King - Av. Paulista",
        valor = 33.00,
        distancia = 4.2,
        tempoEstimado = 18,
        latitude = -23.561684,
        longitude = -46.655981,
        poloGastronomico = "Paulista",
        appOrigem = "iFood"
    ),
    DeliveryOffer(
        id = "stk_02",
        nomeRestaurante = "McDonald's - Henrique Schaumann",
        valor = 15.00,
        distancia = 2.8,
        tempoEstimado = 12,
        latitude = -23.559800,
        longitude = -46.681200,
        poloGastronomico = "Pinheiros",
        appOrigem = "Rappi"
    ),
    DeliveryOffer(
        id = "stk_03",
        nomeRestaurante = "Starbucks - Shopping Frei Caneca",
        valor = 18.00,
        distancia = 3.1,
        tempoEstimado = 14,
        latitude = -23.553200,
        longitude = -46.652800,
        poloGastronomico = "Paulista",
        appOrigem = "iFood"
    ),
    DeliveryOffer(
        id = "stk_04",
        nomeRestaurante = "Pizza Hut - Jardins",
        valor = 26.50,
        distancia = 4.0,
        tempoEstimado = 20,
        latitude = -23.568210,
        longitude = -46.662150,
        poloGastronomico = "Jardins",
        appOrigem = "99Food"
    ),
    DeliveryOffer(
        id = "stk_05",
        nomeRestaurante = "Madero Container - Alameda Santos",
        valor = 22.00,
        distancia = 3.5,
        tempoEstimado = 15,
        latitude = -23.568910,
        longitude = -46.650120,
        poloGastronomico = "Paulista",
        appOrigem = "Uber Eats"
    ),
    DeliveryOffer(
        id = "stk_06",
        nomeRestaurante = "Habib's - Rebouças",
        valor = 12.00,
        distancia = 3.9,
        tempoEstimado = 16,
        latitude = -23.571200,
        longitude = -46.689000,
        poloGastronomico = "Pinheiros",
        appOrigem = "iFood"
    ),
    DeliveryOffer(
        id = "stk_07",
        nomeRestaurante = "Outback Steakhouse - Moema",
        valor = 38.50,
        distancia = 5.2,
        tempoEstimado = 22,
        latitude = -23.601200,
        longitude = -46.662100,
        poloGastronomico = "Moema",
        appOrigem = "iFood"
    ),
    DeliveryOffer(
        id = "stk_08",
        nomeRestaurante = "Pobre Juan - Faria Lima",
        valor = 45.00,
        distancia = 4.8,
        tempoEstimado = 20,
        latitude = -23.582300,
        longitude = -46.684100,
        poloGastronomico = "Faria Lima",
        appOrigem = "Rappi"
    ),
    DeliveryOffer(
        id = "stk_09",
        nomeRestaurante = "Coco Bambu - Anália Franco",
        valor = 52.00,
        distancia = 9.8,
        tempoEstimado = 35,
        latitude = -23.548200,
        longitude = -46.562100,
        poloGastronomico = "Anália Franco",
        appOrigem = "iFood"
    )
)

// ----------------------------------------------------
// PREVIEW PARA O ANDROID STUDIO
// ----------------------------------------------------
@Preview(
    name = "Offer List Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun OfferListScreenPreview() {
    MaterialTheme {
        Surface(color = DarkBackground) {
            OfferListScreen()
        }
    }
}
