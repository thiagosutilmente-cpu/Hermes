import sys

with open('app/src/main/res/values/strings.xml', 'r') as f:
    content = f.read()

content = content.replace('<string name="app_name">My Application</string>', '<string name="app_name">Jarvis Sovereign V22</string>')

with open('app/src/main/res/values/strings.xml', 'w') as f:
    f.write(content)

print("Strings updated")
