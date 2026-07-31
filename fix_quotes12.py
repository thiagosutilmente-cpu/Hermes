with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("descendo.\\`,", "descendo.`,")

with open('index.html', 'w') as f:
    f.write(content)
