import re

with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'r') as f:
    content = f.read()

data_class = """
data class GeofenceZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Nova Zona",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 1000f,
    val isDangerZone: Boolean = false,
    val customVoiceAlert: String = "",
    val active: Boolean = true
)

data class RadarSettings("""

if 'data class GeofenceZone(' not in content:
    content = content.replace('data class RadarSettings(', data_class)

settings_target = """    val licenseExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // +30 dias"""
settings_replacement = """    val licenseExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // +30 dias
    val geofenceZones: List<GeofenceZone> = emptyList(),"""

if 'val geofenceZones: List<GeofenceZone>' not in content:
    content = content.replace(settings_target, settings_replacement)

with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'w') as f:
    f.write(content)

print("Classes Added")
