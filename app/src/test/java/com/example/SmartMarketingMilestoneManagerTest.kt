package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para as métricas e geração de cópia do SmartMarketingMilestoneManager
 */
class SmartMarketingMilestoneManagerTest {

    @Test
    fun testMilestoneDataStructures() {
        val metrics = SmartMarketingMilestoneManager.MarketingMetrics(
            fuelSavedBrl = 45.50,
            badRidesRejected = 14,
            kmOptimized = 52.0,
            netEarningsBrl = 210.00,
            totalDispatches = 5,
            conversionsCount = 2,
            lastDispatchedStage = SmartMarketingMilestoneManager.STAGE_FUEL_SAVED_30,
            lastDispatchFormatted = "09/10 18:30"
        )

        assertEquals(45.50, metrics.fuelSavedBrl, 0.001)
        assertEquals(14, metrics.badRidesRejected)
        assertEquals(52.0, metrics.kmOptimized, 0.001)
        assertEquals(210.00, metrics.netEarningsBrl, 0.001)
        assertEquals(5, metrics.totalDispatches)
        assertEquals(2, metrics.conversionsCount)
        assertEquals(SmartMarketingMilestoneManager.STAGE_FUEL_SAVED_30, metrics.lastDispatchedStage)
    }

    @Test
    fun testMilestoneFunnelItemCreation() {
        val item = SmartMarketingMilestoneManager.MilestoneFunnelItem(
            stageKey = SmartMarketingMilestoneManager.STAGE_FUEL_SAVED_30,
            title = "2. Marco de R$ 30 Economizados",
            triggerCondition = "Economia em gasolina >= R$ 30,00",
            isUnlocked = true,
            isDispatched = false,
            previewCopy = "O Jarvis economizou gasolina!"
        )

        assertEquals("STAGE_FUEL_SAVED_30", item.stageKey)
        assertTrue(item.isUnlocked)
        assertEquals("2. Marco de R$ 30 Economizados", item.title)
        assertNotNull(item.previewCopy)
    }
}
