import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    kt = f.read()

pattern = r"val isOverlayActive = showProfilePanel \|\| showConfigPanel \|\| showGeofenceModal\s+androidx\.compose\.foundation\.layout\.Box\(\s+modifier = Modifier\s+\.fillMaxSize\(\)\s+\.background\(if \(isOverlayActive\) Color\.Black\.copy\(alpha = 0\.7f\) else Color\.Transparent\)\s+\.then\(if \(isOverlayActive\) Modifier\.clickable\(interactionSource = remember \{ androidx\.compose\.foundation\.interaction\.MutableInteractionSource\(\) \}, indication = null\) \{ showProfilePanel = false; showConfigPanel = false; showGeofenceModal = false \} else Modifier\)\s+\.padding\(innerPadding\)\s+\.padding\(horizontal = 16\.dp, vertical = 10\.dp\),\s+contentAlignment = Alignment\.TopCenter\s+\) \{"

replacement = """val isOverlayActive = showProfilePanel || showConfigPanel || showGeofenceModal
            if (isOverlayActive) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { showProfilePanel = false; showConfigPanel = false; showGeofenceModal = false }
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {"""

kt = kt.replace("            // Overlays\n            " + pattern, "            // Overlays\n            " + replacement)
# Use re to replace because exact match might fail due to whitespace
kt = re.sub(pattern, replacement, kt)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(kt)
