import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """            // Overlays
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.TopCenter) {"""

replacement = """            // Overlays
            val isOverlayActive = showProfilePanel || showConfigPanel
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isOverlayActive) Color.Black.copy(alpha = 0.7f) else Color.Transparent)
                    .then(if (isOverlayActive) Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { showProfilePanel = false; showConfigPanel = false } else Modifier)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.TopCenter
            ) {"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Target not found")
