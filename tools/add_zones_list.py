import re

with open('index.html', 'r') as f:
    content = f.read()

target = """                      </button>
                    </div>
                  </div>

                  <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">"""

replacement = """                      </button>
                    </div>
                  </div>

                  <!-- Lista de Zonas Salvas -->
                  <div id="geofenceZonesList" style="display: flex; flex-direction: column; gap: 4px; max-height: 80px; overflow-y: auto;">
                  </div>

                  <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed HTML")
else:
    # Try regex
    content = re.sub(
        r'</button>\s*</div>\s*</div>\s*<div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">',
        r'</button>\n                    </div>\n                  </div>\n\n                  <!-- Lista de Zonas Salvas -->\n                  <div id="geofenceZonesList" style="display: flex; flex-direction: column; gap: 4px; max-height: 80px; overflow-y: auto;">\n                  </div>\n\n                  <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">',
        content
    )
    with open('index.html', 'w') as f:
        f.write(content)
    print("Used regex")
