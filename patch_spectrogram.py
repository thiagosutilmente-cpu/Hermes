import re

with open('index.html', 'r', encoding='utf-8') as f:
    content = f.read()

css_target = """    .voice-pulse-dot {
      display: inline-block;
      animation: voiceDotPulse 1s infinite;
    }"""

css_replace = """    .voice-pulse-dot {
      display: inline-block;
      animation: voiceDotPulse 1s infinite;
    }

    @keyframes barPulse {
      0%, 100% { height: 4px; }
      50% { height: 16px; }
    }
    .voice-spectrogram {
      display: flex;
      align-items: center;
      gap: 3px;
      height: 20px;
    }
    .voice-spectrogram .bar {
      width: 4px;
      background-color: #00ff88;
      border-radius: 2px;
      animation: barPulse 0.5s infinite ease-in-out;
    }
    .voice-spectrogram .bar:nth-child(1) { animation-duration: 0.6s; animation-delay: 0.1s; }
    .voice-spectrogram .bar:nth-child(2) { animation-duration: 0.4s; animation-delay: 0.2s; }
    .voice-spectrogram .bar:nth-child(3) { animation-duration: 0.7s; animation-delay: 0.3s; }
    .voice-spectrogram .bar:nth-child(4) { animation-duration: 0.5s; animation-delay: 0.1s; }
    .voice-spectrogram .bar:nth-child(5) { animation-duration: 0.8s; animation-delay: 0.4s; }"""

content = content.replace(css_target, css_replace)

js_target = "banner.innerHTML = `<span style=\"color:#00ff88; font-size:16px;\" class=\"voice-pulse-dot\">●</span> <span>${text}</span>`;"

js_replace = """banner.innerHTML = `
        <div class="voice-spectrogram">
          <div class="bar"></div><div class="bar"></div><div class="bar"></div><div class="bar"></div><div class="bar"></div>
        </div>
        <span>${text}</span>
      `;"""

if js_target in content:
    content = content.replace(js_target, js_replace)
    with open('index.html', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
