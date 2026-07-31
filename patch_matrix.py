import sys

with open('index.html', 'r') as f:
    content = f.read()

# Replace the inner matrix rendering to use window.matrixColor
old_code = """
        window.matrixInterval = setInterval(() => {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.1)';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            // Defina a cor original (verde). Em GOD MODE será mudada fora daqui
            if (!ctx.fillStyle || ctx.fillStyle === 'rgba(0, 0, 0, 0.1)') {
                ctx.fillStyle = '#0f0'; 
            }
            ctx.font = fontSize + 'px monospace';
"""

new_code = """
        window.matrixColor = window.matrixColor || '#0f0';
        window.matrixInterval = setInterval(() => {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.1)';
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = window.matrixColor; 
            ctx.font = fontSize + 'px monospace';
"""
content = content.replace(old_code, new_code)

old_god_mode = """
        if (window.openGhostSequenceModal) {
            window.openGhostSequenceModal();
            const canvas = document.getElementById('matrixCanvas');
            if (canvas) {
                const ctx = canvas.getContext('2d');
                ctx.fillStyle = '#FF0000'; // overwrite green to red, this gets overwritten by the interval but we can patch the interval
            }
        }
"""
new_god_mode = """
        if (window.openGhostSequenceModal) {
            window.matrixColor = '#FF0000';
            window.openGhostSequenceModal();
        }
"""
content = content.replace(old_god_mode, new_god_mode)

with open('index.html', 'w') as f:
    f.write(content)
print("Matrix color patched")
