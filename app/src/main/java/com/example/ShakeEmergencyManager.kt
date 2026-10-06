package com.example

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ShakeEmergencyAction
 *
 * Ação configurada para quando o entregador chacoalha o celular no guidão.
 */
enum class ShakeActionPreference(val label: String, val description: String) {
    SILENCE_AUDIO_EMERGENCY(
        "Silenciar Áudio de Emergência",
        "Interrompe imediatamente qualquer aviso por voz em situações de trânsito perigoso"
    ),
    REPROCESS_LAST_OFFER(
        "Re-processar Última Oferta",
        "Força re-análise tática e re-leitura dos valores da oferta mais recente"
    ),
    DUAL_ACTION(
        "Silenciar se Falando / Repetir se Calado",
        "Corta o áudio se estiver falando; caso contrário, re-processa e relê a oferta"
    )
}

/**
 * ShakeEmergencyManager
 *
 * Gerencia a resposta do sistema ao gesto de chacoalhar (Shake):
 * 1. Silenciamento emergencial do TextToSpeech (Android TTS) instantâneo para não distrair em manobras de risco.
 * 2. Re-processamento forçado e re-análise da última oferta recebida no radar.
 * 3. Integração direta com [ShakeGestureDetector], [TextToSpeechManager] e [OfferTextToSpeechEngine].
 */
object ShakeEmergencyManager {

    private const val TAG = "ShakeEmergencyMgr"
    private const val PREFS_NAME = "radar_shake_emergency_prefs"
    private const val KEY_SHAKE_ACTION = "pref_shake_action_mode"
    private const val KEY_SHAKE_ENABLED = "pref_shake_detector_enabled"

    private var shakeDetector: ShakeGestureDetector? = null

    private val _isShakeEnabled = MutableStateFlow(true)
    val isShakeEnabled: StateFlow<Boolean> = _isShakeEnabled.asStateFlow()

    private val _selectedAction = MutableStateFlow(ShakeActionPreference.DUAL_ACTION)
    val selectedAction: StateFlow<ShakeActionPreference> = _selectedAction.asStateFlow()

    private val _lastActionExecuted = MutableStateFlow("")
    val lastActionExecuted: StateFlow<String> = _lastActionExecuted.asStateFlow()

    // Callback para re-processar a última oferta na UI / ViewModel
    var onReprocessRequested: (() -> Unit)? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        val savedActionName = prefs.getString(KEY_SHAKE_ACTION, ShakeActionPreference.DUAL_ACTION.name)
        val action = try {
            ShakeActionPreference.valueOf(savedActionName ?: ShakeActionPreference.DUAL_ACTION.name)
        } catch (_: Exception) {
            ShakeActionPreference.DUAL_ACTION
        }

        _isShakeEnabled.value = isEnabled
        _selectedAction.value = action

        if (shakeDetector == null) {
            shakeDetector = ShakeGestureDetector(context) {
                handleShakeTriggered(context)
            }
        }

        if (isEnabled) {
            shakeDetector?.start()
        }
    }

    /**
     * Tratamento central do evento de Shake
     */
    private fun handleShakeTriggered(context: Context) {
        val ttsManager = TextToSpeechManager.getInstance(context)
        val offerTts = OfferTextToSpeechEngine.getInstance(context)
        val isCurrentlySpeaking = ttsManager.isSpeaking.value || offerTts.engineState.value.isSpeaking

        when (_selectedAction.value) {
            ShakeActionPreference.SILENCE_AUDIO_EMERGENCY -> {
                silenceAllAudio(context, ttsManager, offerTts)
                _lastActionExecuted.value = "🔇 Áudio Silenciado via Shake"
            }

            ShakeActionPreference.REPROCESS_LAST_OFFER -> {
                reprocessLastOffer(context)
                _lastActionExecuted.value = "🔄 Re-processamento Forçado via Shake"
            }

            ShakeActionPreference.DUAL_ACTION -> {
                if (isCurrentlySpeaking) {
                    silenceAllAudio(context, ttsManager, offerTts)
                    _lastActionExecuted.value = "🔇 Áudio Silenciado de Emergência"
                } else {
                    reprocessLastOffer(context)
                    _lastActionExecuted.value = "🔄 Re-processamento da Última Oferta"
                }
            }
        }
    }

    private fun silenceAllAudio(
        context: Context,
        ttsManager: TextToSpeechManager,
        offerTts: OfferTextToSpeechEngine
    ) {
        ttsManager.stop()
        offerTts.stop()
        HapticFeedbackHelper.vibrateDecline(context)
        Toast.makeText(context, "🔇 Alerta de áudio silenciado com sucesso!", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Todos os canais de áudio silenciados via gesto de Shake.")
    }

    private fun reprocessLastOffer(context: Context) {
        HapticFeedbackHelper.vibrateAccept(context)
        Toast.makeText(context, "🔄 Re-processando última oferta no radar...", Toast.LENGTH_SHORT).show()

        // 1. Invoca callback de re-avaliação se registrado
        onReprocessRequested?.invoke()

        // 2. Se não houver callback registrado, repete a última oferta no motor TTS
        val repeated = OfferTextToSpeechEngine.getInstance(context).repeatLastOffer()
        if (!repeated) {
            TextToSpeechManager.getInstance(context).repeatLastAlert()
        }
        Log.i(TAG, "Re-processamento da última oferta executado via Shake.")
    }

    fun setShakeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHAKE_ENABLED, enabled).apply()
        _isShakeEnabled.value = enabled

        if (enabled) {
            shakeDetector?.start()
        } else {
            shakeDetector?.stop()
        }
    }

    fun setActionPreference(context: Context, action: ShakeActionPreference) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SHAKE_ACTION, action.name).apply()
        _selectedAction.value = action
    }

    fun destroy() {
        shakeDetector?.stop()
        shakeDetector = null
    }
}
