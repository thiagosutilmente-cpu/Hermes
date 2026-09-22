package com.example

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Renderiza a interface gráfica nativa veicular no painel multimídia do Android Auto.
 * Exibe em alta legibilidade:
 * 1. Lucro estimado e ganho por km da melhor oferta interceptada.
 * 2. Alertas de Hotspots e Polos gastronômicos com alta demanda.
 * 3. Status de telemetria e velocidade calculada via Haversine.
 * 4. Botões táticos veiculares de ação direta [ACEITAR] e [RECUSAR] acionando o Jarvis.
 */
class JarvisCarScreen(carContext: CarContext) : Screen(carContext) {

    private var currentSpeedKmh: Double = 0.0
    private var isSafetyLockActive: Boolean = false
    private var latestOffer: RadarOffer? = null
    private var activeHotspotName: String = "Polo Paulista / Jardins"
    private var hotspotDemandLevel: String = "Alta Demanda (+25% bônus)"

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                // Monitora telemetria em tempo real
                lifecycleScope.launch {
                    LocationForegroundService.telemetryState.collectLatest { telemetry ->
                        currentSpeedKmh = telemetry.speedKmh
                        isSafetyLockActive = telemetry.isSafetyLockActive
                        invalidate()
                    }
                }

                // Monitora ofertas interceptadas de apps de entrega
                lifecycleScope.launch {
                    AppNotificationListenerService.lastInterceptedOffer.collectLatest { offer ->
                        if (offer != null) {
                            latestOffer = offer
                            invalidate()
                        }
                    }
                }

                // Monitora despachador geral de ofertas
                lifecycleScope.launch {
                    BackgroundOfferDispatcher.latestOffers.collectLatest { offers ->
                        if (offers.isNotEmpty() && latestOffer == null) {
                            latestOffer = offers.firstOrNull()
                            invalidate()
                        }
                    }
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        // 1. Linha de Telemetria e Velocímetro
        val speedDisplay = String.format(Locale.GERMANY, "%.1f", currentSpeedKmh)
        val speedTitle = if (isSafetyLockActive) {
            "🔒 $speedDisplay km/h • TRAVA DE PILOTAGEM (>10 km/h)"
        } else {
            "⚡ $speedDisplay km/h • TELEMETRIA HAVERSINE ATIVA"
        }
        val speedSubtitle = if (isSafetyLockActive) {
            "Modo veicular seguro: Use comandos de voz Jarvis no volante"
        } else {
            "GPS de alta precisão calibrado para rotas de entrega"
        }

        paneBuilder.addRow(
            Row.Builder()
                .setTitle(speedTitle)
                .addText(speedSubtitle)
                .build()
        )

        // 2. Linha de Oferta Interceptada e Lucro/km
        val offer = latestOffer
        if (offer != null) {
            val valorFormatado = String.format(Locale.GERMANY, "R$ %.2f", offer.value)
            val ganhoKmFormatado = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)
            val lucroLiquido = String.format(Locale.GERMANY, "R$ %.2f líquido", offer.netProfit)

            val offerTitle = "💰 $valorFormatado ($ganhoKmFormatado) • ${offer.appName}"
            val offerDetails = "${offer.restaurant} • ${offer.distanceKm} km • ~${offer.estimatedTimeMin} min ($lucroLiquido)"

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(offerTitle)
                    .addText(offerDetails)
                    .build()
            )

            // Ações Táticas no Android Auto: Aceitar / Recusar
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("✅ ACEITAR ($valorFormatado)")
                    .setBackgroundColor(CarColor.GREEN)
                    .setOnClickListener {
                        HapticFeedbackHelper.vibrateAccept(carContext)
                        CustomSoundPlayer.previewSound(carContext, NotificationSoundType.CHIME_MELODIC)
                        NeuralVoiceManager.speak(carContext, "Corrida de ${offer.restaurant} aceita com sucesso via comando veicular.")
                        latestOffer = null
                        invalidate()
                    }
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("❌ RECUSAR")
                    .setBackgroundColor(CarColor.RED)
                    .setOnClickListener {
                        HapticFeedbackHelper.vibrateDecline(carContext)
                        NeuralVoiceManager.speak(carContext, "Corrida descartada pelo comando veicular.")
                        latestOffer = null
                        invalidate()
                    }
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("🎯 AGUARDANDO NOVAS OFERTAS (iFood, Uber, Rappi)")
                    .addText("Radar inteligente rastreando corridas acima de R$ 3,50/km")
                    .build()
            )
        }

        // 3. Linha de Alerta de Hotspot / Polo Gastronômico
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("📍 HOTSPOT: $activeHotspotName")
                .addText("Status: $hotspotDemandLevel • Distância: ~1.2 km")
                .build()
        )

        // Botão de alternância e atualização
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("🎙️ JARVIS VOZ")
                    .setOnClickListener {
                        NeuralVoiceManager.speak(carContext, "Jarvis pronto no painel do Android Auto. Velocidade atual $speedDisplay quilômetros por hora.")
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Radar Coordinator — Jarvis Cockpit")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }
}
