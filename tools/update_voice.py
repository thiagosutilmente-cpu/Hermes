import re

voice_path = 'app/src/main/java/com/example/voice/VoiceManager.kt'
with open(voice_path, 'r') as f:
    content = f.read()

content = content.replace('tts = TextToSpeech(context, this)', 'tts = TextToSpeech(context.applicationContext, this)')

with open(voice_path, 'w') as f:
    f.write(content)
