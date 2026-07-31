import re

with open('index.html', 'r') as f:
    html = f.read()

replacement = """
    function safeGetItem(key) {
      try { return localStorage.getItem(key); } catch(e) { return null; }
    }
    window.AppState = {
      user: JSON.parse(safeGetItem('radar_user')) || { id: 'usr_1', name: 'Motorista Pro', email: 'motorista@radar.app', plan: 'pro', onboardingComplete: true },
      session: JSON.parse(safeGetItem('radar_session')) || { isLoggedIn: true, token: 'jwt_mock_token' },
      earnings: { today: 284.50, week: 1420.00, month: 4850.00, totalKm: 142.8, profit: 228.00 },
      stacks: { active: [], pending: [], history: [], autoAccept: false, minGainPerKm: 5.0 },
      health: { score: 94, gpsAccuracy: 4.2, latency: 12, temperature: 28 },
      config: JSON.parse(safeGetItem('radar_config')) || { voiceEnabled: true, focusModeAuto: true, theme: 'dark', aggressiveness: 'EQUILIBRADO', minGainPerKm: 5.0 }
    };
"""

pattern = r"window\.AppState = \{\s*user: JSON\.parse\(localStorage\.getItem\('radar_user'\)\).*?config: JSON\.parse\(localStorage\.getItem\('radar_config'\)\).*?\n\s*\};"

html = re.sub(pattern, replacement.strip(), html, flags=re.DOTALL)

with open('index.html', 'w') as f:
    f.write(html)
