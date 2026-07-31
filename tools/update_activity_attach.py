import re

activity_path = 'app/src/main/java/com/example/MainActivity.kt'
with open(activity_path, 'r') as f:
    content = f.read()

target = """class MainActivity : ComponentActivity() {"""
replacement = """class MainActivity : ComponentActivity() {

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
    with open(activity_path, 'w') as f:
        f.write(content)
    print("Updated activity attachBaseContext")
else:
    print("Target not found")
