package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gerenciador de Atualizações Automáticas Over-The-Air (OTA) do Jarvis Neural Cockpit.
 * Permite ao app verificar atualizações na VPS, baixar o novo APK em segundo plano
 * com barra de progresso e abrir o instalador nativo do Android em 1 clique.
 */
object AutoUpdateManager {

    private const val DEFAULT_VERSION_URL = "http://187.77.248.73:8000/api/version"

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val changelog: String,
        val forceUpdate: Boolean = false
    )

    /**
     * Consulta a VPS para verificar se há uma versão mais recente do APK
     */
    suspend fun checkForUpdate(
        context: Context,
        versionUrl: String = DEFAULT_VERSION_URL
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(versionUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val remoteVersionCode = json.optInt("versionCode", 0)
                val remoteVersionName = json.optString("versionName", "1.0.0")
                val apkUrl = json.optString("apkUrl", "http://187.77.248.73:8000/download")
                val changelog = json.optString("changelog", "Melhorias de desempenho e estabilidade.")
                val forceUpdate = json.optBoolean("forceUpdate", false)

                val currentVersionCode = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }
                } catch (e: Exception) {
                    1
                }

                if (remoteVersionCode > currentVersionCode) {
                    UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        apkUrl = apkUrl,
                        changelog = changelog,
                        forceUpdate = forceUpdate
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Redireciona o usuário com segurança para a página oficial do aplicativo na Google Play Store
     * (conforme políticas de Integridade do Sistema do Google Play, sem sideloading de APK externo).
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.Main) {
        try {
            onProgress(1.0f)
            openPlayStoreListing(context)
            onComplete()
        } catch (e: Exception) {
            onError("Erro ao abrir a Google Play Store: ${e.localizedMessage ?: "tente novamente"}")
        }
    }

    /**
     * Abre a ficha oficial do Radar Coordinator na Google Play Store.
     */
    fun openPlayStoreListing(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}

/**
 * Diálogo Compose de Atualização Automática estilizado para o Jarvis Neural Cockpit
 */
@Composable
fun AutoUpdateDialog(
    updateInfo: AutoUpdateManager.UpdateInfo,
    onDismiss: () -> Unit,
    onConfirmUpdate: () -> Unit,
    isDownloading: Boolean,
    downloadProgress: Float
) {
    Dialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && !isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.forceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.forceUpdate && !isDownloading
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone de Foguete / Atualização
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Atualização",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NOVA VERSÃO DISPONÍVEL!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Versão ${updateInfo.versionName}",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog / O que há de novo
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "O que há de novo:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.changelog,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Baixando atualização: ${(downloadProgress * 100).toInt()}%",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!updateInfo.forceUpdate) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                )
                            ) {
                                Text("Mais Tarde", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onConfirmUpdate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text("Atualizar Agora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
