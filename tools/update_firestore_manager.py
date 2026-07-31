import re

file_path = 'app/src/main/java/com/example/data/FirestoreManager.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern_save = r'("defaultNavigationApp" to settings\.defaultNavigationApp,)'

repl_save = """\1
            "quickReply1Cmd" to settings.quickReply1Cmd,
            "quickReply1Text" to settings.quickReply1Text,
            "quickReply2Cmd" to settings.quickReply2Cmd,
            "quickReply2Text" to settings.quickReply2Text,
            "quickReply3Cmd" to settings.quickReply3Cmd,
            "quickReply3Text" to settings.quickReply3Text,"""

new_content = re.sub(pattern_save, repl_save, content)

if new_content == content:
    print("Failed to replace saveSettings!")
else:
    print("Replaced saveSettings!")

with open(file_path, 'w') as f:
    f.write(new_content)
