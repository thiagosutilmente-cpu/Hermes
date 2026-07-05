package com.example.coordinator

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateCoordinator {
    private val TAG = "StateCoordinator"

    private val _state = MutableStateFlow(RadarState.OUVINDO)
    val state: StateFlow<RadarState> = _state.asStateFlow()

    /**
     * Tenta transitar para um novo estado da máquina de estados.
     * Retorna true se a transição foi válida e realizada com sucesso, false caso contrário.
     * 
     * Regra de Ouro / Interruptor de Segurança Central:
     * A flag PARADO é a única coisa que permite transitar para o estado ACEITANDO.
     */
    fun transitionTo(newState: RadarState, speedState: SpeedState): Boolean {
        val currentState = _state.value
        Log.d(TAG, "Tentativa de transição: $currentState -> $newState [Moto: $speedState]")

        // 1. Validação crítica de segurança: Moto andando não aceita nada.
        if (newState == RadarState.ACEITANDO && speedState != SpeedState.PARADO) {
            Log.e(TAG, "TRANSIÇÃO REJEITADA: Velocidade impede o aceite automático por segurança!")
            return false
        }

        // 2. Lógica padrão de fluxo de transições (Máquina de Estados Unificada)
        val isValid = when (currentState) {
            RadarState.OUVINDO -> newState == RadarState.OFERTA_LIDA
            RadarState.OFERTA_LIDA -> newState == RadarState.ANALISANDO || newState == RadarState.OUVINDO
            RadarState.ANALISANDO -> newState == RadarState.SUGERINDO || newState == RadarState.OUVINDO
            RadarState.SUGERINDO -> newState == RadarState.AGUARDANDO_ACAO || newState == RadarState.OUVINDO
            RadarState.AGUARDANDO_ACAO -> newState == RadarState.ACEITANDO || newState == RadarState.NAVEGANDO || newState == RadarState.OUVINDO
            RadarState.ACEITANDO -> newState == RadarState.NAVEGANDO || newState == RadarState.OUVINDO
            RadarState.NAVEGANDO -> newState == RadarState.OUVINDO
        }

        return if (isValid || newState == RadarState.OUVINDO) {
            _state.value = newState
            Log.d(TAG, "TRANSIÇÃO AUTORIZADA: Novo estado é $newState")
            true
        } else {
            Log.w(TAG, "TRANSIÇÃO INVÁLIDA: Mudança de $currentState para $newState negada pelas regras de fluxo.")
            false
        }
    }

    /**
     * Força o reset do estado para OUVINDO de forma segura.
     */
    fun reset() {
        _state.value = RadarState.OUVINDO
        Log.d(TAG, "Máquina de estados reiniciada para OUVINDO.")
    }
}
