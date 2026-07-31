import sys

with open('index.html', 'r') as f:
    content = f.read()

quick_actions_html = """
    <!-- AÇÕES RÁPIDAS DE TERRENO (QUICK ACTIONS) -->
    <div style="margin-bottom: 12px; display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; scrollbar-width: none;" class="jarvis-quick-pills">
        <button class="jarvis-quick-pill" onclick="window.toggleRainMode()" id="btnRainMode" style="display: flex; align-items: center; gap: 4px; background: rgba(58, 134, 255, 0.1); border: 1px solid var(--accent-blue); color: var(--accent-blue); font-weight: bold; padding: 6px 12px; border-radius: 20px;">
            <span class="material-symbols-rounded" style="font-size: 14px;">rainy</span> Modo Chuva (Nitro)
        </button>
        <button class="jarvis-quick-pill" onclick="window.toggleSecurityTimer()" id="btnSecurityTimer" style="display: flex; align-items: center; gap: 4px; background: rgba(255, 183, 3, 0.1); border: 1px solid #FFB703; color: #FFB703; font-weight: bold; padding: 6px 12px; border-radius: 20px;">
            <span class="material-symbols-rounded" style="font-size: 14px;">timer</span> Timer SOS (15m)
        </button>
        <button class="jarvis-quick-pill" onclick="window.openRiskZoneManager()" style="display: flex; align-items: center; gap: 4px; background: rgba(255, 0, 110, 0.1); border: 1px solid var(--accent-pink); color: var(--accent-pink); font-weight: bold; padding: 6px 12px; border-radius: 20px;">
            <span class="material-symbols-rounded" style="font-size: 14px;">gpp_bad</span> Zonas de Risco
        </button>
    </div>
"""

content = content.replace('    <!-- Estatísticas Rápidas do Piloto -->', quick_actions_html + '\n    <!-- Estatísticas Rápidas do Piloto -->')

js_code = """
    window.rainModeActive = false;
    window.toggleRainMode = function() {
        window.rainModeActive = !window.rainModeActive;
        const btn = document.getElementById('btnRainMode');
        if (window.rainModeActive) {
            btn.style.background = 'var(--accent-blue)';
            btn.style.color = '#fff';
            if (window.showToast) window.showToast("🌧️ MODO CHUVA ATIVADO: Filtro restrito para viagens curtas e alto valor.", "info");
            if (window.speakText) window.speakText("Modo chuva ativado, Thiago. Proteção extra. Filtraremos apenas rotas curtas de altíssimo valor e evitaremos alagamentos.");
            
            // Adjust Firestore settings temporarily
            if (window.currentDriverId) {
                window.saveDriverSettings(window.currentDriverId, {
                    ghostSequenceTrafficWeight: 1.0,
                    ghostSequenceAggressiveness: 'AGRESSIVO'
                });
            }
        } else {
            btn.style.background = 'rgba(58, 134, 255, 0.1)';
            btn.style.color = 'var(--accent-blue)';
            if (window.showToast) window.showToast("☀️ Modo Chuva desativado.", "success");
            if (window.speakText) window.speakText("Modo chuva desativado. Retornando aos parâmetros normais de operação.");
        }
    };

    window.securityTimer = null;
    window.securityTimerCount = 0;
    window.toggleSecurityTimer = function() {
        const btn = document.getElementById('btnSecurityTimer');
        if (window.securityTimer) {
            clearInterval(window.securityTimer);
            window.securityTimer = null;
            btn.style.background = 'rgba(255, 183, 3, 0.1)';
            btn.style.color = '#FFB703';
            btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px;">timer</span> Timer SOS (15m)';
            if (window.showToast) window.showToast("Timer de Segurança Cancelado.", "success");
            if (window.speakText) window.speakText("Check-in de segurança desativado.");
        } else {
            window.securityTimerCount = 15 * 60; // 15 mins
            btn.style.background = '#FFB703';
            btn.style.color = '#000';
            if (window.showToast) window.showToast("⏳ Timer SOS ativado. O SOS será acionado em 15 minutos se não for cancelado.", "warning");
            if (window.speakText) window.speakText("Modo de segurança ativo. Em 15 minutos se você não fizer check-in, enviarei sua localização de resgate.");
            
            window.securityTimer = setInterval(() => {
                window.securityTimerCount--;
                const mins = Math.floor(window.securityTimerCount / 60);
                const secs = window.securityTimerCount % 60;
                btn.innerHTML = `<span class="material-symbols-rounded" style="font-size: 14px;">timer</span> Check-in: ${mins}:${secs.toString().padStart(2, '0')}`;
                
                if (window.securityTimerCount === 60) { // 1 min left
                    if (window.speakText) window.speakText("Atenção, Thiago. Falta 1 minuto para o acionamento do resgate. Pressione o botão para cancelar.");
                    if (window.navigator.vibrate) window.navigator.vibrate([200, 100, 200]);
                }
                
                if (window.securityTimerCount <= 0) {
                    clearInterval(window.securityTimer);
                    window.securityTimer = null;
                    btn.style.background = 'rgba(255, 183, 3, 0.1)';
                    btn.style.color = '#FFB703';
                    btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px;">timer</span> Timer SOS (15m)';
                    if (window.triggerSOSMode) window.triggerSOSMode();
                }
            }, 1000);
        }
    };
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)
print("Rain Mode and SOS timer injected")
