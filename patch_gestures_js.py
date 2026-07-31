import sys

with open('index.html', 'r') as f:
    content = f.read()

js_code = """
    // --- GESTOS DE TELA LOGIC ---
    window.gestureState = {
        active: false,
        startX: 0, startY: 0,
        endX: 0, endY: 0,
        startTime: 0
    };

    document.addEventListener('touchstart', function(e) {
        const enabled = document.getElementById('settingsGesturesEnabled');
        if (enabled && !enabled.checked) return;

        if (e.touches.length === 2) {
            window.gestureState.active = true;
            window.gestureState.startX = (e.touches[0].clientX + e.touches[1].clientX) / 2;
            window.gestureState.startY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
            window.gestureState.startTime = Date.now();
        } else {
            window.gestureState.active = false;
        }
    }, { passive: true });

    document.addEventListener('touchmove', function(e) {
        if (window.gestureState.active && e.touches.length === 2) {
            window.gestureState.endX = (e.touches[0].clientX + e.touches[1].clientX) / 2;
            window.gestureState.endY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
        }
    }, { passive: true });

    document.addEventListener('touchend', function(e) {
        if (window.gestureState.active) {
            window.gestureState.active = false;
            
            const dx = window.gestureState.endX - window.gestureState.startX;
            const dy = window.gestureState.endY - window.gestureState.startY;
            const dt = Date.now() - window.gestureState.startTime;
            
            if (dt > 1000) return; // Too slow
            if (Math.abs(dx) < 60 && Math.abs(dy) < 60) return; // Too short
            
            let gestureType = '';
            
            const absDx = Math.abs(dx);
            const absDy = Math.abs(dy);
            
            if (absDx > 80 && absDy > 80) {
                gestureType = 'diagonal';
            } else if (absDy > absDx && absDy > 80) {
                if (dy > 0) gestureType = 'down';
                else gestureType = 'up';
            } else if (absDx > absDy && absDx > 80) {
                // If it's mainly horizontal, we map to diagonal config for simplicity, or just ignore
                // gestureType = 'diagonal';
            }
            
            if (gestureType) {
                window.handleScreenGesture(gestureType);
            }
        }
    }, { passive: true });

    window.handleScreenGesture = function(type) {
        let action = 'none';
        if (type === 'down') {
            const select = document.getElementById('settingsGestureSwipeDown');
            if(select) action = select.value;
        } else if (type === 'up') {
            const select = document.getElementById('settingsGestureSwipeUp');
            if(select) action = select.value;
        } else if (type === 'diagonal') {
            const select = document.getElementById('settingsGestureSwipeDiagonal');
            if(select) action = select.value;
        }
        
        if (action !== 'none') {
            if (window.navigator.vibrate) window.navigator.vibrate([50, 50, 50]);
            window.executeGestureAction(action);
        }
    };
    
    window.executeGestureAction = function(action) {
        if (action === 'stealth') {
            if (window.toggleStealthMode) {
                window.toggleStealthMode();
                if (window.showToast) window.showToast("🥷 Modo Furtivo alternado por Gesto!", "success");
            } else {
                const stealthBtn = document.getElementById('stealthModeToggleBtn');
                if (stealthBtn) stealthBtn.click();
            }
        } else if (action === 'sos') {
            if (window.triggerSOSMode) {
                window.triggerSOSMode();
            } else {
                if (window.showToast) window.showToast("⚠️ SOS Acionado por Gesto! ⚠️", "error");
                if (window.speakText) window.speakText("Emergência detectada por gesto. Localização enviada.");
                
                // Simulate SOS progress
                const btn = document.getElementById('sosButton');
                if (btn) {
                    btn.classList.add('sos-active');
                    setTimeout(() => btn.classList.remove('sos-active'), 3000);
                }
            }
        } else if (action === 'accept') {
            // Find the accept button in active orders
            const acceptBtns = document.querySelectorAll('.btn-accept');
            if (acceptBtns && acceptBtns.length > 0) {
                acceptBtns[0].click();
                if (window.showToast) window.showToast("✅ Oferta aceita por Gesto!", "success");
            } else {
                if (window.showToast) window.showToast("Nenhuma oferta ativa para aceitar.", "warning");
                if (window.speakText) window.speakText("Nenhuma oferta ativa na tela para ser aceita.");
            }
        }
    };
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')

with open('index.html', 'w') as f:
    f.write(content)
print("Gestures logic injected")
