import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Just place it immediately after the main app container starts. Let's find `<main class="app-container">` or similar
    idx_app = content.find('<main')
    if idx_app != -1:
        idx_app_end = content.find('>', idx_app) + 1
        content = content[:idx_app_end] + '\n\n' + tools_content + '\n' + content[idx_app_end:]
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools moved!")
    else:
        # Just put it under `<div class="app-container">`
        idx_app = content.find('<div class="app-container">')
        if idx_app != -1:
            idx_app_end = content.find('>', idx_app) + 1
            content = content[:idx_app_end] + '\n\n' + tools_content + '\n' + content[idx_app_end:]
            with open('index.html', 'w') as f:
                f.write(content)
            print("Tools moved!")
        else:
            print("Could not find app container")
