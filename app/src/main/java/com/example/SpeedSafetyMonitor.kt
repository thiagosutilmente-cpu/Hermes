package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

/**
 * Estado da telemetria de velocidade e bloqueio de segurança em movimento.
 */
data class SpeedSafetyState(
    val currentSpeedKmh: Double = 0.0,
    val isSafetyLockActive: Boolean = false, // true quando > 10.0 km/h
    val isMoving: Boolean = false, // true quando moto está em movimento
    val isGpsActive: Boolean = false,
    val gpsAccuracyMeters: Float = 3.8f,
    val latitude: Double = -23.561684,
    val longitude: Double = -46.655981,
    val altitudeMeters: Double = 760.0,
    val bearingDegrees: Float = 0f,
    val isSimulating: Boolean = false,
    val safetySpeedThresholdKmh: Double = 10.0,
    val sensorAccelerationMps2: Float = 0f,
    val provider: String = "Fused Location (GPS)"
)

/**
 * Monitor de Velocidade para Segurança do Entregador.
 * Utiliza o [FusedLocationProviderClient] da Google Play Services (API oficial de localização do Android)
 * com fallback para o [LocationManager] nativo e [SensorManager] (Acelerômetro) para detectar movimento.
 *
 * Regra Crítica:
 * Se a velocidade ultrapassar 10 km/h via GPS, ativa automaticamente a trava de segurança, bloqueando e
 * desativando a interface para impedir distrações visuais e acidentes durante a pilotagem.
 */
class SpeedSafetyMonitor(
    private val context: Context,
    private val onSafetyLockChanged: (Boolean, Double) -> Unit
) : LocationListener, SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var fusedLocationCallback: LocationCallback? = null

    // Limiar dinâmico de trava de velocidade carregado das preferências
    var speedThresholdKmh: Double = try {
        FilterPreferencesManager.loadCriteria(context).safetySpeedThresholdKm
    } catch (_: Exception) {
        15.0
    }
        private set

    private val _state = MutableStateFlow(SpeedSafetyState(safetySpeedThresholdKmh = speedThresholdKmh))
    val state: StateFlow<SpeedSafetyState> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var isSimulatingSpeed = false
    private var simulatedSpeedKmh = 0.0

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val SAFETY_SPEED_THRESHOLD_KMH = 15.0
    }

    /**
     * Atualiza o limiar de velocidade dinâmico e reavalia a trava de segurança imediatamente.
     */
    fun updateSpeedThreshold(newThreshold: Double) {
        speedThresholdKmh = newThreshold
        LocationService.updateSafetySpeedThreshold(newThreshold)
        val currentSpeed = _state.value.currentSpeedKmh
        val wasLocked = _state.value.isSafetyLockActive
        val isLocked = currentSpeed > newThreshold

        _state.value = _state.value.copy(
            safetySpeedThresholdKmh = newThreshold,
            isSafetyLockActive = isLocked
        )

        if (wasLocked != isLocked) {
            onSafetyLockChanged(isLocked, currentSpeed)
        }
    }

    init {
        startSensors()
        startLocationUpdates()

        // Sincroniza em tempo real com o LocationService em background
        scope.launch {
            LocationService.globalLocationState.collect { locState ->
                if (!isSimulatingSpeed && locState.isTracking) {
                    val wasLocked = _state.value.isSafetyLockActive
                    val isLocked = locState.isSafetyLockActive
                    _state.value = _state.value.copy(
                        currentSpeedKmh = locState.currentSpeedKmh,
                        isSafetyLockActive = isLocked,
                        isMoving = locState.currentSpeedKmh > 2.0,
                        isGpsActive = true,
                        gpsAccuracyMeters = locState.accuracyMeters,
                        latitude = locState.latitude,
                        longitude = locState.longitude,
                        altitudeMeters = locState.altitudeMeters,
                        bearingDegrees = locState.bearingDegrees,
                        provider = locState.speedSource
                    )
                    if (wasLocked != isLocked) {
                        onSafetyLockChanged(isLocked, locState.currentSpeedKmh)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // 1. Google Play Services FusedLocationProviderClient (API oficial de alta precisão)
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMinUpdateDistanceMeters(0.5f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    processLocationUpdate(location, "Fused Location API")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                fusedLocationCallback!!,
                Looper.getMainLooper()
            )
            Log.d("SpeedSafetyMonitor", "FusedLocationProviderClient conectado com sucesso.")
        } catch (e: Exception) {
            Log.d("SpeedSafetyMonitor", "Falha ao registrar FusedLocationProviderClient: ${e.message}")
        }

        // 2. LocationManager nativo (GPS e Network) para redundância e hardware direto
        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (hasGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0.5f,
                    this
                )
            }
            if (hasNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    800L,
                    1f,
                    this
                )
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { processLocationUpdate(it, "Fused Location (Cache)") }
            }
        } catch (e: Exception) {
            Log.d("SpeedSafetyMonitor", "Falha ao registrar LocationManager: ${e.message}")
        }
    }

    private fun startSensors() {
        try {
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            accelSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            Log.d("SpeedSafetyMonitor", "Falha ao registrar sensores: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        processLocationUpdate(location, location.provider ?: "GPS Nativo")
    }

    private fun processLocationUpdate(location: Location, providerName: String) {
        if (isSimulatingSpeed) return

        var speedKmh = 0.0
        if (location.hasSpeed() && location.speed >= 0f) {
            speedKmh = (location.speed * 3.6).toDouble()
        } else if (lastLocation != null && location.time > lastLocation!!.time) {
            val distMeters = location.distanceTo(lastLocation!!)
            val timeSec = (location.time - lastLocation!!.time) / 1000.0
            if (timeSec in 0.3..15.0) {
                speedKmh = (distMeters / timeSec) * 3.6
            }
        }
        lastLocation = location

        val wasLocked = _state.value.isSafetyLockActive
        val isLocked = speedKmh > speedThresholdKmh
        val isMoving = speedKmh > 2.0 || _state.value.sensorAccelerationMps2 > 1.2f

        _state.value = _state.value.copy(
            currentSpeedKmh = speedKmh,
            isSafetyLockActive = isLocked,
            isMoving = isMoving,
            isGpsActive = true,
            gpsAccuracyMeters = if (location.hasAccuracy()) location.accuracy else 3.8f,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude,
            bearingDegrees = if (location.hasBearing()) location.bearing else 0f,
            provider = providerName,
            isSimulating = false
        )

        // Sincroniza com LocationService
        LocationService.updateSimulatedSpeed(speedKmh)

        // Avalia zonas de alta demanda (Geofencing) com a localização atual
        try {
            GeofencingDemandManager.getInstance(context)
                .evaluateCurrentLocation(location.latitude, location.longitude)
        } catch (_: Exception) {}

        if (wasLocked != isLocked) {
            onSafetyLockChanged(isLocked, speedKmh)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION || event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)

            val isMoving = _state.value.currentSpeedKmh > 2.0 || magnitude > 1.5f

            _state.value = _state.value.copy(
                sensorAccelerationMps2 = magnitude,
                isMoving = isMoving
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    /**
     * Permite testar a velocidade (ex: 0 km/h parado, 18 km/h movimento, 45 km/h trânsito livre)
     * sem precisar estar em uma moto física em movimento.
     */
    fun setSimulatedSpeed(speedKmh: Double) {
        isSimulatingSpeed = true
        simulatedSpeedKmh = speedKmh

        val wasLocked = _state.value.isSafetyLockActive
        val isLocked = speedKmh > speedThresholdKmh
        val isMoving = speedKmh > 2.0

        _state.value = _state.value.copy(
            currentSpeedKmh = speedKmh,
            isSafetyLockActive = isLocked,
            isMoving = isMoving,
            isSimulating = true,
            provider = "Simulação Teste"
        )

        LocationService.updateSimulatedSpeed(speedKmh)

        if (wasLocked != isLocked) {
            onSafetyLockChanged(isLocked, speedKmh)
        }
    }

    fun disableSimulation() {
        isSimulatingSpeed = false
        lastLocation?.let { processLocationUpdate(it, "Fused Location (GPS)") } ?: run {
            val wasLocked = _state.value.isSafetyLockActive
            _state.value = _state.value.copy(
                currentSpeedKmh = 0.0,
                isSafetyLockActive = false,
                isMoving = false,
                isSimulating = false,
                provider = "Fused Location (GPS)"
            )
            LocationService.updateSimulatedSpeed(0.0)
            if (wasLocked) {
                onSafetyLockChanged(false, 0.0)
            }
        }
    }

    fun destroy() {
        try {
            fusedLocationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            locationManager?.removeUpdates(this)
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
    }
}

/**
 * Card Exibido quando a velocidade do entregador ultrapassa 20 km/h.
 * Bloqueia e oculta a lista de ofertas e instrui o uso dos comandos de voz ("Aceitar" / "Recusar").
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpeedSafetyLockCard(
    speedKmh: Double = 24.5,
    thresholdKmh: Double = 15.0,
    isListeningVoice: Boolean = true,
    lastVoiceCommand: String = "",
    onSimulateVoiceCommand: ((String) -> Unit)? = null,
    onTestSpeedChanged: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_safety")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_safety_border"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B120C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = RedDecline.copy(alpha = pulseBorder),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("speed_safety_lock_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RedDecline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODO SEGURANÇA EM TRÂNSITO",
                        color = RedDecline,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RedDecline.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "> ${thresholdKmh.toInt()} KM/H",
                        color = RedDecline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Velocímetro Central com Destaque Máximo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DarkBg)
                    .border(2.dp, RedDecline.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.0f", speedKmh),
                        color = TextLight,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "KM/H",
                        color = RedDecline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Interface desativada por segurança (GPS)",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Para proteger sua vida no trânsito, a interface é desativada e bloqueada automaticamente enquanto o veículo estiver em movimento acima de ${thresholdKmh.toInt()} km/h (configurável nas preferências).",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Aviso e Painel de Comandos por Voz Ativos na Direção
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1F18))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isListeningVoice) "🎙️" else "🔇", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isListeningVoice) "Reconhecimento de Voz Contínuo Ativo" else "Reconhecimento em Espera",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lastVoiceCommand.isNotBlank())
                                "Último comando: \"$lastVoiceCommand\""
                            else
                                "Fale para aceitar corridas ou ajustar filtros sem soltar o guidão.",
                            color = if (lastVoiceCommand.isNotBlank()) Color(0xFF00D2FF) else TextLight.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = if (lastVoiceCommand.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Comandos de Áudio Disponíveis na Pilotagem:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Chips / Botões de Comandos de Voz (Permite falar ou simular com 1 toque tático)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onSimulateVoiceCommand?.invoke("aceitar") }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗣️ \"Aceitar\"",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RedDecline.copy(alpha = 0.15f))
                            .border(1.dp, RedDecline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { onSimulateVoiceCommand?.invoke("recusar") }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗣️ \"Recusar\"",
                            color = RedDecline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Comandos de Filtro por Áudio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00D2FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSimulateVoiceCommand?.invoke("filtro chuva") }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌧️ \"Filtro Chuva\"",
                            color = Color(0xFF00D2FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB800).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSimulateVoiceCommand?.invoke("tiro curto") }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ \"Tiro Curto\"",
                            color = Color(0xFFFFB800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF9D4EDD).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF9D4EDD).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSimulateVoiceCommand?.invoke("resetar filtros") }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔄 \"Resetar\"",
                            color = Color(0xFF9D4EDD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seletor de Velocidade para Teste / Simulação do Piloto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Testar Velocidade:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpeedTestButton(label = "0 km/h", isSelected = speedKmh <= 3.0) {
                        onTestSpeedChanged(0.0)
                    }
                    SpeedTestButton(label = "8 km/h", isSelected = speedKmh in 3.1..10.0) {
                        onTestSpeedChanged(8.0)
                    }
                    SpeedTestButton(label = "15 km/h", isSelected = speedKmh in 10.1..25.0) {
                        onTestSpeedChanged(15.0)
                    }
                    SpeedTestButton(label = "35 km/h", isSelected = speedKmh > 25.0) {
                        onTestSpeedChanged(35.0)
                    }
                }
            }
        }
    }
}

/**
 * Card de Telemetria de Velocidade e GPS em Tempo Real.
 * Exibe no dashboard do entregador a velocidade instantânea de pilotagem,
 * o status dos satélites e o estado do bloqueio de segurança.
 */
@Composable
fun RealtimeSpeedTelemetryCard(
    speedState: SpeedSafetyState = SpeedSafetyState(
        currentSpeedKmh = 14.5,
        isSafetyLockActive = true,
        isGpsActive = true,
        gpsAccuracyMeters = 3.8f
    ),
    isBackgroundLocationGranted: Boolean = true,
    onRequestBackgroundLocation: (() -> Unit)? = null,
    onSimulateSpeed: (Double) -> Unit = {},
    onResetRealGps: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLocked = speedState.isSafetyLockActive

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFF160E0B) else DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = if (isLocked) RedDecline else DarkBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("realtime_speed_telemetry_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) RedDecline else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TELEMETRIA DE VELOCIDADE (GPS)",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (speedState.isGpsActive) "🛰️ Satélites Ativos • Precisão ±${String.format(Locale.GERMANY, "%.1f", speedState.gpsAccuracyMeters)}m" else "Procurando sinal GPS...",
                            color = if (speedState.isGpsActive) NeonGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLocked) RedDecline.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.15f))
                        .border(0.8.dp, if (isLocked) RedDecline else NeonGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isLocked) "🚨 BLOQUEIO ATIVO" else "🛡️ TOQUE LIVRE",
                        color = if (isLocked) RedDecline else NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Indicador de Serviço em Background (Foreground Service)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F141C))
                    .border(0.6.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D2FF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SERVIÇO 2º PLANO: ATIVO (FOREGROUND)",
                        color = Color(0xFF00D2FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Áudio + Vibração",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            // Banner se a permissão de segundo plano contínua estiver pendente no Android 10+
            if (!isBackgroundLocationGranted && onRequestBackgroundLocation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A1C0A))
                        .border(1.dp, Color(0xFFFFB800), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 Localização em 2º plano pendente",
                            color = Color(0xFFFFB800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Permita 'O tempo todo' para manter a trava ativa no Waze.",
                            color = TextLight,
                            fontSize = 9.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB800))
                            .clickable { onRequestBackgroundLocation() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "ATIVAR",
                            color = Color(0xFF0A0A0F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Velocímetro e Detalhes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f", speedState.currentSpeedKmh),
                        color = if (isLocked) RedDecline else NeonGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "km/h",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (speedState.isMoving) "🏍️ Em Movimento" else "🟢 Parado",
                            color = if (speedState.isMoving) (if (isLocked) RedDecline else NeonGreen) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isLocked) "🚨 Trava Ativa (> ${speedState.safetySpeedThresholdKmh.toInt()} km/h)" else "🛡️ Toque Livre (<= ${speedState.safetySpeedThresholdKmh.toInt()} km/h)",
                        color = if (isLocked) RedDecline else TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Fonte: ${speedState.provider}",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seletor de Simulação Rápida / Teste em Bancada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulação / Teste:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpeedTestButton(label = "0 km/h", isSelected = speedState.currentSpeedKmh <= 3.0 && speedState.isSimulating) {
                        onSimulateSpeed(0.0)
                    }
                    SpeedTestButton(label = "8 km/h", isSelected = speedState.currentSpeedKmh in 3.1..10.0 && speedState.isSimulating) {
                        onSimulateSpeed(8.0)
                    }
                    SpeedTestButton(label = "15 km/h", isSelected = speedState.currentSpeedKmh > 10.0 && speedState.isSimulating) {
                        onSimulateSpeed(15.0)
                    }
                    SpeedTestButton(label = "📡 GPS Real", isSelected = !speedState.isSimulating) {
                        onResetRealGps()
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonGreen else DarkCardElevated)
            .border(
                width = 0.8.dp,
                color = if (isSelected) NeonGreen else DarkBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) DarkBg else TextLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
