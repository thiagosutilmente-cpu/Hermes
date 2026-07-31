import re

service_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'
with open(service_path, 'r') as f:
    content = f.read()

content = content.replace('val locationContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { this.createAttributionContext("Location") } else { this }', 'val locationContext = this')

with open(service_path, 'w') as f:
    f.write(content)
print("Removed attribution context")
