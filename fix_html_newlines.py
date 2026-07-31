import re

with open('index.html', 'r') as f:
    content = f.read()

# Replace literal '\n' outside of javascript strings where they might have leaked
content = content.replace("<head>\\n  <!-- D3.js -->\\n  <script src=\"https://d3js.org/d3.v7.min.js\"></script>", "<head>\n  <!-- D3.js -->\n  <script src=\"https://d3js.org/d3.v7.min.js\"></script>")

with open('index.html', 'w') as f:
    f.write(content)
