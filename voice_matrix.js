      // PROTOCOLO MATRIX / MODO DEUS
      if (cleanText.includes("ativar protocolo matrix") || cleanText.includes("modo hacker") || cleanText.includes("modo deus") || cleanText.includes("hacker mode")) {
          if (typeof window.activateMatrixProtocol === 'function') {
              window.activateMatrixProtocol();
          }
          return;
      }
