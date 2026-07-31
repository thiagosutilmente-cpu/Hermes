import sys

def find_closing(filename, target_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    target_idx = target_line - 1
    
    # We want to find the closing brace that corresponds to the opening brace at target_line
    line = lines[target_idx]
    brace_pos = line.find('{')
    if brace_pos == -1:
        print("No opening brace on target line")
        return
        
    stack = 1
    
    for i in range(target_idx, len(lines)):
        l = lines[i]
        start_char = brace_pos + 1 if i == target_idx else 0
        for char in l[start_char:]:
            if char == '{':
                stack += 1
            elif char == '}':
                stack -= 1
                if stack == 0:
                    print(f"Closing brace found at line {i + 1}")
                    return

find_closing('app/src/main/java/com/example/MainActivity.kt', 2525)
