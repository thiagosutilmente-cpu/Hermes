import re

with open('index.html', 'r') as f:
    content = f.read()

target = """         if (window.showToast) window.showToast("Cerca Salva com Sucesso!", "success");"""
replacement = """         if (window.showToast) window.showToast("Cerca Salva com Sucesso!", "success");
         if (window.renderGeofenceZonesList) window.renderGeofenceZonesList();"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed save")
else:
    print("Not found save")
