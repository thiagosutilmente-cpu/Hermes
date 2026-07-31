import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    lines = f.readlines()

def fix_line(line):
    # Fix "error`); -> "error");
    line = re.sub(r'\"error\`\)\;', r'"error");', line)
    # Fix "info`); -> "info");
    line = re.sub(r'\"info\`\)\;', r'"info");', line)
    # Fix "warning`); -> "warning");
    line = re.sub(r'\"warning\`\)\;', r'"warning");', line)
    # Fix `success"); -> `success`); or "success");
    line = re.sub(r'\`success"\)\;', r'"success");', line)
    # Fix `warning"); -> "warning");
    line = re.sub(r'\`warning"\)\;', r'"warning");', line)
    # Fix `error"); -> "error");
    line = re.sub(r'\`error"\)\;', r'"error");', line)
    # Fix styleKey typo: "${styleKey}` -> "${styleKey}"
    line = re.sub(r'\"\$(\{styleKey\})\`', r'"$\1"', line)
    
    # Fix `... ", "info");
    line = re.sub(r'\`([^\`]+)\", \"info\"\)\;', r'`\1`, "info");', line)
    # Fix `... ", "warning");
    line = re.sub(r'\`([^\`]+)\", \"warning\"\)\;', r'`\1`, "warning");', line)
    # Fix `... ", "success");
    line = re.sub(r'\`([^\`]+)\", \"success\"\)\;', r'`\1`, "success");', line)
    # Fix `... ", "error");
    line = re.sub(r'\`([^\`]+)\", \"error\"\)\;', r'`\1`, "error");', line)

    return line

for i in range(len(lines)):
    if 'showToast' in lines[i]:
        lines[i] = fix_line(lines[i])

with open(file_path, 'w') as f:
    f.writelines(lines)
