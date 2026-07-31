with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("<span class=\"material-symbols-rounded\" style=\"font-size: 14px;`>navigation</span>", "<span class=\"material-symbols-rounded\" style=\"font-size: 14px;\">navigation</span>")

with open('index.html', 'w') as f:
    f.write(content)
