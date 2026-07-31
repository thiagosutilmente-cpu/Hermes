import sys

def trace_back(filename, target_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    target_idx = target_line - 1
    
    # We want to find the opening brace that corresponds to the closing brace at target_line
    # We'll traverse backwards from target_idx
    
    # Find the closing brace on the target line
    line = lines[target_idx]
    brace_pos = line.rfind('}')
    if brace_pos == -1:
        print("No closing brace on target line")
        return
        
    stack = 1 # We start with the brace we found
    
    # Check the rest of the target line backwards
    for char in reversed(line[:brace_pos]):
        if char == '}':
            stack += 1
        elif char == '{':
            stack -= 1
            if stack == 0:
                print(f"Opening brace is on the same line: {target_line}")
                return

    # Check previous lines
    for i in range(target_idx - 1, -1, -1):
        line = lines[i]
        for char in reversed(line):
            if char == '}':
                stack += 1
            elif char == '{':
                stack -= 1
                if stack == 0:
                    print(f"Opening brace found at line {i + 1}")
                    print(lines[i].strip())
                    return

trace_back('app/src/main/java/com/example/MainActivity.kt', 5448)
