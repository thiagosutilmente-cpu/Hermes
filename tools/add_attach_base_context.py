import os
import re

code_to_add = """
    override fun attachBaseContext(base: android.content.Context) {
        val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            base.createAttributionContext("Radar")
        } else {
            base
        }
        super.attachBaseContext(context)
    }
"""

files = [
    'app/src/main/java/com/example/RadarApplication.kt',
    'app/src/main/java/com/example/MainActivity.kt',
    'app/src/main/java/com/example/service/RadarCoordinatorService.kt',
    'app/src/main/java/com/example/service/RadarAccessibilityService.kt',
    'app/src/main/java/com/example/service/RadarNotificationListenerService.kt'
]

for p in files:
    if os.path.exists(p):
        with open(p, 'r') as f:
            content = f.read()
        
        if 'attachBaseContext' not in content:
            # Find the first { after class declaration
            # This is a bit tricky, let's just find "class Name : SuperClass() {" or similar
            class_match = re.search(r'(class\s+[^{]+{)', content)
            if class_match:
                insert_pos = class_match.end()
                new_content = content[:insert_pos] + code_to_add + content[insert_pos:]
                with open(p, 'w') as f:
                    f.write(new_content)
                print(f"Added to {p}")
            else:
                print(f"Could not find class declaration in {p}")
        else:
            print(f"attachBaseContext already exists in {p}")

