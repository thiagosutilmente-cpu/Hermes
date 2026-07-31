import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

replacement = """
      if (window.trafficMapInstance) {
          // If we already have an instance, we should clear it or we can just create a new one since the DOM changed
      }
      
      window.trafficMapInstance = new google.maps.Map(mapDiv, {
"""

content = content.replace("      if (!window.trafficMapInstance) {\n        window.trafficMapInstance = new google.maps.Map(mapDiv, {", replacement)

# We also need to close the `if (!window.trafficMapInstance)` block. Wait, the block was:
# if (!window.trafficMapInstance) { ... }
# Let's just remove the `if (!window.trafficMapInstance) {` and the matching `}`.

