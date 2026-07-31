const fs = require('fs');
const jsdom = require('jsdom');
const { JSDOM } = jsdom;
const babelCore = require('@babel/core');

const html = fs.readFileSync('index.html', 'utf8');
const dom = new JSDOM(html);
const scripts = dom.window.document.querySelectorAll('script');

let count = 0;
scripts.forEach((script) => {
  count++;
  if (script.type === 'importmap' || script.type === 'application/json' || script.src) {
    return;
  }
  
  const code = script.textContent;
  
  if (script.type === 'text/babel' || script.type === 'module') {
    try {
      babelCore.transformSync(code, { presets: ['@babel/preset-react'] });
    } catch (e) {
      console.error(`Babel compilation error in script ${count}:`, e.message);
    }
  } else {
    try {
      new Function(code);
    } catch (e) {
      console.error(`Syntax error in script ${count}:`, e.message);
    }
  }
});
console.log("Done checking scripts.");
