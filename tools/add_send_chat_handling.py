import re

file_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'("PREDICTIVE_INSIGHT" -> \{\s*RadarCoordinator\.addLog\("INSIGHT: \$\{result\.thoughtProcess\}", com\.example\.coordinator\.LogType\.INFO\)\s*\})'

repl = r"""\1
                        "SEND_CHAT_MESSAGE" -> {
                            if (result.memoryContent.isNotEmpty()) {
                                val intent = android.content.Intent("com.example.ACTION_AUTOFILL_CHAT")
                                intent.putExtra("MESSAGE_TEXT", result.memoryContent)
                                intent.setPackage(packageName)
                                sendBroadcast(intent)
                                RadarCoordinator.addLog("Voz: Jarvis enviou mensagem no chat - ${result.memoryContent}", com.example.coordinator.LogType.SUCCESS)
                            }
                        }"""

new_content = re.sub(pattern, repl, content)

if new_content == content:
    print("Failed to replace!")
else:
    print("Replaced!")
    with open(file_path, 'w') as f:
        f.write(new_content)
