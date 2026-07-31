import sys

with open('index.html', 'r') as f:
    content = f.read()

# Fix the duplicate JS
# Oh I see there is duplicated toggleToolsModal
import re

content = re.sub(r'    window\.toggleToolsModal = function\(\) \{.*?    \};\n\n', '', content, flags=re.DOTALL, count=1)


with open('index.html', 'w') as f:
    f.write(content)

print("Duplicates removed")
