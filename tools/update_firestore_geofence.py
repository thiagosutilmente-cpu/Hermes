import re

with open('app/src/main/java/com/example/data/FirestoreManager.kt', 'r') as f:
    content = f.read()

# Add geofenceZones parsing in listenToSettings
listen_target = """                        emergencyMessage = data["emergencyMessage"] as? String ?: "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}",
                        defaultNavigationApp = data["defaultNavigationApp"] as? String ?: "waze"
"""
listen_repl = listen_target.strip() + """,
                        geofenceZones = (data["geofenceZones"] as? List<Map<String, Any>>)?.map {
                            com.example.coordinator.GeofenceZone(
                                id = it["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                                name = it["name"] as? String ?: "Nova Zona",
                                latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                                longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                                radiusMeters = (it["radiusMeters"] as? Number)?.toFloat() ?: 1000f,
                                isDangerZone = it["isDangerZone"] as? Boolean ?: false,
                                customVoiceAlert = it["customVoiceAlert"] as? String ?: "",
                                active = it["active"] as? Boolean ?: true
                            )
                        } ?: emptyList()
"""

if listen_target.strip() in content:
    content = content.replace(listen_target.strip(), listen_repl.strip())
    print("Fixed listenToSettings/loadSettings")
else:
    print("Target not found for listenToSettings")

# Add geofenceZones to saveSettings map
save_target = """            "emergencyMessage" to settings.emergencyMessage,
            "defaultNavigationApp" to settings.defaultNavigationApp
"""
save_repl = save_target.strip() + """,
            "geofenceZones" to settings.geofenceZones.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "latitude" to it.latitude,
                    "longitude" to it.longitude,
                    "radiusMeters" to it.radiusMeters,
                    "isDangerZone" to it.isDangerZone,
                    "customVoiceAlert" to it.customVoiceAlert,
                    "active" to it.active
                )
            }
"""

if save_target.strip() in content:
    content = content.replace(save_target.strip(), save_repl.strip())
    print("Fixed saveSettings")
else:
    print("Target not found for saveSettings")

with open('app/src/main/java/com/example/data/FirestoreManager.kt', 'w') as f:
    f.write(content)
