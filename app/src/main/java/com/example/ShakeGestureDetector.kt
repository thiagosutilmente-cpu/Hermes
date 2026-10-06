package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * ShakeGestureDetector
 *
 * Detector de gestos de 'shake' (chacoalhar o celular) baseado no acelerômetro de hardware ([SensorManager]).
 * Projetado para motociclistas em situações táteis de trânsito.
 *
 * Funcionalidades:
 * 1. Detecção calibrada para evitar falsos positivos com trepidação da moto (filtro passa-alta da gravidade terrestre).
 * 2. Limiar de aceleração configurável (padrão: 13.5 m/s² acima da gravidade 1G).
 * 3. Ação primária: Silenciar imediatamente alertas sonoros de TTS em situações de emergência viária.
 * 4. Ação secundária: Forçar o re-processamento/re-análise da última oferta recebida no radar.
 * 5. Debounce tático de 1200ms para evitar disparos repetitivos em solavancos sucessivos.
 */
class ShakeGestureDetector(
    private val context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeGestureDetector"

        // Limiar de aceleração resultante (excluindo gravidade terrestre de 9.8 m/s²)
        // Valor de 13.5f filtra vibrações normais do motor monocilíndrico (150cc/160cc) da moto
        private const val DEFAULT_SHAKE_THRESHOLD = 13.5f

        // Intervalo mínimo entre detecções sucessivas (1200 ms)
        private const val SHAKE_DEBOUNCE_MS = 1200L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTimestamp: Long = 0L
    private var isListening = false

    private val _isShakeDetectorActive = MutableStateFlow(false)
    val isShakeDetectorActive: StateFlow<Boolean> = _isShakeDetectorActive.asStateFlow()

    private val _lastShakeEventTime = MutableStateFlow(0L)
    val lastShakeEventTime: StateFlow<Long> = _lastShakeEventTime.asStateFlow()

    /**
     * Inicia o monitoramento dos eventos do acelerômetro
     */
    fun start(): Boolean {
        if (accelerometer == null) {
            Log.w(TAG, "Sensor acelerômetro não disponível no dispositivo.")
            _isShakeDetectorActive.value = false
            return false
        }

        if (isListening) return true

        val registered = sensorManager?.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        ) ?: false

        isListening = registered
        _isShakeDetectorActive.value = registered
        Log.i(TAG, "ShakeGestureDetector iniciado: $registered")
        return registered
    }

    /**
     * Interrompe o sensor para economizar bateria
     */
    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        _isShakeDetectorActive.value = false
        Log.i(TAG, "ShakeGestureDetector pausado.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calcula a aceleração total em m/s²
        val totalAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Subtrai a gravidade padrão da Terra (aprox 9.81 m/s²) para obter apenas aceleração induzida
        val netAcceleration = totalAcceleration - SensorManager.GRAVITY_EARTH

        if (netAcceleration >= DEFAULT_SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp >= SHAKE_DEBOUNCE_MS) {
                lastShakeTimestamp = now
                _lastShakeEventTime.value = now
                Log.w(TAG, "Gesto de Shake detectado! Aceleração líquida: $netAcceleration m/s²")

                // Feedback háptico de confirmação do gesto
                try {
                    HapticFeedbackHelper.vibrateTap(context)
                } catch (_: Exception) {}

                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sem necessidade de ajuste dinâmico de precisão
    }
}
