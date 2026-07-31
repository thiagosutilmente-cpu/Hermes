      if (cleanText.includes("bolsa de corrida") || cleanText.includes("bolsa de valores") || cleanText.includes("escanear mercado") || cleanText.includes("arbitragem") || cleanText.includes("qual app ta pagando mais") || cleanText.includes("melhor app agora")) {
          if (typeof window.fetchCrossAppArbitrage === 'function') {
              if (window.showToast) window.showToast("Iniciando varredura da Bolsa de Valores Logística...", "info");
              window.fetchCrossAppArbitrage();
          }
          return;
      }
