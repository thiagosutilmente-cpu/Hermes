import re
manifest_path = 'app/src/main/AndroidManifest.xml'
with open(manifest_path, 'r') as f:
    content = f.read()

content = content.replace('</queries>', '</queries>\n    <attribution android:tag="" android:label="@string/app_name" />')

with open(manifest_path, 'w') as f:
    f.write(content)
