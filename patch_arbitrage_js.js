  <script>
    window.fetchCrossAppArbitrage = async function() {
        const btn = document.getElementById('btnCrossAppArbitrage');
        const output = document.getElementById('arbitrageInsightOutput');
        const ifoodVal = document.getElementById('ifoodIndexValue');
        const rappiVal = document.getElementById('rappiIndexValue');
        const lalaVal = document.getElementById('lalaIndexValue');
        const ifoodTr = document.getElementById('ifoodTrend');
        const rappiTr = document.getElementById('rappiTrend');
        const lalaTr = document.getElementById('lalaTrend');
        
        if (!btn || !output) return;
        
        btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px; animation: spin 2s linear infinite;">sync</span> Escaneando...';
        btn.disabled = true;
        output.innerHTML = '<div style="display: flex; align-items: center; justify-content: center; gap: 8px; color: #FFB703;"><span class="material-symbols-rounded" style="animation: spin 2s linear infinite;">hourglass_empty</span> Sincronizando com a Bolsa de Valores Logística...</div>';
        
        try {
            let lat = "-23.5505";
            let lng = "-46.6333";
            if (window.googleMapInstance) {
                const center = window.googleMapInstance.getCenter();
                if (center) {
                    lat = center.lat();
                    lng = center.lng();
                }
            }
            
            let fetchResult = null;
            if (window.authenticatedFetch) {
                 const res = await window.authenticatedFetch('/arbitrage_scan', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ lat: lat, lng: lng })
                });
                
                if (res.ok) {
                    fetchResult = await res.json();
                }
            }
            
            if (fetchResult && fetchResult.insight) {
                ifoodVal.innerText = fetchResult.ifood_value;
                ifoodTr.innerText = fetchResult.ifood_trend;
                ifoodTr.style.color = fetchResult.ifood_trend === "Alta" ? "#2BB673" : (fetchResult.ifood_trend === "Baixa" ? "#EA1D2C" : "var(--text-muted)");
                
                rappiVal.innerText = fetchResult.rappi_value;
                rappiTr.innerText = fetchResult.rappi_trend;
                rappiTr.style.color = fetchResult.rappi_trend === "Alta" ? "#2BB673" : (fetchResult.rappi_trend === "Baixa" ? "#EA1D2C" : "var(--text-muted)");
                
                lalaVal.innerText = fetchResult.lalamove_value;
                lalaTr.innerText = fetchResult.lalamove_trend;
                lalaTr.style.color = fetchResult.lalamove_trend === "Alta" ? "#2BB673" : (fetchResult.lalamove_trend === "Baixa" ? "#EA1D2C" : "var(--text-muted)");
                
                output.innerHTML = fetchResult.insight;
                if (typeof window.speakText === 'function') {
                    window.speakText(fetchResult.insight);
                }
            } else {
                output.innerHTML = "Não foi possível conectar à Bolsa Logística. Tente novamente.";
            }
        } catch(e) {
            console.error("Erro Arbitrage Scan:", e);
            output.innerHTML = "Falha de rede ao consultar o mercado. Verifique sua conexão com o uplink.";
        } finally {
            btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px;">radar</span> Escanear Mercado';
            btn.disabled = false;
        }
    };
  </script>
</body>
