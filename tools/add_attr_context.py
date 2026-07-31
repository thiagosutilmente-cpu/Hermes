import re

service_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'
with open(service_path, 'r') as f:
    content = f.read()

content = re.sub(
    r'val locationContext = this\n\s*fusedLocationClient = LocationServices\.getFusedLocationProviderClient\(locationContext\)',
    r'val locationContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { this.createAttributionContext("Location") } else { this }\n            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)',
    content
)

with open(service_path, 'w') as f:
    f.write(content)
print("Regex replaced")
