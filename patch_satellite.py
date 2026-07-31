import sys

with open('index.html', 'r') as f:
    content = f.read()

satellite_html = """
  <!-- SATELLITE UPLINK STATUS (GOD MODE TIER) -->
  <div style="background: #000; padding: 4px 10px; display: flex; justify-content: space-between; align-items: center; font-size: 8px; font-family: monospace; color: #666; overflow: hidden; position: relative;">
      <div style="position: absolute; top: 0; left: 0; bottom: 0; width: 2px; background: #00f0ff; animation: blink 1s infinite;"></div>
      <div style="display: flex; align-items: center; gap: 6px;">
          <span class="material-symbols-rounded" style="font-size: 10px; color: #00f0ff;">satellite_alt</span>
          <span>UPLINK LEO_SAT_09: <b id="satStatus" style="color: #00f0ff;">CONNECTED</b></span>
          <span style="color: #333;">|</span>
          <span>SIG: <b id="satSignal" style="color: #00ff00;">99%</b></span>
      </div>
      <div style="display: flex; align-items: center; gap: 6px;">
          <span id="satEnc" style="color: #ff00ff; animation: pulse 2s infinite;">AES-256 ENCRYPTED</span>
          <span style="color: #333;">|</span>
          <span>NODE: <b style="color: #fff;">OMNISCIENT-1</b></span>
      </div>
  </div>
"""

idx = content.find('  <header>')
if idx != -1:
    content = content[:idx] + satellite_html + '\n' + content[idx:]
else:
    print("Could not find <header>")

js_code = """
    setInterval(() => {
        const sig = document.getElementById('satSignal');
        if (sig) {
            const val = 95 + Math.floor(Math.random() * 5);
            sig.innerText = val + '%';
            sig.style.color = val > 97 ? '#00ff00' : '#ffb703';
        }
    }, 2000);
"""

content = content.replace('    // Start simulation automatically', js_code + '\n    // Start simulation automatically')

with open('index.html', 'w') as f:
    f.write(content)

print("Satellite injected!")
