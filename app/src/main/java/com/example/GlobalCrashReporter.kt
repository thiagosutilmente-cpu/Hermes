package com.example

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * GlobalCrashReporter
 * 
 * Interceptador global de falhas e exceções não tratadas (UncaughtExceptionHandler).
 * Grava o relatório de falha em arquivo local no aparelho e despacha via HTTP POST
 * para a VPS Hostinger (http://187.77.248.73/api/telemetry/crash) para análise em tempo real.
 */
object GlobalCrashReporter {

    private const val TAG = "GlobalCrashReporter"
    private const val VPS_CRASH_URL = "http://187.77.248.73/api/telemetry/crash"
    private const val LOG_FILENAME = "radar_crash_history.log"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleUncaughtException(appContext, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Erro interno ao processar crash: ${e.message}")
            } finally {
                // Encaminha para o tratador original do Android para encerrar de forma limpa
                originalHandler?.uncaughtException(thread, throwable)
            }
        }

        Log.i(TAG, "🛡️ GlobalCrashReporter ativado com telemetria para VPS.")
    }

    private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTraceString = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        val errorMessage = throwable.message ?: throwable.javaClass.simpleName

        Log.e(TAG, "💥 CRASH CAPTURADO: $errorMessage em thread '${thread.name}'")

        // 1. Grava no armazenamento interno do app
        try {
            val logFile = File(context.filesDir, LOG_FILENAME)
            val logEntry = """
                ====================================================
                DATA: $timestamp
                DISPOSITIVO: $deviceModel
                THREAD: ${thread.name} (id: ${thread.id})
                MENSAGEM: $errorMessage
                PILHA DE ERRO:
                $stackTraceString
                ====================================================
            """.trimIndent() + "\n\n"

            logFile.appendText(logEntry)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao gravar crash local: ${e.message}")
        }

        // 2. Tenta enviar imediatamente para a VPS Hostinger
        try {
            val payload = JSONObject().apply {
                put("timestamp", timestamp)
                put("device", deviceModel)
                put("thread", thread.name)
                put("error_type", throwable.javaClass.name)
                put("message", errorMessage)
                put("stack_trace", stackTraceString)
                put("app_version", "3.1.0 (v30)")
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(VPS_CRASH_URL)
                .post(requestBody)
                .build()

            // Execução síncrona curta antes do processo morrer
            val response = httpClient.newCall(request).execute()
            response.close()
            Log.i(TAG, "📡 Crash enviado com sucesso para a VPS Hostinger!")
        } catch (e: Exception) {
            Log.w(TAG, "VPS indisponível para envio imediato de crash: ${e.message}")
        }
    }

    /**
     * Retorna o histórico de erros gravados localmente no aparelho.
     */
    fun getLocalCrashLogs(context: Context): String {
        return try {
            val logFile = File(context.filesDir, LOG_FILENAME)
            if (logFile.exists()) logFile.readText() else "Nenhum erro registrado até o momento."
        } catch (e: Exception) {
            "Erro ao ler logs locais: ${e.message}"
        }
    }

    /**
     * Limpa o histórico de erros do aparelho.
     */
    fun clearLocalCrashLogs(context: Context) {
        try {
            val logFile = File(context.filesDir, LOG_FILENAME)
            if (logFile.exists()) logFile.delete()
        } catch (_: Exception) {}
    }
}
