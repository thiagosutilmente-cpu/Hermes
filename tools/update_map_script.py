import re

file_path = 'index.html'
with open(file_path, 'r') as f:
    content = f.read()

# 1. Remove the hardcoded script from <head>
content = re.sub(r'^\s*<script src="https://maps\.googleapis\.com/maps/api/js\?key=.*?"></script>\s*\n', '', content, flags=re.MULTILINE)

# 2. Update loadGoogleMapsApi
new_load_func = """
    window.loadGoogleMapsApi = function() {
      if (window.google && window.google.maps) {
        window.initGoogleMap();
        return;
      }
      
      const defaultKey = "AIzaSyAMiojqLQ858fFflYTbGQQeWvCGC_911IY";
      const customKey = localStorage.getItem("googleMapsApiKey");
      const keyToUse = customKey && customKey.trim() !== "" ? customKey.trim() : defaultKey;
      
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${keyToUse}&libraries=places,visualization&callback=initGoogleMap`;
      script.async = true;
      script.defer = true;
      script.onerror = () => {
        console.warn("Google Maps API failed to load. Displaying simulated visual route.");
        if (window.showToast) window.showToast("Falha ao carregar Google Maps API. Verifique sua chave nas configurações.", "error");
      };
      document.head.appendChild(script);
    }
    
    window.applyGoogleMapsKey = function() {
      const input = document.getElementById("settingsGoogleMapsApiKey");
      if (input) {
        localStorage.setItem("googleMapsApiKey", input.value.trim());
        if (window.showToast) window.showToast("Chave da API do Google Maps salva! Recarregando para aplicar...", "success");
        setTimeout(() => {
          window.location.reload();
        }, 1500);
      }
    }
"""

# Find the old loadGoogleMapsApi
old_load_func_pattern = r'window\.loadGoogleMapsApi = function\(\) \{.*?(?=window\.initGoogleMap = function\(\))'
content = re.sub(old_load_func_pattern, new_load_func, content, flags=re.DOTALL)

with open(file_path, 'w') as f:
    f.write(content)
