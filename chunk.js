        const pendingOrders = window.currentOrdersList.filter(o => 
            o.status !== 'canceled' && 
            o.status !== 'completed' && 
            o.status !== 'merged_taken' && 
            o.status !== 'accepted' && 
            !window.rejectedOrderIds.has(o.id)
        );
        
        if (pendingOrders.length < 2) {
            document.getElementById('routeMergingDashboard').style.display = 'none';
            return;
        }
        
        const suggestions = [];
        const checkedPairs = new Set();
        
        for (let i = 0; i < pendingOrders.length; i++) {
            for (let j = i + 1; j < pendingOrders.length; j++) {
                const o1 = pendingOrders[i];
                const o2 = pendingOrders[j];
                
                // Prevent duplicate checks
                const pairKey = [o1.id, o2.id].sort().join('-');
                if (checkedPairs.has(pairKey)) continue;
                checkedPairs.add(pairKey);
                
                const comp = window.checkCompatibility ? window.checkCompatibility(o1, o2) : null;
                if (comp && comp.compatibilityPercentage >= 80) { // High threshold for suggestions
                    suggestions.push({
                        order1: o1,
                        order2: o2,
                        compatibility: comp
                    });
                }
            }
        }
        
        const dash = document.getElementById('routeMergingDashboard');
        const contentDiv = document.getElementById('routeMergeSuggestionsContent');
        
        if (suggestions.length > 0) {
            dash.style.display = 'block';
            contentDiv.innerHTML = '';
            
            // Sort by compatibility
            suggestions.sort((a, b) => b.compatibility.compatibilityPercentage - a.compatibility.compatibilityPercentage);
            
            // Render up to 2 suggestions
            suggestions.slice(0, 2).forEach((s, idx) => {
                const fareTotal = (parseFloat(s.order1.fare_value || 0) + parseFloat(s.order2.fare_value || 0)).toFixed(2);
                
                const html = `
                    <div style="background: rgba(0,0,0,0.4); border: 1px solid rgba(255, 0, 255, 0.2); border-radius: 8px; padding: 12px; display: flex; flex-direction: column; gap: 8px;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div style="font-size: 11px; color: var(--text-muted);">Mesclar: <b style="color: #fff;">${s.order1.delivery_app}</b> + <b style="color: #fff;">${s.order2.delivery_app}</b></div>
                            <div style="font-size: 12px; font-weight: bold; color: var(--success);">Ganho: R$ ${fareTotal}</div>
                        </div>
                        <div style="font-size: 10px; color: #aaa;">
                            Otimização de Rota: <b style="color: #ff00ff;">${s.compatibility.compatibilityPercentage}% Sobreposição</b><br>
                            Economia Estimada: <b style="color: #00f5d4;">${s.compatibility.reducedOdometer} km</b>
                        </div>
                        <button onclick="window.mergeOrders('${s.order1.id}', '${s.order2.id}')" style="background: linear-gradient(90deg, rgba(255,0,255,0.2) 0%, rgba(131,56,236,0.2) 100%); border: 1px solid #ff00ff; color: #fff; padding: 8px; border-radius: 6px; font-size: 11px; font-weight: bold; cursor: pointer; text-transform: uppercase; margin-top: 4px; display: flex; align-items: center; justify-content: center; gap: 6px;">
                            <span class="material-symbols-rounded" style="font-size: 16px;">merge_type</span>
                            Mesclar Automaticamente
                        </button>
                    </div>
                `;
                contentDiv.innerHTML += html;
            });
            
            // Notify if new
            if (!window.lastMergeSuggestions || window.lastMergeSuggestions !== suggestions[0].order1.id + suggestions[0].order2.id) {
                window.lastMergeSuggestions = suggestions[0].order1.id + suggestions[0].order2.id;
                if (window.showToast) window.showToast("⚡ Jarvis detectou oportunidade de mesclagem de rota!", "info");
            }
            
        } else {
            dash.style.display = 'none';
        }
    };

    window.mergeOrders = function(activeId, pendingId) {
      if (!currentOrdersList) return;
      const active = currentOrdersList.find(o => o.id === activeId);
      const pending = currentOrdersList.find(o => o.id === pendingId);
      if (active && pending) {
        const detour = 1.2; // Pequeno desvio simulado
        window.mergedActiveOrder = {
          id: "merged_" + active.id + "_" + pending.id,
          isMerged: true,
          originalActiveId: active.id,
          originalPendingId: pending.id,
          delivery_app: active.delivery_app + " + " + pending.delivery_app,
          fare_value: parseFloat(active.fare_value) + parseFloat(pending.fare_value),
          total_distance_km: parseFloat(active.total_distance_km) + detour,
          pickup_address: active.pickup_address,
          delivery_address: active.delivery_address,
          pickup_address_2: pending.pickup_address,
          delivery_address_2: pending.delivery_address,
          currentRoutingOption: 'opt1',