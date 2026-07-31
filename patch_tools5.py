import sys

with open('index.html', 'r') as f:
    content = f.read()

# I will find CAIXA DE FERRAMENTAS DO SISTEMA
idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_aether = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')

if idx_tools != -1 and idx_aether != -1:
    tools_content = content[idx_tools:idx_aether]
    content = content[:idx_tools] + content[idx_aether:]

    # Now I will place it at the very beginning of all these custom tools. Let's find "<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->"
    idx_obfuscation = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')
    if idx_obfuscation != -1:
        content = content[:idx_obfuscation] + tools_content + '\n      ' + content[idx_obfuscation:]
        
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved!")
    else:
        print("Could not find obfuscation")
