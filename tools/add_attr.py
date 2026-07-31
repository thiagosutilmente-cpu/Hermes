import re

manifest_path = 'app/src/main/AndroidManifest.xml'
with open(manifest_path, 'r') as f:
    content = f.read()
    
if '<attribution' not in content:
    content = content.replace('<application', '    <attribution android:tag="Radar" android:label="@string/app_name" />\n    <application')
    with open(manifest_path, 'w') as f:
        f.write(content)
    print("Added attribution tag")
else:
    print("Attribution tag already exists")
