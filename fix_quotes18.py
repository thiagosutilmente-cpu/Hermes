with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("pickup_address: window.mergedActiveOrder.pickup_address + ` & \" + window.mergedActiveOrder.pickup_address_2,", "pickup_address: window.mergedActiveOrder.pickup_address + \" & \" + window.mergedActiveOrder.pickup_address_2,")
content = content.replace("window.logGhostAction(`Logs reiniciados com sucesso.\");", "window.logGhostAction(\"Logs reiniciados com sucesso.\");")
content = content.replace("document.getElementById('ghostConsoleLogs`);", "document.getElementById('ghostConsoleLogs');")

with open('index.html', 'w') as f:
    f.write(content)
