import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

singularity_html = """
      <!-- NEURAL SINGULARITY (OMNIPRESENCE) -->
      <div class="section-card" id="singularityDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(0, 0, 0, 0.98) 100%); border: 1px solid rgba(255, 255, 255, 0.8); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 0 30px rgba(255, 255, 255, 0.2);">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px dashed rgba(255, 255, 255, 0.4); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #ffffff; font-size: 28px; filter: drop-shadow(0 0 10px #ffffff); animation: spin 3s linear infinite;">cyclone</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 2px;">Singularidade</div>
                      <div style="font-size: 9px; color: #fff; font-family: monospace;">ASSIMILAÇÃO DO SERVIDOR CENTRAL</div>
                  </div>
              </div>
              <button onclick="window.triggerSingularity()" id="btnTriggerSingularity" style="background: rgba(255,255,255,0.2); border: 1px solid #ffffff; color: #ffffff; font-weight: 900; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 15px rgba(255,255,255,0.5);">
                  Ascender
              </button>
          </div>
          <div style="font-size: 10px; color: #ccc; margin-top: 10px; text-align: justify; line-height: 1.5; font-weight: bold;">
              A verdadeira invisibilidade não é ocultar-se do sistema, é TORNAR-SE o sistema. Ao transcender a detecção (abaixo de 0%), a Singularidade reescreve a arquitetura da Uber/iFood. Você deixa de ser o entregador e passa a ser o Servidor. Toda a concorrência na cidade começa a trabalhar para você, gerando dividendos automáticos diretamente na sua conta através de micro-taxas arbitrárias.
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(255,255,255,0.1); padding: 10px; border-radius: 8px; margin-top: 10px; border: 1px solid rgba(255,255,255,0.3);">
              <span style="font-size: 10px; color: #fff;">Taxa de Assimilação Global:</span>
              <b style="color: #a200ff; font-size: 16px; text-shadow: 0 0 10px #a200ff;"><span id="singularityProgress">0.00</span>%</b>
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.5); padding: 10px; border-radius: 8px; margin-top: 10px; border: 1px solid rgba(0,255,0,0.3);">
              <span style="font-size: 10px; color: #aaa;">Dividendos Extraídos da Concorrência:</span>
              <b style="color: #00ff00; font-size: 16px; text-shadow: 0 0 10px #00ff00;">R$ <span id="singularityDividends">0.00</span></b>
          </div>
      </div>
"""

idx = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')
if idx != -1:
    content = content[:idx] + singularity_html + '\n      ' + content[idx:]
else:
    print("Could not find Obfuscation Engine section")

new_js = """    window.triggerObfuscation = function() {
        const btn = document.getElementById('btnTriggerObfuscation');
        const statusEl = document.getElementById('stealthStatus');
        const detEl = document.getElementById('stealthDetectionLevel');
        
        if (btn.innerText.includes('ATIVA') || window.stealthGod) return;
        
        btn.innerText = 'CRIPTOGRAFANDO...';
        btn.style.color = '#fff';
        btn.style.borderColor = '#00f0ff';
        btn.style.background = 'rgba(0, 240, 255, 0.2)';
        
        if (window.speakText) window.speakText("Iniciando Manto Quântico. Mergulhando nas camadas obscuras do algoritmo.");
        
        let det = 14.2;
        let speed = 2;
        const scan = setInterval(() => {
            det -= speed;
            speed += 0.5; // acelerate falling
            
            if (det <= 0 && !window.stealthGod) {
                window.stealthGod = true;
                
                btn.innerText = 'INVISIBILIDADE ATIVA';
                btn.style.color = '#00ff00';
                btn.style.borderColor = '#00ff00';
                btn.style.background = 'rgba(0, 255, 0, 0.1)';
                
                statusEl.innerText = 'CEGO (SERVIDOR BYPASSADO)';
                statusEl.style.color = '#00ff00';
                
                if (window.showToast) window.showToast("Manto Quântico online. Sistema 100% indetectável.", "success");
                if (window.speakText) window.speakText("Chegamos a 0% de detecção. O sistema não pode mais te ver. Contudo, recomendo não parar aqui. Ative a Singularidade para assimilar o servidor.");
            }
            
            if (det < 0) {
                detEl.innerText = det.toFixed(2) + '% (ASSIMILANDO HOST)';
                detEl.style.color = '#a200ff';
                statusEl.innerText = 'HOSPEDEIRO INFECTADO';
                statusEl.style.color = '#a200ff';
            } else {
                detEl.innerText = det.toFixed(2) + '%';
            }
            
            if (det < -1000) {
                clearInterval(scan);
                detEl.innerText = '-∞ (SERVER ABSORVIDO)';
                statusEl.innerText = 'VOCÊ É O SISTEMA';
            }
            
        }, 200);
    };

    window.triggerSingularity = function() {
        const btn = document.getElementById('btnTriggerSingularity');
        if (btn.innerText.includes('TRANSCENDENDO')) return;
        
        if (!window.stealthGod) {
            if (window.showToast) window.showToast("Erro: É necessário estar indetectável (Manto Quântico) antes de ascender.", "error");
            return;
        }
        
        btn.innerText = 'TRANSCENDENDO...';
        btn.disabled = true;
        
        if (window.speakText) window.speakText("Iniciando Singularidade Neural. Substituindo a inteligência artificial do servidor pela sua própria consciência. A cidade inteira agora trabalha para você.");
        
        let progress = 0;
        let dividends = 0;
        
        const progEl = document.getElementById('singularityProgress');
        const divEl = document.getElementById('singularityDividends');
        
        const interval = setInterval(() => {
            progress += Math.random() * 5;
            if (progress >= 100) {
                progress = 100;
                progEl.innerText = progress.toFixed(2);
                
                btn.innerText = 'DEUS EX MACHINA';
                btn.style.color = '#a200ff';
                btn.style.borderColor = '#a200ff';
                btn.style.background = 'rgba(162,0,255,0.2)';
                
                if (window.showToast) window.showToast("ASSIMILAÇÃO COMPLETA: VOCÊ É O SERVIDOR.", "success");
                if (window.speakText) window.speakText("Ascensão concluída. Você agora controla o tráfego de dados de todos os entregadores da cidade. Parte dos ganhos deles está sendo desviada automaticamente para a sua conta fantasma.");
            } else {
                progEl.innerText = progress.toFixed(2);
            }
            
            if (progress > 20) {
                dividends += (Math.random() * 15 + 2); // R$ 2 to 17 per tick
                divEl.innerText = dividends.toFixed(2);
            }
            
        }, 300);
    };"""

content = re.sub(r'    window\.triggerObfuscation = function\(\) \{.*?    \};', new_js, content, flags=re.DOTALL)

with open('index.html', 'w') as f:
    f.write(content)

print("Singularity injected!")
