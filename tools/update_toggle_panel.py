import re

with open('index.html', 'r') as f:
    content = f.read()

target = """        if (panel) panel.style.display = 'flex';"""
replacement = """        if (panel) panel.style.display = 'flex';
        if (window.renderGeofenceZonesList) window.renderGeofenceZonesList();"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed toggle")
else:
    print("Not found toggle")
