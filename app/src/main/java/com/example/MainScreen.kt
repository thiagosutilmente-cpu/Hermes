package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

// -------------------------------------------------------------------------
// DESIGN SYSTEM - CORES DO COCKPIT
// -------------------------------------------------------------------------
private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val DarkBg = Color(0xFF0A0A0F)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)
private val RedLock = Color(0xFFFF3366)
private val AmberWarn = Color(0xFFFFB800)
private val IFoodColor = Color(0xFFEA1D2C)
private val RappiColor = Color(0xFFFF441F)
private val UberColor = Color(0xFF1E88E5)

/**
 * Modelo de dados para Ofertas de Entrega
 */
data class DeliveryOfferItem(
    val id: String,
    val appName: String,
    val restaurant: String,
    val deliveryAddress: String,
    val totalPayout: Double,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val isMultiAppBundle: Boolean = false
) {
    val gainPerKm: Double get() = if (distanceKm > 0) totalPayout / distanceKm else 0.0
}

/**
 * Tela Principal (Main Screen) em Jetpack Compose:
 * - Scrollable list (LazyColumn) para delivery offers
 * - Floating Action Button (FAB) para comandos de voz
 * - Status bar destacando o estado do bloqueio de segurança por velocidade (Active / Inactive)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: SpeedSafetyViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostStateState() }
    val coroutineScope = rememberCoroutineScope()

    // 1. Telemetria em Tempo Real via SpeedSafetyViewModel conectado ao LocationService
    val speedUiState by viewModel.uiState.collectAsState()
    val currentSpeedKmh = speedUiState.currentSpeedKmh
    val isSafetyLockActive = speedUiState.isSafetyLockActive

    // 2. Estado do Comando de Voz (FAB)
    var isVoiceListening by remember { mutableStateOf(false) }
    var lastVoiceTranscript by remember { mutableStateOf("Toque no microfone flutuante para falar") }

    // 3. Lista Rolável de Ofertas
    var offersList by remember {
        mutableStateOf(
            listOf(
                DeliveryOfferItem(
                    id = "stk_101",
                    appName = "iFood + Rappi",
                    restaurant = "Burger King Paulista & Pizza Hut",
                    deliveryAddress = "Av. Paulista, 1842 - Torre Norte",
                    totalPayout = 34.50,
                    distanceKm = 4.2,
                    estimatedMinutes = 19,
                    isMultiAppBundle = true
                ),
                DeliveryOfferItem(
                    id = "stk_102",
                    appName = "iFood",
                    restaurant = "Madero Container - Jardins",
                    deliveryAddress = "Rua Oscar Freire, 1140",
                    totalPayout = 21.00,
                    distanceKm = 3.1,
                    estimatedMinutes = 14,
                    isMultiAppBundle = false
                ),
                DeliveryOfferItem(
                    id = "stk_103",
                    appName = "Rappi Turbo",
                    restaurant = "Bacio di Latte - Bela Cintra",
                    deliveryAddress = "Alameda Santos, 980 - Apto 82",
                    totalPayout = 16.50,
                    distanceKm = 1.8,
                    estimatedMinutes = 8,
                    isMultiAppBundle = false
                ),
                DeliveryOfferItem(
                    id = "stk_104",
                    appName = "Uber Direct",
                    restaurant = "Drogaria São Paulo - Consolação",
                    deliveryAddress = "Rua Augusta, 2200",
                    totalPayout = 14.00,
                    distanceKm = 2.4,
                    estimatedMinutes = 11,
                    isMultiAppBundle = false
                ),
                DeliveryOfferItem(
                    id = "stk_105",
                    appName = "iFood + Uber",
                    restaurant = "Subway Paraíso & Starbucks",
                    deliveryAddress = "Rua Vergueiro, 1500",
                    totalPayout = 29.80,
                    distanceKm = 3.9,
                    estimatedMinutes = 16,
                    isMultiAppBundle = true
                )
            )
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        containerColor = DarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg)
            ) {
                // Top App Bar com Branding
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RADAR COORDINATOR",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "JARVIS NEURAL COCKPIT",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBg
                    )
                )

                // -------------------------------------------------------------
                // SPEED-BASED SAFETY LOCK STATUS BAR (ACTIVE / INACTIVE)
                // -------------------------------------------------------------
                SpeedSafetyStatusBar(
                    isSafetyLockActive = isSafetyLockActive,
                    currentSpeedKmh = currentSpeedKmh,
                    onToggleTestSpeed = {
                        viewModel.toggleTestSpeed()
                    }
                )
            }
        },
        floatingActionButton = {
            // -------------------------------------------------------------
            // FLOATING ACTION BUTTON (FAB) FOR VOICE COMMANDS
            // -------------------------------------------------------------
            VoiceCommandFloatingActionButton(
                isListening = isVoiceListening,
                onClick = {
                    isVoiceListening = !isVoiceListening
                    lastVoiceTranscript = if (isVoiceListening) {
                        "Escutando fone Bluetooth... Diga 'Aceitar' ou 'Recusar'"
                    } else {
                        "Microfone pausado"
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Feedback do Comando de Voz
            AnimatedVisibility(
                visible = isVoiceListening || lastVoiceTranscript.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("voice_feedback_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isVoiceListening) NeonGreen.copy(alpha = 0.6f) else CardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isVoiceListening) "🎙️" else "💬",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastVoiceTranscript,
                                color = if (isVoiceListening) NeonGreen else TextLight,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isVoiceListening) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        lastVoiceTranscript = "Voz: 'Aceitar' executado!"
                                        if (offersList.isNotEmpty()) {
                                            offersList = offersList.drop(1)
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, NeonGreen)
                                ) {
                                    Text("Aceitar", fontSize = 9.sp, color = NeonGreen)
                                }

                                OutlinedButton(
                                    onClick = {
                                        lastVoiceTranscript = "Voz: 'Recusar' executado!"
                                        if (offersList.isNotEmpty()) {
                                            offersList = offersList.drop(1)
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, RedLock)
                                ) {
                                    Text("Recusar", fontSize = 9.sp, color = RedLock)
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SCROLLABLE LIST FOR DELIVERY OFFERS (LAZY COLUMN)
            // -------------------------------------------------------------
            if (isSafetyLockActive) {
                // Modo Bloqueio Ativo (velocidade > 10 km/h)
                SafetyLockActivePlaceholder(currentSpeedKmh = currentSpeedKmh)
            } else if (offersList.isEmpty()) {
                // Placeholder de Lista Vazia
                EmptyOffersPlaceholder(
                    onReloadOffers = {
                        offersList = listOf(
                            DeliveryOfferItem("stk_101", "iFood + Rappi", "Burger King Paulista & Pizza Hut", "Av. Paulista, 1842", 34.50, 4.2, 19, true),
                            DeliveryOfferItem("stk_102", "iFood", "Madero Container", "Rua Oscar Freire, 1140", 21.00, 3.1, 14, false),
                            DeliveryOfferItem("stk_103", "Rappi Turbo", "Bacio di Latte", "Alameda Santos, 980", 16.50, 1.8, 8, false)
                        )
                    }
                )
            } else {
                Text(
                    text = "OFERTAS DISPONÍVEIS (${offersList.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("delivery_offers_scrollable_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(offersList, key = { it.id }) { offer ->
                        OfferCardItem(
                            offer = offer,
                            onAccept = {
                                offersList = offersList.filter { it.id != offer.id }
                                lastVoiceTranscript = "Oferta ${offer.restaurant} aceita!"
                            },
                            onDecline = {
                                offersList = offersList.filter { it.id != offer.id }
                                lastVoiceTranscript = "Oferta recusada."
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// STATUS BAR: SPEED-BASED SAFETY LOCK (ACTIVE / INACTIVE)
// -------------------------------------------------------------------------
@Composable
fun SpeedSafetyStatusBar(
    isSafetyLockActive: Boolean,
    currentSpeedKmh: Double,
    onToggleTestSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBackgroundColor by animateColorAsState(
        targetValue = if (isSafetyLockActive) RedLock.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.12f),
        label = "status_bar_bg"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (isSafetyLockActive) RedLock else NeonGreen,
        label = "indicator_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(barBackgroundColor)
            .border(
                1.dp,
                if (isSafetyLockActive) RedLock.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onToggleTestSpeed() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("speed_safety_status_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bolinha pulsante
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SAFETY LOCK: ",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSafetyLockActive) "ACTIVE" else "INACTIVE",
                            color = indicatorColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = if (isSafetyLockActive) {
                            "Movement detected (> 10 km/h) • Touch blocked"
                        } else {
                            "Vehicle stopped (<= 10 km/h) • Screen unlocked"
                        },
                        color = TextLight.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            // Velocímetro e botão de teste
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBg)
                    .border(0.8.dp, indicatorColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format(Locale.GERMANY, "%.0f km/h 🔄", currentSpeedKmh),
                    color = indicatorColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// FLOATING ACTION BUTTON FOR VOICE COMMANDS
// -------------------------------------------------------------------------
@Composable
fun VoiceCommandFloatingActionButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .scale(pulseScale)
            .testTag("voice_command_fab"),
        shape = CircleShape,
        containerColor = if (isListening) NeonGreen else Color(0xFF1E1E2C),
        contentColor = if (isListening) DarkBg else TextLight,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(
                    2.dp,
                    if (isListening) NeonGreen else Color(0xFF333346),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isListening) "🎙️" else "🎤",
                fontSize = 24.sp
            )
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENTE DO CARD DE OFERTA DE ENTREGA
// -------------------------------------------------------------------------
@Composable
fun OfferCardItem(
    offer: DeliveryOfferItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (offer.isMultiAppBundle) NeonGreen.copy(alpha = 0.5f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag("delivery_offer_item_${offer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Linha superior: Origem do App & Valor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = offer.appName,
                        color = when {
                            offer.appName.contains("iFood") -> IFoodColor
                            offer.appName.contains("Rappi") -> RappiColor
                            else -> UberColor
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )

                    if (offer.isMultiAppBundle) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(0.5.dp, NeonGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✨ MESCLADA",
                                color = NeonGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Text(
                    text = String.format(Locale.GERMANY, "R$ %.2f", offer.totalPayout),
                    color = NeonGreen,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Restaurante & Endereço
            Text(
                text = offer.restaurant,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "➔ ${offer.deliveryAddress}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Métricas da rota: km, minutos e ganho por km
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${offer.distanceKm} km • ${offer.estimatedMinutes} min",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format(Locale.GERMANY, "R$ %.2f / km", offer.gainPerKm),
                    color = if (offer.gainPerKm >= 5.0) NeonGreen else AmberWarn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botões Recusar / Aceitar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_decline_${offer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedLock.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedLock)
                ) {
                    Text("❌ RECUSAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("btn_accept_${offer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    )
                ) {
                    Text("✅ ACEITAR", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// PLACEHOLDER: SEGURANÇA ATIVA (VELOCIDADE > 10 KM/H)
// -------------------------------------------------------------------------
@Composable
fun SafetyLockActivePlaceholder(
    currentSpeedKmh: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0B13))
            .border(1.dp, RedLock.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(24.dp)
            .testTag("safety_lock_active_placeholder"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(RedLock.copy(alpha = 0.15f))
                .border(2.dp, RedLock, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Safety Lock Active",
                tint = RedLock,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SAFETY LOCK ACTIVE",
            color = RedLock,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${String.format(Locale.GERMANY, "%.0f", currentSpeedKmh)} KM/H DETECTED",
            color = TextLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Delivery offers list is locked while in motion to prevent road accidents. Use the floating voice button or helmet mic to accept calls hands-free.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBg)
                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🎙️ Hands-Free Voice Commands Active",
                color = NeonGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------
// PLACEHOLDER: LISTA VAZIA DE OFERTAS
// -------------------------------------------------------------------------
@Composable
fun EmptyOffersPlaceholder(
    onReloadOffers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(24.dp)
            .testTag("empty_offers_placeholder"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🎯", fontSize = 42.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Varredura Neural Ativa",
            color = TextLight,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Nenhuma oferta pendente no momento. Monitorando iFood, Rappi e Uber...",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onReloadOffers,
            colors = ButtonDefaults.buttonColors(
                containerColor = CardSurface,
                contentColor = NeonGreen
            ),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
        ) {
            Text("Simular Novas Ofertas 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------
// CLASSE AUXILIAR DE SNACKBAR STATE
// -------------------------------------------------------------------------
@Composable
fun SnackbarHostStateState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}

// -------------------------------------------------------------------------
// PREVIEW DO JETPACK COMPOSE
// -------------------------------------------------------------------------
@Preview(
    name = "Main Screen - Offers List & Voice FAB",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun MainScreenScrollableOffersPreview() {
    MaterialTheme {
        Surface(color = DarkBg) {
            MainScreen()
        }
    }
}
