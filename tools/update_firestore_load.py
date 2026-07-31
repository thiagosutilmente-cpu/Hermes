import re

file_path = 'app/src/main/java/com/example/data/FirestoreManager.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern_load = r'(defaultNavigationApp = data\["defaultNavigationApp"\] as\? String \?\: "waze")'

repl_load = """\1,
                    quickReply1Cmd = data["quickReply1Cmd"] as? String ?: "cheguei",
                    quickReply1Text = data["quickReply1Text"] as? String ?: "Olá, já estou no local aguardando com o seu pedido.",
                    quickReply2Cmd = data["quickReply2Cmd"] as? String ?: "subindo",
                    quickReply2Text = data["quickReply2Text"] as? String ?: "Olá, estou subindo para entregar na sua porta.",
                    quickReply3Cmd = data["quickReply3Cmd"] as? String ?: "trânsito",
                    quickReply3Text = data["quickReply3Text"] as? String ?: "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve." """

new_content = re.sub(pattern_load, repl_load, content)

if new_content == content:
    print("Failed to replace load!")
else:
    print("Replaced load!")

with open(file_path, 'w') as f:
    f.write(new_content)
