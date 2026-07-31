import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

pattern = r'if \(!window\.trafficMapInstance\) \{'
replacement = r'''if (true) {
        if (window.trafficMapInstance && window.trafficMapInstance.getDiv() === mapDiv) {
            // Already initialized for this div
        } else {
            // Either first time or div was recreated
            if (window.trafficMapInstance) {
                // Clear old map event listeners if any
                google.maps.event.clearInstanceListeners(window.trafficMapInstance);
            }
'''

content = re.sub(pattern, replacement, content)

with open(file_path, 'w') as f:
    f.write(content)

