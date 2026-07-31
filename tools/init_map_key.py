import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

init_code = """
      const savedMapKey = localStorage.getItem("googleMapsApiKey");
      if (savedMapKey && document.getElementById("settingsGoogleMapsApiKey")) {
        document.getElementById("settingsGoogleMapsApiKey").value = savedMapKey;
      }
"""

content = content.replace("window.addEventListener('DOMContentLoaded', () => {", "window.addEventListener('DOMContentLoaded', () => {\n" + init_code)

with open(file_path, 'w') as f:
    f.write(content)
