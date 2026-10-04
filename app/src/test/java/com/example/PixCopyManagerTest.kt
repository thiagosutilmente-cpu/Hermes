package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o gerador de códigos PIX (PixCopyManager).
 */
class PixCopyManagerTest {

    @Test
    fun testGenerateMockPixCodeStructure() {
        val pixWeekly = PixCopyManager.generateMockPixCode(
            amount = 25.00,
            planIdentifier = "semanal",
            txId = "TX998877"
        )

        assertNotNull(pixWeekly)
        assertTrue("Deve começar com payload format indicator do BACEN", pixWeekly.startsWith("00020126"))
        assertTrue("Deve conter o identificador br.gov.bcb.pix", pixWeekly.contains("br.gov.bcb.pix"))
        assertTrue("Deve conter a moeda BRL (986)", pixWeekly.contains("5303986"))
        assertTrue("Deve conter o valor formatado 25.00", pixWeekly.contains("25.00"))
        assertTrue("Deve conter o país BR", pixWeekly.contains("5802BR"))
        assertTrue("Deve conter o nome do recebedor JARVIS", pixWeekly.contains("JARVIS NEURAL COCKPIT"))
        assertTrue("Deve conter a cidade SAO PAULO", pixWeekly.contains("SAO PAULO"))
        assertTrue("Deve conter o TxID", pixWeekly.contains("TX998877"))
        assertTrue("Deve terminar com checksum CRC16", pixWeekly.endsWith("6304ABCD"))
    }

    @Test
    fun testGenerateMockPixCodeForDifferentPlans() {
        val pixAnnual = PixCopyManager.generateMockPixCode(amount = 650.00, planIdentifier = "anual")
        assertTrue(pixAnnual.contains("650.00"))
        assertTrue(pixAnnual.contains("cobranca-anual@pix.jarvis.app.br"))

        val pixMonthly = PixCopyManager.generateMockPixCode(amount = 99.90, planIdentifier = "mensal")
        assertTrue(pixMonthly.contains("99.90"))
        assertTrue(pixMonthly.contains("cobranca-mensal@pix.jarvis.app.br"))
    }
}
