package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AdminLoginModal(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            border = BorderStroke(1.dp, Color(0xFF3A86FF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "ACESSO_ADMINISTRATIVO_RESTRITO", color = Color(0xFF3A86FF), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("PIN", color = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3A86FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                Button(
                    onClick = {
                        if (pin == "748596") {
                            onLoginSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFF3A86FF))
                ) {
                    Text("VALIDAR_ACESSO", color = Color(0xFF3A86FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardModal(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "SISTEMA_OPERACIONAL_JARVIS_DEBUG", color = Color(0xFF00F5D4), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                
                Text(text = "CONSTRUÇÃO: KOTLIN/COMPOSE", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                Text(text = "INTEGRAÇÃO: GEMINI_API/ELEVENLABS", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                Text(text = "STATUS_PERSISTÊNCIA: ATIVO", color = Color(0xFF00F5D4), fontFamily = FontFamily.Monospace)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color.Red)) {
                    Text("ENCERRAR_SESSÃO_ADMIN", color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
