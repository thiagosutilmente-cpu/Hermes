import re

with open('index.html', 'r') as f:
    content = f.read()

target = """    window.updateVisualGeofenceRadius = function(val) {"""

replacement = """    window.saveGeofenceZoneToFirestore = async function() {
      if (!window.currentDriverId || !window.saveDriverSettings) {
        if (window.showToast) window.showToast("Erro: Piloto não autenticado.", "error");
        return;
      }
      if (!window.geofenceCenterLat || !window.geofenceCenterLng) {
         if (window.showToast) window.showToast("Posição GPS não definida.", "error");
         return;
      }
      const newZone = {
         id: "zone-" + Date.now(),
         name: "Nova Cerca IA " + new Date().toLocaleTimeString(),
         latitude: window.geofenceCenterLat,
         longitude: window.geofenceCenterLng,
         radiusMeters: (window.geofenceRadius || 3.0) * 1000,
         isDangerZone: false,
         customVoiceAlert: "Atenção. Você cruzou o limite da sua zona salva.",
         active: true
      };
      const currentZones = window.currentDriverSettings?.geofenceZones || [];
      const updatedZones = [...currentZones, newZone];
      const { success, error } = await window.saveDriverSettings(window.currentDriverId, { geofenceZones: updatedZones });
      if (success) {
         if (window.currentDriverSettings) window.currentDriverSettings.geofenceZones = updatedZones;
         if (window.showToast) window.showToast("Cerca Salva com Sucesso!", "success");
         if (window.speakText) window.speakText("Nova cerca virtual salva e ativada no sistema Jarvis.");
      } else {
         if (window.showToast) window.showToast("Erro ao salvar cerca.", "error");
      }
    };

    window.updateVisualGeofenceRadius = function(val) {"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed")
else:
    print("Not found")
