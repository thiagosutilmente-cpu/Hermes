package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonEmerald = Color(0xFF00FF88)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

@Composable
fun UltraOledBatterySaverCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val oledManager = remember { UltraOledBatterySaverManager.getInstance(context) }
    val oledState by oledManager.state.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_ultra_oled_battery_saver"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (oledState.isOledModeActive) Color(0xFF050508) else DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (oledState.isOledModeActive) NeonEmerald.copy(alpha = 0.6f) else CardBorderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (oledState.isOledModeActive) NeonEmerald.copy(alpha = 0.2f) else Color(0xFF1C2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = if (oledState.isOledModeActive) NeonEmerald else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MODO NOTURNO OLED",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (oledState.isOledModeActive) NeonEmerald.copy(alpha = 0.2f) else Color(0xFF1F2838),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (oledState.isOledModeActive) "PITCH BLACK ATIVO" else "PADRÃO",
                                    color = if (oledState.isOledModeActive) NeonEmerald else TextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Preto absoluto (#000000) economiza até 40% de bateria",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Switch(
                    checked = oledState.isOledModeActive,
                    onCheckedChange = { enable ->
                        oledManager.setOledMode(enable)
                    },
                    modifier = Modifier.testTag("switch_ultra_oled_mode"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonEmerald,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF1A2230)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Benefícios do Modo Noturno OLED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF080C14))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pixels desligados em telas AMOLED",
                        color = Color(0xFFD6E2F0),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "+4h de autonomia",
                    color = NeonEmerald,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
