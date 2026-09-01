package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RadarMainScreen(
    modifier: Modifier = Modifier,
    viewModel: RadarViewModel = viewModel()
) {
    val isAtivo by viewModel.isRadarActive.collectAsState()

    RadarMainContent(
        isAtivo = isAtivo,
        onToggleRadar = { viewModel.toggleRadar() },
        modifier = modifier
    )
}

@Composable
fun RadarMainContent(
    isAtivo: Boolean,
    onToggleRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("radar_card"),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo do Aplicativo em Destaque
                Image(
                    painter = painterResource(id = R.drawable.radar_delivery_logo_1788290986448),
                    contentDescription = "Logo Radar Delivery",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(
                            2.5.dp,
                            if (isAtivo) Color(0xFF00FF88) else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                        .testTag("radar_app_logo")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAtivo) "Status: Ativado" else "Status: Desativado",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = if (isAtivo) Color(0xFF00FF88) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("radar_status_text")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onToggleRadar,
                    modifier = Modifier.testTag("btn_toggle_radar")
                ) {
                    Text(text = "Ativar/Desativar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadarMainScreenPreview() {
    MaterialTheme {
        Surface {
            RadarMainContent(
                isAtivo = false,
                onToggleRadar = {}
            )
        }
    }
}
