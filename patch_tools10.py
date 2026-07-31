import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Place right below the "Atividade da Concorrência" string section
    # Let's search for "<!-- ATIVIDADE DA CONCORRÊNCIA"
    idx_app_container = content.find('<!-- ATIVIDADE DA CONCORRÊNCIA E ALERTA DE CHUVA -->')
    if idx_app_container != -1:
        content = content[:idx_app_container] + tools_content + '\n' + content[idx_app_container:]
        
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved above concorrencia!")
    else:
        print("Could not find concorrencia")
