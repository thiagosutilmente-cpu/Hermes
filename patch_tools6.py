import sys

with open('index.html', 'r') as f:
    content = f.read()

# I want the tools box BEFORE the QUANTUM OBFUSCATION ENGINE and AFTER the NEURAL SINGULARITY? 
# Ah, I added Neural Singularity before Quantum Obfuscation in a previous step. Let's see where Neural Singularity is.
idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_obfuscation = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')

if idx_tools != -1 and idx_obfuscation != -1:
    tools_content = content[idx_tools:idx_obfuscation]
    content = content[:idx_tools] + content[idx_obfuscation:]

    # Place right before NEURAL SINGULARITY instead
    idx_singularity = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')
    if idx_singularity != -1:
        content = content[:idx_singularity] + tools_content + '\n      ' + content[idx_singularity:]
        
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved!")
    else:
        print("Could not find singularity")
