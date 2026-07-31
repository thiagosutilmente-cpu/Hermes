with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("[`touchstart', 'mousedown', 'scroll', 'keydown']", "['touchstart', 'mousedown', 'scroll', 'keydown']")

with open('index.html', 'w') as f:
    f.write(content)
