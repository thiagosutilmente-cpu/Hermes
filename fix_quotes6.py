with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("const fullRouteText = (pickup + \" ` + delivery).toLowerCase();", "const fullRouteText = (pickup + \" \" + delivery).toLowerCase();")

with open('index.html', 'w') as f:
    f.write(content)
