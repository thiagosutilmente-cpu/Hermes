package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.LogType
import com.example.data.FirestoreManager
import com.example.data.FirebaseAuthManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RadarNotificationListenerService : NotificationListenerService() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
    }

    companion object {
        private const val TAG = "RadarNotifListener"
        
        // Hold onto the latest WhatsApp reply action so we can reply programmatically
        private var latestReplyAction: Notification.Action? = null
        private var latestSbnKey: String? = null
        
        fun getLatestReplyAction(): Notification.Action? = latestReplyAction
    }

    private var replyListener: ListenerRegistration? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RadarNotificationListenerService created")
        
        // Reactively listen to Firestore reply commands based on current user
        serviceScope.launch {
            FirebaseAuthManager.currentUser.collectLatest { user ->
                replyListener?.remove()
                replyListener = null
                
                val riderId = user?.uid ?: FirestoreManager.DEFAULT_RIDER_ID
                Log.d(TAG, "Subscribing to WhatsApp reply commands for rider: $riderId")
                
                try {
                    replyListener = FirestoreManager.listenToWhatsAppReply(riderId) { replyText ->
                        val action = latestReplyAction
                        if (action != null) {
                            sendReply(action, replyText)
                            RadarCoordinator.addLog("Jarvis WhatsApp: Resposta automática enviada via sistema: '$replyText'", LogType.SUCCESS)
                        } else {
                            Log.w(TAG, "Cannot send reply: no cached reply action.")
                            RadarCoordinator.addLog("Jarvis WhatsApp: Não foi possível responder (nenhuma notificação ativa encontrada).", LogType.WARNING)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Firestore reply listener for $riderId", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        replyListener?.remove()
        serviceScope.cancel()
        Log.d(TAG, "RadarNotificationListenerService destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        
        // Only target WhatsApp and Delivery/Ride notifications
        val deliveryApps = listOf(
            "com.ubercab.driver",
            "com.taxis99",
            "com.99taxis.driver",
            "com.ifood.driver",
            "sinet.startup.inDriver",
            "com.lalamove.rider.driver",
            "com.maxim.driver",
            "com.borzo.driver",
            "br.com.loggi.driver"
        )

        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification?.extras ?: return
            
            // Extract sender name and content
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            if (title.isBlank() || text.isBlank()) return
            
            // Avoid group chats or keep it simple
            Log.d(TAG, "Received WhatsApp Notification: $title -> $text")
            
            // Look for RemoteInput action
            val actions = sbn.notification.actions
            if (actions != null) {
                for (action in actions) {
                    if (action.remoteInputs != null) {
                        for (remoteInput in action.remoteInputs) {
                            if (remoteInput.resultKey != null) {
                                latestReplyAction = action
                                latestSbnKey = sbn.key
                                Log.d(TAG, "Captured WhatsApp reply action for $title")
                                break
                            }
                        }
                    }
                }
            }
            
            // Log in the centralized console
            RadarCoordinator.addLog("Jarvis WhatsApp: Nova mensagem de '$title': '$text'", LogType.INFO)
            
            // Speak the incoming message out loud for the helmet
            try {
                RadarCoordinator.voiceManager?.speak("Thiago, nova mensagem de $title no WhatsApp. Ela diz o seguinte: $text")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to speak WhatsApp notification", e)
            }
            
            // Sync with Firestore so frontend dashboard knows in real-time
            try {
                FirestoreManager.saveWhatsAppNotification(title, text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save WhatsApp notification to Firestore", e)
            }
        } else if (deliveryApps.any { packageName.contains(it) }) {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val joined = "$title $text"

            Log.d(TAG, "Received Delivery App Notification from $packageName: $joined")

            // Regex for Fare (e.g. R$ 15,00)
            val fareRegex = java.util.regex.Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")
            val fareMatcher = fareRegex.matcher(joined)
            var fareValue = 0.0
            if (fareMatcher.find()) {
                val fareStr = fareMatcher.group(1)?.replace(",", ".") ?: ""
                fareValue = fareStr.toDoubleOrNull() ?: 0.0
            }

            // Regex for Distance (e.g. 5.2 km)
            val distRegex = java.util.regex.Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", java.util.regex.Pattern.CASE_INSENSITIVE)
            val distMatcher = distRegex.matcher(joined)
            var distanceValue = 0.0
            if (distMatcher.find()) {
                val number = distMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                val unit = distMatcher.group(3)?.lowercase() ?: "km"
                distanceValue = if (unit == "m") number / 1000.0 else number
            }

            if (fareValue > 0.0) {
                val appName = when {
                    packageName.contains("uber") -> "Uber"
                    packageName.contains("99") -> "99"
                    packageName.contains("ifood") -> "iFood"
                    packageName.contains("indriver") -> "inDrive"
                    packageName.contains("lalamove") -> "Lalamove"
                    packageName.contains("maxim") -> "Maxim"
                    packageName.contains("borzo") -> "Borzo"
                    packageName.contains("loggi") -> "Loggi"
                    else -> "App de Entrega"
                }

                Log.d(TAG, "Ghost Eye: Oferta de Fundo Detectada: $appName | R$ $fareValue | $distanceValue km")
                RadarCoordinator.addLog("Jarvis Ghost Eye: Interceptando pacotes de dados do $appName... Analisando ROI Preditivo.", LogType.INFO)

                // Trigger analysis via Coordinator Service
                val intent = Intent(this, RadarCoordinatorService::class.java).apply {
                    putExtra("ACCESSIBILITY_OFFER", true)
                    putExtra("APP_NAME", appName)
                    putExtra("FARE_VALUE", fareValue)
                    putExtra("DISTANCE_VALUE", distanceValue)
                    putExtra("PICKUP_ADDRESS", "Interceptação Ghost")
                    putExtra("DELIVERY_ADDRESS", text.take(60))
                    putExtra("BACKGROUND_OFFER", true)
                }
                startService(intent)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        if (sbn.key == latestSbnKey) {
            Log.d(TAG, "WhatsApp notification dismissed by user")
        }
    }

    private fun sendReply(action: Notification.Action, message: String) {
        val intent = Intent()
        val bundle = Bundle()
        
        for (remoteInput in action.remoteInputs) {
            bundle.putCharSequence(remoteInput.resultKey, message)
        }
        
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        try {
            action.actionIntent.send(this, 0, intent)
            Log.d(TAG, "WhatsApp notification reply executed successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute reply actionIntent", e)
            RadarCoordinator.addLog("Jarvis WhatsApp: Erro ao enviar resposta: ${e.message}", LogType.ALERT)
        }
    }
}
