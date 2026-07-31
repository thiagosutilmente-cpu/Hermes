import re

file_path = 'app/src/main/java/com/example/coordinator/RadarCoordinator.kt'
with open(file_path, 'r') as f:
    content = f.read()

pattern = r'putString\("jarvis_voice_style", cloudSettings\.jarvisVoiceStyle\)'
repl = r'''putString("jarvis_voice_style", cloudSettings.jarvisVoiceStyle)
                        putString("jarvis_voice_engine", cloudSettings.jarvisVoiceEngine)
                        putFloat("jarvis_voice_pitch", cloudSettings.jarvisVoicePitch)
                        putFloat("jarvis_voice_rate", cloudSettings.jarvisVoiceRate)
                        putFloat("jarvis_voice_volume", cloudSettings.jarvisVoiceVolume)
                        putBoolean("jarvis_continuous_frequency", cloudSettings.jarvisContinuousFrequency)'''

content = re.sub(pattern, repl, content)

with open(file_path, 'w') as f:
    f.write(content)

