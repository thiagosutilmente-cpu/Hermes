import re

with open('index.html', 'r') as f:
    content = f.read()

target = """       unsubscribeSettings = subscribeToDriverSettings(currentDriverId, (settings) => {
         if (settings) {
           currentDriverSettings = settings;"""

replacement = """       unsubscribeSettings = subscribeToDriverSettings(currentDriverId, (settings) => {
         if (settings) {
           currentDriverSettings = settings;
           window.currentDriverSettings = settings;
           if (window.renderGeofenceZonesList) window.renderGeofenceZonesList();"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed subscribeToDriverSettings")
else:
    print("Target not found")
