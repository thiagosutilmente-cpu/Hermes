with open('index.html', 'r') as f:
    lines = f.readlines()

script_start = 18070
script_end = 35000

s_text = "".join(lines[script_start:script_end])

def find_unclosed(text):
    in_s = False
    in_d = False
    in_t = False
    in_lc = False
    in_bc = False
    escaped = False
    
    for i, c in enumerate(text):
        if in_lc:
            if c == '\n': in_lc = False
            continue
        if in_bc:
            if c == '*' and i+1 < len(text) and text[i+1] == '/':
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
            elif c == '`': in_t = True
            elif c == '/' and i+1 < len(text):
                if text[i+1] == '/': in_lc = True
                elif text[i+1] == '*': in_bc = True
        else:
            if in_s and c == "'": in_s = False
            elif in_d and c == '"': in_d = False
            elif in_t and c == '`': in_t = False

    print(f"in_s: {in_s}, in_d: {in_d}, in_t: {in_t}, in_lc: {in_lc}, in_bc: {in_bc}")

find_unclosed(s_text)
