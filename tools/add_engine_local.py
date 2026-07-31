import re

file_path = 'app/src/main/java/com/example/coordinator/RadarCoordinator.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace(
    'jarvisVoiceTone = prefs.getString("jarvis_voice_tone", "AMIGÁVEL") ?: "AMIGÁVEL",',
    'jarvisVoiceTone = prefs.getString("jarvis_voice_tone", "AMIGÁVEL") ?: "AMIGÁVEL",\n                jarvisVoiceEngine = prefs.getString("jarvis_voice_engine", "NEURAL") ?: "NEURAL",'
)

content = content.replace(
    'putString("jarvis_voice_tone", newSettings.jarvisVoiceTone)',
    'putString("jarvis_voice_tone", newSettings.jarvisVoiceTone)\n            putString("jarvis_voice_engine", newSettings.jarvisVoiceEngine)'
)

with open(file_path, 'w') as f:
    f.write(content)

