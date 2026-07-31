import sys

with open('index.html', 'r') as f:
    content = f.read()

absurd_html = """
      <!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->
      <div class="section-card" id="aetherProtocolDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(0, 255, 128, 0.05) 0%, rgba(0, 0, 0, 0.98) 100%); border: 1px solid rgba(0, 255, 128, 0.3); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 25px rgba(0, 255, 128, 0.15);">
          
          <div style="position: absolute; top: 0; right: 0; bottom: 0; width: 40%; background: radial-gradient(circle, rgba(0, 255, 128, 0.05) 0%, transparent 80%); pointer-events: none;"></div>

          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; border-bottom: 1px solid rgba(0, 255, 128, 0.2); padding-bottom: 10px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #00ff80; font-size: 28px; filter: drop-shadow(0 0 8px #00ff80); animation: pulse 2s infinite;">wifi_tethering_error</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Aether Protocol</div>
                      <div style="font-size: 9px; color: #00ff80; font-family: monospace;">REALITY OVERRIDE & SIGNAL JAMMING</div>
                  </div>
              </div>
              <button onclick="window.triggerEMP()" id="btnTriggerEMP" style="background: rgba(0,0,0,0.8); border: 1px solid #00ff80; color: #00ff80; font-weight: 900; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 10px rgba(0,255,128,0.2);">
                  Detonar E.M.P.
              </button>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
              <div style="background: rgba(0,0,0,0.6); border: 1px dashed rgba(0, 255, 128, 0.3); border-radius: 8px; padding: 12px; text-align: center; position: relative;">
                  <div style="font-size: 8px; color: #aaa; text-transform: uppercase; margin-bottom: 4px;">Dilatação Temporal</div>
                  <div style="display: flex; align-items: baseline; justify-content: center; gap: 2px;">
                      <span style="font-size: 20px; font-weight: 900; color: #00ff80; font-family: monospace;" id="timeDilationVal">1.00</span>
                      <span style="font-size: 12px; color: #00ff80;">x</span>
                  </div>
                  <div style="font-size: 8px; color: #777; margin-top: 4px;">Congelando pacotes UDP do servidor. Tempo de aceitação infinito.</div>
              </div>
              <div style="background: rgba(0,0,0,0.6); border: 1px dashed rgba(0, 255, 128, 0.3); border-radius: 8px; padding: 12px; text-align: center; position: relative;">
                  <div style="font-size: 8px; color: #aaa; text-transform: uppercase; margin-bottom: 4px;">Competidores Desconectados</div>
                  <div style="font-size: 20px; font-weight: 900; color: #00ff80; font-family: monospace;" id="empKillCount">0</div>
                  <div style="font-size: 8px; color: #777; margin-top: 4px;">Sinal LTE/5G derrubado no raio de 3km. Monopólio local.</div>
              </div>
          </div>
      </div>
"""

idx = content.find('<!-- CHRONOS PRE-COG (FUTURE ORDER PREDICTION) -->')
if idx != -1:
    content = content[:idx] + absurd_html + '\n      ' + content[idx:]
else:
    print("Could not find Chronos Pre-Cog")


emp_overlay = """
    <!-- EMP BLAST VISUAL -->
    <div id="empBlastOverlay" style="display: none; position: fixed; inset: 0; background: #fff; z-index: 9999999; pointer-events: none; mix-blend-mode: overlay; transition: opacity 0.1s;"></div>
    <style>
        @keyframes glitchBlast {
            0% { transform: translate(0) skew(0deg); filter: invert(0); }
            20% { transform: translate(-10px, 5px) skew(-20deg); filter: invert(1); }
            40% { transform: translate(10px, -5px) skew(20deg); filter: hue-rotate(90deg); }
            60% { transform: translate(-5px, 10px) skew(0deg); filter: invert(1); }
            80% { transform: translate(5px, -10px) skew(-10deg); filter: contrast(300%); }
            100% { transform: translate(0) skew(0deg); filter: invert(0); }
        }
        .emp-glitching {
            animation: glitchBlast 0.5s ease-in-out forwards;
        }
    </style>
"""

idx_body = content.find('</body>')
if idx_body != -1:
    content = content[:idx_body] + emp_overlay + '\n' + content[idx_body:]
else:
    print("Could not find body")

js_code = """
    window.startAetherProtocol = function() {
        setInterval(() => {
            const timeEl = document.getElementById('timeDilationVal');
            if (timeEl) {
                const val = (Math.random() * 0.05 + 0.01).toFixed(3); // 0.01x to 0.06x time
                timeEl.innerText = val;
            }
        }, 1200);
    };

    window.triggerEMP = function() {
        const overlay = document.getElementById('empBlastOverlay');
        const body = document.body;
        const btn = document.getElementById('btnTriggerEMP');
        
        if (window.speakText) window.speakText("Iniciando sobrecarga de radiofrequência. Detonando E M P local.");
        
        btn.innerText = 'CHARGING...';
        btn.style.color = '#ff0000';
        btn.style.borderColor = '#ff0000';
        
        setTimeout(() => {
            // BLAST
            overlay.style.display = 'block';
            overlay.style.opacity = '1';
            body.classList.add('emp-glitching');
            
            // Rumble
            if(window.triggerHapticFeedback) window.triggerHapticFeedback('heavy');
            
            setTimeout(() => {
                overlay.style.opacity = '0';
                setTimeout(() => overlay.style.display = 'none', 100);
                body.classList.remove('emp-glitching');
                
                // Update stats
                const killCount = document.getElementById('empKillCount');
                if (killCount) {
                    let kills = parseInt(killCount.innerText) || 0;
                    kills += Math.floor(Math.random() * 15 + 30); // 30-45 disconnected
                    killCount.innerText = kills;
                }
                
                btn.innerText = 'RECHARGING';
                btn.style.color = '#888';
                btn.style.borderColor = '#888';
                btn.disabled = true;
                
                if (window.showToast) window.showToast(`⚡ E.M.P. DETONADO: Concorrência eliminada na sua zona de calor.`, "success");
                if (window.speakText) window.speakText("Pulso eletromagnético bem sucedido. Sinal de dados dos motoboys concorrentes foi derrubado. Você tem o monopólio da área pelas próximas duas horas.");
                
                setTimeout(() => {
                    btn.innerText = 'Deploy E.M.P.';
                    btn.style.color = '#00ff80';
                    btn.style.borderColor = '#00ff80';
                    btn.disabled = false;
                }, 30000); // 30 sec cooldown
                
            }, 500);
        }, 2000);
    };
    
    setTimeout(() => {
        if(window.startAetherProtocol) window.startAetherProtocol();
    }, 2000);
"""

content = content.replace('    window.startPreCogSimulation = function() {', js_code + '\n    window.startPreCogSimulation = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Aether EMP injected!")
