const fs = require('fs');
const html = fs.readFileSync('index.html', 'utf8');
const babelCore = require('@babel/core');

const scriptRegex = /<script type="text\/babel">([\s\S]*?)<\/script>/gi;
let match;
let count = 0;
let errors = 0;

while ((match = scriptRegex.exec(html)) !== null) {
  count++;
  try {
    babelCore.transformSync(match[1], { presets: ['@babel/preset-react'] });
  } catch (e) {
    console.error(`Error compiling React script ${count}:`, e.message);
    errors++;
  }
}
if (errors === 0) console.log("All Babel scripts compiled successfully.");
