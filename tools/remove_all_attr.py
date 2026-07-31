import re
import os

manifest_path = 'app/src/main/AndroidManifest.xml'
with open(manifest_path, 'r') as f:
    content = f.read()
content = re.sub(r'\s*<attribution[^>]+/>', '', content)
with open(manifest_path, 'w') as f:
    f.write(content)

def remove_attach_base_context(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Simple regex to remove the overridden attachBaseContext block
    pattern = r'\s*override fun attachBaseContext\(base: android\.content\.Context\) \{.*?\n    \}'
    content = re.sub(pattern, '', content, flags=re.DOTALL)
    
    # also with Context
    pattern2 = r'\s*override fun attachBaseContext\(base: Context\) \{.*?\n    \}'
    content = re.sub(pattern2, '', content, flags=re.DOTALL)
    
    with open(file_path, 'w') as f:
        f.write(content)

files = [
    'app/src/main/java/com/example/RadarApplication.kt',
    'app/src/main/java/com/example/MainActivity.kt',
    'app/src/main/java/com/example/service/RadarCoordinatorService.kt',
    'app/src/main/java/com/example/service/RadarAccessibilityService.kt',
    'app/src/main/java/com/example/service/RadarNotificationListenerService.kt'
]
for p in files:
    if os.path.exists(p):
        remove_attach_base_context(p)

print("Removed all attribution and attachBaseContext")
