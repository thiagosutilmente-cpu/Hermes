import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

target = """      const elFocus = document.getElementById('settingFocusAuto');
      if (elFocus && c.focusModeAuto !== undefined) elFocus.checked = c.focusModeAuto;
    }"""

replacement = """      const elFocus = document.getElementById('settingFocusAuto');
      if (elFocus && c.focusModeAuto !== undefined) elFocus.checked = c.focusModeAuto;
      
      if (c.platformMinGain) {
        const elIfood = document.getElementById('autoMinGain_ifood');
        const elRappi = document.getElementById('autoMinGain_rappi');
        const elUber = document.getElementById('autoMinGain_uber');
        const el99 = document.getElementById('autoMinGain_99');
        if (elIfood) elIfood.value = c.platformMinGain.ifood;
        if (elRappi) elRappi.value = c.platformMinGain.rappi;
        if (elUber) elUber.value = c.platformMinGain.uber;
        if (el99) el99.value = c.platformMinGain['99'];
      }
    }
    
    function updateAutoAcceptPlatformGain() {
      if (!window.AppState) return;
      const valIfood = parseFloat(document.getElementById('autoMinGain_ifood')?.value || 5.0);
      const valRappi = parseFloat(document.getElementById('autoMinGain_rappi')?.value || 5.5);
      const valUber = parseFloat(document.getElementById('autoMinGain_uber')?.value || 4.5);
      const val99 = parseFloat(document.getElementById('autoMinGain_99')?.value || 4.0);
      
      if (!window.AppState.config.platformMinGain) {
        window.AppState.config.platformMinGain = {};
      }
      
      window.AppState.config.platformMinGain.ifood = valIfood;
      window.AppState.config.platformMinGain.rappi = valRappi;
      window.AppState.config.platformMinGain.uber = valUber;
      window.AppState.config.platformMinGain['99'] = val99;
      
      saveAppState();
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
