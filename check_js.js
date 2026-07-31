const fs = require('fs');
const html = fs.readFileSync('index.html', 'utf8');

const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
let match;
let count = 0;

while ((match = scriptRegex.exec(html)) !== null) {
  const code = match[1];
  count++;
  try {
    // If it's a module, use import, but here we can just use new Function to parse
    if (match[0].includes('type="text/babel"') || match[0].includes('type="importmap"')) {
       console.log("Script " + count + " skipped (Babel/JSON).");
       continue;
    }
    new Function(code);
  } catch (e) {
    console.error("Syntax error in script block " + count + ":", e);
  }
}
console.log("Done checking scripts.");
