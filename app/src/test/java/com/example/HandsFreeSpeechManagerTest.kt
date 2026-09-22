package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes unitários para validação do analisador léxico de comandos por voz viva-voz (Hands-Free)
 * do Google Speech-to-Text para pilotos de entrega.
 */
class HandsFreeSpeechManagerTest {

    @Test
    fun testAcceptCommands() {
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("aceitar"))
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("sim"))
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("bora"))
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("pegar corrida"))
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("confirma"))
        assertEquals(VoiceActionCommand.ACCEPT, HandsFreeSpeechManager.parseCommand("pode pegar"))
    }

    @Test
    fun testDeclineCommands() {
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("recusar"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("não"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("nao"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("cancela"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("pular"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("deixa passar"))
        assertEquals(VoiceActionCommand.DECLINE, HandsFreeSpeechManager.parseCommand("rejeitar"))
    }

    @Test
    fun testAppSpecificAcceptCommands() {
        assertEquals(VoiceActionCommand.ACCEPT_IFOOD, HandsFreeSpeechManager.parseCommand("aceitar ifood"))
        assertEquals(VoiceActionCommand.ACCEPT_RAPPI, HandsFreeSpeechManager.parseCommand("pegar rappi"))
        assertEquals(VoiceActionCommand.ACCEPT_UBER, HandsFreeSpeechManager.parseCommand("bora uber"))
        assertEquals(VoiceActionCommand.ACCEPT_99, HandsFreeSpeechManager.parseCommand("confirmar 99"))
        assertEquals(VoiceActionCommand.ACCEPT_99, HandsFreeSpeechManager.parseCommand("aceitar noventa e nove"))
    }

    @Test
    fun testAppSpecificDeclineCommands() {
        assertEquals(VoiceActionCommand.DECLINE_IFOOD, HandsFreeSpeechManager.parseCommand("recusar ifood"))
        assertEquals(VoiceActionCommand.DECLINE_RAPPI, HandsFreeSpeechManager.parseCommand("cancela rappi"))
        assertEquals(VoiceActionCommand.DECLINE_UBER, HandsFreeSpeechManager.parseCommand("pular uber"))
        assertEquals(VoiceActionCommand.DECLINE_99, HandsFreeSpeechManager.parseCommand("rejeitar 99"))
    }

    @Test
    fun testTacticalPresetCommands() {
        assertEquals(VoiceActionCommand.FILTER_RAIN_PRESET, HandsFreeSpeechManager.parseCommand("chuva"))
        assertEquals(VoiceActionCommand.FILTER_RAIN_PRESET, HandsFreeSpeechManager.parseCommand("tarifa dinâmica"))
        assertEquals(VoiceActionCommand.FILTER_SHORT_PRESET, HandsFreeSpeechManager.parseCommand("tiro curto"))
        assertEquals(VoiceActionCommand.FILTER_SHORT_PRESET, HandsFreeSpeechManager.parseCommand("corridas curtas"))
        assertEquals(VoiceActionCommand.FILTER_MAX_PROFIT_PRESET, HandsFreeSpeechManager.parseCommand("máximo lucro"))
        assertEquals(VoiceActionCommand.FILTER_RESET, HandsFreeSpeechManager.parseCommand("resetar filtros"))
        assertEquals(VoiceActionCommand.FILTER_RESET, HandsFreeSpeechManager.parseCommand("limpar filtro"))
    }

    @Test
    fun testMinimumPriceVoiceFilters() {
        assertEquals(VoiceActionCommand.FILTER_MIN_15, HandsFreeSpeechManager.parseCommand("mínimo 15"))
        assertEquals(VoiceActionCommand.FILTER_MIN_20, HandsFreeSpeechManager.parseCommand("mínimo 20"))
        assertEquals(VoiceActionCommand.FILTER_MIN_30, HandsFreeSpeechManager.parseCommand("mínimo 30"))
        assertEquals(VoiceActionCommand.FILTER_MIN_20, HandsFreeSpeechManager.parseCommand("vinte reais"))
    }

    @Test
    fun testCockpitInformationCommands() {
        assertEquals(VoiceActionCommand.READ_OFFER, HandsFreeSpeechManager.parseCommand("ler oferta"))
        assertEquals(VoiceActionCommand.READ_OFFER, HandsFreeSpeechManager.parseCommand("qual o valor"))
        assertEquals(VoiceActionCommand.READ_EARNINGS, HandsFreeSpeechManager.parseCommand("quanto faturei"))
        assertEquals(VoiceActionCommand.READ_EARNINGS, HandsFreeSpeechManager.parseCommand("ganhos de hoje"))
        assertEquals(VoiceActionCommand.OPEN_NAVIGATION, HandsFreeSpeechManager.parseCommand("abrir rota"))
        assertEquals(VoiceActionCommand.OPEN_NAVIGATION, HandsFreeSpeechManager.parseCommand("abrir gps"))
        assertEquals(VoiceActionCommand.READ_HEALTH, HandsFreeSpeechManager.parseCommand("saúde"))
        assertEquals(VoiceActionCommand.HELP, HandsFreeSpeechManager.parseCommand("ajuda"))
    }

    @Test
    fun testScreenNavigationCommands() {
        assertEquals(VoiceActionCommand.OPEN_OFFERS_LIST, HandsFreeSpeechManager.parseCommand("abrir ofertas"))
        assertEquals(VoiceActionCommand.OPEN_OFFERS_LIST, HandsFreeSpeechManager.parseCommand("radar de ofertas"))
        assertEquals(VoiceActionCommand.OPEN_OFFERS_LIST, HandsFreeSpeechManager.parseCommand("lista de ofertas"))
        assertEquals(VoiceActionCommand.CLOSE_SCREEN, HandsFreeSpeechManager.parseCommand("voltar"))
        assertEquals(VoiceActionCommand.CLOSE_SCREEN, HandsFreeSpeechManager.parseCommand("voltar ao cockpit"))
        assertEquals(VoiceActionCommand.CLOSE_SCREEN, HandsFreeSpeechManager.parseCommand("painel principal"))
    }

    @Test
    fun testAutoAcceptCommands() {
        assertEquals(VoiceActionCommand.AUTO_ACCEPT_ON, HandsFreeSpeechManager.parseCommand("ativar auto aceite"))
        assertEquals(VoiceActionCommand.AUTO_ACCEPT_OFF, HandsFreeSpeechManager.parseCommand("desativar auto aceite"))
        assertEquals(VoiceActionCommand.AUTO_ACCEPT_TOGGLE, HandsFreeSpeechManager.parseCommand("auto aceite"))
    }

    @Test
    fun testUnrecognizedNoise() {
        assertNull(HandsFreeSpeechManager.parseCommand("buzina de caminhão"))
        assertNull(HandsFreeSpeechManager.parseCommand("barulho de escape"))
        assertNull(HandsFreeSpeechManager.parseCommand(""))
    }
}
