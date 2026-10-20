package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * RadarNotificationActionReceiver
 *
 * BroadcastReceiver para executar ações em 1 toque diretamente da barra de notificações
 * permanente do Android, sem exigir que o motociclista abra a tela do celular ou tire as luvas.
 */
class RadarNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RadarActionReceiver"

        const val ACTION_TRIGGER_SOS = "com.example.radar.ACTION_TRIGGER_SOS"
        const val ACTION_TOGGLE_RADAR = "com.example.radar.ACTION_TOGGLE_RADAR"
        const val ACTION_STOP_KEEPALIVE = "com.example.radar.ACTION_STOP_KEEPALIVE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Notificação de Ação Recebida: $action")

        when (action) {
            ACTION_TRIGGER_SOS -> {
                // Dispara o alerta silencioso imediatamente com envio de GPS
                Toast.makeText(context, "🚨 SOS SILENCIOSO ATIVADO!", Toast.LENGTH_LONG).show()
                HapticFeedbackHelper.vibrateSilentSosConfirmation(context)
                EmergencySosManager.triggerSosImmediately(
                    context = context,
                    deliveryInfo = "Disparado via Botão de Ação Rápida na Notificação"
                )
            }

            ACTION_TOGGLE_RADAR -> {
                // Alterna o estado de monitoramento do Radar
                RadarKeepAliveForegroundService.toggleRadarState(context)
            }

            ACTION_STOP_KEEPALIVE -> {
                // Encerra o serviço em primeiro plano
                RadarKeepAliveForegroundService.stop(context)
                Toast.makeText(context, "Serviço persistente encerrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
