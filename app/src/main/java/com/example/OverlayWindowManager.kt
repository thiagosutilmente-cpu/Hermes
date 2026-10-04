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
import kotlin.math.hypot

/**
 * OverlayWindowManager
 *
 * Gerenciador nativo de interface flutuante (Bolha tática & Mini-HUD) utilizando o [WindowManager] do Android.
 *
 * Permite que o entregador:
 * 1. Mantenha uma bolha flutuante persistente do Jarvis visível sobre outros apps (iFood, Uber, 99 Moto, Rappi).
 * 2. Arraste a bolha livremente pela tela com ancoragem automática nas bordas laterais ("snap-to-edge").
 * 3. Toque para expandir um HUD tático com o status em tempo real (Ganhos/km, Radar de Blitz, Alerta de Rota Dupla).
 * 4. Acesse ações rápidas ("Abrir Cockpit", "Alternar App", "Recolher") sem interromper o app de entrega subjacente.
 */
class OverlayWindowManager private constructor(private val context: Context) {

    data class JarvisOverlayStatus(
        val isOnline: Boolean = true,
        val headline: String = "JARVIS OPERACIONAL",
        val activeApp: String = "Monitorando Apps",
        val profitPerKm: String = "R$ 3,85/km",
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

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var overlayRootView: FrameLayout? = null
    private var bubbleContainer: FrameLayout? = null
    private var hudContainer: LinearLayout? = null

    // Componentes visuais para atualização dinâmica
    private var tvBubbleBadge: TextView? = null
    private var ivBubblePulse: View? = null
    private var tvHudTitle: TextView? = null
    private var tvHudSubstatus: TextView? = null
    private var tvHudProfit: TextView? = null
    private var tvHudBlitz: TextView? = null
    private var tvHudOffer: TextView? = null
    private var offerContainer: LinearLayout? = null

    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isShowing = false
    private var currentStatus = JarvisOverlayStatus()

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

        /**
         * Verifica se o aplicativo possui a permissão SYSTEM_ALERT_WINDOW concedida.
         */
        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        /**
         * Direciona o entregador diretamente para a tela de concessão da permissão de sobreposição.
         */
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
     * Oculta a interface flutuante sem destruir totalmente os dados.
     */
    fun hide() {
        mainHandler.post {
            overlayRootView?.visibility = View.GONE
        }
    }

    /**
     * Alterna a visibilidade da bolha flutuante.
     */
    fun toggle() {
        if (isShowing && overlayRootView?.visibility == View.VISIBLE) {
            hide()
        } else {
            show()
        }
    }

    /**
     * Retorna se a sobreposição está atualmente exibida na tela.
     */
    fun isShowing(): Boolean = isShowing && overlayRootView?.visibility == View.VISIBLE

    /**
     * Atualiza o estado das informações exibidas na bolha e no HUD.
     */
    fun updateStatus(status: JarvisOverlayStatus) {
        this.currentStatus = status
        mainHandler.post {
            applyStatusToViews()
        }
    }

    /**
     * Remove a sobreposição do WindowManager e libera recursos.
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

        // Container Raiz
        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        // 1. Bolha Flutuante Compacta
        val bubble = buildBubbleView()
        this.bubbleContainer = bubble

        // 2. Mini-HUD Tático Expansível
        val hud = buildHudView()
        this.hudContainer = hud

        root.addView(bubble)
        root.addView(hud)

        // Configuração dos gestos de arraste e clique na bolha
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
     * Monta visualmente a bolha circular compacta do Jarvis.
     */
    private fun buildBubbleView(): FrameLayout {
        val bubbleSize = dpToPx(60)

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#121820"))
                setStroke(dpToPx(2), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(8).toFloat()
        }

        // Ponto de pulso de status
        val pulse = View(context).apply {
            val pulseSize = dpToPx(10)
            layoutParams = FrameLayout.LayoutParams(pulseSize, pulseSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(6)
                rightMargin = dpToPx(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E676"))
            }
        }
        this.ivBubblePulse = pulse
        container.addView(pulse)

        // Ícone / Letra central do Jarvis
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

        // Badge inferior com valor resumido (ex: R$18)
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
     * Monta o Mini-HUD tático com cantos arredondados e dados em tempo real.
     */
    private fun buildHudView(): LinearLayout {
        val hudWidth = dpToPx(290)

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
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.parseColor("#EE121820"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(12).toFloat()
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
            text = "JARVIS COCKPIT HUD"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudTitle = tvTitle
        headerRow.addView(tvTitle)

        val btnClose = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                setExpandedState(false)
            }
        }
        headerRow.addView(btnClose)
        container.addView(headerRow)

        // Substatus / Aplicativo ativo
        val tvSub = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
                bottomMargin = dpToPx(8)
            }
            text = "● Monitorando: iFood • Uber • 99 • Rappi"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 11f
        }
        this.tvHudSubstatus = tvSub
        container.addView(tvSub)

        // Linha Divisória
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                bottomMargin = dpToPx(8)
            }
            setBackgroundColor(Color.parseColor("#263238"))
        }
        container.addView(divider)

        // Linha de Lucro e Blitz
        val metricsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        // Card Rentabilidade
        val profitBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        val lblProfit = TextView(context).apply {
            text = "RENTABILIDADE"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 9f
        }
        val valProfit = TextView(context).apply {
            text = "R$ 3,85/km"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudProfit = valProfit
        profitBox.addView(lblProfit)
        profitBox.addView(valProfit)
        metricsRow.addView(profitBox)

        // Card Radar Blitz
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

        container.addView(metricsRow)

        // Card de Oferta Ativa / Rota Dupla (Opcional)
        val offerBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#1B2631"))
            }
        }
        val lblOffer = TextView(context).apply {
            text = "OPORTUNIDADE DE ROTA DUPLA"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val valOffer = TextView(context).apply {
            text = "iFood R$ 18,00 + 99 R$ 16,00 (+R$ 34,00)"
            setTextColor(Color.WHITE)
            textSize = 11f
        }
        this.tvHudOffer = valOffer
        this.offerContainer = offerBox
        offerBox.addView(lblOffer)
        offerBox.addView(valOffer)
        container.addView(offerBox)

        // Botões de Ação Inferiores
        val actionsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
            orientation = LinearLayout.HORIZONTAL
        }

        // Botão Abrir Cockpit
        val btnOpenApp = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
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
        actionsRow.addView(btnOpenApp)

        // Botão Alternar App (Opção B)
        val btnSwitch = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
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
        actionsRow.addView(btnSwitch)

        container.addView(actionsRow)

        return container
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
                    // Clique detectado! Alterna entre expandido e recolhido
                    toggleExpandedState()
                } else if (isDragging && !isExpanded) {
                    // Ancoragem suave na borda lateral mais próxima (Snap-to-edge)
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
        val bubbleWidth = dpToPx(60)
        val edgePadding = dpToPx(12)

        val targetX = if (params.x + (bubbleWidth / 2) < screenWidth / 2) {
            edgePadding // Grudar na esquerda
        } else {
            screenWidth - bubbleWidth - edgePadding // Grudar na direita
        }

        // Animação suave de transição de coordenada X
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
        // Cor de alerta
        val alertColor = when (currentStatus.alertLevel) {
            JarvisOverlayStatus.AlertLevel.NORMAL -> Color.parseColor("#00E676")
            JarvisOverlayStatus.AlertLevel.OPPORTUNITY -> Color.parseColor("#00E5FF")
            JarvisOverlayStatus.AlertLevel.WARNING -> Color.parseColor("#FF5252")
        }

        // Atualizar Bolha
        ivBubblePulse?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(alertColor)
        }

        tvBubbleBadge?.apply {
            text = if (currentStatus.latestOffer != null) "OFERTA" else "JARVIS"
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(alertColor)
            }
        }

        // Atualizar Mini-HUD
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

        if (currentStatus.latestOffer != null) {
            offerContainer?.visibility = View.VISIBLE
            tvHudOffer?.text = currentStatus.latestOffer
        } else {
            offerContainer?.visibility = View.GONE
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
}
