import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

maps_key_html = """
          <!-- CONFIGURAÇÃO DE GOOGLE MAPS API KEY -->
          <div class="form-group" style="margin-top: 20px; border-top: 1px solid var(--border-light); padding-top: 14px;">
            <label style="margin-bottom: 2px;">Chave da API do Google Maps</label>
            <div style="font-size: 11px; color: var(--text-muted); margin-bottom: 8px;">
              Necessário para trânsito real, rotas preditivas e heatmaps. Ative <b>Maps JavaScript API</b> e <b>Directions API</b>.
            </div>
            <div class="input-wrapper" style="position: relative;">
              <input type="password" id="settingsGoogleMapsApiKey" placeholder="AIzaSy..." style="font-family: monospace; width: 100%;" />
              <span class="prefix-icon material-symbols-rounded">key</span>
              <button type="button" onclick="const i = document.getElementById('settingsGoogleMapsApiKey'); i.type = i.type === 'password' ? 'text' : 'password';" style="position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: transparent; border: none; color: var(--text-muted); cursor: pointer;"><span class="material-symbols-rounded" style="font-size: 16px;">visibility</span></button>
            </div>
            <button type="button" onclick="window.applyGoogleMapsKey()" style="background: rgba(0, 245, 212, 0.15); border: 1px solid var(--success); color: var(--success); padding: 8px; border-radius: 6px; width: 100%; margin-top: 10px; font-weight: bold; cursor: pointer;">Aplicar e Recarregar Mapas</button>
          </div>
"""

content = content.replace('<button type="submit" class="btn-save"', maps_key_html + '\n          <button type="submit" class="btn-save"')

with open(file_path, 'w') as f:
    f.write(content)
