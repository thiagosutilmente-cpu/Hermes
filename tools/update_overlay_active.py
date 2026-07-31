import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = "val isOverlayActive = showProfilePanel || showConfigPanel"
replacement = "val isOverlayActive = showProfilePanel || showConfigPanel || showGeofenceModal"

if target in content:
    content = content.replace(target, replacement)
    
target_dismiss = "showProfilePanel = false; showConfigPanel = false"
replacement_dismiss = "showProfilePanel = false; showConfigPanel = false; showGeofenceModal = false"

if target_dismiss in content:
    content = content.replace(target_dismiss, replacement_dismiss)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Updated overlay")
