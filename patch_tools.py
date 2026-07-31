import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

tools_html = """
      <!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->
      <div class="section-card" id="toolsDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(20, 22, 34, 0.95) 0%, rgba(10, 11, 16, 0.98) 100%); border: 1px solid var(--border); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px dashed var(--border); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: var(--accent-blue); font-size: 28px;">handyman</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Caixa de Ferramentas</div>
                      <div style="font-size: 9px; color: var(--text-dim); font-family: monospace;">ARSENAL OFENSIVO / DEFENSIVO</div>
                  </div>
              </div>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
             <!-- Button for EMP -->
             <button onclick="document.getElementById('empDashboard').scrollIntoView({behavior: 'smooth', block: 'center'});" style="background: rgba(255, 0, 85, 0.1); border: 1px solid #ff0055; color: #ff0055; padding: 10px; border-radius: 8px; font-size: 10px; font-weight: bold; cursor: pointer; text-transform: uppercase; display: flex; flex-direction: column; align-items: center; gap: 5px;">
                 <span class="material-symbols-rounded" style="font-size: 24px;">wifi_tethering_error</span>
                 Aether Protocol (EMP)
             </button>
             
             <!-- Button for HAARP -->
             <button onclick="document.getElementById('haarpDashboard').scrollIntoView({behavior: 'smooth', block: 'center'});" style="background: rgba(0, 102, 255, 0.1); border: 1px solid #0066ff; color: #0066ff; padding: 10px; border-radius: 8px; font-size: 10px; font-weight: bold; cursor: pointer; text-transform: uppercase; display: flex; flex-direction: column; align-items: center; gap: 5px;">
                 <span class="material-symbols-rounded" style="font-size: 24px;">thunderstorm</span>
                 HAARP Array Link
             </button>
             
             <!-- Button for Inception -->
             <button onclick="document.getElementById('inceptionDashboard').scrollIntoView({behavior: 'smooth', block: 'center'});" style="background: rgba(255, 215, 0, 0.1); border: 1px solid #ffd700; color: #ffd700; padding: 10px; border-radius: 8px; font-size: 10px; font-weight: bold; cursor: pointer; text-transform: uppercase; display: flex; flex-direction: column; align-items: center; gap: 5px;">
                 <span class="material-symbols-rounded" style="font-size: 24px;">psychology_alt</span>
                 Inception Protocol
             </button>
             
             <!-- Button for Stealth / Singularity -->
             <button onclick="document.getElementById('obfuscationDashboard').scrollIntoView({behavior: 'smooth', block: 'center'});" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #ffffff; color: #ffffff; padding: 10px; border-radius: 8px; font-size: 10px; font-weight: bold; cursor: pointer; text-transform: uppercase; display: flex; flex-direction: column; align-items: center; gap: 5px;">
                 <span class="material-symbols-rounded" style="font-size: 24px;">visibility_off</span>
                 Manto Quântico & Singularidade
             </button>
          </div>
      </div>
"""

idx = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')
# We need to find singularity first
idx_singularity = content.find('<!-- NEURAL SINGULARITY (OMNIPRESENCE) -->')
if idx_singularity != -1:
    content = content[:idx_singularity] + tools_html + '\n      ' + content[idx_singularity:]
else:
    print("Could not find singularity section")

with open('index.html', 'w') as f:
    f.write(content)

print("Tools injected!")
