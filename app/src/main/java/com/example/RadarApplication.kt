package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.FirebaseInitializer

class RadarApplication : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        unattributedContextInstance = base
        super.attachBaseContext(base)
    }


    companion object {
        private const val TAG = "RadarApplication"
        var unattributedContextInstance: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RadarApplication onCreate - Initializing core services")
        
        // Explicitly initialize Firebase (Firestore and Auth) in application context
        FirebaseInitializer.initialize(this)

        // Agendamento do WorkManager para sincronização periódica e imediata de ofertas e telemetria
        try {
            com.example.worker.OfferSyncScheduler.schedulePeriodicSync(this, intervalMinutes = 15)
            com.example.worker.OfferSyncScheduler.triggerImmediateSync(this)
            Log.i(TAG, "RadarApplication - WorkManager OfferSyncScheduler ativado com sucesso.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar WorkManager em RadarApplication: ${e.message}", e)
        }

        // Inicialização do Gerenciador de Geofencing Google Location para detecção de zonas de alta demanda
        try {
            com.example.geofence.RadarGeofenceManager.initialize(this)
            Log.i(TAG, "RadarApplication - RadarGeofenceManager inicializado com sucesso.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar RadarGeofenceManager: ${e.message}", e)
        }

        // Inicialização do cache offline e persistência Room para Rotas Ghost Mescladas
        try {
            com.example.manager.MergedDeliveryDispatcher.initializeWithCache(this)
            Log.i(TAG, "RadarApplication - MergedDeliveryDispatcher Room Cache inicializado.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar cache de rotas em RadarApplication: ${e.message}", e)
        }

        // Inicialização segura do motor de mapas Google Maps
        try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                com.google.android.gms.maps.MapsInitializer.initialize(
                    this,
                    com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
                ) { renderer ->
                    Log.i(TAG, "MapsInitializer concluído com renderer: $renderer")
                }
            } else {
                Log.w(TAG, "Google Play Services não disponível para MapsInitializer (código: $resultCode), utilizando renderização offline resiliente.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Aviso ao inicializar MapsInitializer (usando fallback autônomo): ${e.message}")
        }
    }
}
