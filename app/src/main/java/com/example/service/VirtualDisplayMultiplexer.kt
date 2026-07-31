package com.example.service

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.Log

object VirtualDisplayMultiplexer {
    private const val TAG = "VirtualMultiplexer"
    private val vms = mutableMapOf<String, VirtualDisplay>()

    fun startHeadlessContainer(context: Context, appName: String, width: Int = 1080, height: Int = 1920) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val surface = android.media.ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2).surface
        
        try {
            val virtualDisplay = displayManager.createVirtualDisplay(
                "JarvisVM_$appName",
                width,
                height,
                320, // dpi
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            )
            vms[appName] = virtualDisplay
            Log.d(TAG, "Contêiner Headless (VM) iniciado com sucesso para: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar VM Headless para $appName", e)
        }
    }

    fun stopAll() {
        vms.values.forEach { it.release() }
        vms.clear()
        Log.d(TAG, "Todas as VMs Headless foram encerradas.")
    }
}
