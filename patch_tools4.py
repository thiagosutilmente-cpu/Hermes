import sys

with open('index.html', 'r') as f:
    content = f.read()

# I will find CAIXA DE FERRAMENTAS DO SISTEMA
idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_singularity = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->', idx_tools)

if idx_tools != -1 and idx_singularity != -1:
    tools_content = content[idx_tools:idx_singularity]
    content = content[:idx_tools] + content[idx_singularity:]

    # Place tools before EMP (AETHER PROTOCOL)
    idx_aether = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')
    if idx_aether != -1:
        content = content[:idx_aether] + tools_content + '\n      ' + content[idx_aether:]
    
    with open('index.html', 'w') as f:
        f.write(content)
    print("Tools moved!")
