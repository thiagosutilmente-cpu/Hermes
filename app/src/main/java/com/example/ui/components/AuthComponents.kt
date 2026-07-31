package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseAuthManager
import com.example.coordinator.RadarSettings
import com.example.AccentBlue
import com.example.AccentGreen
import com.example.AccentRed
import com.example.TextLight
import kotlinx.coroutines.launch

@Composable
fun FirebaseAccountSection(settings: RadarSettings) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by FirebaseAuthManager.currentUser.collectAsState()
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authSuccessMsg by remember { mutableStateOf<String?>(null) }

    HorizontalDivider(color = AccentBlue.copy(alpha = 0.2f), thickness = 1.dp)

    Text(
        text = "Conta do Entregador",
        color = AccentBlue,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp)
    )

    if (currentUser == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Faça login para salvar seus ganhos, histórico de ofertas e preferências de forma permanente vinculados à sua conta pessoal.",
                color = TextLight.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            OutlinedTextField(
                value = authEmail,
                onValueChange = { 
                    authEmail = it
                    authError = null
                },
                label = { Text("E-mail do Entregador") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight, 
                    unfocusedTextColor = TextLight, 
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
            )

            OutlinedTextField(
                value = authPassword,
                onValueChange = { 
                    authPassword = it
                    authError = null
                },
                label = { Text("Senha") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight, 
                    unfocusedTextColor = TextLight, 
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = TextLight.copy(alpha = 0.3f)
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
            )

            if (authError != null) {
                Text(
                    text = authError ?: "",
                    color = AccentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            if (authSuccessMsg != null) {
                Text(
                    text = authSuccessMsg ?: "",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (authEmail.isBlank() || authPassword.isBlank()) {
                            authError = "Por favor, preencha o e-mail e a senha."
                            return@Button
                        }
                        authLoading = true
                        authError = null
                        authSuccessMsg = null
                        coroutineScope.launch {
                            val res = FirebaseAuthManager.loginWithEmail(authEmail.trim(), authPassword)
                            authLoading = false
                            if (res.isSuccess) {
                                authSuccessMsg = "Login realizado com sucesso!"
                                authEmail = ""
                                authPassword = ""
                            } else {
                                authError = "Erro ao entrar: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !authLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.weight(1f).testTag("auth_login_button")
                ) {
                    if (authLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextLight, strokeWidth = 2.dp)
                    } else {
                        Text("Entrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (authEmail.isBlank() || authPassword.isBlank()) {
                            authError = "Por favor, preencha o e-mail e a senha."
                            return@OutlinedButton
                        }
                        if (authPassword.length < 6) {
                            authError = "A senha deve conter pelo menos 6 caracteres."
                            return@OutlinedButton
                        }
                        authLoading = true
                        authError = null
                        authSuccessMsg = null
                        coroutineScope.launch {
                            val res = FirebaseAuthManager.signUpWithEmail(authEmail.trim(), authPassword)
                            authLoading = false
                            if (res.isSuccess) {
                                authSuccessMsg = "Conta criada e conectada!"
                                authEmail = ""
                                authPassword = ""
                            } else {
                                authError = "Erro ao cadastrar: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !authLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                    border = BorderStroke(1.dp, AccentBlue),
                    modifier = Modifier.weight(1f).testTag("auth_signup_button")
                ) {
                    Text("Criar Conta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conectado como:",
                        color = TextLight.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        FirebaseAuthManager.logout()
                        authSuccessMsg = "Sessão encerrada."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f)),
                    modifier = Modifier.testTag("auth_logout_button")
                ) {
                    Text("Sair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "✓ Ganhos, preferências de rotas e históricos vinculados e sincronizados na nuvem.",
                color = AccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (authSuccessMsg != null) {
        Text(
            text = authSuccessMsg ?: "",
            color = AccentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }

    HorizontalDivider(color = AccentBlue.copy(alpha = 0.2f), thickness = 1.dp)
}
