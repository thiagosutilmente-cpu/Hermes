import re

with open('index.html', 'r') as f:
    html = f.read()

pattern = r"function showActiveRouteSequencePanel\(\) \{.*?\}"

replacement = """function showActiveRouteSequencePanel() {
      const cardsContainer = document.getElementById('cardsContainer');
      const apiStacksContainer = document.getElementById('apiStacksContainer');
      const stackHeader = document.querySelector('.side-panel .stack-panel-header');
      if (cardsContainer) cardsContainer.style.display = 'none';
      if (apiStacksContainer) apiStacksContainer.style.display = 'none';
      if (stackHeader) stackHeader.style.display = 'none';
      
      const panel = document.getElementById('activeRouteSequencePanel');
      if (panel) {
        panel.style.display = 'block';
        try { panel.scrollIntoView({ behavior: 'smooth' }); } catch(e) {}
      }
    }"""

html = re.sub(pattern, replacement, html, flags=re.DOTALL)

with open('index.html', 'w') as f:
    f.write(html)
