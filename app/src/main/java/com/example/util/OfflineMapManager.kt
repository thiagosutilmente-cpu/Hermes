package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.coordinator.LogType
import com.example.coordinator.RadarCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfflineRegion(
    val id: String,
    val name: String,
    val city: String,
    val sizeMB: Double,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadSpeed: String = "",
    val lastUpdated: String = ""
)

object OfflineMapManager {
    private const val PREFS_NAME = "offline_maps_prefs"
    private const val PREF_DOWNLOADED_PREFIX = "downloaded_"
    private const val PREF_DATE_PREFIX = "date_"

    private val _regions = MutableStateFlow<List<OfflineRegion>>(emptyList())
    val regions = _regions.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultRegions = listOf(
            OfflineRegion("centro_sp", "Centro Expandido - SP", "São Paulo", 85.0),
            OfflineRegion("sul_sp", "Zona Sul - SP", "São Paulo", 120.0),
            OfflineRegion("leste_sp", "Zona Leste - SP", "São Paulo", 95.0),
            OfflineRegion("norte_sp", "Zona Norte - SP", "São Paulo", 70.0),
            OfflineRegion("oeste_sp", "Zona Oeste - SP", "São Paulo", 80.0),
            OfflineRegion("abc_sp", "Grande ABC - SP", "São Paulo", 110.0),
            OfflineRegion("campinas", "Campinas & Região", "Campinas", 140.0),
            OfflineRegion("santos", "Santos & Baixada", "Santos", 65.0),
            OfflineRegion("curitiba", "Curitiba - PR", "Curitiba", 130.0),
            OfflineRegion("rio_centro", "Centro & Z. Sul - RJ", "Rio de Janeiro", 105.0),
            OfflineRegion("bh_centro", "Belo Horizonte - MG", "Belo Horizonte", 115.0)
        )

        val updatedList = defaultRegions.map { region ->
            val isDownloaded = prefs.getBoolean(PREF_DOWNLOADED_PREFIX + region.id, false)
            val lastUpdated = prefs.getString(PREF_DATE_PREFIX + region.id, "") ?: ""
            region.copy(
                isDownloaded = isDownloaded,
                lastUpdated = if (isDownloaded) (if (lastUpdated.isNotEmpty()) lastUpdated else "12/07/2026") else "Não Baixado"
            )
        }
        _regions.value = updatedList
    }

    fun startDownload(regionId: String, context: Context, scope: CoroutineScope) {
        if (activeJobs.containsKey(regionId)) return

        val job = scope.launch {
            _regions.update { list ->
                list.map { region ->
                    if (region.id == regionId) {
                        region.copy(isDownloading = true, progress = 0f, downloadSpeed = "0.0 MB/s")
                    } else region
                }
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var currentProgress = 0f
            val totalSteps = 40
            val regionName = _regions.value.find { it.id == regionId }?.name ?: regionId

            RadarCoordinator.addLog("Iniciando download do mapa offline: $regionName", LogType.INFO)

            for (step in 1..totalSteps) {
                delay(120) // total duration around 4.8 seconds
                currentProgress = step.toFloat() / totalSteps.toFloat()
                val speed = String.format(Locale.US, "%.1f MB/s", (2.5 + Math.random() * 3.5))

                _regions.update { list ->
                    list.map { region ->
                        if (region.id == regionId) {
                            region.copy(progress = currentProgress, downloadSpeed = speed)
                        } else region
                    }
                }
            }

            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            prefs.edit()
                .putBoolean(PREF_DOWNLOADED_PREFIX + regionId, true)
                .putString(PREF_DATE_PREFIX + regionId, todayStr)
                .apply()

            _regions.update { list ->
                list.map { region ->
                    if (region.id == regionId) {
                        region.copy(
                            isDownloading = false,
                            isDownloaded = true,
                            progress = 1.0f,
                            downloadSpeed = "",
                            lastUpdated = todayStr
                        )
                    } else region
                }
            }

            activeJobs.remove(regionId)
            RadarCoordinator.addLog("Mapa offline da região $regionName baixado com sucesso!", LogType.SUCCESS)
            RadarCoordinator.voiceManager?.speak("Mapa de $regionName baixado com sucesso.")
        }
        activeJobs[regionId] = job
    }

    fun removeRegion(regionId: String, context: Context) {
        activeJobs[regionId]?.cancel()
        activeJobs.remove(regionId)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREF_DOWNLOADED_PREFIX + regionId, false)
            .putString(PREF_DATE_PREFIX + regionId, "")
            .apply()

        val regionName = _regions.value.find { it.id == regionId }?.name ?: regionId

        _regions.update { list ->
            list.map { region ->
                if (region.id == regionId) {
                    region.copy(
                        isDownloading = false,
                        isDownloaded = false,
                        progress = 0f,
                        downloadSpeed = "",
                        lastUpdated = "Não Baixado"
                    )
                } else region
            }
        }

        RadarCoordinator.addLog("Mapa offline da região $regionName foi removido do dispositivo.", LogType.INFO)
        RadarCoordinator.voiceManager?.speak("Mapa de $regionName removido.")
    }

    fun getTotalStorageOccupiedMB(): Double {
        return _regions.value
            .filter { it.isDownloaded }
            .sumOf { it.sizeMB }
    }
}
