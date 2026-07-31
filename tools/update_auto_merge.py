import re

with open('index.html', 'r') as f:
    content = f.read()

target = """              } else if (netProfit >= threshold && !activeOrderId) {"""

replacement = """              } else if (netProfit >= threshold) {
                  if (activeOrderId) {
                      // Attempt Auto-Merge if compatible
                      if (window.checkCompatibility && !window.mergedActiveOrder) {
                          const activeOrderObj = (typeof currentOrdersList !== 'undefined' && currentOrdersList) ? currentOrdersList.find(o => o.id === activeOrderId && o.status !== 'canceled') : null;
                          if (activeOrderObj && window.checkCompatibility(activeOrderObj, order) !== null) {
                              if (!window.pendingAcceptIds) window.pendingAcceptIds = new Set();
                              if (!window.pendingAcceptIds.has(order.id)) {
                                  window.pendingAcceptIds.add(order.id);
                                  setTimeout(() => {
                                      if (window.pendingAcceptIds.has(order.id)) {
                                          showToast(`[Auto-Mesclagem] Juntando rotas para lucro otimizado!`, 'success');
                                          if (window.mergeOrders) {
                                              window.mergeOrders(activeOrderId, order.id);
                                              if (window.logGhostAction) {
                                                  window.logGhostAction(`Mesclagem automática realizada: R$ ${fare.toFixed(2)} extra (Lucro real totalizado).`);
                                              }
                                          }
                                      }
                                      window.pendingAcceptIds.delete(order.id);
                                  }, 1500 + Math.random() * 2000); // slight delay for realism
                              }
                          }
                      }
                      return; // Do not proceed to normal auto-accept logic if already active
                  }
"""

if target in content:
    content = content.replace(target, replacement)
    with open('index.html', 'w') as f:
        f.write(content)
    print("Fixed auto-merge logic")
else:
    print("Target not found")
