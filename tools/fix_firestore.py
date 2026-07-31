import re

file_path = 'app/src/main/java/com/example/data/FirestoreManager.kt'

with open(file_path, 'r') as f:
    content = f.read()

content = re.sub(
    r'\n\s+,\n\s+quickReply1Cmd',
    r'\n                        defaultNavigationApp = data["defaultNavigationApp"] as? String ?: "waze",\n                        quickReply1Cmd',
    content
)

with open(file_path, 'w') as f:
    f.write(content)

