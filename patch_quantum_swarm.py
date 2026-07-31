import sys

with open('index.html', 'r') as f:
    content = f.read()

quantum_swarm_html = """
      <!-- PROJECT MOCKINGBIRD (GHOST SWARM MANIPULATION) -->
      <div class="section-card" id="mockingbirdDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 0, 85, 0.05) 0%, rgba(10, 11, 18, 0.98) 100%); border: 1px solid rgba(255, 0, 85, 0.3); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 20px rgba(255, 0, 85, 0.15);">
          
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; border-bottom: 1px solid rgba(255, 0, 85, 0.2); padding-bottom: 10px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #ff0055; font-size: 28px; filter: drop-shadow(0 0 5px #ff0055);">hub</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Project Mockingbird</div>
                      <div style="font-size: 9px; color: #ff0055; font-family: monospace;">GHOST SWARM MANIPULATION ENGINE</div>
                  </div>
              </div>
              <button onclick="window.toggleMockingbird()" id="btnToggleMockingbird" style="background: rgba(255,0,85,0.1); border: 1px solid #ff0055; color: #ff0055; font-weight: bold; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase;">
                  Deploy Swarm
              </button>
          </div>
          
          <div id="mockingbirdActiveUI" style="display: none; flex-direction: column; gap: 12px;">
              <div style="display: flex; gap: 10px; align-items: center;">
                  <!-- Radar Mini -->
                  <div style="width: 80px; height: 80px; border-radius: 50%; border: 1px solid #ff0055; position: relative; background: radial-gradient(circle, rgba(255,0,85,0.1) 0%, transparent 70%);">
                      <div style="position: absolute; inset: 0; background: conic-gradient(from 0deg, transparent 0%, rgba(255,0,85,0.4) 10%, transparent 40%); animation: spin 1.5s linear infinite; border-radius: 50%;"></div>
                      <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 4px; height: 4px; background: #fff; border-radius: 50%; box-shadow: 0 0 5px #fff;"></div>
                      <!-- Blips will be generated via CSS/JS -->
                      <div class="swarm-blip" style="top: 20%; left: 30%;"></div>
                      <div class="swarm-blip" style="top: 70%; left: 60%;"></div>
                      <div class="swarm-blip" style="top: 40%; left: 80%;"></div>
                  </div>
                  
                  <div style="flex: 1; display: flex; flex-direction: column; gap: 6px;">
                      <div style="font-size: 10px; color: #aaa; font-family: monospace;">Atraindo <b style="color: #ff0055;">Multiplicador Dinâmico</b> para o seu setor falsificando <span id="swarmCount">0</span> entregadores em zonas periféricas...</div>
                      <div style="display: flex; justify-content: space-between; font-size: 11px; background: rgba(0,0,0,0.5); padding: 6px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1);">
                          <span style="color: #888;">Demanda Simulada:</span>
                          <b style="color: #00ff00;">+340%</b>
                      </div>
                      <div style="display: flex; justify-content: space-between; font-size: 11px; background: rgba(0,0,0,0.5); padding: 6px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1);">
                          <span style="color: #888;">Concorrência Local:</span>
                          <b style="color: #00f0ff;">Zero (Spoofed)</b>
                      </div>
                  </div>
              </div>
          </div>
      </div>
      <style>
          .swarm-blip {
              position: absolute; width: 3px; height: 3px; background: #ff0055; border-radius: 50%; box-shadow: 0 0 8px #ff0055; animation: blink 1s infinite;
          }
      </style>

      <!-- CHRONOS PRE-COG (FUTURE ORDER PREDICTION) -->
      <div class="section-card" id="chronosPreCogDashboardV2" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(162, 0, 255, 0.05) 0%, rgba(0, 0, 0, 0.95) 100%); border: 1px solid rgba(162, 0, 255, 0.3); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <div style="display: flex; align-items: center; gap: 8px;">
                  <span class="material-symbols-rounded" style="color: #A200FF; font-size: 24px;">all_inclusive</span>
                  <div style="font-size: 13px; font-weight: 900; color: #fff; text-transform: uppercase;">Chronos Pre-Cog V2</div>
              </div>
              <span style="font-size: 8px; color: #A200FF; background: rgba(162,0,255,0.1); padding: 2px 6px; border-radius: 4px;">QUANTUM PREDICTION</span>
          </div>
          
          <div style="background: rgba(0,0,0,0.4); border: 1px solid rgba(162,0,255,0.2); border-radius: 8px; padding: 10px;">
              <div style="font-size: 9px; color: #888; text-transform: uppercase; margin-bottom: 6px;">Pedidos Sendo Preparados (Interceptação de Cozinha)</div>
              
              <div id="precogList" style="display: flex; flex-direction: column; gap: 6px;">
                  <!-- Items generated by JS -->
              </div>
          </div>
      </div>
"""

idx = content.find('<!-- AÇÕES RÁPIDAS DE TERRENO (QUICK ACTIONS) -->')
if idx != -1:
    content = content[:idx] + quantum_swarm_html + '\n      ' + content[idx:]
else:
    print("Could not find quick actions")

js_code = """
    window.mockingbirdActive = false;
    window.toggleMockingbird = function() {
        window.mockingbirdActive = !window.mockingbirdActive;
        const btn = document.getElementById('btnToggleMockingbird');
        const ui = document.getElementById('mockingbirdActiveUI');
        
        if (window.mockingbirdActive) {
            btn.style.background = '#ff0055';
            btn.style.color = '#fff';
            btn.innerText = 'SWARM ACTIVE';
            ui.style.display = 'flex';
            if(window.showToast) window.showToast("🚨 MOCKINGBIRD DEPLOYED: Injetando 50 motoboys fantasmas na rede.", "warning");
            
            let count = 0;
            window.swarmInterval = setInterval(() => {
                count += Math.floor(Math.random() * 5);
                if (count > 250) count = 250;
                const cEl = document.getElementById('swarmCount');
                if (cEl) cEl.innerText = count;
            }, 1000);
            
        } else {
            btn.style.background = 'rgba(255,0,85,0.1)';
            btn.style.color = '#ff0055';
            btn.innerText = 'Deploy Swarm';
            ui.style.display = 'none';
            clearInterval(window.swarmInterval);
            if(window.showToast) window.showToast("Mockingbird Recalled.", "info");
        }
    };
    
    window.startPreCogSimulation = function() {
        const rests = ["McDonald's", "Outback", "Habib's", "Madero", "Pizza Hut"];
        const dests = ["Jardins", "Pinheiros", "Vila Mariana", "Moema", "Itaim Bibi"];
        
        setInterval(() => {
            const list = document.getElementById('precogList');
            if (!list) return;
            
            const r = rests[Math.floor(Math.random() * rests.length)];
            const d = dests[Math.floor(Math.random() * dests.length)];
            const v = (Math.random() * 30 + 15).toFixed(2);
            const time = Math.floor(Math.random() * 5 + 1);
            
            const html = `
                <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(162,0,255,0.05); padding: 6px 8px; border-radius: 4px; border-left: 2px solid #A200FF; animation: slideIn 0.3s ease;">
                    <div>
                        <div style="font-size: 11px; font-weight: bold; color: #fff;">${r} ➔ ${d}</div>
                        <div style="font-size: 9px; color: #aaa;">Previsão de Despacho: <span style="color: #00f0ff;">${time} min</span></div>
                    </div>
                    <div style="font-size: 12px; font-weight: 900; color: #00ff00;">~R$ ${v}</div>
                </div>
            `;
            
            list.insertAdjacentHTML('afterbegin', html);
            if (list.children.length > 3) {
                list.removeChild(list.lastChild);
            }
        }, 8000);
    };
    
    setTimeout(() => {
        if(window.startPreCogSimulation) window.startPreCogSimulation();
    }, 2000);
"""

content = content.replace('    window.startSurgeForcerSimulation = function() {', js_code + '\n    window.startSurgeForcerSimulation = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Quantum Swarm + PreCog injected!")
