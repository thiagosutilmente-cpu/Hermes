import sys

with open('index.html', 'r') as f:
    content = f.read()

multiplex_html = """
      <!-- Multiplex Neural Grid (UNIMAGINABLE TIER) -->
      <div id="multiplexNeuralGrid" style="display: none; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 10px; grid-column: 1 / -1; height: 360px; animation: slideDown 0.3s ease; margin-bottom: 20px;">
          <!-- Instância 1: UBER -->
          <div style="background: linear-gradient(180deg, rgba(0,0,0,0.9) 0%, rgba(0,240,255,0.05) 100%); border: 1px solid #00f0ff; border-radius: 12px; padding: 10px; position: relative; overflow: hidden; box-shadow: inset 0 0 20px rgba(0, 240, 255, 0.2);">
              <div style="position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, transparent, #00f0ff, transparent); animation: scanline 2s linear infinite;"></div>
              <div style="font-size: 10px; color: #00f0ff; font-weight: 900; letter-spacing: 1px; margin-bottom: 5px; display: flex; justify-content: space-between;">
                  <span>VM_01: UBER_DRV</span><span style="color: #fff; font-size: 8px; background: rgba(0,240,255,0.3); padding: 2px 4px; border-radius: 4px;">ROOT</span>
              </div>
              <div style="height: 70px; display: flex; flex-direction: column; justify-content: center; position: relative;">
                  <div style="position: absolute; inset: 0; opacity: 0.2; background: repeating-linear-gradient(0deg, #00f0ff 0px, transparent 1px, transparent 4px);"></div>
                  <canvas id="canvasUber" style="width: 100%; height: 100%; position: absolute; inset: 0; opacity: 0.5;"></canvas>
                  <div style="text-align: center; z-index: 1;">
                      <span class="material-symbols-rounded" style="font-size: 24px; color: #00f0ff; animation: pulse 1s infinite;">explore</span>
                      <div id="uber-status-text" style="font-size: 8px; color: #fff; margin-top: 4px; font-family: monospace;">Aguardando corrida...</div>
                  </div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 8px; color: #888; margin-top: 10px; border-top: 1px solid rgba(0,240,255,0.2); padding-top: 5px;">
                  <span>PING: <b id="uber-latency" style="color: #fff;">1.2ms</b></span>
                  <span>HOOK: <b id="uber-profit" style="color: #00ff00;">ACTV</b></span>
              </div>
          </div>

          <!-- Instância 2: IFOOD -->
          <div style="background: linear-gradient(180deg, rgba(0,0,0,0.9) 0%, rgba(255,0,255,0.05) 100%); border: 1px solid #ff00ff; border-radius: 12px; padding: 10px; position: relative; overflow: hidden; box-shadow: inset 0 0 20px rgba(255, 0, 255, 0.2);">
              <div style="position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, transparent, #ff00ff, transparent); animation: scanline 2.5s linear infinite;"></div>
              <div style="font-size: 10px; color: #ff00ff; font-weight: 900; letter-spacing: 1px; margin-bottom: 5px; display: flex; justify-content: space-between;">
                  <span>VM_02: IFOOD_LOG</span><span style="color: #fff; font-size: 8px; background: rgba(255,0,255,0.3); padding: 2px 4px; border-radius: 4px;">INJ</span>
              </div>
              <div style="height: 70px; display: flex; flex-direction: column; justify-content: center; position: relative;">
                  <canvas id="canvasIfood" style="width: 100%; height: 100%; position: absolute; inset: 0; opacity: 0.5;"></canvas>
                  <div style="text-align: center; z-index: 1;">
                      <span class="material-symbols-rounded" style="font-size: 24px; color: #ff00ff; animation: pulse 1.2s infinite;">restaurant</span>
                      <div id="ifood-status-text" style="font-size: 8px; color: #fff; margin-top: 4px; font-family: monospace;">Interceptando...</div>
                  </div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 8px; color: #888; margin-top: 10px; border-top: 1px solid rgba(255,0,255,0.2); padding-top: 5px;">
                  <span>PING: <b id="ifood-latency" style="color: #fff;">0.8ms</b></span>
                  <span>SYNC: <b id="ifood-profit" style="color: #00ff00;">99%</b></span>
              </div>
          </div>

          <!-- Instância 3: WAZE -->
          <div style="background: linear-gradient(180deg, rgba(0,0,0,0.9) 0%, rgba(0,245,212,0.05) 100%); border: 1px solid #00f5d4; border-radius: 12px; padding: 10px; position: relative; overflow: hidden; box-shadow: inset 0 0 20px rgba(0, 245, 212, 0.2);">
              <div style="position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, transparent, #00f5d4, transparent); animation: scanline 1.8s linear infinite;"></div>
              <div style="font-size: 10px; color: #00f5d4; font-weight: 900; letter-spacing: 1px; margin-bottom: 5px; display: flex; justify-content: space-between;">
                  <span>VM_03: G_MAPS_SYS</span><span style="color: #fff; font-size: 8px; background: rgba(0,245,212,0.3); padding: 2px 4px; border-radius: 4px;">LIVE</span>
              </div>
              <div style="height: 70px; display: flex; flex-direction: column; justify-content: center; position: relative;">
                  <div id="mapsRadarSweep" style="position: absolute; inset: 0; background: conic-gradient(from 0deg, transparent 0%, rgba(0,245,212,0.4) 10%, transparent 40%); animation: spin 2s linear infinite; border-radius: 50%;"></div>
                  <div style="text-align: center; z-index: 1;">
                      <span class="material-symbols-rounded" style="font-size: 24px; color: #00f5d4;">radar</span>
                      <div id="maps-status-text" style="font-size: 8px; color: #fff; margin-top: 4px; font-family: monospace;">Mapeando tráfego...</div>
                  </div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 8px; color: #888; margin-top: 10px; border-top: 1px solid rgba(0,245,212,0.2); padding-top: 5px;">
                  <span>SATS: <b style="color: #00f5d4;">14 ONLINE</b></span>
                  <span>TRAFFIC: <b style="color: #ff006e;">BYPASS</b></span>
              </div>
          </div>

          <!-- Instância 4: RAPPI / LOGGI -->
          <div style="background: linear-gradient(180deg, rgba(0,0,0,0.9) 0%, rgba(255,183,3,0.05) 100%); border: 1px solid #ffb703; border-radius: 12px; padding: 10px; position: relative; overflow: hidden; box-shadow: inset 0 0 20px rgba(255, 183, 3, 0.2);">
              <div style="position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, transparent, #ffb703, transparent); animation: scanline 2.2s linear infinite;"></div>
              <div style="font-size: 10px; color: #ffb703; font-weight: 900; letter-spacing: 1px; margin-bottom: 5px; display: flex; justify-content: space-between;">
                  <span>VM_04: RAPPI_LOG</span><span style="color: #fff; font-size: 8px; background: rgba(255,183,3,0.3); padding: 2px 4px; border-radius: 4px;">STEALTH</span>
              </div>
              <div style="height: 70px; display: flex; flex-direction: column; justify-content: center; position: relative;">
                  <canvas id="canvasRappi" style="width: 100%; height: 100%; position: absolute; inset: 0; opacity: 0.5;"></canvas>
                  <div style="text-align: center; z-index: 1;">
                      <span class="material-symbols-rounded" style="font-size: 24px; color: #ffb703; animation: pulse 1.5s infinite;">shopping_cart</span>
                      <div id="rappi-status-text" style="font-size: 8px; color: #fff; margin-top: 4px; font-family: monospace;">Scraping pool...</div>
                  </div>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 8px; color: #888; margin-top: 10px; border-top: 1px solid rgba(255,183,3,0.2); padding-top: 5px;">
                  <span>PING: <b id="rappi-latency" style="color: #fff;">1.5ms</b></span>
                  <span>SPOOF: <b id="rappi-profit" style="color: #00ff00;">ON</b></span>
              </div>
          </div>
      </div>
"""

old_grid_start = '<div id="multiplexNeuralGrid"'
old_grid_end = '<!-- Dashboard de Horários de Ouro -->'

start_idx = content.find(old_grid_start)
end_idx = content.find(old_grid_end)

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + multiplex_html + '\n      ' + content[end_idx:]
else:
    print("Could not find multiplex grid")

js_code = """
    window.startMultiplexSimulation = function() {
        const apps = ['uber', 'ifood', 'rappi'];
        setInterval(() => {
            apps.forEach(app => {
                const latEl = document.getElementById(`${app}-latency`);
                if (latEl) latEl.innerText = (Math.random() * 2 + 0.1).toFixed(2) + 'ms';
            });
            
            const texts = {
                uber: ['Injetando coordenadas...', 'Lendo memória RAM...', 'Bypass de validação...', 'Interceptando oferta...'],
                ifood: ['Decodificando JSON...', 'Burlando timeout...', 'Simulando tap...', 'Aguardando socket...'],
                rappi: ['Mascarando GPS...', 'Clonando token...', 'Buscando pool...', 'Forçando prioridade...'],
                maps: ['Rotas calculadas: 14', 'Desvio agressivo: ON', 'Sincronizando satélites...', 'Tráfego fantasma ativo']
            };
            
            ['uber', 'ifood', 'rappi', 'maps'].forEach(app => {
                const textEl = document.getElementById(`${app}-status-text`);
                if (textEl && Math.random() > 0.5) {
                    textEl.innerText = texts[app][Math.floor(Math.random() * texts[app].length)];
                }
            });
        }, 1500);
        
        // Draw fake sine waves on canvas
        ['canvasUber', 'canvasIfood', 'canvasRappi'].forEach((canvasId, index) => {
            const canvas = document.getElementById(canvasId);
            if (!canvas) return;
            const ctx = canvas.getContext('2d');
            let offset = 0;
            const color = index === 0 ? '#00f0ff' : (index === 1 ? '#ff00ff' : '#ffb703');
            
            setInterval(() => {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                ctx.beginPath();
                ctx.strokeStyle = color;
                ctx.lineWidth = 1;
                for(let i = 0; i < canvas.width; i++) {
                    const y = Math.sin((i + offset) * 0.1) * 15 + canvas.height/2;
                    if(i === 0) ctx.moveTo(i, y);
                    else ctx.lineTo(i, y);
                }
                ctx.stroke();
                offset += 2;
            }, 50);
        });
    };
"""

content = content.replace('    window.startMultiplexSimulation = function() {', '    window.startMultiplexSimulation_OLD = function() {')
content = content.replace('    // Simulated Biometrics', js_code + '\n    // Simulated Biometrics')

with open('index.html', 'w') as f:
    f.write(content)

print("Multiplex Grid Upgraded")
