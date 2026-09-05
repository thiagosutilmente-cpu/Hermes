const { spawn } = require('child_process');
const http = require('http');
const path = require('path');

function checkPort(port, callback) {
  const req = http.request({ host: '127.0.0.1', port, path: '/', method: 'GET', timeout: 1000 }, (res) => {
    callback(true);
  });
  req.on('error', () => callback(false));
  req.on('timeout', () => { req.destroy(); callback(false); });
  req.end();
}

function startPython() {
  console.log('[NODE SUPERVISOR] Starting python3 app.py...');
  const py = spawn('python3', [path.join(__dirname, 'app.py')], {
    stdio: 'inherit',
    cwd: __dirname
  });

  py.on('error', (err) => {
    console.error('[NODE SUPERVISOR] Failed to start python3:', err);
  });

  py.on('close', (code) => {
    console.log(`[NODE SUPERVISOR] Python exited with code ${code}, restarting in 2s...`);
    setTimeout(startPython, 2000);
  });
}

// Check if Python app is already responding on port 3000
checkPort(3000, (running) => {
  if (running) {
    console.log('[NODE SUPERVISOR] Python app is already active on port 3000.');
  } else {
    startPython();
  }
});

// Periodic heartbeat to keep supervisor alive and ensure port 3000 stays up
setInterval(() => {
  checkPort(3000, (running) => {
    if (!running) {
      console.log('[NODE SUPERVISOR] Port 3000 is down, triggering startPython...');
      startPython();
    }
  });
}, 5000);
