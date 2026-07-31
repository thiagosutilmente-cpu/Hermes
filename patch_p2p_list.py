import sys

with open('index.html', 'r') as f:
    content = f.read()

html_injection = """
            <div style="background: rgba(0,0,0,0.4); border-radius: 8px; padding: 15px; margin-bottom: 15px; position: relative; overflow: hidden;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 10px; color: var(--text-dim);">
                    <span>COOPERADOS PRÓXIMOS (SINALIZADORES)</span>
                </div>
                <div id="p2pCoopList" style="max-height: 120px; overflow-y: auto; display: flex; flex-direction: column; gap: 5px; font-size: 11px;">
                    <div style="text-align: center; color: var(--text-dim);">Buscando cooperados...</div>
                </div>
            </div>
"""

idx = content.find('<!-- Elementos de nós injetados via JS -->')
# We'll just replace the whole section to add our list box after the visualizer box

# Find the end of the p2pMeshVisualizer container box
search_str = """                <div id="p2pMeshVisualizer" style="width: 100%; height: 200px; position: relative; background: radial-gradient(circle, rgba(162, 0, 255, 0.1) 0%, transparent 70%); border: 1px solid rgba(255,255,255,0.05); border-radius: 4px;">
                    <!-- Elementos de nós injetados via JS -->
                    <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: var(--text-dim); font-size: 11px;">Mapeando infraestrutura cooperativa...</div>
                </div>
            </div>"""

if search_str in content:
    content = content.replace(search_str, search_str + '\n' + html_injection)
else:
    print("Could not find search string.")

with open('index.html', 'w') as f:
    f.write(content)

print("HTML injected!")
