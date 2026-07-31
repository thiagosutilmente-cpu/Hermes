import re

with open('index.html', 'r') as f:
    content = f.read()

# The css variable from before
css = """
    <style>
      @keyframes borderPulseDamping {
          0% { box-shadow: 0 0 0 0 rgba(0, 245, 212, 0.4); border-color: rgba(0, 245, 212, 0.5); }
          70% { box-shadow: 0 0 0 8px rgba(0, 245, 212, 0); border-color: rgba(0, 245, 212, 0.8); }
          100% { box-shadow: 0 0 0 0 rgba(0, 245, 212, 0); border-color: rgba(0, 245, 212, 0.25); }
      }
      .damping-pulse-active {
          animation: borderPulseDamping 1.5s infinite !important;
      }
"""

content = content.replace(css, "<style>")
# But I still want one of them!
content = content.replace("<style>", css, 1) # Only replace the first one

with open('index.html', 'w') as f:
    f.write(content)
