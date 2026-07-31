import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = "                    Spacer(modifier = Modifier.height(24.dp))"

button_code = """
            // Geofence / Cerca Virtual Advanced Config
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { showGeofenceModal = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Cerca Virtual Avançada", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Desenhe raios e gerencie alertas por voz", fontSize = 12.sp, color = TextDim)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextDim)
                }
            }
"""

replacement = button_code + "\n" + target

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Inserted button")
else:
    print("Target not found")

