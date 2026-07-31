import sys

with open('index.html', 'r') as f:
    content = f.read()

modal_html = """
  <!-- Gamification Modal -->
  <div id="gamificationModal" style="display: none; position: fixed; inset: 0; background: rgba(10, 11, 16, 0.85); backdrop-filter: blur(8px); z-index: 9999; align-items: center; justify-content: center; padding: 20px;">
    <div style="background: var(--surface); border: 1px solid var(--accent-blue); border-radius: 16px; width: 100%; max-width: 400px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 0 30px rgba(58, 134, 255, 0.2); animation: slideDownAndHighlight 0.4s ease forwards;">
      <div style="padding: 16px 20px; border-bottom: 1px solid var(--border-light); display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, rgba(58, 134, 255, 0.1) 0%, var(--surface-variant) 100%);">
        <div style="display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 16px;">
          <span class="material-symbols-rounded" style="color: var(--accent-blue); font-size: 24px;">military_tech</span>
          Patente & Missões
        </div>
        <button onclick="window.closeGamificationModal()" style="background: none; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: color 0.2s;" onmouseover="this.style.color='#fff';" onmouseout="this.style.color='var(--text-muted)';">
          <span class="material-symbols-rounded">close</span>
        </button>
      </div>
      <div style="padding: 20px; overflow-y: auto;">
        
        <div style="text-align: center; margin-bottom: 20px;">
            <div id="modalGamificationIcon" style="width: 80px; height: 80px; margin: 0 auto 10px auto; background: linear-gradient(135deg, #FFD700, #FF8C00); border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 20px rgba(255, 215, 0, 0.4);">
                <span class="material-symbols-rounded" style="font-size: 45px; color: #000;">military_tech</span>
            </div>
            <h3 id="modalGamificationTitle" style="margin: 0; font-size: 20px; color: #FFD700; text-transform: uppercase; letter-spacing: 1px;">Líder de Esquadrão</h3>
            <p style="margin: 5px 0 0 0; font-size: 13px; color: var(--text-muted);">Continue completando missões para subir de patente!</p>
        </div>

        <div style="margin-bottom: 20px;">
            <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px; font-weight: bold;">
                <span style="color: var(--text-main);">Progresso</span>
                <span style="color: #FFD700;" id="modalGamificationXpText">0 / 1000 XP</span>
            </div>
            <div style="width: 100%; height: 10px; background: rgba(255,255,255,0.05); border-radius: 5px; overflow: hidden; border: 1px solid rgba(255,255,255,0.1);">
                <div id="modalGamificationXpBar" style="height: 100%; width: 50%; background: linear-gradient(90deg, #FF8C00, #FFD700); border-radius: 5px; box-shadow: 0 0 10px rgba(255, 215, 0, 0.5);"></div>
            </div>
        </div>

        <div style="background: rgba(0,0,0,0.2); border-radius: 12px; padding: 15px; border: 1px solid var(--border-light);">
            <div style="font-size: 13px; font-weight: bold; margin-bottom: 12px; display: flex; align-items: center; gap: 6px;">
                <span class="material-symbols-rounded" style="font-size: 18px; color: var(--accent-pink);">workspace_premium</span>
                Vantagens da Patente
            </div>
            <ul id="modalGamificationPerks" style="margin: 0; padding-left: 20px; font-size: 12px; color: var(--text-muted); line-height: 1.6;">
                <li>Prioridade no Auto-Accept Inteligente</li>
                <li>Radar de Zonas Quentes (Ativado)</li>
                <li>Desvio de Rota Preditivo (+Eficiência)</li>
            </ul>
        </div>
      </div>
      <div style="padding: 16px 20px; border-top: 1px solid var(--border-light); background: rgba(0,0,0,0.15);">
        <button onclick="window.closeGamificationModal()" style="width: 100%; background: linear-gradient(135deg, var(--accent-blue) 0%, #2a68d4 100%); color: white; border: none; padding: 12px; border-radius: 8px; font-size: 14px; font-weight: 700; cursor: pointer; transition: opacity 0.2s;" onmouseover="this.style.opacity='0.9';" onmouseout="this.style.opacity='1';">ENTENDIDO</button>
      </div>
    </div>
  </div>
"""

quests_card_html = """
    <!-- Missões Diárias -->
    <div class="section-card" id="dailyQuestsCard" style="margin-bottom: 20px; background: linear-gradient(135deg, rgba(255, 215, 0, 0.05) 0%, rgba(10, 11, 16, 0.8) 100%); border: 1px solid rgba(255, 215, 0, 0.2);">
      <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div class="section-title" style="margin-bottom: 0; font-size: 13.5px; display: flex; align-items: center; gap: 6px; font-weight: bold;">
          <span class="material-symbols-rounded" style="color: #FFD700; font-size: 20px;">workspace_premium</span>
          Missões de Batalha (Hoje)
        </div>
        <span style="font-size: 10px; color: #FFD700; background: rgba(255, 215, 0, 0.15); padding: 2px 6px; border-radius: 4px; font-weight: bold;">XP EXTRA</span>
      </div>
      
      <div id="questsContainer" style="display: flex; flex-direction: column; gap: 8px;">
        <!-- Quest 1 -->
        <div style="background: rgba(0,0,0,0.25); border: 1px solid var(--border-light); border-radius: 8px; padding: 10px; display: flex; align-items: center; gap: 12px;">
            <div style="width: 36px; height: 36px; background: rgba(255, 215, 0, 0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255, 215, 0, 0.3);">
                <span class="material-symbols-rounded" style="color: #FFD700; font-size: 20px;">speed</span>
            </div>
            <div style="flex: 1;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                    <span style="font-size: 11.5px; font-weight: bold; color: #fff;">Rodar 50 KM</span>
                    <span style="font-size: 10px; color: #FFD700; font-weight: bold;">+300 XP</span>
                </div>
                <div style="width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; margin-bottom: 4px;">
                    <div id="quest1Progress" style="width: 0%; height: 100%; background: linear-gradient(90deg, #FF8C00, #FFD700); border-radius: 3px; transition: width 0.5s ease;"></div>
                </div>
                <div style="font-size: 9px; color: var(--text-muted); text-align: right;" id="quest1Text">0 / 50 KM</div>
            </div>
        </div>

        <!-- Quest 2 -->
        <div style="background: rgba(0,0,0,0.25); border: 1px solid var(--border-light); border-radius: 8px; padding: 10px; display: flex; align-items: center; gap: 12px;">
            <div style="width: 36px; height: 36px; background: rgba(58, 134, 255, 0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(58, 134, 255, 0.3);">
                <span class="material-symbols-rounded" style="color: var(--accent-blue); font-size: 20px;">alt_route</span>
            </div>
            <div style="flex: 1;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                    <span style="font-size: 11.5px; font-weight: bold; color: #fff;">3 Corridas Mescladas</span>
                    <span style="font-size: 10px; color: var(--accent-blue); font-weight: bold;">+500 XP</span>
                </div>
                <div style="width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; margin-bottom: 4px;">
                    <div id="quest2Progress" style="width: 33%; height: 100%; background: linear-gradient(90deg, #2a68d4, var(--accent-blue)); border-radius: 3px; transition: width 0.5s ease;"></div>
                </div>
                <div style="font-size: 9px; color: var(--text-muted); text-align: right;" id="quest2Text">1 / 3 Mesclas</div>
            </div>
        </div>
      </div>
    </div>
"""

js_code = """
    window.openGamificationModal = function() {
        const modal = document.getElementById('gamificationModal');
        if (modal) {
            modal.style.display = 'flex';
            
            // Populate data
            const lvlInfo = window.getDriverLevel(window.driverXP);
            document.getElementById('modalGamificationTitle').innerText = lvlInfo.name;
            document.getElementById('modalGamificationIcon').innerHTML = `<span class="material-symbols-rounded" style="font-size: 45px; color: #000;">${lvlInfo.icon}</span>`;
            document.getElementById('modalGamificationIcon').style.background = `linear-gradient(135deg, ${lvlInfo.color}, rgba(20,20,20,0.9))`;
            document.getElementById('modalGamificationTitle').style.color = lvlInfo.color;
            
            let prevMax = 0;
            if (lvlInfo.level === 2) prevMax = 500;
            if (lvlInfo.level === 3) prevMax = 1500;
            if (lvlInfo.level === 4) prevMax = 3000;
            if (lvlInfo.level === 5) prevMax = 6000;
            if (lvlInfo.level === 6) prevMax = 10000;
            
            const currentLevelXP = window.driverXP - prevMax;
            const neededXP = lvlInfo.max - prevMax;
            let percent = 100;
            
            const xpText = document.getElementById('modalGamificationXpText');
            const xpBar = document.getElementById('modalGamificationXpBar');
            
            if (lvlInfo.level < 6) {
                percent = Math.min(100, Math.max(0, (currentLevelXP / neededXP) * 100));
                xpText.innerText = `${currentLevelXP} / ${neededXP} XP`;
            } else {
                xpText.innerText = `MÁXIMO (${window.driverXP} XP)`;
            }
            xpBar.style.width = `${percent}%`;
            xpBar.style.background = `linear-gradient(90deg, ${lvlInfo.color}, #FFF)`;
            
            const perksUl = document.getElementById('modalGamificationPerks');
            if (perksUl) {
                if (lvlInfo.level >= 2) {
                    perksUl.innerHTML = `
                        <li>Prioridade no Auto-Accept Inteligente</li>
                        <li>Radar de Zonas Quentes (Ativado)</li>
                        ${lvlInfo.level >= 3 ? '<li>Desvio de Rota Preditivo (+Eficiência)</li>' : ''}
                        ${lvlInfo.level >= 4 ? '<li>Rejeição Stealth sem queda de score</li>' : ''}
                        ${lvlInfo.level >= 5 ? '<li>Modo Fantasma (Total Invisibilidade)</li>' : ''}
                    `;
                } else {
                    perksUl.innerHTML = `<li>Complete missões para desbloquear vantagens.</li>`;
                }
            }
        }
    };

    window.closeGamificationModal = function() {
        const modal = document.getElementById('gamificationModal');
        if (modal) {
            modal.style.display = 'none';
        }
    };

    window.updateQuestsProgress = function(km) {
        // Quest 1: 50KM
        const maxKm = 50;
        let percent = Math.min(100, (km / maxKm) * 100);
        const bar = document.getElementById('quest1Progress');
        const text = document.getElementById('quest1Text');
        if (bar && text) {
            bar.style.width = `${percent}%`;
            text.innerText = `${km.toFixed(1)} / ${maxKm} KM`;
            if (percent >= 100 && !window.quest1Completed) {
                window.quest1Completed = true;
                window.addDriverXP(300);
                if (window.showToast) window.showToast("🌟 Missão Concluída: 50 KM! +300 XP", "success");
                if (window.speakText) window.speakText("Parabéns, Piloto. Missão de rodagem concluída. 300 pontos de experiência adicionados.");
            }
        }
    };
"""

# Insert modal before Calendar Modal
content = content.replace('  <!-- Calendar Modal -->', modal_html + '\n  <!-- Calendar Modal -->')

# Insert quests card after Maintenance card
content = content.replace('    <!-- Sugestões de Zonas de Alta Demanda e Estacionamento -->', quests_card_html + '\n    <!-- Sugestões de Zonas de Alta Demanda e Estacionamento -->')

# Insert JS before window.showCalendarModal = function()
content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Injections successful!")
