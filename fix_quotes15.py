with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("SecureStorage.getItem('abs_last_action_time') || '0`);", "SecureStorage.getItem('abs_last_action_time') || '0');")

with open('index.html', 'w') as f:
    f.write(content)
