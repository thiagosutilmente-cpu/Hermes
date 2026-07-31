      if (cleanText.includes("diagnóstico de tendência") || cleanText.includes("diagnostico de tendencia") || cleanText.includes("analisar trânsito") || cleanText.includes("analisa o transito") || cleanText.includes("tendência do trânsito") || cleanText.includes("tendencia do transito")) {
          if (typeof window.fetchTrendDiagnostic === 'function') {
              if (window.showToast) window.showToast("Iniciando Diagnóstico de Tendência...", "info");
              window.fetchTrendDiagnostic();
          }
          return;
      }
