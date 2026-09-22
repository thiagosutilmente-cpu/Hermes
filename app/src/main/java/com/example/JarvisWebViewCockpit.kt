package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.util.Locale

/**
 * Ponte bidirecional JavaScript <-> Kotlin (JavascriptInterface).
 * Permite que a SPA HTML5 do Jarvis Cockpit acione recursos nativos:
 * - Fala neural TTS (Hands-Free no capacete)
 * - Vibração háptica tática
 * - Toques sonoros de alta prioridade
 * - Telemetria de alta precisão (GPS e velocímetro Haversine)
 * - Trava de segurança de velocidade (> 10 km/h)
 */
class JarvisJavascriptBridge(
    private val context: Context,
    private val onToggleSunlightMode: () -> Unit,
    private val onOfferAccepted: (String) -> Unit,
    private val onOfferDeclined: (String) -> Unit
) {
    companion object {
        private const val TAG = "JarvisJSBridge"
    }

    @JavascriptInterface
    fun speak(text: String?) {
        if (!text.isNullOrBlank()) {
            NeuralVoiceManager.speak(context, text)
            Log.d(TAG, "JS acionou voz TTS: $text")
        }
    }

    @JavascriptInterface
    fun triggerHaptic(type: String?) {
        when (type?.lowercase(Locale.ROOT)) {
            "accept", "success" -> HapticFeedbackHelper.vibrateAccept(context)
            "decline", "error" -> HapticFeedbackHelper.vibrateDecline(context)
            "tap" -> HapticFeedbackHelper.vibrateTap(context)
            "warning", "alert" -> HapticFeedbackHelper.vibrateHighPriorityOffer(context)
            else -> HapticFeedbackHelper.vibrateTap(context)
        }
    }

    @JavascriptInterface
    fun playAlertSound(soundType: String?) {
        val sound = when (soundType?.lowercase(Locale.ROOT)) {
            "sonar", "radar" -> NotificationSoundType.CHIME_MELODIC
            "urgent", "rush" -> NotificationSoundType.TACTICAL_URGENT
            else -> NotificationSoundType.CHIME_MELODIC
        }
        CustomSoundPlayer.previewSound(context, sound)
    }

    @JavascriptInterface
    fun toggleSunlightMode() {
        onToggleSunlightMode()
    }

    @JavascriptInterface
    fun acceptOffer(offerId: String?) {
        val id = offerId ?: "offer_web"
        HapticFeedbackHelper.vibrateAccept(context)
        onOfferAccepted(id)
    }

    @JavascriptInterface
    fun declineOffer(offerId: String?) {
        val id = offerId ?: "offer_web"
        HapticFeedbackHelper.vibrateDecline(context)
        onOfferDeclined(id)
    }

    @JavascriptInterface
    fun getTelemetryJson(): String {
        val telemetry = LocationForegroundService.telemetryState.value
        val json = JSONObject().apply {
            put("latitude", telemetry.latitude)
            put("longitude", telemetry.longitude)
            put("speedKmh", telemetry.speedKmh)
            put("isSafetyLockActive", telemetry.isSafetyLockActive)
            put("accuracyMeters", telemetry.accuracyMeters.toDouble())
            put("timestamp", telemetry.timestamp)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun isSpeedLocked(): Boolean {
        return LocationForegroundService.telemetryState.value.isSafetyLockActive
    }
}

/**
 * Composable que renderiza a WebView otimizada com HTML5, WebSockets,
 * LocalStorage, Geolocalização injetada e bridge nativa do Jarvis.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JarvisWebViewCockpit(
    isNightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onCloseWebView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by LocationForegroundService.telemetryState.collectAsState()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var webTitle by remember { mutableStateOf("Jarvis Neural Cockpit") }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Injeta telemetria em tempo real no contexto da página web via evaluateJavascript
    LaunchedEffect(telemetry) {
        webViewInstance?.let { wv ->
            val json = JSONObject().apply {
                put("speed", telemetry.speedKmh)
                put("lat", telemetry.latitude)
                put("lng", telemetry.longitude)
                put("locked", telemetry.isSafetyLockActive)
            }
            val jsCode = "if (window.onRadarTelemetryUpdate) { window.onRadarTelemetryUpdate($json); }"
            wv.evaluateJavascript(jsCode, null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Barra Superior de Controle da WebView
        Surface(
            color = DarkCardElevated,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isSafetyLockActive) Color(0xFFFF5252) else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "WEBVIEW COCKPIT SPA",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (telemetry.isSafetyLockActive) "🔒 Trava > 10 km/h Ativa" else "⚡ Bridge JS/Kotlin Conectada • ${String.format(Locale.GERMANY, "%.1f", telemetry.speedKmh)} km/h",
                            color = if (telemetry.isSafetyLockActive) Color(0xFFFF5252) else NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Botão Recarregar
                    OutlinedButton(
                        onClick = {
                            loadError = null
                            isLoading = true
                            webViewInstance?.reload()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("↻ Recarregar", fontSize = 10.sp, color = TextLight)
                    }

                    // Botão Voltar para HUD Nativo
                    Button(
                        onClick = onCloseWebView,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkCard,
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text("✕ Voltar ao HUD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Indicador de Carregamento
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF222222))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .background(NeonGreen)
                )
            }
        }

        // Área Central: WebView Nativa
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Configurações Otimizadas de Motor WebKit
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            setGeolocationEnabled(true)
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mediaPlaybackRequiresUserGesture = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                        }

                        // Registra a Ponte JavascriptInterface
                        val bridge = JarvisJavascriptBridge(
                            context = ctx,
                            onToggleSunlightMode = onToggleNightMode,
                            onOfferAccepted = { offerId ->
                                Toast.makeText(ctx, "Oferta $offerId aceita via Web Cockpit!", Toast.LENGTH_SHORT).show()
                            },
                            onOfferDeclined = { offerId ->
                                Toast.makeText(ctx, "Oferta $offerId recusada via Web Cockpit.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        addJavascriptInterface(bridge, "JarvisNative")

                        webChromeClient = object : WebChromeClient() {
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                // Concede permissão de geolocalização nativa para o mapa web
                                callback?.invoke(origin, true, false)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) webTitle = title
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d("JarvisWebConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()}")
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Injeta listener inicial de telemetria e tema
                                val themeMode = if (isNightMode) "dark" else "light"
                                val initScript = """
                                    window.isJarvisAndroidApp = true;
                                    document.documentElement.setAttribute('data-theme', '$themeMode');
                                    console.log('Jarvis Native Bridge registrada com sucesso.');
                                """.trimIndent()
                                evaluateJavascript(initScript, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    // Se o servidor local estiver offline, carrega o Cockpit SPA HTML embutido
                                    loadDataWithBaseURL(
                                        "http://127.0.0.1:5000",
                                        getEmbeddedFallbackHtml(isNightMode),
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        }

                        // Carrega servidor Flask/SPA local na porta 5000 ou fallback local
                        loadUrl("http://127.0.0.1:5000")
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Trava de Velocidade Visual sobre a WebView quando velocidade ultrapassar 10 km/h
            if (telemetry.isSafetyLockActive) {
                Surface(
                    color = Color.Black.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A1111))
                                .border(2.dp, Color(0xFFFF5252), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔒", fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "TRAVA DE PILOTAGEM ATIVA",
                            color = Color(0xFFFF5252),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "${String.format(Locale.GERMANY, "%.1f", telemetry.speedKmh)} km/h • LIMITE SEGURO DE 10 km/h EXCEDIDO",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "A tela tátil do Cockpit foi temporariamente bloqueada para evitar acidentes no trânsito.\n\nUse os comandos de voz no intercomunicador do capacete:\n\"Jarvis, aceitar\" ou \"Jarvis, recusar\".",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gera o template SPA HTML5/CSS3/JS de contingência offline caso o backend remoto não responda.
 */
private fun getEmbeddedFallbackHtml(isNight: Boolean): String {
    val bg = if (isNight) "#0B0E14" else "#F4F6F9"
    val card = if (isNight) "#161B22" else "#FFFFFF"
    val text = if (isNight) "#F0F6FC" else "#1A202C"

    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <title>Radar Coordinator — Jarvis Cockpit SPA</title>
            <style>
                body {
                    margin: 0;
                    padding: 16px;
                    background-color: $bg;
                    color: $text;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                .hud-card {
                    background: $card;
                    border: 1px solid #00FF8844;
                    border-radius: 14px;
                    padding: 16px;
                    margin-bottom: 12px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
                }
                .title {
                    font-size: 14px;
                    font-weight: 900;
                    color: #00FF88;
                    letter-spacing: 1px;
                }
                .speed-val {
                    font-size: 38px;
                    font-weight: 900;
                    color: #FFFFFF;
                }
                .btn {
                    display: inline-block;
                    width: 100%;
                    padding: 14px;
                    margin-top: 8px;
                    background: #00FF88;
                    color: #0B0E14;
                    text-align: center;
                    font-weight: 900;
                    border-radius: 10px;
                    border: none;
                    font-size: 14px;
                    cursor: pointer;
                }
                .btn-decline {
                    background: #FF5252;
                    color: #FFF;
                }
            </style>
        </head>
        <body>
            <div class="hud-card">
                <div class="title">⚡ JARVIS NEURAL COCKPIT (MODO SPA EMBUTIDO)</div>
                <p style="font-size: 11px; opacity: 0.8;">Ponte nativa Android conectada ao WebKit com telemetria contínua.</p>
                <div class="speed-val" id="speedDisplay">0.0 <span style="font-size: 14px;">km/h</span></div>
                <div style="font-size: 12px; color: #00FF88;" id="statusText">GPS Fused Haversine Operacional</div>
            </div>

            <div class="hud-card">
                <div class="title">💰 MELHOR OFERTA RADAR (iFood / Rappi)</div>
                <div style="font-size: 18px; font-weight: bold; margin-top: 6px;">Madero Prime • R$ 38,50</div>
                <div style="font-size: 12px; color: #00FF88; font-weight: bold;">R$ 7,70 / km (Excepcional)</div>
                <button class="btn" onclick="acceptCurrent()">✅ ACEITAR CORRIDA VIA BRIDGE</button>
                <button class="btn btn-decline" onclick="declineCurrent()">❌ RECUSAR CORRIDA</button>
            </div>

            <script>
                function acceptCurrent() {
                    if (window.JarvisNative) {
                        window.JarvisNative.speak("Corrida aceita pelo cockpit web.");
                        window.JarvisNative.triggerHaptic("accept");
                        window.JarvisNative.acceptOffer("offer_spa_1");
                    }
                }
                function declineCurrent() {
                    if (window.JarvisNative) {
                        window.JarvisNative.speak("Corrida recusada.");
                        window.JarvisNative.triggerHaptic("decline");
                        window.JarvisNative.declineOffer("offer_spa_1");
                    }
                }
                window.onRadarTelemetryUpdate = function(data) {
                    if (data && data.speed !== undefined) {
                        document.getElementById('speedDisplay').innerHTML = data.speed.toFixed(1) + ' <span style="font-size: 14px;">km/h</span>';
                    }
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}
