    window.toggleTacticalView = function() {
        const mapDiv = document.getElementById('googleMap');
        if (!mapDiv) return;
        
        window.isTacticalViewEnabled = !window.isTacticalViewEnabled;
        const btn = document.getElementById('btnToggleTacticalView');
        
        if (window.isTacticalViewEnabled) {
            mapDiv.style.transition = 'transform 1s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 1s ease, filter 1s ease';
            mapDiv.style.transformOrigin = 'center bottom';
            mapDiv.style.transform = 'perspective(800px) rotateX(45deg) scale(1.1)';
            mapDiv.style.boxShadow = '0 -40px 60px rgba(0, 245, 212, 0.15) inset';
            mapDiv.style.filter = 'contrast(1.2) saturate(1.5) hue-rotate(15deg)';
            if (btn) {
                btn.style.color = '#00F5D4';
                btn.style.borderColor = 'rgba(0, 245, 212, 0.5)';
                btn.style.background = 'rgba(0, 245, 212, 0.15)';
                btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px; color: #00F5D4;">3d_rotation</span> 3D ATIVO';
            }
            if (typeof window.speakText === 'function') window.speakText('Modo de Vista Tática 3D ativado. Mapeando terreno tridimensionalmente.');
        } else {
            mapDiv.style.transform = 'perspective(800px) rotateX(0deg) scale(1.0)';
            mapDiv.style.boxShadow = 'none';
            mapDiv.style.filter = 'none';
            if (btn) {
                btn.style.color = 'var(--text-muted)';
                btn.style.borderColor = 'rgba(255, 255, 255, 0.15)';
                btn.style.background = 'rgba(0, 0, 0, 0.75)';
                btn.innerHTML = '<span class="material-symbols-rounded" style="font-size: 14px;">3d_rotation</span> Vista 3D';
            }
            if (typeof window.speakText === 'function') window.speakText('Vista Tática 3D desativada. Retornando ao modo satélite padrão.');
        }
    };
