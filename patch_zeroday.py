import sys

with open('index.html', 'r') as f:
    content = f.read()

zero_day_html = """
    <!-- ZERO-DAY KERNEL INJECTION OVERLAY -->
    <div id="zeroDayOverlay" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.95); z-index: 999999; font-family: 'Courier New', Courier, monospace; color: #00ff00; padding: 20px; flex-direction: column; overflow: hidden;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #00ff00; padding-bottom: 10px; margin-bottom: 15px;">
            <div style="font-size: 18px; font-weight: bold; color: #ff0055;">⚠️ UNAUTHORIZED KERNEL ACCESS ⚠️</div>
            <button onclick="document.getElementById('zeroDayOverlay').style.display = 'none'" style="background: none; border: 1px solid #00ff00; color: #00ff00; padding: 5px 10px; cursor: pointer;">ABORT</button>
        </div>
        
        <div id="zeroDayTerminal" style="flex: 1; overflow-y: hidden; display: flex; flex-direction: column; justify-content: flex-end; font-size: 12px; line-height: 1.5; text-shadow: 0 0 5px #00ff00;">
            <!-- Terminal Output -->
        </div>
    </div>
"""

idx = content.find('</body>')
if idx != -1:
    content = content[:idx] + zero_day_html + '\n' + content[idx:]
else:
    print("Could not find body end")

# Inject button in settings menu
btn_html = """
        <div class="menu-item" onclick="window.triggerZeroDay()" style="border-top: 1px solid #333; margin-top: 10px; padding-top: 10px;">
          <div class="icon" style="background: rgba(255,0,85,0.1); color: #ff0055;"><span class="material-symbols-rounded">terminal</span></div>
          <div class="text">
            <span>Root Exploit (0-Day)</span>
            <small style="color: #ff0055;">Override algoritmos de bloqueio</small>
          </div>
        </div>
"""

idx2 = content.find('<div class="menu-item" onclick="toggleSettingsMenu(); showGodModeModal();">')
if idx2 != -1:
    content = content[:idx2] + btn_html + '\n' + content[idx2:]
else:
    print("Could not find god mode button")

js_code = """
    window.triggerZeroDay = function() {
        if(typeof toggleSettingsMenu === 'function') toggleSettingsMenu();
        const overlay = document.getElementById('zeroDayOverlay');
        const term = document.getElementById('zeroDayTerminal');
        
        overlay.style.display = 'flex';
        term.innerHTML = '';
        
        const lines = [
            "INITIALIZING KERNEL HOOK...",
            "BYPASSING SECURE BOOT (SSL PINNING)... OK",
            "INJECTING PAYLOAD INTO COM.UBER.DRIVER... OK",
            "INJECTING PAYLOAD INTO BR.COM.BRAINWEB.IFOOD... OK",
            "MEMORY ADDRESS 0x7FFA8342 ACQUIRED.",
            "EXECUTING SURGE MULTIPLIER OVERRIDE...",
            "CURRENT MULTIPLIER: 1.1x -> FORCING TO 4.5x...",
            "WARNING: SYSTEM INTEGRITY COMPROMISED",
            "SPOOFING GPS COORDINATES TO HIGH DEMAND ZONE...",
            "WIPING BAN HISTORY RECORDS...",
            "DELETE FROM banned_drivers WHERE id = 'CURRENT_USER';",
            "1 ROW(S) AFFECTED.",
            "ROOT ACCESS GRANTED.",
            "GHOST PROTOCOL ACTIVATED. ENJOY YOUR EARNINGS."
        ];
        
        let i = 0;
        const interval = setInterval(() => {
            if (i >= lines.length) {
                clearInterval(interval);
                term.innerHTML += `<div style="color: #ff0055; margin-top: 20px; font-weight: bold; text-align: center; font-size: 20px; animation: blink 1s infinite;">HACK COMPLETED</div>`;
                setTimeout(() => {
                    overlay.style.display = 'none';
                    if(window.showToast) window.showToast("ROOT EXPLOIT ATIVADO. Algoritmos dominados.", "success");
                }, 3000);
                return;
            }
            term.innerHTML += `<div>> ${lines[i]}</div>`;
            term.scrollTop = term.scrollHeight;
            i++;
        }, 500);
    };
"""

content = content.replace('    window.startPreCogSimulation = function() {', js_code + '\n    window.startPreCogSimulation = function() {')

with open('index.html', 'w') as f:
    f.write(content)

print("Zero-Day Exploit injected!")
