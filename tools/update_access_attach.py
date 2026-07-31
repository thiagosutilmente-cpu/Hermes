import re

service_path = 'app/src/main/java/com/example/service/RadarAccessibilityService.kt'
with open(service_path, 'r') as f:
    content = f.read()

target = """class RadarAccessibilityService : AccessibilityService() {"""
replacement = """class RadarAccessibilityService : AccessibilityService() {

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
    print("Updated accessibility service attachBaseContext")
else:
    print("Target not found")
