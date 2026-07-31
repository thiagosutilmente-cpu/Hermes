import sys

with open('index.html', 'r') as f:
    content = f.read()

neural_html = """
      <!-- BIOMETRIC NEURAL SYNC (FATIGUE MONITOR) -->
      <div class="section-card" id="neuralSyncDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(0, 240, 255, 0.05) 0%, rgba(10, 11, 18, 0.95) 100%); border: 1px solid rgba(0, 240, 255, 0.3); padding: 16px; margin-top: 15px; border-radius: 12px; display: flex; align-items: center; gap: 15px; position: relative; overflow: hidden;">
          <div style="position: absolute; left: -20px; top: -20px; width: 100px; height: 100px; background: radial-gradient(circle, rgba(0, 240, 255, 0.1) 0%, transparent 70%);"></div>
          
          <!-- Avatar / Brain Icon -->
          <div style="width: 50px; height: 50px; border-radius: 50%; border: 2px solid #00f0ff; display: flex; align-items: center; justify-content: center; position: relative; background: rgba(0,0,0,0.5); box-shadow: 0 0 15px rgba(0, 240, 255, 0.2);">
              <span class="material-symbols-rounded" style="color: #00f0ff; font-size: 28px; animation: pulse 1s infinite;">neurology</span>
              <svg style="position: absolute; inset: -4px; width: 58px; height: 58px; transform: rotate(-90deg);">
                  <circle cx="29" cy="29" r="27" fill="none" stroke="rgba(0, 240, 255, 0.2)" stroke-width="2"/>
                  <circle id="neuralLoadRing" cx="29" cy="29" r="27" fill="none" stroke="#00f0ff" stroke-width="2" stroke-dasharray="170" stroke-dashoffset="50" style="transition: stroke-dashoffset 1s ease;"/>
              </svg>
          </div>
          
          <!-- Metrics -->
          <div style="flex: 1; display: flex; flex-direction: column; gap: 8px;">
              <div style="display: flex; justify-content: space-between; align-items: flex-end;">
                  <div>
                      <div style="font-size: 13px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Sincronia Neural</div>
                      <div style="font-size: 9px; color: #00f0ff; font-family: monospace;">TELEMETRIA COGNITIVA ATIVA</div>
                  </div>
                  <div style="text-align: right;">
                      <span id="neuralStatusText" style="font-size: 10px; font-weight: bold; background: rgba(0, 255, 0, 0.1); color: #00ff00; padding: 2px 6px; border-radius: 4px; border: 1px solid #00ff00;">FOCADO</span>
                  </div>
              </div>
              
              <div style="display: flex; gap: 10px;">
                  <div style="flex: 1; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; padding: 6px; text-align: center;">
                      <div style="font-size: 8px; color: #888; text-transform: uppercase;">Carga Cognitiva</div>
                      <div style="font-size: 14px; font-weight: bold; color: #fff; font-family: monospace;"><span id="cogLoadVal">34</span>%</div>
                  </div>
                  <div style="flex: 1; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; padding: 6px; text-align: center;">
                      <div style="font-size: 8px; color: #888; text-transform: uppercase;">Tempo de Reação</div>
                      <div style="font-size: 14px; font-weight: bold; color: #fff; font-family: monospace;"><span id="reactTimeVal">180</span>ms</div>
                  </div>
                  <div style="flex: 1; background: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; padding: 6px; text-align: center;">
                      <div style="font-size: 8px; color: #888; text-transform: uppercase;">Fadiga Visual</div>
                      <div style="font-size: 14px; font-weight: bold; color: #fff; font-family: monospace;"><span id="fatigueVal">12</span>%</div>
                  </div>
              </div>
          </div>
      </div>
"""

idx = content.find('<!-- ALGORITHMIC FARE NEGOTIATOR (SURGE FORCER) -->')
if idx != -1:
    content = content[:idx] + neural_html + '\n      ' + content[idx:]
else:
    print("Could not find Surge Forcer")

js_code = """
    window.startNeuralSync = function() {
        setInterval(() => {
            const cogBase = 30;
            const cogFluc = Math.floor(Math.random() * 40);
            const load = cogBase + cogFluc;
            
            const react = Math.floor(150 + Math.random() * 100);
            const fatigue = Math.floor(10 + (load * 0.2) + Math.random() * 10);
            
            const loadEl = document.getElementById('cogLoadVal');
            const reactEl = document.getElementById('reactTimeVal');
            const fatigueEl = document.getElementById('fatigueVal');
            const ringEl = document.getElementById('neuralLoadRing');
            const statusEl = document.getElementById('neuralStatusText');
            
            if (loadEl) loadEl.innerText = load;
            if (reactEl) reactEl.innerText = react;
            if (fatigueEl) fatigueEl.innerText = fatigue;
            
            if (ringEl) {
                // 170 is full circle dasharray. offset 0 = full, 170 = empty
                const offset = 170 - ((load / 100) * 170);
                ringEl.style.strokeDashoffset = offset;
                
                if (load > 60) {
                    ringEl.style.stroke = '#ff3c00';
                    statusEl.innerText = 'SOBRECARGA';
                    statusEl.style.color = '#ff3c00';
                    statusEl.style.borderColor = '#ff3c00';
                    statusEl.style.background = 'rgba(255, 60, 0, 0.1)';
                } else if (load > 40) {
                    ringEl.style.stroke = '#ffb703';
                    statusEl.innerText = 'ATENTO';
                    statusEl.style.color = '#ffb703';
                    statusEl.style.borderColor = '#ffb703';
                    statusEl.style.background = 'rgba(255, 183, 3, 0.1)';
                } else {
                    ringEl.style.stroke = '#00f0ff';
                    statusEl.innerText = 'FOCADO';
                    statusEl.style.color = '#00f0ff';
                    statusEl.style.borderColor = '#00f0ff';
                    statusEl.style.background = 'rgba(0, 240, 255, 0.1)';
                }
            }
        }, 3000);
    };
    
    setTimeout(() => {
        if (window.startNeuralSync) window.startNeuralSync();
    }, 2000);
"""

content = content.replace('    window.startSurgeForcerSimulation = function() {', js_code + '\n    window.startSurgeForcerSimulation = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Neural Sync Engine injected!")
