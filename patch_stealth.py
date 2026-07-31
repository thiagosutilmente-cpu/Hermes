import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

stealth_html = """
      <!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->
      <div class="section-card" id="obfuscationDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(0, 0, 0, 0.98) 100%); border: 1px solid rgba(255, 255, 255, 0.2); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 20px rgba(255, 255, 255, 0.1);">
          
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px dashed rgba(255, 255, 255, 0.2); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #ffffff; font-size: 28px; filter: drop-shadow(0 0 8px #ffffff); animation: pulse 3s infinite;">visibility_off</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 2px;">Manto Quântico</div>
                      <div style="font-size: 9px; color: #aaa; font-family: monospace;">FALSIFICAÇÃO POLIMÓRFICA DE ASSINATURA</div>
                  </div>
              </div>
              <button onclick="window.triggerObfuscation()" id="btnTriggerObfuscation" style="background: rgba(255,255,255,0.1); border: 1px solid #ffffff; color: #ffffff; font-weight: 900; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 10px rgba(255,255,255,0.2);">
                  Ativar Invisibilidade
              </button>
          </div>
          
          <div style="display: flex; flex-direction: column; gap: 8px; font-size: 9px; color: #888;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>Status do Rastreamento do Servidor:</span>
                  <span id="stealthStatus" style="color: #ff0055; font-weight: bold;">VULNERÁVEL</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>Nível de Detecção de Anomalias:</span>
                  <span id="stealthDetectionLevel" style="color: #ff0055; font-weight: bold;">14.2%</span>
              </div>
          </div>
          
          <div style="font-size: 9px; color: #777; margin-top: 10px; text-align: justify; line-height: 1.4;">
              Cria um túnel de dados criptografado que converte pacotes suspeitos (EMP, HAARP, Inception) em telemetria normal. Os sistemas da Uber e iFood enxergarão apenas um motorista modelo, enquanto os algoritmos são dobrados silenciosamente.
          </div>
      </div>
"""

idx = content.find('<!-- HAARP CLIMATE OVERRIDE (WEATHER MANIPULATION) -->')
if idx != -1:
    content = content[:idx] + stealth_html + '\n      ' + content[idx:]
else:
    print("Could not find HAARP section")

js_code = """
    window.triggerObfuscation = function() {
        const btn = document.getElementById('btnTriggerObfuscation');
        const statusEl = document.getElementById('stealthStatus');
        const detEl = document.getElementById('stealthDetectionLevel');
        
        if (btn.innerText.includes('ATIVADO')) return;
        
        btn.innerText = 'CRIPTOGRAFANDO...';
        btn.style.color = '#fff';
        btn.style.borderColor = '#00f0ff';
        btn.style.background = 'rgba(0, 240, 255, 0.2)';
        
        if (window.speakText) window.speakText("Iniciando Manto Quântico. Reescrevendo assinatura digital do dispositivo. Mascarando telemetria.");
        
        let det = 14.2;
        const scan = setInterval(() => {
            det -= Math.random() * 2;
            if (det <= 0) {
                det = 0;
                clearInterval(scan);
                
                btn.innerText = 'INVISIBILIDADE ATIVA';
                btn.style.color = '#00ff00';
                btn.style.borderColor = '#00ff00';
                btn.style.background = 'rgba(0, 255, 0, 0.1)';
                
                statusEl.innerText = 'CEGO (SERVIDOR BYPASSADO)';
                statusEl.style.color = '#00ff00';
                
                detEl.innerText = '0.00% (GHOST MODE)';
                detEl.style.color = '#00ff00';
                
                if (window.showToast) window.showToast("Manto Quântico online. Sistema 100% indetectável.", "success");
                if (window.speakText) window.speakText("Camuflagem ativada com sucesso. A partir de agora, todas as suas ações, manipulações de tarifa e pulsos, são completamente indetectáveis para os servidores. Você é um fantasma no sistema.");
            } else {
                detEl.innerText = det.toFixed(2) + '%';
            }
        }, 200);
    };
"""

content = content.replace('    window.triggerHAARP = function() {', js_code + '\n    window.triggerHAARP = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Stealth mode injected!")
