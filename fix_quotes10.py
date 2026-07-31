with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("window.logGhostAction(\"Aceite manual detectado. Contador de aceites consecutivos zerado.`);", "window.logGhostAction(\"Aceite manual detectado. Contador de aceites consecutivos zerado.\");")

with open('index.html', 'w') as f:
    f.write(content)
