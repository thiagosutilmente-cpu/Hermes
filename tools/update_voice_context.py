import re

voice_path = 'app/src/main/java/com/example/voice/VoiceManager.kt'
with open(voice_path, 'r') as f:
    content = f.read()

content = content.replace('context.getSystemService(Context.AUDIO_SERVICE)', 'context.applicationContext.getSystemService(Context.AUDIO_SERVICE)')

with open(voice_path, 'w') as f:
    f.write(content)
