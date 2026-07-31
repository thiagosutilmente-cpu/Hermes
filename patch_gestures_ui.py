import sys

with open('index.html', 'r') as f:
    content = f.read()

gestures_html = """
          <!-- GESTOS DE TELA -->
          <div class="form-group" style="background: rgba(255, 183, 3, 0.05); border: 1px solid rgba(255, 183, 3, 0.2); padding: 16px; border-radius: 12px; margin-bottom: 16px; box-shadow: 0 4px 20px rgba(255, 183, 3, 0.1);">
            <div style="font-weight: bold; color: #FFB703; display: flex; align-items: center; gap: 6px; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; border-bottom: 1px solid rgba(255, 183, 3, 0.1); padding-bottom: 8px;">
              <span class="material-symbols-rounded" style="font-size: 20px;">swipe</span> Gestos de Tela (2 Dedos)
            </div>
            
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; gap: 12px;">
              <div style="flex: 1;">
                <span style="font-weight: bold; color: var(--text-main); font-size: 12px; display: flex; align-items: center; gap: 4px;">
                  <span class="material-symbols-rounded" style="font-size: 16px; color: var(--success);">touch_app</span> Ativar Gestos de Tela
                </span>
                <span style="font-size: 10.5px; color: var(--text-muted); display: block; margin-top: 2px; line-height: 1.4;">Permite acionar comandos críticos deslizando com 2 dedos em qualquer lugar da tela, sem precisar de botões ou voz.</span>
              </div>
              <label class="hud-switch" style="margin: 0; transform: scale(0.9);">
                <input type="checkbox" id="settingsGesturesEnabled" checked>
                <span class="hud-slider"></span>
              </label>
            </div>

            <div style="margin-bottom: 10px;">
              <label style="font-size: 11px; margin-bottom: 4px; display: block; color: var(--text-muted);">2 Dedos: Deslizar para Baixo</label>
              <select id="settingsGestureSwipeDown" style="width: 100%; background: var(--bg-dark); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 6px; padding: 6px; font-size: 11px; outline: none;">
                <option value="stealth">Modo Furtivo (Stealth)</option>
                <option value="sos">SOS (Emergência)</option>
                <option value="accept">Aceitar Rota / Oferta Ativa</option>
                <option value="none">Nenhuma Ação</option>
              </select>
            </div>

            <div style="margin-bottom: 10px;">
              <label style="font-size: 11px; margin-bottom: 4px; display: block; color: var(--text-muted);">2 Dedos: Deslizar para Cima</label>
              <select id="settingsGestureSwipeUp" style="width: 100%; background: var(--bg-dark); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 6px; padding: 6px; font-size: 11px; outline: none;">
                <option value="accept">Aceitar Rota / Oferta Ativa</option>
                <option value="stealth">Modo Furtivo (Stealth)</option>
                <option value="sos">SOS (Emergência)</option>
                <option value="none">Nenhuma Ação</option>
              </select>
            </div>

            <div style="margin-bottom: 4px;">
              <label style="font-size: 11px; margin-bottom: 4px; display: block; color: var(--text-muted);">2 Dedos: Deslizar na Diagonal</label>
              <select id="settingsGestureSwipeDiagonal" style="width: 100%; background: var(--bg-dark); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 6px; padding: 6px; font-size: 11px; outline: none;">
                <option value="sos">SOS (Emergência)</option>
                <option value="accept">Aceitar Rota / Oferta Ativa</option>
                <option value="stealth">Modo Furtivo (Stealth)</option>
                <option value="none">Nenhuma Ação</option>
              </select>
            </div>
          </div>
"""

content = content.replace('          <!-- CONFIGURAÇÃO DE GOOGLE MAPS API KEY -->', gestures_html + '\n          <!-- CONFIGURAÇÃO DE GOOGLE MAPS API KEY -->')

with open('index.html', 'w') as f:
    f.write(content)
print("Gestures UI injected")
