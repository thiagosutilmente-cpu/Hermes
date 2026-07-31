      if (cleanText.includes("multiplexador") || cleanText.includes("vários celulares") || cleanText.includes("varios celulares") || cleanText.includes("ler no fundo") || cleanText.includes("segundo plano") || cleanText.includes("virtualizar") || cleanText.includes("dispositivos virtuais") || cleanText.includes("escutar todos")) {
          speakText("O motor Multiplexador Virtual está ativo. Ele cria ambientes contêiner independentes no Android para cada aplicativo, como se fossem celulares virtuais. Assim, eu intercepto as ofertas do iFood, Rappi e Uber em segundo plano, simultaneamente, sem que eles saibam.");
          if (window.showToast) window.showToast("Multiplexador Headless Ativo (VM 1-4)", "success");
          return;
      }
