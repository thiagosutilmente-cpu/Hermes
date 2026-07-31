import re

with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'r') as f:
    content = f.read()

target = """                    com.example.voice.VoiceManager.getInstance(appContext!!)?.speak(customAlert)
                    triggerJarvisResponse(customAlert, if (isDanger) "BUG" else "SUGGESTION")"""

replacement = """                    voiceManager?.speak(customAlert)
                    addLog(customAlert, if (isDanger) LogType.ALERT else LogType.INFO)"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/coordinator/RadarCoordinator.kt', 'w') as f:
        f.write(content)
    print("Fixed")
else:
    print("Not found")

