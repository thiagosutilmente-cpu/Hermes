import re

service_path = 'app/src/main/java/com/example/service/RadarNotificationListenerService.kt'
with open(service_path, 'r') as f:
    content = f.read()

target = """class RadarNotificationListenerService : NotificationListenerService() {"""
replacement = """class RadarNotificationListenerService : NotificationListenerService() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                base.createAttributionContext("Radar")
            } else {
                base
            }
        )
    }
"""

if target in content:
    content = content.replace(target, replacement)
    with open(service_path, 'w') as f:
        f.write(content)
    print("Updated notification service attachBaseContext")
else:
    print("Target not found")
