import re

with open('index.html', 'r') as f:
    content = f.read()

target = """                      <button onclick="window.openTtsSettingsModal()" style="background: rgba(58, 134, 255, 0.15); border: 1px solid rgba(58, 134, 255, 0.3); border-radius: 4px; padding: 4px; color: var(--accent-blue); cursor: pointer; display: flex; align-items: center; justify-content: center; margin-left: 4px;" title="Configurar Voz TTS">
                        <span class="material-symbols-rounded" style="font-size: 14px;">record_voice_over</span>
                      </button>"""

replacement = """                      <button onclick="window.saveGeofenceZoneToFirestore()" style="background: rgba(0, 245, 212, 0.15); border: 1px solid rgba(0, 245, 212, 0.3); border-radius: 4px; padding: 4px; color: var(--success); cursor: pointer; display: flex; align-items: center; justify-content: center; margin-left: 4px;" title="Salvar Zona no Firestore">
                        <span class="material-symbols-rounded" style="font-size: 14px;">save</span>
                      </button>
                      <button onclick="window.openTtsSettingsModal()" style="background: rgba(58, 134, 255, 0.15); border: 1px solid rgba(58, 134, 255, 0.3); border-radius: 4px; padding: 4px; color: var(--accent-blue); cursor: pointer; display: flex; align-items: center; justify-content: center; margin-left: 4px;" title="Configurar Voz TTS">
                        <span class="material-symbols-rounded" style="font-size: 14px;">record_voice_over</span>
                      </button>"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed")
else:
    print("Not found")
