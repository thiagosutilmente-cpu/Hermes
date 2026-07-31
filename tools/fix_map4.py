import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace(
    '        // --------------------------------------------------------------\n      }\n    };\n\n    window.suggestAlternativeRoute = function() {',
    '        // --------------------------------------------------------------\n      }\n      }\n    };\n\n    window.suggestAlternativeRoute = function() {'
)

with open(file_path, 'w') as f:
    f.write(content)

