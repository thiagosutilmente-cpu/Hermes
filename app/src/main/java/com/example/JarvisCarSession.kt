package com.example

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Gerencia o ciclo de vida da sessão veicular do Android Auto.
 * Inicializa a tela nativa principal JarvisCarScreen com suporte a atualização dinâmica.
 */
class JarvisCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return JarvisCarScreen(carContext)
    }
}
