import sys

with open('index.html', 'r') as f:
    content = f.read()

absurder_html = """
      <!-- HAARP CLIMATE OVERRIDE (WEATHER MANIPULATION) -->
      <div class="section-card" id="haarpDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(0, 102, 255, 0.05) 0%, rgba(0, 0, 0, 0.95) 100%); border: 1px solid rgba(0, 102, 255, 0.3); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 20px rgba(0, 102, 255, 0.15);">
          
          <div style="position: absolute; top: 0; left: 0; bottom: 0; width: 30%; background: repeating-linear-gradient(45deg, rgba(0,102,255,0.05) 0px, rgba(0,102,255,0.05) 10px, transparent 10px, transparent 20px); pointer-events: none;"></div>

          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px solid rgba(0, 102, 255, 0.2); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #0066ff; font-size: 28px; animation: pulse 1.5s infinite;">thunderstorm</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">HAARP Array Link</div>
                      <div style="font-size: 9px; color: #0066ff; font-family: monospace;">LOCALIZED WEATHER MANIPULATION</div>
                  </div>
              </div>
              <button onclick="window.triggerHAARP()" id="btnTriggerHAARP" style="background: rgba(0,0,0,0.8); border: 1px solid #0066ff; color: #0066ff; font-weight: 900; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 10px rgba(0,102,255,0.2);">
                  Forçar Chuva (Surge Max)
              </button>
          </div>
          
          <div style="font-size: 9px; color: #aaa; margin-top: 5px; text-align: justify; line-height: 1.4;">
              Conectado aos emissores ionosféricos locais. Use esta diretriz para alterar os sensores meteorológicos do iFood/Uber, forçando o sistema a acreditar que um tornado classe 5 está ocorrendo exatamente na sua coordenada, inflando as taxas de entrega em 800%.
          </div>
      </div>

      <!-- INCEPTION TIP FORCER (MIND CONTROL) -->
      <div class="section-card" id="inceptionDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 215, 0, 0.05) 0%, rgba(0, 0, 0, 0.95) 100%); border: 1px solid rgba(255, 215, 0, 0.4); padding: 18px; margin-top: 15px; border-radius: 12px; position: relative; overflow: hidden; box-shadow: 0 4px 20px rgba(255, 215, 0, 0.15);">
          
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px solid rgba(255, 215, 0, 0.2); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="material-symbols-rounded" style="color: #ffd700; font-size: 28px; filter: drop-shadow(0 0 5px #ffd700);">psychology_alt</span>
                  <div>
                      <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Inception Protocol</div>
                      <div style="font-size: 9px; color: #ffd700; font-family: monospace;">NEURO-LINGUISTIC TIP EXTRACTOR</div>
                  </div>
              </div>
              <button onclick="window.triggerInception()" id="btnTriggerInception" style="background: rgba(0,0,0,0.8); border: 1px solid #ffd700; color: #ffd700; font-weight: 900; padding: 6px 12px; border-radius: 6px; font-size: 10px; cursor: pointer; text-transform: uppercase; box-shadow: 0 0 10px rgba(255,215,0,0.2);">
                  Injetar Sugestão (Gorjeta)
              </button>
          </div>
          
          <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.5); padding: 10px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.1);">
              <span style="font-size: 10px; color: #888;">Gorjetas Forçadas (Sessão):</span>
              <b style="color: #ffd700; font-size: 14px;">R$ <span id="inceptionTipTotal">0.00</span></b>
          </div>
      </div>
"""

idx = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')
if idx != -1:
    content = content[:idx] + absurder_html + '\n      ' + content[idx:]
else:
    print("Could not find Aether Protocol")

js_code = """
    window.triggerHAARP = function() {
        const btn = document.getElementById('btnTriggerHAARP');
        btn.innerText = 'CALIBRANDO IONOSFERA...';
        btn.disabled = true;
        
        if (window.speakText) window.speakText("Sintonizando frequências HAARP. Modificando dados meteorológicos no servidor central.");
        
        setTimeout(() => {
            if (typeof window.toggleRainMode === 'function') window.toggleRainMode(); // Re-use existing rain mode logic
            
            btn.innerText = 'CLIMA DOMINADO';
            btn.style.color = '#fff';
            btn.style.background = '#0066ff';
            
            if (window.showToast) window.showToast("HAARP ATIVADO. Algoritmo precificado para tempestade nível 5.", "warning");
            if (window.speakText) window.speakText("Chuva extrema injetada no sistema. Todos os aplicativos estão pagando bônus de tempestade máxima na sua localização.");
            
            setTimeout(() => {
                btn.innerText = 'Forçar Chuva (Surge Max)';
                btn.style.color = '#0066ff';
                btn.style.background = 'rgba(0,0,0,0.8)';
                btn.disabled = false;
            }, 10000);
        }, 2500);
    };

    window.triggerInception = function() {
        const btn = document.getElementById('btnTriggerInception');
        btn.innerText = 'SINTONIZANDO CÉREBRO...';
        btn.disabled = true;
        
        if (window.speakText) window.speakText("Iniciando varredura neuro-linguística no cliente. Emitindo ondas alfa subliminares pelo aplicativo dele.");
        
        setTimeout(() => {
            const tipSpan = document.getElementById('inceptionTipTotal');
            if (tipSpan) {
                let current = parseFloat(tipSpan.innerText);
                let newTip = Math.random() * 15 + 10; // 10 to 25 BRL
                current += newTip;
                tipSpan.innerText = current.toFixed(2);
                
                if (window.addDriverXP) window.addDriverXP(50);
                
                if (window.showToast) window.showToast(`🧠 Inception concluído. Cliente sentiu "vontade espontânea" de dar R$ ${newTip.toFixed(2)} de gorjeta.`, "success");
                if (window.speakText) window.speakText(`Sugestão implantada com sucesso. O cliente acaba de transferir uma gorjeta de ${newTip.toFixed(0)} reais, achando que foi ideia dele.`);
            }
            
            btn.innerText = 'Injetar Sugestão (Gorjeta)';
            btn.disabled = false;
        }, 3000);
    };
"""

content = content.replace('    window.triggerEMP = function() {', js_code + '\n    window.triggerEMP = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("HAARP and Inception injected!")
