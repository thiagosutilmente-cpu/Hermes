const fs = require('fs');
const html = fs.readFileSync('index.html', 'utf8');

const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
let match;
let count = 0;

while ((match = scriptRegex.exec(html)) !== null) {
  count++;
  if (count === 1 || count === 2 || count === 3) {
    fs.writeFileSync(`script_${count}.js`, match[1]);
  }
}
