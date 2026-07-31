import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Place right below the "<div id="realtimeDashboard""
    idx_app_container = content.find('<div id="realtimeDashboard"')
    if idx_app_container != -1:
        content = content[:idx_app_container] + tools_content + '\n' + content[idx_app_container:]
        
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved above realtimeDashboard!")
    else:
        print("Could not find realtimeDashboard")
