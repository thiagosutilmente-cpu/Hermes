import sys

with open('index.html', 'r') as f:
    content = f.read()

radar_html = """
      <!-- Radar Hotspots Preditivo -->
      <div style="width: 100%; height: 180px; margin-top: 15px; background: radial-gradient(circle at center, rgba(10, 11, 16, 1) 0%, rgba(20, 22, 34, 1) 100%); border: 1px solid var(--accent-blue); border-radius: 12px; position: relative; overflow: hidden; display: flex; align-items: center; justify-content: center; box-shadow: inset 0 0 40px rgba(58, 134, 255, 0.1);">
        <!-- Grid -->
        <div style="position: absolute; inset: 0; background-image: linear-gradient(rgba(58, 134, 255, 0.2) 1px, transparent 1px), linear-gradient(90deg, rgba(58, 134, 255, 0.2) 1px, transparent 1px); background-size: 20px 20px; opacity: 0.5;"></div>
        <!-- Radar Circle Base -->
        <div style="width: 150px; height: 150px; border: 1px solid rgba(58, 134, 255, 0.4); border-radius: 50%; position: absolute; box-shadow: inset 0 0 20px rgba(58, 134, 255, 0.2);"></div>
        <div style="width: 100px; height: 100px; border: 1px solid rgba(58, 134, 255, 0.3); border-radius: 50%; position: absolute;"></div>
        <div style="width: 50px; height: 50px; border: 1px dashed rgba(58, 134, 255, 0.5); border-radius: 50%; position: absolute;"></div>
        <!-- Crosshairs -->
        <div style="width: 1px; height: 100%; background: rgba(58, 134, 255, 0.3); position: absolute;"></div>
        <div style="width: 100%; height: 1px; background: rgba(58, 134, 255, 0.3); position: absolute;"></div>
        <!-- Sweeper -->
        <div style="width: 75px; height: 75px; background: conic-gradient(from 0deg, rgba(58, 134, 255, 0.6), transparent 60deg); border-radius: 100% 0 0 0; position: absolute; top: 50%; left: 50%; transform-origin: bottom right; animation: spin 4s linear infinite; transform: translate(-100%, -100%);"></div>
        <!-- Blips (Hotspots) -->
        <div class="radar-blip" style="position: absolute; top: 30%; left: 60%; width: 6px; height: 6px; background: var(--success); border-radius: 50%; box-shadow: 0 0 10px var(--success); animation: pulse 2s infinite;"></div>
        <div class="radar-blip" style="position: absolute; top: 70%; left: 40%; width: 8px; height: 8px; background: var(--accent-pink); border-radius: 50%; box-shadow: 0 0 10px var(--accent-pink); animation: pulse 1.5s infinite;"></div>
        <div class="radar-blip" style="position: absolute; top: 40%; left: 30%; width: 5px; height: 5px; background: #FFD700; border-radius: 50%; box-shadow: 0 0 10px #FFD700; animation: pulse 3s infinite;"></div>
        
        <div style="position: absolute; bottom: 8px; right: 12px; font-size: 9px; color: var(--accent-blue); font-family: var(--font-mono); font-weight: bold; text-shadow: 0 0 5px rgba(58,134,255,0.8);">
            UPLINK: ACTIVE • SCANNING SECTORS
        </div>
      </div>
"""

content = content.replace('      <div id="parkingSuggestionsContainer"', radar_html + '\n      <div id="parkingSuggestionsContainer"')

with open('index.html', 'w') as f:
    f.write(content)
print("Radar visual injected!")
