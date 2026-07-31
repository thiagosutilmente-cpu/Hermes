with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("console.error(\"Erro ao registrar rejeição no Firestore:\`, error);", "console.error(\"Erro ao registrar rejeição no Firestore:\", error);")

with open('index.html', 'w') as f:
    f.write(content)
