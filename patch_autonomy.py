import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

# 1. Modify the toolsModal content
# We want to replace the whole <div class="section-card" id="toolsDashboard" ...> inside the modal
# with a new Autonomy Dashboard.

autonomy_html = """
      <!-- JARVIS AUTONOMY CORE -->
      <div class="section-card" id="autonomyDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(162, 0, 255, 0.1) 0%, rgba(0, 0, 0, 0.98) 100%); border: 1px solid #a200ff; padding: 25px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; text-align: center; box-shadow: 0 0 30px rgba(162, 0, 255, 0.3);">
          <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: radial-gradient(circle, rgba(162,0,255,0.1) 0%, transparent 60%); pointer-events: none; animation: pulseGlow 3s infinite;"></div>
          
          <span class="material-symbols-rounded" style="color: #00f0ff; font-size: 48px; filter: drop-shadow(0 0 15px #00f0ff); margin-bottom: 10px;">smart_toy</span>
          
          <div style="font-size: 18px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 5px;">Omni-Autonomia Jarvis</div>
          <div style="font-size: 11px; color: #aaa; margin-bottom: 20px; font-family: monospace; padding: 0 10px;">
              Assume o controle total do ecossistema. EMP, Manipulação Climática (HAARP), Manto Quântico e Engenharia Social (Inception) serão ativados <b style="color:#00f0ff">silenciosa e autonomamente</b> pelo Jarvis conforme a necessidade tática em tempo real.
          </div>
          
          <button onclick="window.toggleJarvisAutonomy()" id="btnJarvisAutonomy" style="background: rgba(0, 240, 255, 0.2); border: 2px solid #00f0ff; color: #00f0ff; font-weight: 900; padding: 15px 30px; border-radius: 12px; font-size: 14px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 20px rgba(0,240,255,0.4); width: 100%; letter-spacing: 1px; transition: all 0.3s ease;">
              INICIAR AUTONOMIA TOTAL
          </button>
          
          <div id="autonomyLog" style="margin-top: 20px; text-align: left; background: rgba(0,0,0,0.6); padding: 15px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.1); height: 120px; overflow-y: auto; font-family: monospace; font-size: 10px; color: #00f0ff; display: flex; flex-direction: column; gap: 5px;">
              <div style="color: #666;">> Sistema em repouso. Aguardando comando...</div>
          </div>
      </div>
      
      <div id="hiddenDashboards" style="display: none;">
"""

# Find toolsDashboard
idx_tools = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
idx_obfuscation = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')

if idx_tools != -1 and idx_obfuscation != -1:
    content = content[:idx_tools] + autonomy_html + content[idx_obfuscation:]
    
    # Close the hiddenDashboards div after the Singularity dashboard (which is inside the modal)
    # We need to find the end of the modal body
    idx_modal_body_end = content.find('</div>\n            </div>\n        </div>', idx_obfuscation)
    if idx_modal_body_end != -1:
        content = content[:idx_modal_body_end] + '      </div>\n' + content[idx_modal_body_end:]

# Add JS logic for Autonomy Loop
js_code = """
    window.jarvisAutonomyActive = false;

    window.toggleJarvisAutonomy = function() {
        window.jarvisAutonomyActive = !window.jarvisAutonomyActive;
        const btn = document.getElementById('btnJarvisAutonomy');
        
        if (window.jarvisAutonomyActive) {
            btn.innerHTML = 'AUTONOMIA ATIVADA (OPERANDO)';
            btn.style.background = 'rgba(0, 255, 0, 0.2)';
            btn.style.borderColor = '#00ff00';
            btn.style.color = '#00ff00';
            btn.style.boxShadow = '0 0 20px rgba(0,255,0,0.4)';
            
            if (window.speakText) window.speakText("Autonomia total concedida. Assumindo o controle de todas as defesas, ofensores e protocolos de alteração de realidade. Relaxe e deixe comigo.");
            if (window.showToast) window.showToast("Jarvis Autonomy: ON", "success");
            
            window.logAutonomy("> Autonomia acoplada. Motores quânticos online.");
            window.startAutonomyLoop();
        } else {
            btn.innerHTML = 'INICIAR AUTONOMIA TOTAL';
            btn.style.background = 'rgba(0, 240, 255, 0.2)';
            btn.style.borderColor = '#00f0ff';
            btn.style.color = '#00f0ff';
            btn.style.boxShadow = '0 0 20px rgba(0,240,255,0.4)';
            
            clearInterval(window.autonomyInterval);
            if (window.speakText) window.speakText("Modo manual restaurado.");
            window.logAutonomy("> Autonomia desativada. Sistema em repouso.");
        }
    };

    window.logAutonomy = function(msg) {
        const logEl = document.getElementById('autonomyLog');
        if (!logEl) return;
        const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        const div = document.createElement('div');
        div.innerHTML = `<span style="color:#666;">[${time}]</span> ${msg}`;
        logEl.appendChild(div);
        logEl.scrollTop = logEl.scrollHeight;
    };

    window.startAutonomyLoop = function() {
        window.autonomyInterval = setInterval(() => {
            if (!window.jarvisAutonomyActive) return;
            
            const r = Math.random();
            
            if (!window.stealthGod && r < 0.4) {
                window.logAutonomy("> Risco detectado. Acionando Manto Quântico...");
                if (typeof window.triggerObfuscation === 'function') {
                    window.triggerObfuscation();
                    setTimeout(() => window.logAutonomy("> Manto Quântico estabilizado."), 3000);
                }
            } else if (window.stealthGod && !window.singularityActive && r < 0.3) {
                window.logAutonomy("> Brecha encontrada no Kernel. Acionando Singularidade...");
                if (typeof window.triggerSingularity === 'function') {
                    window.triggerSingularity();
                    window.singularityActive = true;
                    setTimeout(() => window.logAutonomy("> Singularidade neural alcançada."), 5000);
                }
            } else if (r < 0.15) {
                window.logAutonomy("> Concorrência densa detectada. Inicializando Aether Protocol (EMP)...");
                if (typeof window.triggerEMPBlast === 'function') {
                    window.triggerEMPBlast();
                    setTimeout(() => window.logAutonomy("> Área limpa. Negação de área concluída."), 1500);
                }
            } else if (r < 0.1) {
                window.logAutonomy("> Taxas de demanda baixas. Interceptando HAARP para manipulação climática...");
                if (typeof window.triggerHAARP === 'function') {
                    window.triggerHAARP();
                    setTimeout(() => window.logAutonomy("> Falsa tempestade injetada. Taxas infladas no servidor."), 4000);
                }
            } else if (r < 0.08) {
                window.logAutonomy("> Viagem finalizada detectada. Iniciando Inception Neural...");
                if (typeof window.triggerInception === 'function') {
                    window.triggerInception();
                    setTimeout(() => window.logAutonomy("> Neuro-linguística aplicada. Gorjeta extrema forçada."), 6000);
                }
            } else {
                window.logAutonomy("> Varredura de rotinas... Sistema estável. Monitorando...");
            }
            
        }, 15000); // 15 seconds
    };
"""

content = content.replace("window.toggleToolsModal = function() {", js_code + "\n    window.toggleToolsModal = function() {")

with open('index.html', 'w') as f:
    f.write(content)

print("Jarvis Autonomy injected successfully!")
