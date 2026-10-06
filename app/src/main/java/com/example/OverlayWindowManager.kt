package com.example

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.hypot

/**
 * OverlayWindowManager
 *
 * Gerenciador nativo de interface flutuante (Bolha tática & Mini-HUD Compacto)
 * utilizando o [WindowManager] do Android.
 *
 * Funcionalidades da Bolha Flutuante Tática Aprimorada (HUD sobre iFood/99/Uber):
 * 1. Bolha Flutuante Compacta persistente com indicador de rentabilidade em tempo real.
 * 2. Card Flutuante Compacto de Inspeção de Corridas com cálculo imediato de R$/km:
 *    - Ganho real por km (ex: R$ 7,20/km 🟢 BOA ou R$ 2,80/km 🔴 PREJUÍZO).
 *    - Indicador visual luminoso de Rota Dupla Compatível (sobreposição de pedidos).
 *    - Ações em 1 Toque: Botões gigantes táteis de ACEITAR ou DISPENSAR sem precisar alternar manualmente.
 * 3. Ancoragem magnética suave nas bordas laterais da tela (Snap-to-Edge).
 */
class OverlayWindowManager private constructor(private val context: Context) {

    data class JarvisOverlayStatus(
        val isOnline: Boolean = true,
        val headline: String = "JARVIS OPERACIONAL",
        val activeApp: String = "Monitorando Apps",
        val profitPerKm: String = "R$ 4,50/km",
        val blitzAlert: String = "Raio Seguro (Sem Blitz)",
        val latestOffer: String? = null,
        val alertLevel: AlertLevel = AlertLevel.NORMAL
    ) {
        enum class AlertLevel {
            NORMAL,      // Verde Neon
            OPPORTUNITY, // Azul / Ciano
            WARNING      // Laranja / Vermelho
        }
    }

    /**
     * Modelo da Corrida Detectada na Bolha Flutuante
     */
    data class FloatingTacticalOffer(
        val appName: String = "iFood",
        val packageName: String = "com.ifood.driver",
        val value: Double = 28.50,
        val distanceKm: Double = 3.9,
        val pricePerKm: Double = 7.30,
        val isGoodDeal: Boolean = true,
        val dealLabel: String = "R$ 7,20/km 🟢 BOA",
        val destination: String = "Av. Rebouças, 1200",
        val pickup: String = "Shopping Eldorado",
        val hasDualRoute: Boolean = true,
        val dualRouteAppName: String = "99 Moto",
        val dualRouteExtraGain: Double = 16.50
    )

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var overlayRootView: FrameLayout? = null
    private var bubbleContainer: FrameLayout? = null
    private var hudContainer: LinearLayout? = null

    // Componentes da Bolha Circular
    private var tvBubbleBadge: TextView? = null
    private var ivBubblePulse: View? = null

    // Componentes do Card Flutuante de Corrida
    private var offerCardBox: LinearLayout? = null
    private var tvOfferAppTag: TextView? = null
    private var tvOfferProfitMain: TextView? = null
    private var tvOfferProfitSub: TextView? = null
    private var tvOfferMetrics: TextView? = null
    private var tvOfferAddresses: TextView? = null
    private var dualRouteBox: LinearLayout? = null
    private var tvDualRouteText: TextView? = null
    private var btnAcceptRide: TextView? = null
    private var btnDeclineRide: TextView? = null
    private var btnSwitchApp: TextView? = null

    // Componentes de Status Geral
    private var tvHudTitle: TextView? = null
    private var tvHudSubstatus: TextView? = null
    private var tvHudProfit: TextView? = null
    private var tvHudBlitz: TextView? = null
    private var defaultMetricsBox: LinearLayout? = null

    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isShowing = false
    private var currentStatus = JarvisOverlayStatus()
    private var currentOffer: FloatingTacticalOffer? = null

    // Variáveis para cálculo de arraste
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchStartTime: Long = 0

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: OverlayWindowManager? = null

        fun getInstance(context: Context): OverlayWindowManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OverlayWindowManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Exibe ou atualiza a interface flutuante sobreposta aos outros apps.
     */
    fun show() {
        if (!canDrawOverlays(context)) return

        mainHandler.post {
            if (isShowing && overlayRootView != null) {
                overlayRootView?.visibility = View.VISIBLE
                return@post
            }
            createAndAttachOverlayView()
        }
    }

    /**
     * Oculta a interface flutuante.
     */
    fun hide() {
        mainHandler.post {
            overlayRootView?.visibility = View.GONE
        }
    }

    fun toggle() {
        if (isShowing && overlayRootView?.visibility == View.VISIBLE) {
            hide()
        } else {
            show()
        }
    }

    fun isShowing(): Boolean = isShowing && overlayRootView?.visibility == View.VISIBLE

    /**
     * Atualiza o estado geral exibido na bolha.
     */
    fun updateStatus(status: JarvisOverlayStatus) {
        this.currentStatus = status
        mainHandler.post {
            applyStatusToViews()
        }
    }

    /**
     * Dispara o Card Flutuante Compacto de Inspeção de Corrida com cálculo de R$/km e Rota Dupla
     */
    fun showTacticalOffer(offer: FloatingTacticalOffer) {
        this.currentOffer = offer
        mainHandler.post {
            if (!isShowing || overlayRootView == null) {
                show()
            }
            // Força a expansão do HUD para visualização imediata pelo motorista
            setExpandedState(true)
            applyOfferToViews(offer)

            // Resposta tátil e sonora
            try {
                if (offer.isGoodDeal) {
                    HapticFeedbackHelper.vibrateSuccess(context)
                } else {
                    HapticFeedbackHelper.vibrateWarning(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Leitura automática em voz alta do resumo da oferta (valor, distância e lucro/km)
            try {
                OfferTextToSpeechEngine.getInstance(context).speakTacticalOffer(offer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Limpa a oferta atual e recolhe o HUD
     */
    fun clearTacticalOffer() {
        this.currentOffer = null
        mainHandler.post {
            offerCardBox?.visibility = View.GONE
            defaultMetricsBox?.visibility = View.VISIBLE
            setExpandedState(false)
            applyStatusToViews()
        }
    }

    /**
     * Remove a sobreposição do WindowManager.
     */
    fun destroy() {
        mainHandler.post {
            overlayRootView?.let { view ->
                if (view.isAttachedToWindow) {
                    try {
                        windowManager.removeViewImmediate(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            overlayRootView = null
            bubbleContainer = null
            hudContainer = null
            isShowing = false
            isExpanded = false
            currentOffer = null
        }
    }

    // =========================================================================
    // CONSTRUÇÃO E ANCORAGEM DA VIEW NATIVA
    // =========================================================================

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndAttachOverlayView() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val startX = screenWidth - dpToPx(76)
        val startY = dpToPx(160)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }
        this.layoutParams = params

        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        val bubble = buildBubbleView()
        this.bubbleContainer = bubble

        val hud = buildHudView()
        this.hudContainer = hud

        root.addView(bubble)
        root.addView(hud)

        bubble.setOnTouchListener { _, event ->
            handleBubbleTouch(event)
        }

        this.overlayRootView = root

        try {
            windowManager.addView(root, params)
            isShowing = true
            isExpanded = false
            setExpandedState(false)
            applyStatusToViews()
        } catch (e: Exception) {
            e.printStackTrace()
            isShowing = false
        }
    }

    /**
     * Monta a bolha circular compacta
     */
    private fun buildBubbleView(): FrameLayout {
        val bubbleSize = dpToPx(62)

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10151E"))
                setStroke(dpToPx(2.2f), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(10).toFloat()
        }

        val pulse = View(context).apply {
            val pulseSize = dpToPx(11)
            layoutParams = FrameLayout.LayoutParams(pulseSize, pulseSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(5)
                rightMargin = dpToPx(5)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E676"))
            }
        }
        this.ivBubblePulse = pulse
        container.addView(pulse)

        val tvIcon = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = "⚡"
            textSize = 24f
        }
        container.addView(tvIcon)

        val tvBadge = TextView(context).apply {
            val badgeHeight = dpToPx(18)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                badgeHeight
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(2)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            text = "JARVIS"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        this.tvBubbleBadge = tvBadge
        container.addView(tvBadge)

        return container
    }

    /**
     * Monta o HUD tático compacto completo com a seção de inspeção de corridas
     */
    private fun buildHudView(): LinearLayout {
        val hudWidth = dpToPx(310)

        val container = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                hudWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#F40A0D14"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(14).toFloat()
        }

        // Cabeçalho: Título + Botão Fechar
        val headerRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "HUD TÁTICO COCKPIT"
            setTextColor(Color.WHITE)
            textSize = 13.spToSp()
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudTitle = tvTitle
        headerRow.addView(tvTitle)

        // Botão de Áudio TTS (Ouvir resumo em voz alta / Mudo)
        val btnSpeaker = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)).apply {
                rightMargin = dpToPx(6)
            }
            gravity = Gravity.CENTER
            text = "🔊"
            textSize = 14f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1C2533"))
            }
            setOnClickListener {
                val ttsEngine = OfferTextToSpeechEngine.getInstance(context)
                val offer = currentOffer
                if (offer != null) {
                    ttsEngine.speakTacticalOffer(offer)
                    Toast.makeText(context, "🔊 Lendo resumo da oferta em voz alta...", Toast.LENGTH_SHORT).show()
                } else {
                    val repeated = ttsEngine.repeatLastOffer()
                    if (!repeated) {
                        ttsEngine.speak("Nenhuma oferta recente para leitura.")
                    }
                    Toast.makeText(context, "🔊 Reproduzindo áudio TTS...", Toast.LENGTH_SHORT).show()
                }
            }
            setOnLongClickListener {
                val ttsEngine = OfferTextToSpeechEngine.getInstance(context)
                val isMuted = ttsEngine.toggleMute()
                text = if (isMuted) "🔈" else "🔊"
                Toast.makeText(
                    context,
                    if (isMuted) "Voz TTS silenciada" else "Voz TTS ativada",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }
        headerRow.addView(btnSpeaker)

        val btnClose = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                setExpandedState(false)
            }
        }
        headerRow.addView(btnClose)
        container.addView(headerRow)

        // Substatus / Aplicativos monitorados
        val tvSub = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
                bottomMargin = dpToPx(8)
            }
            text = "● Monitorando: iFood • Uber • 99 Moto"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 10.5f
        }
        this.tvHudSubstatus = tvSub
        container.addView(tvSub)

        // =====================================================================
        // CARD FLUTUANTE COMPACTO DE OFERTA (CORRIDA DETECTADA)
        // =====================================================================
        val offerBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(Color.parseColor("#151B26"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
        }
        this.offerCardBox = offerBox

        // Linha 1 do Card: Tag da Plataforma (ex: iFood 🔴) + Timer
        val tagRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvAppTag = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#EA1D2C"))
            }
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            text = "iFood"
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvOfferAppTag = tvAppTag
        tagRow.addView(tvAppTag)

        val spacerTag = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        tagRow.addView(spacerTag)

        val tvTimer = TextView(context).apply {
            text = "⏱ 15s p/ aceitar"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        tagRow.addView(tvTimer)
        offerBox.addView(tagRow)

        // Linha 2 do Card: GANHO REAL POR KM (Destaque Principal)
        val profitBanner = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(6)
            }
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#122A1E"))
                setStroke(dpToPx(1.2f), Color.parseColor("#00E676"))
            }
        }

        val tvProfitMain = TextView(context).apply {
            text = "R$ 7,20/km 🟢 BOA"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        this.tvOfferProfitMain = tvProfitMain
        profitBanner.addView(tvProfitMain)

        val tvProfitSub = TextView(context).apply {
            text = "Excelente rentabilidade • Acima do piso"
            setTextColor(Color.parseColor("#B0BEC5"))
            textSize = 10f
            gravity = Gravity.CENTER
        }
        this.tvOfferProfitSub = tvProfitSub
        profitBanner.addView(tvProfitSub)
        offerBox.addView(profitBanner)

        // Linha 3 do Card: Métricas (Valor Total, Distância)
        val tvMetrics = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(4)
            }
            text = "💰 R$ 28,50 • 📏 3,9 km • ⏱ 12 min"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvOfferMetrics = tvMetrics
        offerBox.addView(tvMetrics)

        // Linha 4 do Card: Destino e Coleta
        val tvAddresses = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            text = "📍 De: Shopping Eldorado ➔ Para: Av. Rebouças"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 10.5f
            maxLines = 2
        }
        this.tvOfferAddresses = tvAddresses
        offerBox.addView(tvAddresses)

        // Linha 5: INDICADOR DE ROTA DUPLA COMPATÍVEL
        val dualBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#0C2030"))
                setStroke(dpToPx(1f), Color.parseColor("#00E5FF"))
            }
            gravity = Gravity.CENTER_VERTICAL
        }
        this.dualRouteBox = dualBox

        val tvDual = TextView(context).apply {
            text = "🔥 ROTA DUPLA COMPATÍVEL! (+R$ 16,50 c/ 99 Moto)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvDualRouteText = tvDual
        dualBox.addView(tvDual)
        offerBox.addView(dualBox)

        // Banner Viva-Voz no HUD Flutuante (Speech-to-Text Ativo)
        val voiceRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(5), dpToPx(8), dpToPx(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#0F1E19"))
                setStroke(dpToPx(1f), Color.parseColor("#00FF88"))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvVoiceStatus = TextView(context).apply {
            text = "🎤 VIVA-VOZ: Diga 'ACEITAR' ou 'RECUSAR'"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        voiceRow.addView(tvVoiceStatus)
        offerBox.addView(voiceRow)

        // =====================================================================
        // BOTÕES GIGANTES DE AÇÃO EM 1 TOQUE (ACEITAR OU DISPENSAR)
        // =====================================================================
        val oneTouchActionsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        // Botão Gigante 1: ACEITAR CORRIDA 🟢
        val btnAccept = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 1.2f).apply {
                rightMargin = dpToPx(6)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            text = "ACEITAR 🟢"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                handleAcceptCurrentOffer()
            }
        }
        this.btnAcceptRide = btnAccept
        oneTouchActionsRow.addView(btnAccept)

        // Botão Tático: ROTA 🗺️ (Waze / Google Maps em 1 toque)
        val btnRoute = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 0.9f).apply {
                leftMargin = dpToPx(4)
                rightMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#0C2030"))
                setStroke(dpToPx(1.2f), Color.parseColor("#00E5FF"))
            }
            text = "ROTA 🗺️"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val offer = currentOffer
                val targetAddress = offer?.pickup ?: "Ponto de Coleta"
                NavigationQuickDispatcher.launchBestRoute(
                    context = context,
                    latitude = -23.5616,
                    longitude = -46.6559,
                    addressTitle = targetAddress,
                    preferWaze = true
                )
            }
        }
        oneTouchActionsRow.addView(btnRoute)

        // Botão Gigante 2: DISPENSAR 🔴
        val btnDecline = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 1f).apply {
                leftMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#2A1218"))
                setStroke(dpToPx(1.2f), Color.parseColor("#FF3366"))
            }
            text = "DISPENSAR 🔴"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                handleDeclineCurrentOffer()
            }
        }
        this.btnDeclineRide = btnDecline
        oneTouchActionsRow.addView(btnDecline)

        offerBox.addView(oneTouchActionsRow)
        container.addView(offerBox)

        // =====================================================================
        // MÉTRICAS PADRÃO QUANDO NÃO HÁ CORRIDA ATIVA
        // =====================================================================
        val defaultBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }
        this.defaultMetricsBox = defaultBox

        val metricsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val profitBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        val lblProfit = TextView(context).apply {
            text = "PISO MÍNIMO"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 9f
        }
        val valProfit = TextView(context).apply {
            text = "R$ 4,50/km"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudProfit = valProfit
        profitBox.addView(lblProfit)
        profitBox.addView(valProfit)
        metricsRow.addView(profitBox)

        val blitzBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            orientation = LinearLayout.VERTICAL
        }
        val lblBlitz = TextView(context).apply {
            text = "RADAR / BLITZ"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 9f
        }
        val valBlitz = TextView(context).apply {
            text = "Raio Seguro"
            setTextColor(Color.parseColor("#81C784"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudBlitz = valBlitz
        blitzBox.addView(lblBlitz)
        blitzBox.addView(valBlitz)
        metricsRow.addView(blitzBox)
        defaultBox.addView(metricsRow)

        // Botões Padrão: Abrir Jarvis e Trocar App
        val defaultActionsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
            orientation = LinearLayout.HORIZONTAL
        }

        val btnOpenApp = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                rightMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            text = "Abrir Jarvis"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                openJarvisMainActivity()
                setExpandedState(false)
            }
        }
        defaultActionsRow.addView(btnOpenApp)

        val btnSwitch = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                leftMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#263238"))
                setStroke(dpToPx(1), Color.parseColor("#546E7A"))
            }
            text = "Recolher"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                setExpandedState(false)
            }
        }
        this.btnSwitchApp = btnSwitch
        defaultActionsRow.addView(btnSwitch)

        defaultBox.addView(defaultActionsRow)
        container.addView(defaultBox)

        return container
    }

    // =========================================================================
    // AÇÕES EM 1 TOQUE (ACEITAR OU DISPENSAR)
    // =========================================================================

    /**
     * Aciona o aceite da oferta atual via comando de voz (Speech-to-Text do Google)
     */
    fun acceptCurrentOfferByVoice(): Boolean {
        if (currentOffer == null) return false
        mainHandler.post {
            handleAcceptCurrentOffer()
        }
        return true
    }

    /**
     * Aciona a recusa da oferta atual via comando de voz (Speech-to-Text do Google)
     */
    fun declineCurrentOfferByVoice(): Boolean {
        if (currentOffer == null) return false
        mainHandler.post {
            handleDeclineCurrentOffer()
        }
        return true
    }

    private fun handleAcceptCurrentOffer() {
        val offer = currentOffer ?: return
        try {
            HapticFeedbackHelper.vibrateSuccess(context)
            Toast.makeText(context, "✅ Corrida do ${offer.appName} Aceita!", Toast.LENGTH_SHORT).show()
            try {
                OfferTextToSpeechEngine.getInstance(context).speakAcceptance(offer.pickup, offer.value)
            } catch (_: Exception) {}

            // Registra os ganhos no Rastreador de Meta Diária
            try {
                val fuelCost = offer.distanceKm * 0.45
                DailyGoalTrackerManager.getInstance(context).recordDeliveryEarnings(offer.value, fuelCost)
            } catch (_: Exception) {}

            // Alterna para o app da entrega se o pacote estiver disponível
            if (offer.packageName.isNotBlank()) {
                AssistedQuickSwitchManager.switchToSecondaryApp(context, offer.packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clearTacticalOffer()
        }
    }

    private fun handleDeclineCurrentOffer() {
        val offer = currentOffer
        try {
            HapticFeedbackHelper.vibrateWarning(context)
            val name = offer?.appName ?: "Corrida"
            Toast.makeText(context, "❌ $name Dispensada!", Toast.LENGTH_SHORT).show()
            try {
                OfferTextToSpeechEngine.getInstance(context).speakDecline()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clearTacticalOffer()
        }
    }

    // =========================================================================
    // TRATAMENTO DE TOUCH, ARRASTE E EXPANSÃO
    // =========================================================================

    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchStartTime = System.currentTimeMillis()
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                if (!isDragging && hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    updateLayoutSafe(params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - touchStartTime
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                val distance = hypot(deltaX.toDouble(), deltaY.toDouble())

                if (!isDragging && (duration < 300 || distance < touchSlop)) {
                    toggleExpandedState()
                } else if (isDragging && !isExpanded) {
                    snapBubbleToEdge(params)
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun toggleExpandedState() {
        setExpandedState(!isExpanded)
    }

    private fun setExpandedState(expanded: Boolean) {
        this.isExpanded = expanded
        mainHandler.post {
            if (expanded) {
                bubbleContainer?.visibility = View.GONE
                hudContainer?.visibility = View.VISIBLE
            } else {
                hudContainer?.visibility = View.GONE
                bubbleContainer?.visibility = View.VISIBLE
                layoutParams?.let { snapBubbleToEdge(it) }
            }
        }
    }

    private fun snapBubbleToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleWidth = dpToPx(62)
        val edgePadding = dpToPx(12)

        val targetX = if (params.x + (bubbleWidth / 2) < screenWidth / 2) {
            edgePadding
        } else {
            screenWidth - bubbleWidth - edgePadding
        }

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { va ->
            params.x = va.animatedValue as Int
            updateLayoutSafe(params)
        }
        animator.start()
    }

    private fun updateLayoutSafe(params: WindowManager.LayoutParams) {
        overlayRootView?.let { root ->
            if (root.isAttachedToWindow) {
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyStatusToViews() {
        val alertColor = when (currentStatus.alertLevel) {
            JarvisOverlayStatus.AlertLevel.NORMAL -> Color.parseColor("#00E676")
            JarvisOverlayStatus.AlertLevel.OPPORTUNITY -> Color.parseColor("#00E5FF")
            JarvisOverlayStatus.AlertLevel.WARNING -> Color.parseColor("#FF5252")
        }

        ivBubblePulse?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(alertColor)
        }

        tvBubbleBadge?.apply {
            text = if (currentOffer != null) {
                if (currentOffer!!.isGoodDeal) "BOA" else "RUIM"
            } else {
                "JARVIS"
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(alertColor)
            }
        }

        tvHudTitle?.text = currentStatus.headline
        tvHudSubstatus?.apply {
            text = "● ${currentStatus.activeApp}"
            setTextColor(alertColor)
        }
        tvHudProfit?.text = currentStatus.profitPerKm
        tvHudBlitz?.apply {
            text = currentStatus.blitzAlert
            setTextColor(if (currentStatus.blitzAlert.contains("Blitz", ignoreCase = true)) Color.parseColor("#FF5252") else Color.parseColor("#81C784"))
        }

        if (currentOffer == null) {
            offerCardBox?.visibility = View.GONE
            defaultMetricsBox?.visibility = View.VISIBLE
        }
    }

    /**
     * Aplica os dados da oferta ativa no Card Flutuante Compacto
     */
    private fun applyOfferToViews(offer: FloatingTacticalOffer) {
        offerCardBox?.visibility = View.VISIBLE
        defaultMetricsBox?.visibility = View.GONE

        // 1. Tag do App
        val (appColor, appName) = when (offer.appName.lowercase()) {
            "uber" -> Pair(Color.parseColor("#000000"), "Uber")
            "99", "99 moto", "99moto" -> Pair(Color.parseColor("#FFB300"), "99 Moto")
            else -> Pair(Color.parseColor("#EA1D2C"), "iFood")
        }
        tvOfferAppTag?.apply {
            text = appName
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(appColor)
            }
        }

        // 2. Cálculo em Tempo Real de Rentabilidade e Combustível via RealTimeProfitEngine
        val metrics = RealTimeProfitEngine.calculateTripMetrics(
            fareValue = offer.value,
            distanceKm = offer.distanceKm
        )

        val profitColor = Color.parseColor(metrics.thresholdLevel.colorHex)
        val profitBgColor = Color.parseColor(metrics.thresholdLevel.bgHex)

        tvOfferProfitMain?.apply {
            text = metrics.thresholdLabel
            setTextColor(profitColor)
        }
        tvOfferProfitSub?.apply {
            text = "Líq: %s • Gasolina: %s • Margem: %s".format(
                metrics.formattedNetPerKm,
                metrics.formattedFuelCost,
                metrics.formattedMargin
            )
            setTextColor(Color.parseColor("#CFD8DC"))
        }

        (tvOfferProfitMain?.parent as? LinearLayout)?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(10).toFloat()
            setColor(profitBgColor)
            setStroke(dpToPx(1.5f), profitColor)
        }

        // 3. Métricas Detalhadas (Tarifa Bruta, Lucro Líquido Real, Distância)
        val estMinutes = (offer.distanceKm * 3.2).toInt().coerceAtLeast(6)
        tvOfferMetrics?.text = "💰 Tarifa R$ %.2f (Líq: %s) • 📏 %.1f km • ⏱ %d min".format(
            offer.value,
            metrics.formattedNetProfit,
            offer.distanceKm,
            estMinutes
        )
        tvOfferAddresses?.text = "📍 De: ${offer.pickup}\n➔ Para: ${offer.destination}"

        // 4. Indicador de Rota Dupla
        if (offer.hasDualRoute) {
            dualRouteBox?.visibility = View.VISIBLE
            tvDualRouteText?.text = "🔥 ROTA DUPLA DETECTADA! (+R$ %.2f c/ %s)".format(offer.dualRouteExtraGain, offer.dualRouteAppName)
        } else {
            dualRouteBox?.visibility = View.GONE
        }

        // Atualiza a bolha quando minimizada com o threshold exato
        tvBubbleBadge?.apply {
            text = when (metrics.thresholdLevel) {
                ProfitThresholdLevel.HIGH_PROFIT -> "R$ %.1f/k 🟢".format(metrics.grossPricePerKm)
                ProfitThresholdLevel.MEDIUM_PROFIT -> "R$ %.1f/k 🟡".format(metrics.grossPricePerKm)
                ProfitThresholdLevel.LOW_PROFIT -> "R$ %.1f/k 🔴".format(metrics.grossPricePerKm)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(profitColor)
            }
            setTextColor(if (metrics.thresholdLevel == ProfitThresholdLevel.HIGH_PROFIT) Color.BLACK else Color.WHITE)
        }

        ivBubblePulse?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(profitColor)
        }
    }

    private fun openJarvisMainActivity() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun Int.spToSp(): Float {
        return this.toFloat()
    }
}
