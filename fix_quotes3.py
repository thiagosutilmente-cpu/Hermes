with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("timeInput.value.replace(':', ' e `)}", "timeInput.value.replace(':', ' e ')}")

with open('index.html', 'w') as f:
    f.write(content)
