import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

# Fix cleanText.includes("...") mismatches
content = re.sub(r'includes\("([^\"]+)\`\)', r'includes("\1")', content)
content = re.sub(r'includes\(\`([^\`]+)\"\)', r'includes("\1")', content)

# Fix 'accepted`)
content = content.replace("'accepted`)", "'accepted')")

# Fix speakText("...`);
content = re.sub(r'speakText\(\"([^\"]+)\`\)\;', r'speakText("\1");', content)

# Fix addLog(..., `warning`);
content = re.sub(r'addLog\(\`([^\`]+)\`\,\s*\`warning\`\)\;', r'addLog(`\1`, "warning");', content)

# Fix showToast(..., `warning`);
content = re.sub(r'showToast\(\"([^\"]+)\"\,\s*\`warning\`\)\;', r'showToast("\1", "warning");', content)

with open(file_path, 'w') as f:
    f.write(content)
