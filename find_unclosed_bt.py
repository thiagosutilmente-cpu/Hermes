with open('index.html', 'r') as f:
    lines = f.readlines()

s_lines = lines[18070:21000]
bt_count = 0
for i, line in enumerate(s_lines):
    # Only count actual backticks outside of single/double quotes and comments!
    # A naive count is usually enough if we just grep the odd lines.
    c = line.count('`')
    if c % 2 != 0:
        print(f"Line {i+18070}: {line.strip()}")
