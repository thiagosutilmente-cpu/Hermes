import sys

with open('index.html', 'r') as f:
    content = f.read()

modal_html = """
  <!-- GHOST SEQUENCE MODAL -->
  <div id="ghostSequenceModal" style="display: none; position: fixed; inset: 0; background: rgba(0, 0, 0, 0.9); backdrop-filter: blur(10px); z-index: 100000; align-items: center; justify-content: center; padding: 20px;">
    <!-- Matrix Rain Canvas -->
    <canvas id="matrixCanvas" style="position: absolute; inset: 0; z-index: 0; opacity: 0.3; pointer-events: none;"></canvas>
    
    <div style="background: rgba(10, 11, 16, 0.95); border: 2px solid #A200FF; border-radius: 16px; width: 100%; max-width: 450px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 0 50px rgba(162, 0, 255, 0.3); position: relative; z-index: 1;">
      <div style="padding: 16px 20px; border-bottom: 1px solid rgba(162, 0, 255, 0.3); display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, rgba(162, 0, 255, 0.15) 0%, rgba(10, 11, 16, 1) 100%);">
        <div style="display: flex; align-items: center; gap: 8px; font-weight: 900; font-size: 16px; color: #A200FF; letter-spacing: 1px;">
          <span class="material-symbols-rounded">psychology</span>
          GHOST SEQUENCE CONFIG
        </div>
        <button onclick="window.closeGhostSequenceModal()" style="background: none; border: none; color: #A200FF; cursor: pointer; transition: 0.2s;" onmouseover="this.style.color='#fff';" onmouseout="this.style.color='#A200FF';">
          <span class="material-symbols-rounded">close</span>
        </button>
      </div>
      
      <div style="padding: 20px; overflow-y: auto; max-height: 70vh;">
        <div style="background: rgba(162, 0, 255, 0.05); border: 1px solid rgba(162, 0, 255, 0.2); border-radius: 8px; padding: 12px; margin-bottom: 20px; font-size: 11.5px; color: var(--text-muted); line-height: 1.5;">
          A Sequência Fantasma utiliza IA preditiva para antecipar corridas ocultas nos servidores da Uber e iFood antes que cheguem a outros motoristas. Ajuste os pesos do algoritmo abaixo.
        </div>

        <div style="margin-bottom: 20px;">
          <label style="font-size: 12px; color: var(--text-main); font-weight: bold; margin-bottom: 8px; display: block;">Agressividade do Algoritmo</label>
          <div style="display: flex; background: rgba(0,0,0,0.3); border-radius: 8px; overflow: hidden; border: 1px solid var(--border-light);">
            <button id="btnGhostAggro1" onclick="window.setGhostAggro('CONSERVADOR')" style="flex: 1; padding: 10px; border: none; background: transparent; color: var(--text-muted); font-size: 11px; font-weight: bold; cursor: pointer;">CONSERVADOR</button>
            <button id="btnGhostAggro2" onclick="window.setGhostAggro('EQUILIBRADO')" style="flex: 1; padding: 10px; border: none; border-left: 1px solid var(--border-light); border-right: 1px solid var(--border-light); background: rgba(162, 0, 255, 0.3); color: #fff; font-size: 11px; font-weight: bold; cursor: pointer;">EQUILIBRADO</button>
            <button id="btnGhostAggro3" onclick="window.setGhostAggro('AGRESSIVO')" style="flex: 1; padding: 10px; border: none; background: transparent; color: var(--text-muted); font-size: 11px; font-weight: bold; cursor: pointer;">AGRESSIVO</button>
          </div>
        </div>

        <div style="margin-bottom: 20px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <label style="font-size: 12px; color: var(--text-main); font-weight: bold;">Peso de Tráfego (Traffic Weight)</label>
            <span id="ghostTrafficValue" style="color: #A200FF; font-weight: bold; font-size: 12px;">50%</span>
          </div>
          <input type="range" id="ghostTrafficSlider" min="0" max="100" value="50" style="width: 100%; accent-color: #A200FF;" oninput="document.getElementById('ghostTrafficValue').innerText = this.value + '%'">
          <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Define o quanto o algoritmo deve desviar de rotas com trânsito pesado.</div>
        </div>

        <div style="margin-bottom: 20px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <label style="font-size: 12px; color: var(--text-main); font-weight: bold;">Sensibilidade de Latência</label>
            <span id="ghostLatencyValue" style="color: #A200FF; font-weight: bold; font-size: 12px;">30%</span>
          </div>
          <input type="range" id="ghostLatencySlider" min="0" max="100" value="30" style="width: 100%; accent-color: #A200FF;" oninput="document.getElementById('ghostLatencyValue').innerText = this.value + '%'">
          <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">Tempo de reação para interceptar corridas. Valores mais altos exigem mais processamento.</div>
        </div>
      </div>
      
      <div style="padding: 16px 20px; border-top: 1px solid rgba(162, 0, 255, 0.3); background: rgba(0,0,0,0.3);">
        <button onclick="window.saveGhostSequenceConfig()" style="width: 100%; background: linear-gradient(135deg, #A200FF 0%, #5e00ff 100%); color: white; border: none; padding: 14px; border-radius: 8px; font-size: 14px; font-weight: 900; cursor: pointer; transition: 0.2s; letter-spacing: 2px;" onmouseover="this.style.opacity='0.9';" onmouseout="this.style.opacity='1';">INJETAR CONFIGURAÇÃO</button>
      </div>
    </div>
  </div>
"""

content = content.replace('  <!-- Gamification Modal -->', modal_html + '\n  <!-- Gamification Modal -->')


js_code = """
    window.matrixInterval = null;
    
    window.startMatrixRain = function() {
        const canvas = document.getElementById('matrixCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        
        const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%^&*';
        const fontSize = 14;
        const columns = canvas.width / fontSize;
        const drops = [];
        for (let x = 0; x < columns; x++) drops[x] = 1;
        
        if (window.matrixInterval) clearInterval(window.matrixInterval);
        
        window.matrixInterval = setInterval(() => {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.1)';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            // Defina a cor original (verde). Em GOD MODE será mudada fora daqui
            if (!ctx.fillStyle || ctx.fillStyle === 'rgba(0, 0, 0, 0.1)') {
                ctx.fillStyle = '#0f0'; 
            }
            ctx.font = fontSize + 'px monospace';
            
            for (let i = 0; i < drops.length; i++) {
                const text = letters[Math.floor(Math.random() * letters.length)];
                ctx.fillText(text, i * fontSize, drops[i] * fontSize);
                if (drops[i] * fontSize > canvas.height && Math.random() > 0.975) drops[i] = 0;
                drops[i]++;
            }
        }, 33);
    };

    window.openGhostSequenceModal = function() {
        const modal = document.getElementById('ghostSequenceModal');
        if (modal) {
            modal.style.display = 'flex';
            window.startMatrixRain();
            
            // Carregar config
            if (window.currentDriverSettings) {
                const s = window.currentDriverSettings;
                if (s.ghostSequenceAggressiveness) window.setGhostAggro(s.ghostSequenceAggressiveness);
                
                if (s.ghostSequenceTrafficWeight !== undefined) {
                    const tw = Math.round(s.ghostSequenceTrafficWeight * 100);
                    document.getElementById('ghostTrafficSlider').value = tw;
                    document.getElementById('ghostTrafficValue').innerText = tw + '%';
                }
                
                if (s.ghostSequenceLatencyWeight !== undefined) {
                    const lw = Math.round(s.ghostSequenceLatencyWeight * 100);
                    document.getElementById('ghostLatencySlider').value = lw;
                    document.getElementById('ghostLatencyValue').innerText = lw + '%';
                }
            }
        }
    };

    window.closeGhostSequenceModal = function() {
        const modal = document.getElementById('ghostSequenceModal');
        if (modal) {
            modal.style.display = 'none';
            if (window.matrixInterval) clearInterval(window.matrixInterval);
        }
    };

    window.currentGhostAggro = 'EQUILIBRADO';
    window.setGhostAggro = function(level) {
        window.currentGhostAggro = level;
        document.getElementById('btnGhostAggro1').style.background = 'transparent';
        document.getElementById('btnGhostAggro1').style.color = 'var(--text-muted)';
        document.getElementById('btnGhostAggro2').style.background = 'transparent';
        document.getElementById('btnGhostAggro2').style.color = 'var(--text-muted)';
        document.getElementById('btnGhostAggro3').style.background = 'transparent';
        document.getElementById('btnGhostAggro3').style.color = 'var(--text-muted)';
        
        let targetBtn = null;
        if (level === 'CONSERVADOR') targetBtn = document.getElementById('btnGhostAggro1');
        if (level === 'EQUILIBRADO') targetBtn = document.getElementById('btnGhostAggro2');
        if (level === 'AGRESSIVO') targetBtn = document.getElementById('btnGhostAggro3');
        
        if (targetBtn) {
            targetBtn.style.background = 'rgba(162, 0, 255, 0.3)';
            targetBtn.style.color = '#fff';
        }
    };

    window.saveGhostSequenceConfig = function() {
        const tw = parseInt(document.getElementById('ghostTrafficSlider').value) / 100;
        const lw = parseInt(document.getElementById('ghostLatencySlider').value) / 100;
        
        if (window.currentDriverId) {
            window.saveDriverSettings(window.currentDriverId, {
                isGhostSequenceEnabled: true,
                ghostSequenceAggressiveness: window.currentGhostAggro,
                ghostSequenceTrafficWeight: tw,
                ghostSequenceLatencyWeight: lw
            });
            if (window.showToast) window.showToast("⚡ Sequência Fantasma Injetada com Sucesso!", "success");
            if (window.speakText) window.speakText("Parâmetros da sequência fantasma atualizados e injetados nos servidores da Uber.");
            window.closeGhostSequenceModal();
        } else {
            if (window.showToast) window.showToast("Falha: Você precisa estar logado.", "error");
        }
    };
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)
print("Ghost sequence logic injected")
