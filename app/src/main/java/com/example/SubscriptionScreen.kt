package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Ponto de entrada da Tela de Paywall e Planos Pro:
 * Redireciona para a FintechPlansSelectionScreen com design premium,
 * 3 planos (Semanal, Mensal, Anual) e checkout Pix com confirmação em tempo real.
 */
@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FintechPlansSelectionScreen(
        onDismiss = onDismiss,
        modifier = modifier
    )
}
