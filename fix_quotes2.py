with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("document.getElementById('weeklyCycleCountdown`);", "document.getElementById('weeklyCycleCountdown');")

with open('index.html', 'w') as f:
    f.write(content)
