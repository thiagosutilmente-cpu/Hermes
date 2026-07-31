import re

service_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'
with open(service_path, 'r') as f:
    content = f.read()

content = content.replace('fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)', 'fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)')
content = content.replace('val locationContext = this\n            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)', 'val locationContext = applicationContext\n            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)')

with open(service_path, 'w') as f:
    f.write(content)
