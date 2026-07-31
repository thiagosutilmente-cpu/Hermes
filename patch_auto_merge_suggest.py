import sys

with open('index.html', 'r') as f:
    content = f.read()

merge_card_html = """
      <!-- ROUTE MERGING SUGGESTIONS (JARVIS INTELLIGENCE) -->
      <div class="section-card" id="routeMergingDashboard" style="grid-column: 1 / -1; background: linear-gradient(135deg, rgba(255, 0, 255, 0.05) 0%, rgba(10, 11, 18, 0.95) 100%); border: 1px solid rgba(255, 0, 255, 0.3); padding: 18px; margin-top: 15px; border-radius: 12px; display: none; position: relative; overflow: hidden; box-shadow: 0 4px 15px rgba(255, 0, 255, 0.15);">
          <div style="position: absolute; top: -50px; right: -50px; width: 150px; height: 150px; background: radial-gradient(circle, rgba(255, 0, 255, 0.1) 0%, transparent 70%); pointer-events: none;"></div>
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px solid rgba(255, 0, 255, 0.1); padding-bottom: 8px;">
              <div style="display: flex; align-items: center; gap: 8px;">
                  <span class="material-symbols-rounded" style="color: #ff00ff; font-size: 24px; animation: pulse 2s infinite;">join_inner</span>
                  <div style="font-size: 14px; font-weight: 900; color: #fff; text-transform: uppercase; letter-spacing: 1px;">Route Merging Detectado</div>
              </div>
              <span style="font-size: 9px; font-weight: bold; background: rgba(255, 0, 255, 0.2); color: #ff00ff; padding: 2px 6px; border-radius: 4px;">ECONOMIA EXTREMA</span>
          </div>
          <div id="routeMergeSuggestionsContent" style="display: flex; flex-direction: column; gap: 10px;">
              <!-- Javascript will inject suggestions here -->
          </div>
      </div>
"""

idx = content.find('<!-- AÇÕES RÁPIDAS DE TERRENO (QUICK ACTIONS) -->')
if idx != -1:
    content = content[:idx] + merge_card_html + '\n      ' + content[idx:]
else:
    print("Could not find quick actions")


js_code = """
    window.lastMergeCheck = 0;
    
    window.analyzeAndSuggestMerges = function() {
        if (!window.currentOrdersList || window.currentOrdersList.length < 2) {
            document.getElementById('routeMergingDashboard').style.display = 'none';
            return;
        }
        
        const now = Date.now();
        if (now - window.lastMergeCheck < 5000) return; // Prevent excessive checking
        window.lastMergeCheck = now;
        
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
"""

content = content.replace('    window.mergeOrders = function(activeId, pendingId) {', js_code + '\n    window.mergeOrders = function(activeId, pendingId) {')

# Hook analyzeAndSuggestMerges to renderOffers or when updating orders
render_hook = """
      if (window.analyzeAndSuggestMerges) window.analyzeAndSuggestMerges();
"""
content = content.replace('      countText.textContent = activeOrPendingOrders.length;', '      countText.textContent = activeOrPendingOrders.length;\n' + render_hook)

with open('index.html', 'w') as f:
    f.write(content)

print("Route Merging suggestions injected!")
