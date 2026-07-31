import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
# We need to find the end of it
idx_hud = content.find('<div id="sovereignFloatingHUD"')

if idx_tools != -1 and idx_hud != -1:
    tools_content = content[idx_tools:idx_hud]
    content = content[:idx_tools] + content[idx_hud:]
    
    # We will put it right before the "Painel de Configurações" section starts. Let's find "<!-- Settings View (Hidden by default) -->" or similar, or "AETHER PROTOCOL"
    idx_aether = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')
    if idx_aether != -1:
        # Move tools box right above Aether protocol
        # Then we'll use Javascript to open it in a modal instead, or we'll add a fixed Floating action button to open these tools. Let's add a button to the HUD to toggle this box.
        
        # Let's put the tools box inside a modal div.
        tools_modal = f"""
        <!-- MODAL CAIXA DE FERRAMENTAS -->
        <div id="toolsModal" style="display: none; position: fixed; inset: 0; background: rgba(0, 0, 0, 0.95); backdrop-filter: blur(15px); z-index: 99999999; align-items: center; justify-content: center; padding: 20px;">
            <div style="width: 100%; max-width: 400px; background: #0a0b10; border: 2px solid #00f0ff; border-radius: 20px; box-shadow: 0 0 50px rgba(0, 240, 255, 0.2); overflow: hidden; position: relative; animation: slideUp 0.3s ease;">
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; border-bottom: 1px solid rgba(0, 240, 255, 0.2); background: linear-gradient(135deg, rgba(0,240,255,0.1), transparent);">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span class="material-symbols-rounded" style="color: #00f0ff; font-size: 24px;">construction</span>
                        <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase;">Arsenal Hacker</div>
                    </div>
                    <button onclick="document.getElementById('toolsModal').style.display='none';" style="background: none; border: none; color: #fff; font-size: 24px; cursor: pointer;">&times;</button>
                </div>
                <div style="padding: 20px; display: flex; flex-direction: column; gap: 12px; max-height: 70vh; overflow-y: auto;">
                    {tools_content}
                </div>
            </div>
        </div>
        """
        
        # Inject the modal
        idx_end_body = content.find('</body>')
        content = content[:idx_end_body] + tools_modal + '\n' + content[idx_end_body:]
        
        # Now let's find the HUD and add a button to it
        hud_btn = """
            <div onclick="document.getElementById('toolsModal').style.display='flex';" style="width: 35px; height: 35px; background: #00f0ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 15px #00f0ff; cursor: pointer; margin-left: 10px;">
                <span class="material-symbols-rounded" style="color: black; font-size: 20px;">construction</span>
            </div>
        """
        
        idx_hud_inner = content.find('<div style="font-size: 10px; font-weight: 950; color: white; letter-spacing: 1px;">SOVEREIGN HUD V10</div>')
        if idx_hud_inner != -1:
            idx_hud_inner_end = idx_hud_inner + len('<div style="font-size: 10px; font-weight: 950; color: white; letter-spacing: 1px;">SOVEREIGN HUD V10</div>')
            content = content[:idx_hud_inner_end] + hud_btn + content[idx_hud_inner_end:]
            
        with open('index.html', 'w') as f:
            f.write(content)
        print("Tools modal and button created!")
    else:
        print("Could not find aether protocol")
