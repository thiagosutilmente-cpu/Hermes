with open('index.html', 'r') as f:
    content = f.read()

# Replace "}, 45); }" with "}, 45);"
content = content.replace("}, 45); } // Alta velocidade de animação", "}, 45); // Alta velocidade de animação")

with open('index.html', 'w') as f:
    f.write(content)
