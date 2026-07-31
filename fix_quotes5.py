with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("window.showToast(`Resumo Diário: \" + msg, \"success\");", "window.showToast(\"Resumo Diário: \" + msg, \"success\");")

with open('index.html', 'w') as f:
    f.write(content)
