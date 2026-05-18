// Tiny script: wraps marked-rendered HTML in a print-ready template.
// Run: node build-html.js
const fs   = require('fs');
const path = require('path');
const { marked } = require('marked');

const STYLE = `
  @page { size: A4; margin: 18mm 16mm; }
  * { box-sizing: border-box; }
  body {
    font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
    color: #1a1a1a;
    line-height: 1.55;
    max-width: 780px;
    margin: 24px auto;
    padding: 0 18px;
    font-size: 11.5pt;
  }
  h1 { font-size: 22pt; margin: 0 0 6pt; border-bottom: 3px solid #0d6efd; padding-bottom: 6pt; color: #0d6efd; }
  h2 { font-size: 16pt; margin: 28pt 0 8pt; color: #0d6efd; border-bottom: 1px solid #dee2e6; padding-bottom: 4pt; }
  h3 { font-size: 13pt; margin: 18pt 0 6pt; color: #212529; }
  h4 { font-size: 11.5pt; margin: 12pt 0 4pt; color: #495057; }
  p, ul, ol { margin: 6pt 0; }
  ul, ol { padding-left: 22pt; }
  li { margin: 2pt 0; }
  code {
    background: #f4f5f7;
    padding: 1pt 4pt;
    border-radius: 3pt;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 10pt;
    color: #c7254e;
  }
  pre {
    background: #f4f5f7;
    border: 1px solid #e9ecef;
    border-radius: 4pt;
    padding: 10pt 14pt;
    overflow-x: auto;
    font-size: 9.5pt;
    line-height: 1.45;
    page-break-inside: avoid;
  }
  pre code { background: transparent; padding: 0; color: #1a1a1a; }
  table { border-collapse: collapse; margin: 8pt 0; width: 100%; font-size: 10pt; }
  th, td { border: 1px solid #dee2e6; padding: 5pt 8pt; text-align: left; vertical-align: top; }
  th { background: #f8f9fa; font-weight: 600; }
  tr:nth-child(even) td { background: #fcfcfd; }
  blockquote {
    border-left: 3px solid #0d6efd;
    background: #f0f4ff;
    margin: 8pt 0;
    padding: 6pt 12pt;
    color: #495057;
  }
  hr { border: none; border-top: 1px solid #dee2e6; margin: 18pt 0; }
  a { color: #0d6efd; text-decoration: none; }
  .meta-banner {
    background: linear-gradient(135deg, #0d6efd, #6610f2);
    color: white;
    padding: 10pt 16pt;
    border-radius: 5pt;
    margin-bottom: 18pt;
    font-size: 10pt;
  }
  @media print {
    body { margin: 0; max-width: 100%; }
    h2 { page-break-after: avoid; }
    pre, table { page-break-inside: avoid; }
  }
`;

function build(srcMd, outHtml, title) {
  const md   = fs.readFileSync(srcMd, 'utf8');
  const body = marked.parse(md);
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>${title}</title>
<style>${STYLE}</style>
</head>
<body>
<div class="meta-banner">
  <strong>TrackMyFunds</strong> — Personal Finance Tracker · Generated ${new Date().toISOString().slice(0,10)}
</div>
${body}
</body>
</html>`;
  fs.writeFileSync(outHtml, html);
  console.log('Wrote', outHtml);
}

build(path.join(__dirname, 'PRD.md'),
      path.join(__dirname, 'PRD.html'),
      'Project Requirement Document — TrackMyFunds');

build(path.join(__dirname, 'TECHNICAL_DESIGN.md'),
      path.join(__dirname, 'TECHNICAL_DESIGN.html'),
      'Technical & Functional Design — TrackMyFunds');
