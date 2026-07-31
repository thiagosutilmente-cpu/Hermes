with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("textContent || '${window.driverFirstName || 'Piloto'}';", "textContent || `${window.driverFirstName || 'Piloto'}`;")

with open('index.html', 'w') as f:
    f.write(content)
