package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do serviço de telemetria de velocidade baseado no Fused Location Provider
 * e da trava automática de interface para velocidades acima de 10 km/h.
 */
class LocationSpeedSensorTest {

    @Test
    fun testDefaultSpeedThresholdIs10Kmh() {
        assertEquals(10.0, LocationService.SAFETY_SPEED_LOCK_THRESHOLD_KMH, 0.001)
        assertEquals(10.0, LocationService.dynamicSafetySpeedThresholdKmh, 0.001)
    }

    @Test
    fun testSpeedVelocityLockDecisions() {
        val threshold = LocationService.SAFETY_SPEED_LOCK_THRESHOLD_KMH

        // Velocidades abaixo ou iguais a 10 km/h NÃO devem bloquear a interface
        assertFalse(0.0 > threshold)
        assertFalse(5.0 > threshold)
        assertFalse(8.5 > threshold)
        assertFalse(10.0 > threshold)

        // Velocidades estritamente acima de 10 km/h DEVEM ativar a trava de segurança
        assertTrue(10.1 > threshold)
        assertTrue(12.0 > threshold)
        assertTrue(25.0 > threshold)
        assertTrue(60.0 > threshold)
    }

    @Test
    fun testMpsToKmhSpeedConversion() {
        // Conversão física de m/s para km/h: velocidade (km/h) = m/s * 3.6
        val speedMpsWalk = 1.38889f // ~5.0 km/h
        val speedKmhWalk = speedMpsWalk * 3.6
        assertEquals(5.0, speedKmhWalk, 0.1)
        assertFalse(speedKmhWalk > 10.0)

        val speedMpsThreshold = 2.77778f // ~10.0 km/h
        val speedKmhThreshold = speedMpsThreshold * 3.6
        assertEquals(10.0, speedKmhThreshold, 0.1)

        val speedMpsUrbanRide = 8.33333f // ~30.0 km/h
        val speedKmhUrbanRide = speedMpsUrbanRide * 3.6
        assertEquals(30.0, speedKmhUrbanRide, 0.1)
        assertTrue(speedKmhUrbanRide > 10.0)
    }

    @Test
    fun testLocationServiceSimulatedSpeedUpdatesStateFlow() {
        // Atualiza para velocidade de movimento urbano (20 km/h)
        LocationService.updateSimulatedSpeed(20.0)
        val movingState = LocationService.globalLocationState.value
        assertEquals(20.0, movingState.currentSpeedKmh, 0.01)
        assertTrue("Interface deve estar travada em 20 km/h", movingState.isSafetyLockActive)
        assertTrue("Notificações devem estar silenciadas em movimento", movingState.isNotificationsMuted)

        // Atualiza para moto parada (0 km/h)
        LocationService.updateSimulatedSpeed(0.0)
        val stoppedState = LocationService.globalLocationState.value
        assertEquals(0.0, stoppedState.currentSpeedKmh, 0.01)
        assertFalse("Interface deve ser liberada quando parado", stoppedState.isSafetyLockActive)
        assertFalse("Notificações liberadas quando parado", stoppedState.isNotificationsMuted)

        // Atualiza para velocidade de manobra lenta (6 km/h)
        LocationService.updateSimulatedSpeed(6.0)
        val slowState = LocationService.globalLocationState.value
        assertEquals(6.0, slowState.currentSpeedKmh, 0.01)
        assertFalse("Velocidade <= 10 km/h não ativa a trava", slowState.isSafetyLockActive)
    }

    @Test
    fun testDynamicThresholdAdjustment() {
        // Ajusta temporariamente o limiar e verifica a reação
        LocationService.updateSafetySpeedThreshold(15.0)
        assertEquals(15.0, LocationService.dynamicSafetySpeedThresholdKmh, 0.001)

        // Em 12 km/h com limiar de 15 km/h, o bloqueio deve ser falso
        LocationService.updateSimulatedSpeed(12.0)
        assertFalse(LocationService.globalLocationState.value.isSafetyLockActive)

        // Restaura para o padrão de 10 km/h
        LocationService.updateSafetySpeedThreshold(10.0)
        assertEquals(10.0, LocationService.dynamicSafetySpeedThresholdKmh, 0.001)
        LocationService.updateSimulatedSpeed(12.0)
        assertTrue(LocationService.globalLocationState.value.isSafetyLockActive)

        // Volta ao estado inicial seguro
        LocationService.updateSimulatedSpeed(0.0)
    }
}
