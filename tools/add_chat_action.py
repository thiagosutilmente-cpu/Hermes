import re

file_path = 'app/src/main/java/com/example/voice/JarvisPersonaEngine.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'(6\. NEURAL_HANDSHAKE.*?)\n(        7\. SURREAL_CLICK.*?)'

repl = r'\1\n        - SEND_CHAT_MESSAGE: Enviar mensagem para o chat do aplicativo atual. (Coloque o texto da mensagem no campo memory_content).\n\2'

content = re.sub(pattern, repl, content)

pattern2 = r'("action": "AUTHORIZE_OPERATION \| ABORT_MISSION \| INDEX_STRATEGY \| PREDICTIVE_INSIGHT \| OVERCLOCK_SYSTEM \| NEURAL_HANDSHAKE \| SURREAL_CLICK)'

repl2 = r'"action": "AUTHORIZE_OPERATION | ABORT_MISSION | INDEX_STRATEGY | PREDICTIVE_INSIGHT | OVERCLOCK_SYSTEM | NEURAL_HANDSHAKE | SURREAL_CLICK | SEND_CHAT_MESSAGE"'

content = re.sub(pattern2, repl2, content)

with open(file_path, 'w') as f:
    f.write(content)
