package com.example.radar.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.data.ApiConnectionStatus
import com.example.radar.data.ConnectionState
import com.example.radar.ui.theme.DarkSurfaceElevated
import com.example.radar.ui.theme.StatusConnecting
import com.example.radar.ui.theme.StatusOffline
import com.example.radar.ui.theme.StatusOnline
import com.example.radar.ui.theme.TextPrimary
import com.example.radar.ui.theme.TextSecondary

@Composable
fun ApiConnectionStatusBadge(
    status: ApiConnectionStatus,
    onReconnectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (status.state) {
        ConnectionState.CONNECTED -> StatusOnline to "ONLINE • ${status.latencyMs}ms"
        ConnectionState.CONNECTING -> StatusConnecting to "SINCRONIZANDO..."
        ConnectionState.DISCONNECTED -> StatusOffline to "OFFLINE"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onReconnectClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador LED pulsante
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(14.dp)
        ) {
            if (status.state == ConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.35f))
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = statusText,
            color = if (status.state == ConnectionState.CONNECTED) TextPrimary else statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
