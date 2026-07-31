with open('index.html', 'r') as f:
    content = f.read()

content = content.replace('console.error("Erro ao ler ganhos semanais do DB`, e);', 'console.error("Erro ao ler ganhos semanais do DB", e);')

with open('index.html', 'w') as f:
    f.write(content)
