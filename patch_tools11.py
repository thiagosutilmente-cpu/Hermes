import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_sing = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')

if idx_tools != -1 and idx_sing != -1:
    tools_content = content[idx_tools:idx_sing]
    content = content[:idx_tools] + content[idx_sing:]
    
    # Let's put it right after the REALTIME DASHBOARD 
    idx_realtime = content.find('id="realtimeDashboard"')
    if idx_realtime != -1:
        # We need to find the close of the div that contains it, or just insert it right inside dashboardGrid
        idx_grid = content.find('<div id="dashboardGrid"', idx_realtime)
        if idx_grid != -1:
            idx_grid_end = content.find('>', idx_grid) + 1
            content = content[:idx_grid_end] + '\n\n' + tools_content + '\n' + content[idx_grid_end:]
            
            with open('index.html', 'w') as f:
                f.write(content)
            print("Tools moved inside dashboardGrid!")
        else:
            print("Could not find dashboardGrid")
    else:
        print("Could not find realtimeDashboard")
