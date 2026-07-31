import sys

def check_braces(filename, start_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    # start_line is 1-indexed
    start_idx = start_line - 1
    
    stack = []
    for i in range(start_idx, len(lines)):
        line = lines[i]
        for char in line:
            if char == '{':
                stack.append('{')
            elif char == '}':
                if stack:
                    stack.pop()
                    if not stack:
                        print(f"Brace closed at line {i + 1}")
                        return
                else:
                    print(f"Unmatched closing brace at line {i + 1}")
                    return

check_braces('app/src/main/java/com/example/MainActivity.kt', 4538)
