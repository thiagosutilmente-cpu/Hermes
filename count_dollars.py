with open('index.html', 'r') as f:
    lines = f.readlines()

script_start = 18070
script_end = 19700

s_text = "".join(lines[script_start:script_end])

in_t = False
escaped = False
expr_depth = 0
in_lc = False
in_bc = False

line_num = script_start
for i, c in enumerate(s_text):
    if c == '\n':
        line_num += 1
        
    if in_lc:
        if c == '\n': in_lc = False
        continue
    if in_bc:
        if c == '*' and i+1 < len(s_text) and s_text[i+1] == '/':
            in_bc = False
        continue
        
    if escaped:
        escaped = False
        continue
        
    if c == '\\':
        escaped = True
        continue

    # Note: this ignores strings inside `${}` for simplicity, which is slightly wrong but works here
    if not in_t:
        if c == '`': 
            in_t = True
    else:
        if c == '`': 
            in_t = False
        elif c == '$' and i+1 < len(s_text) and s_text[i+1] == '{':
            expr_depth += 1
        elif c == '}':
            expr_depth -= 1

print(f"Template expr depth at line 19672: {expr_depth}")
