import re

with open('app/src/main/java/com/example/service/RadarCoordinatorService.kt', 'r') as f:
    content = f.read()

target = """        // Initialize FusedLocation with attribution tag for privacy
        try {
            val locationContext = this
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)"""

replacement = """        // Initialize FusedLocation with attribution tag for privacy
        try {
            val locationContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                this.createAttributionContext("Location")
            } else {
                this
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/service/RadarCoordinatorService.kt', 'w') as f:
        f.write(content)
    print("Fixed attribution context")
else:
    print("Target not found")
