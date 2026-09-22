package com.example

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Ponto de entrada nativo para inicialização na tela multimídia do veículo (Android Auto).
 * Gerencia a sessão veicular e estabelece a ponte com a telemetria do smartphone.
 */
class JarvisCarAppService : CarAppService() {

    companion object {
        private const val TAG = "JarvisCarAppService"
    }

    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        Log.d(TAG, "Sessão do Android Auto inicializada pelo sistema do veículo.")
        return JarvisCarSession()
    }
}
