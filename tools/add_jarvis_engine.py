import re

file_path = 'app/src/main/java/com/example/data/FirestoreManager.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace(
    '                        jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,',
    '                        jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,\n                        jarvisVoiceEngine = data["jarvisVoiceEngine"] as? String ?: "NEURAL",'
)

content = content.replace(
    '                    jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,',
    '                    jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,\n                    jarvisVoiceEngine = data["jarvisVoiceEngine"] as? String ?: "NEURAL",'
)

content = content.replace(
    '            "jarvisOverlayMode" to settings.jarvisOverlayMode,',
    '            "jarvisOverlayMode" to settings.jarvisOverlayMode,\n            "jarvisVoiceEngine" to settings.jarvisVoiceEngine,'
)

with open(file_path, 'w') as f:
    f.write(content)

