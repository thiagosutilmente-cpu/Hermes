with open('index.html', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if i > 18000 and i < 35000:
        c = line.count('`')
        if c % 2 != 0:
            print(f"Line {i+1}: {line.strip()}")
