package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Diálogo seguro de autenticação por PIN para desbloquear o Modo Desenvolvedor / Administrador.
 */
@Composable
fun AdminPinAuthDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinVisible by remember { mutableStateOf(false) }

    val handleConfirm = {
        if (pinText.isBlank()) {
            errorMessage = "Digite o PIN de 4 dígitos"
        } else {
            val success = AdminSecurityManager.verifyAndUnlock(context, pinText)
            if (success) {
                HapticFeedbackHelper.vibrateSuccess(context)
                Toast.makeText(context, "🔓 Modo Administrador Desbloqueado com Sucesso!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                HapticFeedbackHelper.vibrateDecline(context)
                errorMessage = "PIN incorreto. Acesso restrito ao proprietário."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        icon = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                        .border(1.2.dp, Color(0xFF00FF88).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Trava de Segurança Admin",
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Cockpit do Proprietário",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column {
                Text(
                    text = "Área reservada para o dono do sistema. Digite o PIN mestre para inspecionar a VPS Hostinger, webhooks Asaas e telemetria interna.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { input ->
                        if (input.length <= 8) {
                            pinText = input.filter { it.isDigit() }
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN de Acesso", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    keyboardActions = KeyboardActions(onDone = { handleConfirm() }),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar Visibilidade",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF88),
                        unfocusedBorderColor = Color(0xFF334155),
                        errorBorderColor = Color(0xFFFF4444)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_pin")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dica: PIN padrão de fábrica é 1984",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = handleConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF88),
                    contentColor = Color(0xFF0A0A0F)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_admin_pin")
            ) {
                Text(
                    text = "DESBLOQUEAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                modifier = Modifier.testTag("btn_cancel_admin_pin")
            ) {
                Text(
                    text = "CANCELAR",
                    fontSize = 12.sp
                )
            }
        }
    )
}
