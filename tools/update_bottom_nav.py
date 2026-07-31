import re

with open('app/src/main/java/com/example/HolographicCockpit.kt', 'r') as f:
    content = f.read()

target = """fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit = {}) {
    val cyan = Color(0xFF00F5D4)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = if (isActive) cyan else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = if (isActive) cyan else Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}"""

replacement = """fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit = {}) {
    val cyan = Color(0xFF00F5D4)
    val scale by animateFloatAsState(targetValue = if (isActive) 1.1f else 1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    val alpha by animateFloatAsState(targetValue = if (isActive) 1.0f else 0.4f)
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale)
        ) {
            Icon(icon, contentDescription = null, tint = if (isActive) cyan else Color.White.copy(alpha = alpha), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = if (isActive) cyan else Color.White.copy(alpha = alpha), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/HolographicCockpit.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Target not found")
