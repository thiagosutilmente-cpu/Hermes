    window.fetchTrendDiagnostic = async function() {
        const btn = document.getElementById('btnTrendDiagnostic');
        const output = document.getElementById('trendDiagnosticOutput');
        
        if (!btn || !output) return;
        
        btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px; animation: spin 2s linear infinite;">sync</span> Analisando...';
        btn.disabled = true;
        output.innerHTML = '<div style="display: flex; align-items: center; gap: 8px; color: #A200FF;"><span class="material-symbols-rounded" style="animation: spin 2s linear infinite;">hourglass_empty</span> Sincronizando dados locais de trânsito com a IA Gemini...</div>';
        
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
            
            const prompt = `Faça um diagnóstico curto (máximo 4 linhas) de tendência do trânsito na minha região agora (Lat: ${lat}, Lng: ${lng}). Considere o horário atual e padrões típicos (pico, almoço, madrugada). Explique de forma técnica, mas em linguagem natural, por que o trânsito está aumentando ou diminuindo, e como isso afeta a rentabilidade das entregas neste momento.`;
            
            let fetchResult = null;
            if (window.authenticatedFetch) {
                 const res = await window.authenticatedFetch('/jarvis_chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        message: prompt,
                        settings: (typeof currentDriverSettings !== 'undefined' ? currentDriverSettings : {})
                    })
                });
                
                if (res.ok) {
                    fetchResult = await res.json();
                }
            }
            
            if (fetchResult && fetchResult.reply) {
                output.innerHTML = fetchResult.reply.replace(/\n/g, '<br>');
                if (typeof window.speakText === 'function') {
                    window.speakText("Diagnóstico de tendência concluído.");
                }
            } else {
                output.innerHTML = "Não foi possível conectar ao motor Gemini IA neste momento. Tente novamente em alguns instantes.";
            }
        } catch(e) {
            console.error("Erro Trend Diagnostic:", e);
            output.innerHTML = "Falha de rede ao consultar o diagnóstico. Verifique sua conexão com o uplink.";
        } finally {
            btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px;">psychology</span> Analisar Trânsito';
            btn.disabled = false;
        }
    };
