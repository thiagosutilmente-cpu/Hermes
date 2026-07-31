const { JSDOM } = require('jsdom');
const fs = require('fs');

const html = fs.readFileSync('index.html', 'utf8');

try {
  const dom = new JSDOM(html, {
    runScripts: "dangerously",
    resources: "usable"
  });
  const window = dom.window;
  const document = window.document;

  // Mock Leaflet L
  window.L = {
    map: () => ({
      setView: () => {},
      addLayer: () => {},
      fitBounds: () => {},
      removeLayer: () => {},
      invalidateSize: () => {}
    }),
    tileLayer: () => ({ addTo: () => {} }),
    divIcon: () => ({}),
    marker: () => ({ addTo: () => ({ bindPopup: () => {} }) }),
    polyline: () => ({ addTo: () => {}, getBounds: () => ({}) })
  };

  // Trigger load event
  window.dispatchEvent(new window.Event('load'));

  const btn = document.querySelector('.btn-accept');
  console.log("Button before click:", btn ? btn.textContent : "null");

  btn.click();

  console.log("Button after click:", btn ? btn.textContent : "null");
  const modal = document.getElementById('gpsNavigationModal');
  console.log("GPS Modal created:", !!modal);
  if (modal) {
    console.log("GPS Modal display:", modal.style.display);
  }
} catch (err) {
  console.error("EXPLICIT ERROR DURING CLICK:", err);
}
