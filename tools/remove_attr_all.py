import re

manifest_path = 'app/src/main/AndroidManifest.xml'
with open(manifest_path, 'r') as f:
    content = f.read()

content = re.sub(r'\s*<attribution[^>]+/>', '', content)

with open(manifest_path, 'w') as f:
    f.write(content)

