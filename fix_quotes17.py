with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("do ${window.driverFirstName || \\'Piloto\\'} para mimetismo", "do ${window.driverFirstName || 'Piloto'} para mimetismo")

with open('index.html', 'w') as f:
    f.write(content)
