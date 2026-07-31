with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("? detectedBottleneck.name : `vias diretas\"}", "? detectedBottleneck.name : \"vias diretas\"}")

with open('index.html', 'w') as f:
    f.write(content)
