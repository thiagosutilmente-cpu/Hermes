import sys

def check_balance(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    stack = []
    for i, char in enumerate(content):
        if char == '{':
            stack.append(i)
        elif char == '}':
            if stack:
                stack.pop()
            else:
                print(f"Unmatched closing brace at index {i}")
                return
    if stack:
        print(f"Unmatched opening braces left: {len(stack)}")
    else:
        print("Perfectly balanced!")

check_balance('app/src/main/java/com/example/MainActivity.kt')
