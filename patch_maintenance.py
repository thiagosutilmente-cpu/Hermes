import sys

with open('index.html', 'r') as f:
    content = f.read()

maintenance_html = """
          <!-- MANUTENÇÃO INTELIGENTE JARVIS -->
          <div class="form-group" style="background: rgba(0, 245, 212, 0.05); border: 1px solid rgba(0, 245, 212, 0.2); padding: 16px; border-radius: 12px; margin-bottom: 16px; box-shadow: 0 4px 20px rgba(0, 245, 212, 0.1);">
            <div style="font-weight: bold; color: var(--accent-green); display: flex; align-items: center; justify-content: space-between; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; border-bottom: 1px solid rgba(0, 245, 212, 0.1); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 6px;">
                <span class="material-symbols-rounded" style="font-size: 20px;">build_circle</span> Manutenção Preditiva (Moto)
              </div>
              <span style="font-size: 9px; background: rgba(0,245,212,0.1); padding: 2px 6px; border-radius: 4px; color: var(--accent-green);">BETA</span>
            </div>
            
            <div style="margin-bottom: 14px;">
              <label style="font-size: 11px; margin-bottom: 4px; display: block; color: var(--text-muted);">KM Atual da Moto</label>
              <div class="input-wrapper" style="position: relative;">
                <input type="number" id="settingsMotoKm" placeholder="Ex: 15400" style="width: 100%; background: var(--bg-dark); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 6px; padding: 8px; font-size: 12px; outline: none; padding-left: 32px;" onchange="window.updateMaintenancePredictor()">
                <span class="material-symbols-rounded" style="position: absolute; left: 8px; top: 50%; transform: translateY(-50%); font-size: 16px; color: var(--text-muted);">speed</span>
              </div>
            </div>

            <div style="display: flex; flex-direction: column; gap: 10px;">
                <div style="display: flex; align-items: center; justify-content: space-between; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.05);">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span class="material-symbols-rounded" style="color: #FFB703; font-size: 18px;">oil_barrel</span>
                        <div>
                            <div style="font-size: 11px; font-weight: bold; color: var(--text-main);">Óleo do Motor</div>
                            <div style="font-size: 9px; color: var(--text-muted);">Troca a cada 3.000 km</div>
                        </div>
                    </div>
                    <div id="maintOilStatus" style="font-size: 10px; font-weight: bold; color: var(--success); text-align: right;">
                        Faltam 1.200 km
                    </div>
                </div>

                <div style="display: flex; align-items: center; justify-content: space-between; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.05);">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span class="material-symbols-rounded" style="color: var(--accent-pink); font-size: 18px;">tire_repair</span>
                        <div>
                            <div style="font-size: 11px; font-weight: bold; color: var(--text-main);">Pneu Traseiro</div>
                            <div style="font-size: 9px; color: var(--text-muted);">Troca a cada 12.000 km</div>
                        </div>
                    </div>
                    <div id="maintTireStatus" style="font-size: 10px; font-weight: bold; color: #FFB703; text-align: right;">
                        Faltam 800 km
                    </div>
                </div>
                
                <div style="display: flex; align-items: center; justify-content: space-between; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.05);">
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <span class="material-symbols-rounded" style="color: var(--accent-blue); font-size: 18px;">settings</span>
                        <div>
                            <div style="font-size: 11px; font-weight: bold; color: var(--text-main);">Relação (Kit)</div>
                            <div style="font-size: 9px; color: var(--text-muted);">Troca a cada 20.000 km</div>
                        </div>
                    </div>
                    <div id="maintChainStatus" style="font-size: 10px; font-weight: bold; color: var(--success); text-align: right;">
                        Faltam 5.000 km
                    </div>
                </div>
            </div>
          </div>
"""

content = content.replace('          <!-- GESTOS DE TELA -->', maintenance_html + '\n          <!-- GESTOS DE TELA -->')

js_code = """
    window.updateMaintenancePredictor = function() {
        const kmInput = document.getElementById('settingsMotoKm');
        if (!kmInput) return;
        const currentKm = parseInt(kmInput.value) || 0;
        
        if (currentKm > 0) {
            // Logic for Oil (3k interval)
            const nextOil = Math.ceil(currentKm / 3000) * 3000;
            const diffOil = nextOil - currentKm;
            const oilEl = document.getElementById('maintOilStatus');
            if (oilEl) {
                oilEl.innerText = `Faltam ${diffOil} km`;
                oilEl.style.color = diffOil < 300 ? 'var(--accent-pink)' : (diffOil < 1000 ? '#FFB703' : 'var(--success)');
            }
            
            // Logic for Tire (12k interval)
            const nextTire = Math.ceil(currentKm / 12000) * 12000;
            const diffTire = nextTire - currentKm;
            const tireEl = document.getElementById('maintTireStatus');
            if (tireEl) {
                tireEl.innerText = `Faltam ${diffTire} km`;
                tireEl.style.color = diffTire < 1000 ? 'var(--accent-pink)' : (diffTire < 3000 ? '#FFB703' : 'var(--success)');
            }
            
            // Logic for Chain (20k interval)
            const nextChain = Math.ceil(currentKm / 20000) * 20000;
            const diffChain = nextChain - currentKm;
            const chainEl = document.getElementById('maintChainStatus');
            if (chainEl) {
                chainEl.innerText = `Faltam ${diffChain} km`;
                chainEl.style.color = diffChain < 1500 ? 'var(--accent-pink)' : (diffChain < 5000 ? '#FFB703' : 'var(--success)');
            }
            
            SecureStorage.setItem('moto_current_km', currentKm);
        }
    };
    
    // Auto load
    setTimeout(() => {
        const savedKm = SecureStorage.getItem('moto_current_km');
        if (savedKm) {
            const input = document.getElementById('settingsMotoKm');
            if (input) {
                input.value = savedKm;
                window.updateMaintenancePredictor();
            }
        }
    }, 1500);
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)
print("Maintenance Dashboard Injected")
