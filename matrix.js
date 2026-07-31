    window.activateMatrixProtocol = function() {
        if (typeof window.speakText === 'function') {
            window.speakText("Protocolo Matrix ativado. Sobrescrevendo barreiras de segurança. Hackeando algoritmos de precificação e prioridade das plataformas. Acesso root concedido.");
        }
        
        const matrixDiv = document.createElement('div');
        matrixDiv.id = 'matrixProtocolOverlay';
        matrixDiv.style.position = 'fixed';
        matrixDiv.style.top = '0';
        matrixDiv.style.left = '0';
        matrixDiv.style.width = '100vw';
        matrixDiv.style.height = '100vh';
        matrixDiv.style.backgroundColor = 'black';
        matrixDiv.style.zIndex = '99999999';
        matrixDiv.style.overflow = 'hidden';
        matrixDiv.style.cursor = 'crosshair';
        
        matrixDiv.innerHTML = `
            <canvas id="matrixCanvas" style="position:absolute; top:0; left:0; width:100%; height:100%;"></canvas>
            <div style="position:absolute; top:50%; left:50%; transform:translate(-50%, -50%); color:#0F0; font-family:'Courier New', monospace; font-size:24px; font-weight:bold; text-align:center; text-shadow: 0 0 10px #0F0; background:rgba(0,0,0,0.8); padding:40px; border:2px solid #0F0; border-radius:10px; width:80%; max-width:600px;">
                <div style="font-size:36px; margin-bottom:20px; letter-spacing:4px;">GHOST S.Y.S. ROOT</div>
                <div id="matrixLog" style="font-size:16px; text-align:left; height:200px; overflow-y:hidden; line-height:1.5;"></div>
                <button onclick="window.deactivateMatrixProtocol()" style="margin-top:30px; background:transparent; color:#0F0; border:1px solid #0F0; padding:10px 20px; font-family:'Courier New'; cursor:pointer; font-size:18px; text-transform:uppercase; box-shadow:0 0 15px rgba(0,255,0,0.5) inset;">
                    Abortar Root Access
                </button>
            </div>
        `;
        document.body.appendChild(matrixDiv);
        
        // Matrix Rain Canvas
        const c = document.getElementById("matrixCanvas");
        const ctx = c.getContext("2d");
        c.width = window.innerWidth;
        c.height = window.innerHeight;
        const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789$+-*/=%\"'#&_(),.;:?!\\|{}<>[]^~";
        const chars = letters.split("");
        const fontSize = 16;
        const columns = c.width / fontSize;
        const drops = [];
        for (let x = 0; x < columns; x++) drops[x] = 1;
        
        window.matrixInterval = setInterval(() => {
            ctx.fillStyle = "rgba(0, 0, 0, 0.05)";
            ctx.fillRect(0, 0, c.width, c.height);
            ctx.fillStyle = "#0F0";
            ctx.font = fontSize + "px monospace";
            for (let i = 0; i < drops.length; i++) {
                const text = chars[Math.floor(Math.random() * chars.length)];
                ctx.fillText(text, i * fontSize, drops[i] * fontSize);
                if (drops[i] * fontSize > c.height && Math.random() > 0.975) drops[i] = 0;
                drops[i]++;
            }
        }, 33);
        
        // Fake Logs
        const logs = [
            "Iniciando bypass de firewall...",
            "Decrypting IFOOD_AUTH_TOKEN... [OK]",
            "Decrypting RAPPI_QUEUE_PRIORITY... [OK]",
            "Injetando Ghost Module v9.0...",
            "Overclocking GPS Location Spoofing...",
            "Forçando latência de aceite para 0.01ms...",
            "Interceptando pacotes de precificação...",
            "Multiplicador de ganho forçado para 3.5x...",
            "Acesso nível DEUS ativado."
        ];
        const logContainer = document.getElementById("matrixLog");
        let step = 0;
        
        window.matrixLogInterval = setInterval(() => {
            if(step < logs.length) {
                logContainer.innerHTML += `> ${logs[step]}<br>`;
                step++;
            } else {
                clearInterval(window.matrixLogInterval);
            }
        }, 800);
        
        // Change Settings
        if (window.currentDriverId && window.saveDriverSettings) {
            window.saveDriverSettings(window.currentDriverId, {
                ghostMinDelay: 0.1,
                ghostMaxDelay: 0.5,
                minValuePerKm: 5.0,
                ghostSequenceTrafficWeight: 1.0,
                ghostSequenceLatencyWeight: 1.0,
                ghostSequenceAggressiveness: 'AGRESSIVO'
            });
        }
    };
    
    window.deactivateMatrixProtocol = function() {
        if (typeof window.speakText === 'function') {
            window.speakText("Protocolo Matrix desativado. Limpando logs e restaurando parâmetros de segurança.");
        }
        clearInterval(window.matrixInterval);
        clearInterval(window.matrixLogInterval);
        const matrixDiv = document.getElementById('matrixProtocolOverlay');
        if (matrixDiv) matrixDiv.remove();
        
        if (window.currentDriverId && window.saveDriverSettings) {
            window.saveDriverSettings(window.currentDriverId, {
                ghostMinDelay: 2.0,
                ghostMaxDelay: 7.0,
                minValuePerKm: 1.5,
                ghostSequenceTrafficWeight: 0.5,
                ghostSequenceLatencyWeight: 0.3,
                ghostSequenceAggressiveness: 'EQUILIBRADO'
            });
        }
    };
