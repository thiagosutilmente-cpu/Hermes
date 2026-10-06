package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.Locale

/**
 * NavigationQuickDispatcher
 *
 * Despacho rápido de rotas em 1 toque para Waze e Google Maps.
 * Evita que o entregador precise digitar nomes de ruas ou alternar manualmente
 * de aplicativo durante a condução.
 */
object NavigationQuickDispatcher {

    private const val TAG = "NavQuickDispatcher"

    /**
     * Inicia navegação GPS imediata no Waze
     */
    fun launchWaze(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = ""
    ): Boolean {
        return try {
            val uri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.waze")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                announceNavStart(context, "Waze", addressTitle)
                true
            } else {
                // Fallback: abre via link web ou Google Maps
                launchGoogleMaps(context, latitude, longitude, addressTitle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir Waze: ${e.message}", e)
            launchGoogleMaps(context, latitude, longitude, addressTitle)
        }
    }

    /**
     * Inicia navegação GPS no Google Maps
     */
    fun launchGoogleMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = ""
    ): Boolean {
        return try {
            // Intent otimizado de navegação turn-by-turn do Google Maps
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                announceNavStart(context, "Google Maps", addressTitle)
                true
            } else {
                // Fallback genérico geo:
                val geoUri = Uri.parse("geo:$latitude,$longitude?q=${Uri.encode(addressTitle.ifBlank { "$latitude,$longitude" })}")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                announceNavStart(context, "Mapa GPS", addressTitle)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir Google Maps: ${e.message}", e)
            Toast.makeText(context, "Nenhum aplicativo de GPS encontrado.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Dispara o melhor navegador baseado na preferência ou disponibilidade
     */
    fun launchBestRoute(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = "",
        preferWaze: Boolean = true
    ) {
        if (preferWaze) {
            launchWaze(context, latitude, longitude, addressTitle)
        } else {
            launchGoogleMaps(context, latitude, longitude, addressTitle)
        }
    }

    private fun announceNavStart(context: Context, appName: String, address: String) {
        try {
            HapticFeedbackHelper.vibrateSuccess(context)
            val destText = if (address.isNotBlank()) " para $address" else ""
            val tts = OfferTextToSpeechEngine.getInstance(context)
            tts.speak("Iniciando rota no $appName$destText.")
        } catch (_: Exception) {}
    }
}
