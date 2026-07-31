import re

with open('index.html', 'r') as f:
    content = f.read()

target = """    window.saveGeofenceZoneToFirestore = async function() {"""

replacement = """    window.renderGeofenceZonesList = function() {
      const listDiv = document.getElementById('geofenceZonesList');
      if (!listDiv) return;
      const zones = window.currentDriverSettings?.geofenceZones || [];
      if (zones.length === 0) {
          listDiv.innerHTML = '<span style="font-size: 9px; color: var(--text-dim); text-align: center;">Nenhuma zona salva.</span>';
          return;
      }
      listDiv.innerHTML = '';
      zones.forEach((zone, index) => {
          const div = document.createElement('div');
          div.style.display = 'flex';
          div.style.alignItems = 'center';
          div.style.justifyContent = 'space-between';
          div.style.background = 'rgba(255, 255, 255, 0.05)';
          div.style.padding = '4px 6px';
          div.style.borderRadius = '4px';
          div.style.border = zone.active ? '1px solid var(--success)' : '1px solid rgba(255, 255, 255, 0.1)';

          const nameSpan = document.createElement('span');
          nameSpan.style.fontSize = '9px';
          nameSpan.style.color = 'white';
          nameSpan.innerText = zone.name + " (" + (zone.radiusMeters/1000).toFixed(1) + "km)";

          const btnDiv = document.createElement('div');
          btnDiv.style.display = 'flex';
          btnDiv.style.gap = '4px';

          const toggleBtn = document.createElement('button');
          toggleBtn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 12px;">' + (zone.active ? 'visibility' : 'visibility_off') + '</span>';
          toggleBtn.style.background = 'transparent';
          toggleBtn.style.border = 'none';
          toggleBtn.style.color = zone.active ? 'var(--success)' : 'var(--text-dim)';
          toggleBtn.style.cursor = 'pointer';
          toggleBtn.onclick = async () => {
             zone.active = !zone.active;
             await window.saveDriverSettings(window.currentDriverId, { geofenceZones: zones });
             window.renderGeofenceZonesList();
          };

          const delBtn = document.createElement('button');
          delBtn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 12px;">delete</span>';
          delBtn.style.background = 'transparent';
          delBtn.style.border = 'none';
          delBtn.style.color = 'var(--accent-pink)';
          delBtn.style.cursor = 'pointer';
          delBtn.onclick = async () => {
             const newZones = zones.filter(z => z.id !== zone.id);
             window.currentDriverSettings.geofenceZones = newZones;
             await window.saveDriverSettings(window.currentDriverId, { geofenceZones: newZones });
             window.renderGeofenceZonesList();
          };

          btnDiv.appendChild(toggleBtn);
          btnDiv.appendChild(delBtn);
          div.appendChild(nameSpan);
          div.appendChild(btnDiv);
          listDiv.appendChild(div);
      });
    };

    window.saveGeofenceZoneToFirestore = async function() {"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed logic")
else:
    print("Not found logic")

