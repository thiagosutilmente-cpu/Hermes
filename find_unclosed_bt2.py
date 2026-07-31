with open('index.html', 'r') as f:
    lines = f.readlines()

s_lines = lines[21000:35000]
for i, line in enumerate(s_lines):
    c = line.count('`')
    if c % 2 != 0:
        print(f"Line {i+21000}: {line.strip()}")
