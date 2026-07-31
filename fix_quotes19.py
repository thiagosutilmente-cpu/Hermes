with open('index.html', 'r') as f:
    content = f.read()

replacements = [
    ('"warning`);', '\"warning\");'),
    ('<span class=`material-symbols-rounded"', '<span class=\"material-symbols-rounded\"'),
    ('? "4 minutos" : "6 minutos`;', '? "4 minutos" : "6 minutos";'),
    ('console.warn(`Geolocation watchPosition error:", error);', 'console.warn("Geolocation watchPosition error:", error);'),
    ('window.speakText("Modo passageiro expirado. Trava de velocidade reativada.`);', 'window.speakText("Modo passageiro expirado. Trava de velocidade reativada.");'),
    ('por hora. Por favor, diminua o ritmo por segurança!");', 'por hora. Por favor, diminua o ritmo por segurança!`);'),
    ("triggerHapticFeedback('success`);", "triggerHapticFeedback('success');"),
    ('sidebarBtn.style.display = "flex`;', 'sidebarBtn.style.display = "flex";'),
    ('WhatsApp: `${msg.text}". Quer responder?`;', 'WhatsApp: \\`${msg.text}\\`. Quer responder?`;'),
    ('WhatsApp: `${text}". Diga opção', 'WhatsApp: \\`${text}\\`. Diga opção'),
    ('console.log("[Auth] Piloto autenticado:`, user.email);', 'console.log("[Auth] Piloto autenticado:", user.email);'),
    ('textContent = `Conectado como Piloto";', 'textContent = "Conectado como Piloto";'),
    ('if (!confirm(`Deseja realmente reverter as configurações da Cerca Virtual e Voz do Jarvis para os padrões originais?")) {', 'if (!confirm("Deseja realmente reverter as configurações da Cerca Virtual e Voz do Jarvis para os padrões originais?")) {'),
    ("document.getElementById('geofenceMonitorToggle`);", "document.getElementById('geofenceMonitorToggle');"),
    ('line-height: 1.4;`>', 'line-height: 1.4;">'),
    ('<strong style=`color: var(--text-main);">', '<strong style="color: var(--text-main);">'),
    ('outline: none;`>', 'outline: none;">'),
    ('statusEl.innerText = `Perfil `${styleKey', 'statusEl.innerText = `Perfil \\`${styleKey'),
    ('" escolhido com sucesso!`;', '\\` escolhido com sucesso!`;'),
    ('statusEl.innerText = `Apresentação concluída. Escolha uma das opções acima.";', 'statusEl.innerText = "Apresentação concluída. Escolha uma das opções acima.";'),
    ('transition: width 0.5s ease;`></div>', 'transition: width 0.5s ease;"></div>'),
    ('console.error(`Erro ao processar médias de turnos:", errShifts);', 'console.error("Erro ao processar médias de turnos:", errShifts);'),
    ("document.getElementById('offlineHistoryList`);", "document.getElementById('offlineHistoryList');"),
    ('console.error(`Erro ao salvar quilometragem:", err);', 'console.error("Erro ao salvar quilometragem:", err);')
]

for old, new in replacements:
    content = content.replace(old, new)

with open('index.html', 'w') as f:
    f.write(content)
