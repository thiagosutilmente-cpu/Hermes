package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object OtaUpdater {

    private const val UPDATE_JSON_URL = "https://example.com/update.json" // Update with real URL later
    private var downloadId: Long = -1

    fun checkForUpdate(context: Context, downloadUrl: String) {
        // Just simulating the download directly for now to prove concept
        downloadUpdate(context, downloadUrl)
    }

    private fun downloadUpdate(context: Context, apkUrl: String) {
        val fileName = "app_update.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        if (destination.exists()) {
            destination.delete()
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Atualizando Jarvis")
            .setDescription("Baixando nova versão do App...")
            .setDestinationUri(Uri.fromFile(destination))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        // Register Receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId && context != null) {
                    installApk(context, destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        com.example.util.ToastUtils.showToast(context, "Download da atualização iniciado...", Toast.LENGTH_SHORT)
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val apkUri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            apkFile
        )
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("OtaUpdater", "Error installing APK", e)
            com.example.util.ToastUtils.showToast(context, "Erro ao iniciar instalação.", Toast.LENGTH_SHORT)
        }
    }
}
