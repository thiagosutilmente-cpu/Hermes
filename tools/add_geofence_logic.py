import re

with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'r') as f:
    content = f.read()

target = "    private var deliveryLastLocation: Location? = null"
replacement = target + "\n    private val activeGeofenceZones = mutableSetOf<String>()"

if target in content:
    content = content.replace(target, replacement)
    
update_loc_target = """        // Recalcula a rota multi-app dinamicamente com base na nova posição do motorista
        com.example.util.MultiAppOrderManager.recalculateRoute()"""

update_loc_replacement = """        // Geofence / Zonas de Risco checks
        checkGeofences(location)

        // Recalcula a rota multi-app dinamicamente com base na nova posição do motorista
        com.example.util.MultiAppOrderManager.recalculateRoute()"""

if update_loc_target in content:
    content = content.replace(update_loc_target, update_loc_replacement)
    
check_func = """
    private fun checkGeofences(location: Location) {
        val zones = _settings.value.geofenceZones.filter { it.active }
        val currentZones = mutableSetOf<String>()
        
        zones.forEach { zone ->
            val zoneLoc = Location("").apply {
                latitude = zone.latitude
                longitude = zone.longitude
            }
            val distance = location.distanceTo(zoneLoc)
            if (distance <= zone.radiusMeters) {
                currentZones.add(zone.id)
                if (!activeGeofenceZones.contains(zone.id)) {
                    // Entered zone
                    val isDanger = zone.isDangerZone
                    val prefix = if (isDanger) "Atenção. Você entrou na zona de risco:" else "Você entrou na área:"
                    val customAlert = zone.customVoiceAlert.takeIf { it.isNotBlank() } ?: "$prefix ${zone.name}."
                    
                    voiceManager?.speak(customAlert)
                    
                    // Show a quick visual alert if desired (using jarvis alert)
                    triggerJarvisResponse(customAlert, if (isDanger) "BUG" else "SUGGESTION")
                }
            }
        }
        
        // Check exited zones (optional, we could alert exiting but usually entering is enough)
        /*
        val exited = activeGeofenceZones - currentZones
        exited.forEach { exitedId ->
            val zone = zones.find { it.id == exitedId }
            if (zone != null) {
                voiceManager?.speak("Você saiu da área ${zone.name}.")
            }
        }
        */
        
        activeGeofenceZones.clear()
        activeGeofenceZones.addAll(currentZones)
    }
"""

content = content + "\n" + check_func

with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'w') as f:
    f.write(content)

print("Logic Added")
