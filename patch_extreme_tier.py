import sys

with open('index.html', 'r') as f:
    content = f.read()

extreme_html = """
      <!-- ALGORITHMIC FARE NEGOTIATOR (SURGE FORCER) -->
      <div class="section-card" id="surgeForcerDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 60, 0, 0.1) 0%, rgba(10, 11, 18, 0.98) 100%); border: 1px solid rgba(255, 60, 0, 0.4); padding: 20px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 25px rgba(255, 60, 0, 0.15);">
          <div style="position: absolute; top: -50px; right: -50px; width: 150px; height: 150px; background: radial-gradient(circle, rgba(255, 60, 0, 0.15) 0%, transparent 70%); pointer-events: none; animation: pulse 3s infinite;"></div>
          
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; border-bottom: 1px solid rgba(255, 60, 0, 0.2); padding-bottom: 10px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #ff3c00; font-size: 28px; animation: spin 4s linear infinite;">currency_exchange</span>
                  <div>
                      <div style="font-size: 15px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1.5px;">Surge Forcer Engine</div>
                      <div style="font-size: 9px; color: var(--text-muted); font-family: monospace;">NEGOCIAÇÃO ALGORÍTMICA DE TARIFA EM TEMPO REAL</div>
                  </div>
              </div>
              <div style="display: flex; flex-direction: column; align-items: flex-end;">
                  <span style="font-size: 10px; font-weight: bold; background: rgba(255, 60, 0, 0.2); color: #ff3c00; padding: 3px 8px; border-radius: 4px; border: 1px solid #ff3c00;">A.I. BIDDING ACTIVE</span>
                  <span style="font-size: 8px; color: #888; margin-top: 4px;">Hold Time: <span id="surgeHoldTime">1.2s</span></span>
              </div>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
              <!-- Original Offer -->
              <div style="background: rgba(0,0,0,0.5); border: 1px dashed rgba(255, 255, 255, 0.1); border-radius: 8px; padding: 12px; position: relative;">
                  <div style="font-size: 9px; color: #888; text-transform: uppercase; margin-bottom: 4px;">Oferta Base Detectada</div>
                  <div style="display: flex; align-items: baseline; gap: 4px;">
                      <span style="font-size: 14px; color: var(--text-muted);">R$</span>
                      <span id="surgeBaseFare" style="font-size: 24px; font-weight: 900; color: var(--text-muted); text-decoration: line-through;">--.--</span>
                  </div>
                  <div style="font-size: 10px; color: #666; margin-top: 4px;">Algoritmo: <span style="color: #ff00ff;">Recusado (Baixo Valor)</span></div>
              </div>
              
              <!-- Negotiated Offer -->
              <div style="background: rgba(255, 60, 0, 0.05); border: 1px solid rgba(255, 60, 0, 0.3); border-radius: 8px; padding: 12px; position: relative; box-shadow: inset 0 0 15px rgba(255, 60, 0, 0.1);">
                  <div style="position: absolute; top: -5px; right: -5px; background: #ff3c00; color: #000; font-size: 8px; font-weight: 900; padding: 2px 6px; border-radius: 10px; transform: rotate(10deg);">FORÇADO</div>
                  <div style="font-size: 9px; color: #ff3c00; text-transform: uppercase; font-weight: bold; margin-bottom: 4px;">Tarifa Negociada (Jarvis)</div>
                  <div style="display: flex; align-items: baseline; gap: 4px;">
                      <span style="font-size: 14px; color: #00ff00;">R$</span>
                      <span id="surgeNewFare" style="font-size: 24px; font-weight: 900; color: #00ff00; text-shadow: 0 0 10px rgba(0,255,0,0.5);">--.--</span>
                  </div>
                  <div style="font-size: 10px; color: var(--text-main); margin-top: 4px; display: flex; align-items: center; gap: 4px;">
                      Ganho: <b id="surgeProfitDelta" style="color: #00ff00;">+ R$ 0.00</b> 
                      <span class="material-symbols-rounded" style="font-size: 12px; color: #00ff00;">trending_up</span>
                  </div>
              </div>
          </div>
          
          <div style="margin-top: 15px; font-size: 10px; color: #aaa; text-align: center; background: rgba(0,0,0,0.3); padding: 8px; border-radius: 6px; border: 1px solid rgba(255, 255, 255, 0.05);">
              <span class="material-symbols-rounded" style="font-size: 12px; vertical-align: middle; color: #ff3c00; margin-right: 4px;">memory</span>
              Jarvis está retendo temporariamente as respostas ao servidor para simular escassez de frota e forçar aumento do multiplicador dinâmico.
          </div>
      </div>
"""

idx = content.find('<!-- AÇÕES RÁPIDAS DE TERRENO (QUICK ACTIONS) -->')
if idx != -1:
    content = content[:idx] + extreme_html + '\n      ' + content[idx:]
else:
    print("Could not find quick actions")

js_code = """
    window.startSurgeForcerSimulation = function() {
        setInterval(() => {
            if (document.getElementById('surgeForcerDashboard').style.display === 'none' && Math.random() > 0.8) {
                // Occasionally hide it to seem real
                return;
            }
            
            const base = (Math.random() * 15 + 8).toFixed(2);
            const surgeMultiplier = (Math.random() * 0.4 + 1.15); // 15% to 55% increase
            const negotiated = (base * surgeMultiplier).toFixed(2);
            const delta = (negotiated - base).toFixed(2);
            
            const baseEl = document.getElementById('surgeBaseFare');
            const newEl = document.getElementById('surgeNewFare');
            const deltaEl = document.getElementById('surgeProfitDelta');
            const holdEl = document.getElementById('surgeHoldTime');
            
            if (baseEl) baseEl.innerText = base;
            if (newEl) newEl.innerText = negotiated;
            if (deltaEl) deltaEl.innerText = `+ R$ ${delta}`;
            if (holdEl) holdEl.innerText = (Math.random() * 2 + 0.5).toFixed(1) + 's';
            
            // Add matrix flash effect to new fare
            if (newEl) {
                newEl.style.color = '#fff';
                setTimeout(() => newEl.style.color = '#00ff00', 150);
            }
            
        }, 4500);
    };
    
    // Start simulation automatically
    setTimeout(() => {
        if (window.startSurgeForcerSimulation) window.startSurgeForcerSimulation();
    }, 2000);
"""

content = content.replace('    window.startMultiplexSimulation_OLD = function() {', js_code + '\n    window.startMultiplexSimulation_OLD = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Surge Forcer Engine injected!")
