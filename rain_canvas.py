import sys

with open('index.html', 'r') as f:
    content = f.read()

rain_html = """
  <!-- RAIN OVERLAY -->
  <canvas id="rainCanvas" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; pointer-events: none; z-index: 99990; display: none;"></canvas>
"""

content = content.replace('  <!-- Toasts Flutuantes -->', rain_html + '\n  <!-- Toasts Flutuantes -->')

js_code = """
    window.rainAnimationId = null;
    window.startRainAnimation = function() {
        const canvas = document.getElementById('rainCanvas');
        if (!canvas) return;
        canvas.style.display = 'block';
        const ctx = canvas.getContext('2d');
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        
        const drops = [];
        for(let i=0; i<100; i++) {
            drops.push({
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                len: Math.random() * 20 + 10,
                speed: Math.random() * 10 + 15
            });
        }
        
        function draw() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.strokeStyle = 'rgba(173, 216, 230, 0.5)';
            ctx.lineWidth = 1;
            ctx.lineCap = 'round';
            ctx.beginPath();
            for(let i=0; i<drops.length; i++) {
                const d = drops[i];
                ctx.moveTo(d.x, d.y);
                ctx.lineTo(d.x - d.len/4, d.y + d.len);
                d.y += d.speed;
                d.x -= d.speed/4;
                if(d.y > canvas.height) {
                    d.y = -20;
                    d.x = Math.random() * canvas.width + 50;
                }
            }
            ctx.stroke();
            window.rainAnimationId = requestAnimationFrame(draw);
        }
        draw();
    };
    window.stopRainAnimation = function() {
        const canvas = document.getElementById('rainCanvas');
        if (canvas) canvas.style.display = 'none';
        if (window.rainAnimationId) {
            cancelAnimationFrame(window.rainAnimationId);
            window.rainAnimationId = null;
        }
    };
"""

content = content.replace('    window.rainModeActive = false;', js_code + '\n    window.rainModeActive = false;')
content = content.replace('if (window.rainModeActive) {', 'if (window.rainModeActive) { window.startRainAnimation();')
content = content.replace('btn.style.background = \'rgba(58, 134, 255, 0.1)\';', 'window.stopRainAnimation(); btn.style.background = \'rgba(58, 134, 255, 0.1)\';')

with open('index.html', 'w') as f:
    f.write(content)
print("Rain Animation Injected")
