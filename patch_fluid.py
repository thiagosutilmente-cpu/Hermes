import re

with open('index.html', 'r') as f:
    content = f.read()

# 1. Add CSS
css = """
    <style>
      @keyframes borderPulseDamping {
          0% { box-shadow: 0 0 0 0 rgba(0, 245, 212, 0.4); border-color: rgba(0, 245, 212, 0.5); }
          70% { box-shadow: 0 0 0 8px rgba(0, 245, 212, 0); border-color: rgba(0, 245, 212, 0.8); }
          100% { box-shadow: 0 0 0 0 rgba(0, 245, 212, 0); border-color: rgba(0, 245, 212, 0.25); }
      }
      .damping-pulse-active {
          animation: borderPulseDamping 1.5s infinite !important;
      }
"""
content = content.replace("<style>", css)

# 2. Update Slider HTML
slider_old = """<input type="range" id="smartDampingSlider" min="1" max="100" value="70" oninput="window.updateDampingFactor(this.value)" style="width: 100%; accent-color: var(--accent-green); height: 4px; margin: 0;">"""
slider_new = """<input type="range" id="smartDampingSlider" min="1" max="100" value="70" oninput="window.updateDampingFactor(this.value)" onmouseenter="document.getElementById('smartDampingIndicator').classList.add('damping-pulse-active')" onmouseleave="document.getElementById('smartDampingIndicator').classList.remove('damping-pulse-active')" ontouchstart="document.getElementById('smartDampingIndicator').classList.add('damping-pulse-active')" ontouchend="document.getElementById('smartDampingIndicator').classList.remove('damping-pulse-active')" style="width: 100%; accent-color: var(--accent-green); height: 4px; margin: 0;">"""
content = content.replace(slider_old, slider_new)

# 3. Update updateDampingFactor JS
js_old = """    window.updateDampingFactor = function(val) {
        window.manualDampingFactor = parseFloat(val) / 100.0;
        const display = document.getElementById('smartDampingValueDisplay');
        const latency = document.getElementById('smartDampingLatencyDisplay');
        const icon = document.getElementById('smartDampingIconDisplay');"""

js_new = """    window.targetDampingFactor = 0.70;
    window.currentDampingFactor = 0.70;
    
    window.updateDampingFactor = function(val) {
        window.targetDampingFactor = parseFloat(val) / 100.0;
        window.manualDampingFactor = window.targetDampingFactor;
        const display = document.getElementById('smartDampingValueDisplay');
        const latency = document.getElementById('smartDampingLatencyDisplay');
        const icon = document.getElementById('smartDampingIconDisplay');"""
content = content.replace(js_old, js_new)

# 4. Update GPS Smoothing Logic
gps_old = """        if (window.manualDampingFactor !== null && window.manualDampingFactor !== undefined) {
            alpha = window.manualDampingFactor;
        }"""
gps_new = """        if (window.manualDampingFactor !== null && window.manualDampingFactor !== undefined) {
            // Fluid interpolation of the damping factor itself
            window.currentDampingFactor = window.currentDampingFactor + (window.targetDampingFactor - window.currentDampingFactor) * 0.15;
            alpha = window.currentDampingFactor;
        }"""
content = content.replace(gps_old, gps_new)

with open('index.html', 'w') as f:
    f.write(content)
