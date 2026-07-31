import sys

with open('index.html', 'r') as f:
    content = f.read()

# Update UI to save to SecureStorage
replacements = {
    '<input type="checkbox" id="settingsGesturesEnabled" checked>': '<input type="checkbox" id="settingsGesturesEnabled" onchange="SecureStorage.setItem(\'gestures_enabled\', this.checked)" checked>',
    '<select id="settingsGestureSwipeDown"': '<select id="settingsGestureSwipeDown" onchange="SecureStorage.setItem(\'gesture_down\', this.value)"',
    '<select id="settingsGestureSwipeUp"': '<select id="settingsGestureSwipeUp" onchange="SecureStorage.setItem(\'gesture_up\', this.value)"',
    '<select id="settingsGestureSwipeDiagonal"': '<select id="settingsGestureSwipeDiagonal" onchange="SecureStorage.setItem(\'gesture_diagonal\', this.value)"'
}

for k, v in replacements.items():
    content = content.replace(k, v)
    
# Add loading logic to populate settings
init_code = """
    // --- LOAD GESTURE SETTINGS ---
    setTimeout(() => {
        const gesturesEnabled = SecureStorage.getItem('gestures_enabled');
        if (gesturesEnabled !== null) {
            const cb = document.getElementById('settingsGesturesEnabled');
            if (cb) cb.checked = gesturesEnabled === 'true';
        }
        const gDown = SecureStorage.getItem('gesture_down');
        if (gDown) {
            const el = document.getElementById('settingsGestureSwipeDown');
            if (el) el.value = gDown;
        }
        const gUp = SecureStorage.getItem('gesture_up');
        if (gUp) {
            const el = document.getElementById('settingsGestureSwipeUp');
            if (el) el.value = gUp;
        }
        const gDiag = SecureStorage.getItem('gesture_diagonal');
        if (gDiag) {
            const el = document.getElementById('settingsGestureSwipeDiagonal');
            if (el) el.value = gDiag;
        }
    }, 1000);
"""
content = content.replace('    window.executeGestureAction = function(action) {', init_code + '\n    window.executeGestureAction = function(action) {')

with open('index.html', 'w') as f:
    f.write(content)

print("Gesture save logic patched")
