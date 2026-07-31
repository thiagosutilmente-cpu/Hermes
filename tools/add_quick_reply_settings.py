import re

file_path = 'app/src/main/java/com/example/coordinator/RadarCoordinator.kt'

with open(file_path, 'r') as f:
    content = f.read()

load_pattern = r'(val newSettings = RadarSettings\(\s+.*?)(defaultNavigationApp = prefs\.getString\("default_navigation_app", "waze"\) \?\: "waze"\s*\n\s*\))'

def repl_load(m):
    return m.group(1) + """defaultNavigationApp = prefs.getString("default_navigation_app", "waze") ?: "waze",
                quickReply1Cmd = prefs.getString("quick_reply_1_cmd", "cheguei") ?: "cheguei",
                quickReply1Text = prefs.getString("quick_reply_1_text", "Olá, já estou no local aguardando com o seu pedido.") ?: "Olá, já estou no local aguardando com o seu pedido.",
                quickReply2Cmd = prefs.getString("quick_reply_2_cmd", "subindo") ?: "subindo",
                quickReply2Text = prefs.getString("quick_reply_2_text", "Olá, estou subindo para entregar na sua porta.") ?: "Olá, estou subindo para entregar na sua porta.",
                quickReply3Cmd = prefs.getString("quick_reply_3_cmd", "trânsito") ?: "trânsito",
                quickReply3Text = prefs.getString("quick_reply_3_text", "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve.") ?: "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve."
            )"""

new_content = re.sub(load_pattern, repl_load, content, flags=re.DOTALL)

if new_content == content:
    print("No changes to load function")
else:
    print("Added fields to load function")

with open(file_path, 'w') as f:
    f.write(new_content)

