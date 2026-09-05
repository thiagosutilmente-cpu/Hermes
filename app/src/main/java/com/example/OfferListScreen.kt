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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var offers by remember { mutableStateOf(initialOffers) }
    var minPriceFilter by remember { mutableFloatStateOf(0f) }

    // Estados para controle do SpeechRecognizer
    var isListening by remember { mutableStateOf(false) }
    var lastVoiceCommand by remember { mutableStateOf<String?>(null) }
    var voiceStatusFeedback by remember { mutableStateOf("Toque no microfone para comandos de voz ('Aceitar' ou 'Recusar')") }

    val filteredOffers by remember(offers, minPriceFilter) {
        derivedStateOf {
            offers.filter { it.valor >= minPriceFilter }
        }
    }

    val quickFilterValues = listOf(0f, 15f, 20f, 25f, 30f)

    // Inicialização do SpeechRecognizer nativo do Android
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    // Processa os comandos de voz reconhecidos
    fun processVoiceCommand(command: String) {
        val cleanCommand = command.trim().lowercase(Locale.ROOT)
        lastVoiceCommand = command

        val targetOffer = filteredOffers.firstOrNull()

        if (targetOffer == null) {
            voiceStatusFeedback = "Comando '$command' recebido, mas não há ofertas ativas."
            Toast.makeText(context, voiceStatusFeedback, Toast.LENGTH_SHORT).show()
            return
        }

        when {
            cleanCommand.contains("aceit") || cleanCommand.contains("sim") || cleanCommand.contains("pegar") || cleanCommand.contains("ok") -> {
                onAcceptOffer(targetOffer)
                offers = offers.filter { it.id != targetOffer.id }
                voiceStatusFeedback = "✅ Oferta de ${targetOffer.nomeRestaurante} ACEITA por voz!"
                Toast.makeText(context, voiceStatusFeedback, Toast.LENGTH_SHORT).show()
            }
            cleanCommand.contains("recus") || cleanCommand.contains("rejeit") || cleanCommand.contains("não") || cleanCommand.contains("nao") || cleanCommand.contains("passar") -> {
                onDeclineOffer(targetOffer)
                offers = offers.filter { it.id != targetOffer.id }
                voiceStatusFeedback = "❌ Oferta de ${targetOffer.nomeRestaurante} RECUSADA por voz!"
                Toast.makeText(context, voiceStatusFeedback, Toast.LENGTH_SHORT).show()
            }
            else -> {
                voiceStatusFeedback = "Comando '$command' não reconhecido. Diga 'Aceitar' ou 'Recusar'."
                Toast.makeText(context, voiceStatusFeedback, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Inicia a escuta contínua / pontual do microfone
    fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Reconhecimento de voz não suportado neste dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga 'Aceitar' ou 'Recusar'")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                voiceStatusFeedback = "🎙️ Ouvindo... Diga 'Aceitar' ou 'Recusar'"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nenhum comando detectado. Tente novamente."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo esgotado. Fale após clicar no microfone."
                    SpeechRecognizer.ERROR_AUDIO -> "Erro no microfone."
                    else -> "Erro no reconhecimento ($error)."
                }
                voiceStatusFeedback = errorMsg
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()
                if (!recognizedText.isNullOrBlank()) {
                    processVoiceCommand(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    // Lançador de permissão do microfone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Permissão de áudio necessária para comandos de voz", Toast.LENGTH_SHORT).show()
        }
    }

    // Libera o SpeechRecognizer ao sair da tela
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

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

    Column(
        modifier = modifier
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RADAR DE OFERTAS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (minPriceFilter > 0f) {
                        "${filteredOffers.size} de ${offers.size} ofertas (Min R$ ${String.format(Locale.GERMANY, "%.2f", minPriceFilter.toDouble())})"
                    } else {
                        "${offers.size} ofertas disponíveis"
                    },
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botão de Reconhecimento de Voz
                IconButton(
                    onClick = {
                        if (isListening) {
                            stopListening()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .scale(if (isListening) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(if (isListening) NeonGreen else CardSurface)
                        .border(
                            1.dp,
                            if (isListening) NeonGreen else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .testTag("btn_voice_recognition")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isListening) "Ouvindo comandos de voz" else "Ativar comandos de voz",
                        tint = if (isListening) DarkBackground else if (speechRecognizer != null) CyberCyan else TextMuted
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

        // Barra de Status do Reconhecimento de Voz
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isListening) NeonGreen.copy(alpha = 0.12f) else CardSurface)
                .border(
                    width = 0.5.dp,
                    color = if (isListening) NeonGreen else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isListening) "🎙️" else "🤖",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = voiceStatusFeedback,
                    color = if (isListening) NeonGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Painel de Filtro de Valor Mínimo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtro de Valor Mínimo:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (minPriceFilter == 0f) "Todos os valores" else "≥ R$ ${String.format(Locale.GERMANY, "%.2f", minPriceFilter.toDouble())}",
                    color = if (minPriceFilter > 0f) NeonGreen else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Slider Interativo de Valor Mínimo
            Slider(
                value = minPriceFilter,
                onValueChange = { minPriceFilter = it },
                valueRange = 0f..40f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_min_price_filter")
            )

            // Chips de Seleção Rápida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickFilterValues.forEach { filterVal ->
                    val isSelected = (minPriceFilter == filterVal)
                    FilterChip(
                        selected = isSelected,
                        onClick = { minPriceFilter = filterVal },
                        label = {
                            Text(
                                text = if (filterVal == 0f) "Todos" else "≥ R$ ${filterVal.toInt()}",
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
                        modifier = Modifier.testTag("chip_filter_${filterVal.toInt()}")
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
                    DeliveryOfferCard(
                        offer = offer,
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
        tempoEstimado = 18
    ),
    DeliveryOffer(
        id = "stk_02",
        nomeRestaurante = "McDonald's - Henrique Schaumann",
        valor = 15.00,
        distancia = 2.8,
        tempoEstimado = 12
    ),
    DeliveryOffer(
        id = "stk_03",
        nomeRestaurante = "Starbucks - Shopping Frei Caneca",
        valor = 18.00,
        distancia = 3.1,
        tempoEstimado = 14
    ),
    DeliveryOffer(
        id = "stk_04",
        nomeRestaurante = "Pizza Hut - Jardins",
        valor = 26.50,
        distancia = 4.0,
        tempoEstimado = 20
    ),
    DeliveryOffer(
        id = "stk_05",
        nomeRestaurante = "Madero Container - Alameda Santos",
        valor = 22.00,
        distancia = 3.5,
        tempoEstimado = 15
    ),
    DeliveryOffer(
        id = "stk_06",
        nomeRestaurante = "Habib's - Rebouças",
        valor = 12.00,
        distancia = 3.9,
        tempoEstimado = 16
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
