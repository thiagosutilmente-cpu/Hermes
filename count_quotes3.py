with open('index.html', 'r') as f:
    lines = f.readlines()

script_start = 18070
script_end = 35000

s_text = "".join(lines[script_start:script_end])

in_s = False
in_d = False
in_t = False
in_lc = False
in_bc = False
escaped = False

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
        
    if not in_s and not in_d and not in_t:
        if c == "'": in_s = True
        elif c == '"': in_d = True
        elif c == '`': 
            in_t = True
            if line_num > 19500 and line_num < 19700:
                print(f"Opened template at {line_num}")
        elif c == '/' and i+1 < len(s_text):
            if s_text[i+1] == '/': in_lc = True
            elif s_text[i+1] == '*': in_bc = True
    else:
        if in_s and c == "'": in_s = False
        elif in_d and c == '"': in_d = False
        elif in_t and c == '`': 
            in_t = False
            if line_num > 19500 and line_num < 19700:
                print(f"Closed template at {line_num}")

