with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("getElementById('completedRidesDisplay')?.innerText || \"0`;", "getElementById('completedRidesDisplay')?.innerText || \"0\";")

with open('index.html', 'w') as f:
    f.write(content)
