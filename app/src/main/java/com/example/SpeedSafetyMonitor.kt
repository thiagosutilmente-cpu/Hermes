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
 * Se a velocidade ultrapassar 10 km/h, ativa automaticamente a trava de segurança, ocultando a lista
 * de ofertas para impedir distrações visuais e acidentes durante a pilotagem.
 */
class SpeedSafetyMonitor(
    private val context: Context,
    private val onSafetyLockChanged: (Boolean, Double) -> Unit
) : LocationListener, SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var fusedLocationCallback: LocationCallback? = null

    private val _state = MutableStateFlow(SpeedSafetyState())
    val state: StateFlow<SpeedSafetyState> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var isSimulatingSpeed = false
    private var simulatedSpeedKmh = 0.0

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val SAFETY_SPEED_THRESHOLD_KMH = 10.0
    }

    init {
        startSensors()
        startLocationUpdates()
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
        val isLocked = speedKmh > SAFETY_SPEED_THRESHOLD_KMH
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
        val isLocked = speedKmh > SAFETY_SPEED_THRESHOLD_KMH
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
 * Card Exibido quando a velocidade do entregador ultrapassa 10 km/h.
 * Oculta a lista de ofertas e instrui o uso dos comandos de voz ("Aceitar" / "Recusar").
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpeedSafetyLockCard(
    speedKmh: Double = 18.5,
    isListeningVoice: Boolean = true,
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
                        text = "> 10 KM/H",
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
                        text = String.format(Locale.GERMANY, "%.0f", speedKmh),
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
                text = "Lista de ofertas oculta por segurança",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Para proteger sua vida no trânsito, a lista de pedidos é bloqueada enquanto a moto estiver em movimento acima de 10 km/h.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Aviso de Comandos por Voz Ativos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D1F18))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎙️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Mãos Livres Ativo no Capacete",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Você ainda pode aceitar a melhor oferta dizendo \"Aceitar\" ou \"Cancelar\".",
                        color = TextLight.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
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
                    SpeedTestButton(label = "0 km/h", isSelected = speedKmh <= 10.0) {
                        onTestSpeedChanged(0.0)
                    }
                    SpeedTestButton(label = "18 km/h", isSelected = speedKmh in 11.0..25.0) {
                        onTestSpeedChanged(18.0)
                    }
                    SpeedTestButton(label = "45 km/h", isSelected = speedKmh > 25.0) {
                        onTestSpeedChanged(45.0)
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
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RealtimeSpeedTelemetryCard(
    speedState: SpeedSafetyState = SpeedSafetyState(
        currentSpeedKmh = 14.5,
        isSafetyLockActive = true,
        isGpsActive = true,
        gpsAccuracyMeters = 3.8f
    ),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Velocímetro e Detalhes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.GERMANY, "%.1f", speedState.currentSpeedKmh),
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
                        text = if (isLocked) "🚨 Trava Ativa (> 10 km/h)" else "🛡️ Modo Toque Livre (<= 10 km/h)",
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
                    SpeedTestButton(label = "0 km/h", isSelected = speedState.currentSpeedKmh <= 10.0 && speedState.isSimulating) {
                        onSimulateSpeed(0.0)
                    }
                    SpeedTestButton(label = "18 km/h", isSelected = speedState.currentSpeedKmh in 11.0..25.0 && speedState.isSimulating) {
                        onSimulateSpeed(18.0)
                    }
                    SpeedTestButton(label = "45 km/h", isSelected = speedState.currentSpeedKmh > 25.0 && speedState.isSimulating) {
                        onSimulateSpeed(45.0)
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
