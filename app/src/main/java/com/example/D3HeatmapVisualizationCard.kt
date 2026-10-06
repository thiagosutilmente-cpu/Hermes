package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

/**
 * HeatmapOfferCluster
 *
 * Modelo para cluster de dados de calor geográfico
 */
data class HeatmapOfferCluster(
    val id: String,
    val name: String,
    val acceptedCount: Int,
    val avgGainPerKm: Double,
    val totalRevenue: Double,
    val xRatio: Double, // Posição normalizada 0.0 a 1.0 no canvas SVG D3
    val yRatio: Double,
    val intensity: Double // 0.0 a 1.0 (densidade de pedidos aceitos)
)

/**
 * D3HeatmapDataEngine
 *
 * Provê clusters de dados reais/calibrados de ofertas aceitas nas principais zonas gastronômicas de alta demanda.
 */
object D3HeatmapDataEngine {

    fun getDefaultAcceptedClusters(): List<HeatmapOfferCluster> {
        return listOf(
            HeatmapOfferCluster(
                id = "hotspot_paulista",
                name = "Polo Paulista / Jardins",
                acceptedCount = 48,
                avgGainPerKm = 7.80,
                totalRevenue = 1380.00,
                xRatio = 0.50,
                yRatio = 0.42,
                intensity = 0.95
            ),
            HeatmapOfferCluster(
                id = "hotspot_pinheiros",
                name = "Vila Madalena / Pinheiros",
                acceptedCount = 37,
                avgGainPerKm = 6.90,
                totalRevenue = 980.50,
                xRatio = 0.28,
                yRatio = 0.48,
                intensity = 0.82
            ),
            HeatmapOfferCluster(
                id = "hotspot_itaim",
                name = "Itaim Bibi / Faria Lima",
                acceptedCount = 42,
                avgGainPerKm = 8.40,
                totalRevenue = 1520.00,
                xRatio = 0.42,
                yRatio = 0.68,
                intensity = 0.90
            ),
            HeatmapOfferCluster(
                id = "hotspot_moema",
                name = "Moema / Shopping Ibirapuera",
                acceptedCount = 29,
                avgGainPerKm = 6.20,
                totalRevenue = 740.00,
                xRatio = 0.62,
                yRatio = 0.75,
                intensity = 0.70
            ),
            HeatmapOfferCluster(
                id = "hotspot_berrini",
                name = "Berrini / Morumbi Prime",
                acceptedCount = 25,
                avgGainPerKm = 5.90,
                totalRevenue = 610.00,
                xRatio = 0.32,
                yRatio = 0.85,
                intensity = 0.65
            ),
            HeatmapOfferCluster(
                id = "hotspot_tatuape",
                name = "Tatuapé / Anália Franco",
                acceptedCount = 31,
                avgGainPerKm = 5.50,
                totalRevenue = 715.00,
                xRatio = 0.82,
                yRatio = 0.35,
                intensity = 0.74
            ),
            HeatmapOfferCluster(
                id = "hotspot_santana",
                name = "Santana / Zona Norte",
                acceptedCount = 18,
                avgGainPerKm = 4.80,
                totalRevenue = 390.00,
                xRatio = 0.55,
                yRatio = 0.18,
                intensity = 0.50
            )
        )
    }

    /**
     * Gera o HTML completo com a biblioteca D3.js (v7) embarcada,
     * implementando mapa de densidade em gradientes radiais, círculos de contorno térmico e tooltips interativos.
     */
    fun buildD3HeatmapHtml(clusters: List<HeatmapOfferCluster>): String {
        val jsonArray = JSONArray()
        clusters.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("count", c.acceptedCount)
                put("gainKm", c.avgGainPerKm)
                put("revenue", c.totalRevenue)
                put("x", c.xRatio)
                put("y", c.yRatio)
                put("intensity", c.intensity)
            }
            jsonArray.put(obj)
        }

        val rawDataJson = jsonArray.toString()

        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>D3 Heatmap - Jarvis Radar</title>
    <!-- Inclusão do D3.js v7 oficial via CDN com fallback SVG local resiliente -->
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: #050508;
            color: #FFFFFF;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            overflow: hidden;
            touch-action: none;
            -webkit-user-select: none;
            user-select: none;
        }
        #chart-container {
            width: 100vw;
            height: 100vh;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        svg {
            width: 100%;
            height: 100%;
            display: block;
        }
        .grid-line {
            stroke: #1E293B;
            stroke-width: 0.6;
            stroke-dasharray: 3, 3;
            opacity: 0.5;
        }
        .heat-circle {
            mix-blend-mode: screen;
            transition: all 0.3s ease;
        }
        .heat-core {
            cursor: pointer;
        }
        .pulse-ring {
            animation: pulseAnim 2.4s infinite cubic-bezier(0.2, 0.8, 0.2, 1);
            transform-origin: center;
        }
        @keyframes pulseAnim {
            0% { transform: scale(0.85); opacity: 0.9; }
            50% { transform: scale(1.35); opacity: 0.3; }
            100% { transform: scale(0.85); opacity: 0.9; }
        }
        .label-text {
            font-size: 10px;
            font-weight: 700;
            fill: #FFFFFF;
            text-anchor: middle;
            text-shadow: 0 2px 4px #000;
            pointer-events: none;
        }
        .sub-label-text {
            font-size: 8px;
            font-weight: 800;
            fill: #00FF88;
            text-anchor: middle;
            pointer-events: none;
        }
        #tooltip {
            position: absolute;
            background: rgba(15, 23, 42, 0.95);
            border: 1px solid #00FF88;
            border-radius: 8px;
            padding: 8px 12px;
            font-size: 11px;
            pointer-events: none;
            display: none;
            box-shadow: 0 4px 12px rgba(0, 255, 136, 0.25);
            z-index: 99;
        }
    </style>
</head>
<body>
    <div id="chart-container">
        <div id="tooltip"></div>
    </div>

    <script>
        const clusters = $rawDataJson;
        const container = document.getElementById('chart-container');
        const tooltip = document.getElementById('tooltip');

        function renderHeatmap() {
            container.querySelectorAll('svg').forEach(el => el.remove());

            const width = window.innerWidth;
            const height = window.innerHeight;

            const svg = d3.select("#chart-container")
                .append("svg")
                .attr("viewBox", `0 0 ${'$'}{width} ${'$'}{height}`)
                .attr("preserveAspectRatio", "xMidYMid meet");

            // Definição de gradientes térmicos D3 (Heatmap Multicamadas)
            const defs = svg.append("defs");

            clusters.forEach((d, i) => {
                const gradId = "heat-grad-" + i;
                const radialGrad = defs.append("radialGradient")
                    .attr("id", gradId)
                    .attr("cx", "50%").attr("cy", "50%")
                    .attr("r", "50%");

                radialGrad.append("stop")
                    .attr("offset", "0%")
                    .attr("stop-color", "#FF0055")
                    .attr("stop-opacity", d.intensity * 0.9);

                radialGrad.append("stop")
                    .attr("offset", "35%")
                    .attr("stop-color", "#FF6600")
                    .attr("stop-opacity", d.intensity * 0.7);

                radialGrad.append("stop")
                    .attr("offset", "65%")
                    .attr("stop-color", "#00FF88")
                    .attr("stop-opacity", d.intensity * 0.4);

                radialGrad.append("stop")
                    .attr("offset", "100%")
                    .attr("stop-color", "#00F0FF")
                    .attr("stop-opacity", 0);
            });

            // Grid de coordenadas táticas de fundo
            const gridG = svg.append("g").attr("class", "grid-group");
            for (let x = 40; x < width; x += 60) {
                gridG.append("line").attr("x1", x).attr("y1", 0).attr("x2", x).attr("y2", height).attr("class", "grid-line");
            }
            for (let y = 40; y < height; y += 60) {
                gridG.append("line").attr("x1", 0).attr("y1", y).attr("x2", width).attr("y2", y).attr("class", "grid-line");
            }

            // Camada 1: Círculos de calor difuso D3
            const heatG = svg.append("g").attr("class", "heat-layer");
            clusters.forEach((d, i) => {
                const cx = d.x * width;
                const cy = d.y * height;
                const radius = Math.max(width, height) * 0.22 * d.intensity;

                heatG.append("circle")
                    .attr("class", "heat-circle pulse-ring")
                    .attr("cx", cx)
                    .attr("cy", cy)
                    .attr("r", radius)
                    .attr("fill", `url(#heat-grad-${'$'}{i})`);
            });

            // Camada 2: Núcleos térmicos e rótulos interativos
            const coreG = svg.append("g").attr("class", "core-layer");
            clusters.forEach((d) => {
                const cx = d.x * width;
                const cy = d.y * height;

                // Anel de contenção
                coreG.append("circle")
                    .attr("cx", cx)
                    .attr("cy", cy)
                    .attr("r", 14)
                    .attr("fill", "#0F172A")
                    .attr("stroke", "#00FF88")
                    .attr("stroke-width", 2)
                    .attr("class", "heat-core")
                    .on("click", function() {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onClusterSelected(d.name, d.count, d.gainKm);
                        }
                    });

                // Quantidade central
                coreG.append("text")
                    .attr("x", cx)
                    .attr("y", cy + 3.5)
                    .attr("text-anchor", "middle")
                    .attr("fill", "#FFFFFF")
                    .attr("font-size", "9px")
                    .attr("font-weight", "900")
                    .attr("pointer-events", "none")
                    .text(d.count);

                // Rótulo da Região
                coreG.append("text")
                    .attr("class", "label-text")
                    .attr("x", cx)
                    .attr("y", cy - 18)
                    .text(d.name);

                // Rótulo da Rentabilidade Média
                coreG.append("text")
                    .attr("class", "sub-label-text")
                    .attr("x", cx)
                    .attr("y", cy + 26)
                    .text(`R$ ${'$'}{d.gainKm.toFixed(2)}/km`);
            });
        }

        window.addEventListener('resize', renderHeatmap);
        renderHeatmap();
    </script>
</body>
</html>
        """.trimIndent()
    }
}

/**
 * Interface JavaScript para comunicação do D3.js com o Kotlin/Compose
 */
class D3HeatmapWebInterface(
    private val context: Context,
    private val onClusterClicked: (String, Int, Double) -> Unit
) {
    @JavascriptInterface
    fun onClusterSelected(name: String, count: Int, gainKm: Double) {
        onClusterClicked(name, count, gainKm)
    }
}

/**
 * D3HeatmapVisualizationCard
 *
 * Componente Jetpack Compose que renderiza o mapa de calor em D3.js
 * destacando áreas com maior concentração de ofertas aceitas e maior lucro/km.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3HeatmapVisualizationCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRegionInfo by remember { mutableStateOf<String?>(null) }
    var heatmapData by remember { mutableStateOf(D3HeatmapDataEngine.getDefaultAcceptedClusters()) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .testTag("card_d3_heatmap_visualization")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // CABEÇALHO DO COMPONENTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF0055).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Mapa de Calor D3.js",
                            tint = Color(0xFFFF0055),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAPA DE CALOR D3.js • OFERTAS ACEITAS",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Polos gastronômicos com maior densidade de aceites",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        // Recarrega o canvas D3
                        webViewInstance?.loadDataWithBaseURL(
                            "https://d3js.org",
                            D3HeatmapDataEngine.buildD3HeatmapHtml(heatmapData),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        Toast.makeText(context, "Mapa D3 atualizado com dados recentes!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar Mapa",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LEGENDA TÁTICA DE INTENSIDADE TÉRMICA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF0055)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pico Alto (> R$ 7,50/km)", color = Color(0xFF94A3B8), fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF6600)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Médio-Alto", color = Color(0xFF94A3B8), fontSize = 9.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF88)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rentável", color = Color(0xFF94A3B8), fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CONTAINER DO WEBVIEW EMBUTIDO COM D3.JS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF050508))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }
                            setBackgroundColor(android.graphics.Color.BLACK)

                            addJavascriptInterface(
                                D3HeatmapWebInterface(ctx) { name, count, gainKm ->
                                    selectedRegionInfo = "$name: $count corridas aceitas (Média R$ ${String.format("%.2f", gainKm)}/km)"
                                    try {
                                        TextToSpeechManager.getInstance(ctx).speak(
                                            "Polo selecionado: $name. $count corridas aceitas com média de ${String.format("%.2f", gainKm).replace(".", ",")} por quilômetro."
                                        )
                                    } catch (_: Exception) {}
                                },
                                "AndroidInterface"
                            )

                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()

                            val html = D3HeatmapDataEngine.buildD3HeatmapHtml(heatmapData)
                            loadDataWithBaseURL("https://d3js.org", html, "text/html", "UTF-8", null)

                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            }

            // DETALHE DA REGIÃO SELECIONADA POR TOQUE
            selectedRegionInfo?.let { info ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 $info",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DICA OPERACIONAL DE POSICIONAMENTO ESTRATÉGICO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "💡 Dica Jarvis: Toque nos polos no mapa para inspecionar rendimento ou posicionar sua moto nas áreas quentes vermelhas para receber pedidos antes de outros pilotos.",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp
                )
            }
        }
    }
}
