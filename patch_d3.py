import sys

with open('index.html', 'r') as f:
    content = f.read()

content = content.replace('<head>', '<head>\\n  <!-- D3.js -->\\n  <script src="https://d3js.org/d3.v7.min.js"></script>', 1)

with open('index.html', 'w') as f:
    f.write(content)

print("D3 injected!")
