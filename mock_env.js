const fs = require('fs');
const code = fs.readFileSync('index.html', 'utf8');
const scriptMatch = code.match(/<script(?![^>]*src=)([^>]*)>([\s\S]*?)<\/script>/i);

const mockWindow = {
  location: { hash: '' },
  addEventListener: (event, cb) => {
    if (event === 'load') cb();
  },
  setInterval: () => {},
  setTimeout: () => {},
  SpeechSynthesisUtterance: function() {},
  speechSynthesis: { speak: () => {}, cancel: () => {} },
  localStorage: { getItem: () => null, setItem: () => {} },
  navigator: { userAgent: '' }
};

const mockDocument = {
  getElementById: () => ({ style: {}, classList: { add: ()=>{}, remove: ()=>{}, toggle: ()=>{} }, appendChild: ()=>{} }),
  querySelector: () => ({ style: {}, classList: { add: ()=>{}, remove: ()=>{}, toggle: ()=>{} } }),
  querySelectorAll: () => [],
  createElement: () => ({ style: {}, classList: { add: ()=>{}, remove: ()=>{}, toggle: ()=>{} } }),
  body: { appendChild: () => {} }
};

const fn = new Function('window', 'document', 'localStorage', 'navigator', 'setTimeout', 'setInterval', scriptMatch[2]);
try {
  fn(mockWindow, mockDocument, mockWindow.localStorage, mockWindow.navigator, mockWindow.setTimeout, mockWindow.setInterval);
  console.log("Mock execution OK");
} catch (e) {
  console.error("Runtime error in mock environment: ", e);
}
