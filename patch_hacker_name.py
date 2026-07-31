import sys

with open('index.html', 'r') as f:
    content = f.read()

js_code = """
    window.scrambleName = function(element) {
        if (element.dataset.scrambling === "true") return;
        element.dataset.scrambling = "true";
        
        const originalText = element.innerText;
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*';
        let iterations = 0;
        
        const interval = setInterval(() => {
            element.innerText = originalText.split('').map((char, index) => {
                if (index < iterations) return originalText[index];
                return chars[Math.floor(Math.random() * chars.length)];
            }).join('');
            
            if (iterations >= originalText.length) {
                clearInterval(interval);
                element.innerText = originalText;
                element.dataset.scrambling = "false";
            }
            iterations += 1/3;
        }, 30);
    };
"""

content = content.replace('    window.showCalendarModal = function() {', js_code + '\n    window.showCalendarModal = function() {')
content = content.replace('id="headerDriverName"', 'id="headerDriverName" onclick="window.scrambleName(this)" style="cursor: pointer;"')

with open('index.html', 'w') as f:
    f.write(content)
print("Hacker Name Easter Egg Injected")
