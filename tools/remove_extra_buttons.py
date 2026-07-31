import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# The button code looks like this:
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextDim)
                }
            }
"""

# Let's count them
count = content.count(button_code)
print(f"Found {count} instances")

# Let's replace the first two with empty string
if count > 1:
    content = content.replace(button_code, "", count - 1)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Removed extra instances")
