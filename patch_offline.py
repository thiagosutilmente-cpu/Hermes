import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

btn_target = """      <!-- Real Interactive Map Canvas (Leaflet / Dark Tiles) -->
      <div id="cockpitRealMapContainer"></div>"""

btn_replace = """      <!-- Real Interactive Map Canvas (Leaflet / Dark Tiles) -->
      <div style="position: absolute; top: 12px; left: 12px; z-index: 1000; display: flex; gap: 8px;">
        <button onclick="downloadOfflineMap()" id="btnOfflineMap" style="background: rgba(17,17,24,0.9); border: 1px solid var(--accent-cyan); color: var(--accent-cyan); padding: 8px 12px; border-radius: 8px; font-size: 11px; font-weight: bold; display: flex; align-items: center; gap: 6px; box-shadow: 0 4px 10px rgba(0,0,0,0.5); backdrop-filter: blur(8px); cursor: pointer; transition: all 0.3s ease;">
          <span id="offlineMapIcon">☁️</span> <span id="offlineMapText">Baixar Mapa Offline (SP)</span>
        </button>
      </div>
      <div id="cockpitRealMapContainer"></div>"""

content = content.replace(btn_target, btn_replace)

script_target = "    // Start GPS Tracking"

script_replace = """    // Offline Map Simulation
    function downloadOfflineMap() {
      const btnText = document.getElementById('offlineMapText');
      const btnIcon = document.getElementById('offlineMapIcon');
      const btn = document.getElementById('btnOfflineMap');
      
      if (window.AppState && window.AppState.config && window.AppState.config.offlineMapDownloaded) {
        speak('O mapa offline desta região já está baixado e atualizado.');
        return;
      }
      
      speak('Iniciando o download do mapa offline da região atual para navegação sem internet.');
      if (btnIcon) btnIcon.textContent = '🔄';
      if (btnText) btnText.textContent = 'Baixando... 0%';
      
      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        if (btnText) btnText.textContent = `Baixando... ${progress}%`;
        if (progress >= 100) {
          clearInterval(interval);
          if (btnIcon) btnIcon.textContent = '✅';
          if (btnText) btnText.textContent = 'Mapa Offline Pronto';
          if (btn) {
            btn.style.borderColor = 'var(--accent-success)';
            btn.style.color = 'var(--accent-success)';
          }
          speak('Download do mapa concluído. O rastreamento e rotas funcionarão mesmo sem conexão de dados.');
          
          if (window.AppState) {
            window.AppState.config.offlineMapDownloaded = true;
            saveAppState();
          }
        }
      }, 200);
    }
    
    // Start GPS Tracking"""

if script_target in content:
    content = content.replace(script_target, script_replace)
    
    # Add checkOfflineMapStatus inside loadSettingsToForm
    load_target = "const elFocus = document.getElementById('settingFocusAuto');"
    load_replace = """const elFocus = document.getElementById('settingFocusAuto');
      if (window.AppState && window.AppState.config && window.AppState.config.offlineMapDownloaded) {
        const btnText = document.getElementById('offlineMapText');
        const btnIcon = document.getElementById('offlineMapIcon');
        const btn = document.getElementById('btnOfflineMap');
        if (btnText && btnIcon && btn) {
          btnIcon.textContent = '✅';
          btnText.textContent = 'Mapa Offline Pronto';
          btn.style.borderColor = 'var(--accent-success)';
          btn.style.color = 'var(--accent-success)';
        }
      }"""
    content = content.replace(load_target, load_replace)
    
    with open('index.html', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
