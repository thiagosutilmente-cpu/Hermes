const fs = require('fs');
const babelCore = require('@babel/core');
const html = fs.readFileSync('index.html', 'utf8');
const dom = require('jsdom').JSDOM;
const document = new (dom(html)).window.document;
const s = document.querySelectorAll('script')[22].textContent;

// Let's replace ALL backticks with a placeholder, then put them back one by one until Babel fails at 1603:25!
// Actually, just find the backtick in string/comment.
