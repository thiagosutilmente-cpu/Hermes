const fs = require('fs');
const content = fs.readFileSync('index.html', 'utf-8');
const match = content.match(/dbOffline\./g);
console.log(match ? match.length : 0);
