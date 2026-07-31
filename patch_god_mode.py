import sys

with open('index.html', 'r') as f:
    content = f.read()

god_mode_button = """
        <button class="jarvis-quick-pill" onclick="window.triggerGodModeUI()" id="btnGodModeUI" style="display: flex; align-items: center; gap: 4px; background: rgba(162, 0, 255, 0.1); border: 1px solid #A200FF; color: #A200FF; font-weight: bold; padding: 6px 12px; border-radius: 20px;">
            <span class="material-symbols-rounded" style="font-size: 14px;">bolt</span> Modo Combate (God Mode)
        </button>
"""

content = content.replace('<button class="jarvis-quick-pill" onclick="window.openRiskZoneManager()"', god_mode_button + '\n        <button class="jarvis-quick-pill" onclick="window.openRiskZoneManager()"')

js_code = """
    window.godModeUIActive = false;
    window.triggerGodModeUI = function() {
        if (!window.godModeUIActive) {
            window.godModeUIActive = true;
            const btn = document.getElementById('btnGodModeUI');
            if(btn) {
                btn.style.background = '#A200FF';
                btn.style.color = '#FFF';
            }
            if (window.showToast) window.showToast("⚡ GOD MODE ATIVADO: Bypass de velocidade e prioridade máxima.", "info");
            
            // Execute voice command logically
            window.processVoiceCommand("sys_god_mode", true);
            
            // Enable visual red matrix
            if (window.matrixColor !== '#FF0000') {
                window.matrixColor = '#FF0000';
                if (!document.getElementById('ghostSequenceModal') || document.getElementById('ghostSequenceModal').style.display === 'none') {
                    if (window.openGhostSequenceModal) window.openGhostSequenceModal();
                }
            }
        } else {
            window.godModeUIActive = false;
            const btn = document.getElementById('btnGodModeUI');
            if(btn) {
                btn.style.background = 'rgba(162, 0, 255, 0.1)';
                btn.style.color = '#A200FF';
            }
            if (window.showToast) window.showToast("God Mode Desativado.", "success");
            window.matrixColor = '#0f0';
            if (window.currentDriverId) {
                window.saveDriverSettings(window.currentDriverId, {
                    ghostSequenceAggressiveness: 'EQUILIBRADO',
                    ghostSequenceTrafficWeight: 0.5
                });
            }
            window.closeGhostSequenceModal();
            if (window.speakText) window.speakText("Modo combate desativado. Sistemas retornando à normalidade.");
        }
    };
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)
print("God Mode Toggle Injected")
