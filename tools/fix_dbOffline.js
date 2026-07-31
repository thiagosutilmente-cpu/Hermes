const fs = require('fs');
let lines = fs.readFileSync('index.html', 'utf-8').split('\n');

for(let i=0; i<lines.length; i++) {
    // If we are before line 13526 or after 28567, replace dbOffline with window.dbOffline
    // Careful: window.window.dbOffline is bad, so only replace if not already preceded by window.
    if (i < 13526 || i > 28567) {
        lines[i] = lines[i].replace(/(?<!window\.)dbOffline/g, 'window.dbOffline');
    }
}

fs.writeFileSync('index.html', lines.join('\n'));
