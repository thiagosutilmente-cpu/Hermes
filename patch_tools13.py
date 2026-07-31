import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Just place it immediately before `<div id="sovereignFloatingHUD"`
    idx_hud = content.find('<div id="sovereignFloatingHUD"')
    if idx_hud != -1:
        content = content[:idx_hud] + tools_content + '\n' + content[idx_hud:]
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved!")
    else:
        print("Could not find HUD")
