import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Place right below the "Atividade da Concorrência / Mapa" or at the very top of the app container
    idx_app_container = content.find('<div class="app-container">')
    if idx_app_container != -1:
        # put it right inside the container
        content = content[:idx_app_container+27] + '\n\n' + tools_content + '\n' + content[idx_app_container+27:]
        
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved to the very top!")
    else:
        print("Could not find app container")
