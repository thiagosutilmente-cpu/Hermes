package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.RadarCoordinator
import java.util.Calendar
import java.util.Locale

/**
 * Ponto de dado para o gráfico de ganhos por hora
 */
data class HourlyEarningsPoint(
    val hourLabel: String,
    val earnings: Double,
    val deliveriesCount: Int,
    val kmDriven: Double,
    val appLeading: String
)

/**
 * Widget Visual rico com Recharts & D3 para a tela principal (Holographic Cockpit).
 * Exibe um gráfico de barras interativo de performance de ganhos (R$/hora)
 * nas últimas 8 horas operacionais, detalhando médias, picos e distribuição por app.
 */
@Composable
fun RechartsHourlyEarningsCard(
    modifier: Modifier = Modifier,
    onFilterChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedAppFilter by remember { mutableStateOf("TODOS") }
    var chartType by remember { mutableStateOf("recharts") } // recharts ou d3

    // Gera os dados das últimas 8 horas
    val hourlyData = remember(selectedAppFilter) {
        generateLast8HoursEarnings(selectedAppFilter)
    }

    val totalLast8h = remember(hourlyData) { hourlyData.sumOf { it.earnings } }
    val avgPerHour = remember(hourlyData) { if (hourlyData.isNotEmpty()) totalLast8h / hourlyData.size else 0.0 }
    val peakHour = remember(hourlyData) { hourlyData.maxByOrNull { it.earnings } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("recharts_hourly_earnings_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header do Gráfico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📊", fontSize = 14.sp)
                    Column {
                        Text(
                            text = "GANHOS POR HORA (ÚLTIMAS 8H)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FF88),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Performance com Recharts & D3 Data Engine",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Total 8h Badge
                Surface(
                    color = Color(0x2200FF88),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x6600FF88))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total:",
                            fontSize = 8.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "R$ ${String.format(Locale.US, "%.2f", totalLast8h)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF88)
                        )
                    }
                }
            }

            // Barra de Estatísticas Rápidas (Média/h, Pico/h, KM total)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161C2C))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MÉDIA / HORA", fontSize = 7.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        "R$ ${String.format(Locale.US, "%.2f", avgPerHour)}/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Text("PICO MÁXIMO", fontSize = 7.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        "${peakHour?.hourLabel ?: "--"}: R$ ${String.format(Locale.US, "%.0f", peakHour?.earnings ?: 0.0)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                }

                Column {
                    Text("ENTREGAS", fontSize = 7.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        "${hourlyData.sumOf { it.deliveriesCount }} corridas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB800)
                    )
                }
            }

            // Chips de Filtro por App (Todos, iFood, Rappi, 99, Uber)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("TODOS", "iFood", "Rappi", "99", "Uber").forEach { app ->
                    val isSelected = selectedAppFilter == app
                    val chipColor = when (app) {
                        "iFood" -> Color(0xFFEA1D2C)
                        "Rappi" -> Color(0xFFFF441F)
                        "99" -> Color(0xFFF7C200)
                        "Uber" -> Color(0xFFFFFFFF)
                        else -> Color(0xFF00FF88)
                    }

                    Surface(
                        color = if (isSelected) chipColor.copy(alpha = 0.25f) else Color(0xFF161C2C),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSelected) chipColor else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .clickable {
                                selectedAppFilter = app
                                onFilterChange?.invoke(app)
                            }
                    ) {
                        Text(
                            text = app,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) chipColor else Color.Gray,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Renderização do Gráfico Interativo com WebView Standalone (Recharts + D3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070A11))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            ) {
                RechartsWebViewContainer(
                    data = hourlyData,
                    totalEarnings = totalLast8h
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsWebViewContainer(
    data: List<HourlyEarningsPoint>,
    totalEarnings: Double
) {
    val jsonData = remember(data) {
        val list = data.map {
            """{"hour": "${it.hourLabel}", "earnings": ${it.earnings}, "deliveries": ${it.deliveriesCount}, "km": ${it.kmDriven}, "app": "${it.appLeading}"}"""
        }
        "[${list.joinToString(",")}]"
    }

    val htmlContent = remember(jsonData) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
            <script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
            <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
            <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/d3@7"></script>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                body { background: #070A11; color: #fff; font-family: -apple-system, system-ui, sans-serif; overflow: hidden; padding: 6px; }
                #recharts-root { width: 100%; height: 188px; }
                .custom-tooltip {
                    background: rgba(14, 19, 31, 0.95);
                    border: 1px solid #00FF88;
                    border-radius: 8px;
                    padding: 6px 10px;
                    box-shadow: 0 4px 12px rgba(0,255,136,0.25);
                    font-size: 10px;
                }
                .tooltip-title { font-weight: 800; color: #00FF88; margin-bottom: 2px; }
                .tooltip-val { color: #fff; font-size: 11px; font-weight: 700; }
                .tooltip-sub { color: #8a8a9a; font-size: 9px; }
            </style>
        </head>
        <body>
            <div id="recharts-root"></div>
            <script>
                const data = $jsonData;
                const { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell, ReferenceLine } = window.Recharts;

                const CustomTooltip = ({ active, payload }) => {
                    if (active && payload && payload.length) {
                        const d = payload[0].payload;
                        return React.createElement('div', { className: 'custom-tooltip' },
                            React.createElement('div', { className: 'tooltip-title' }, d.hour + 'h — ' + d.app),
                            React.createElement('div', { className: 'tooltip-val' }, 'R$ ' + d.earnings.toFixed(2)),
                            React.createElement('div', { className: 'tooltip-sub' }, d.deliveries + ' entregas (' + d.km.toFixed(1) + ' km)')
                        );
                    }
                    return null;
                };

                const getBarColor = (app) => {
                    if (app.includes('iFood')) return '#EA1D2C';
                    if (app.includes('Rappi')) return '#FF441F';
                    if (app.includes('99')) return '#F7C200';
                    if (app.includes('Uber')) return '#E0E0E0';
                    return '#00FF88';
                };

                const App = () => {
                    return React.createElement(ResponsiveContainer, { width: '100%', height: '100%' },
                        React.createElement(BarChart, { data: data, margin: { top: 12, right: 10, left: -24, bottom: 0 } },
                            React.createElement(XAxis, { 
                                dataKey: 'hour', 
                                stroke: '#555', 
                                fontSize: 9, 
                                tickLine: false,
                                tick: { fill: '#8A8A9A' }
                            }),
                            React.createElement(YAxis, { 
                                stroke: '#333', 
                                fontSize: 8, 
                                tickLine: false,
                                tick: { fill: '#666' },
                                tickFormatter: (v) => 'R$' + v
                            }),
                            React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
                            React.createElement(Bar, { dataKey: 'earnings', radius: [4, 4, 0, 0] },
                                data.map((entry, index) => 
                                    React.createElement(Cell, { 
                                        key: 'cell-' + index, 
                                        fill: getBarColor(entry.app),
                                        fillOpacity: 0.85
                                    })
                                )
                            )
                        )
                    );
                };

                ReactDOM.render(React.createElement(App), document.getElementById('recharts-root'));
            </script>
        </body>
        </html>
        """
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0) // Transparente
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://radar.cockpit.local", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://radar.cockpit.local", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Gera pontos de ganhos realistas para as últimas 8 horas com base no filtro selecionado
 */
private fun generateLast8HoursEarnings(appFilter: String): List<HourlyEarningsPoint> {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val points = mutableListOf<HourlyEarningsPoint>()

    val appDistribution = when (appFilter) {
        "iFood" -> listOf("iFood")
        "Rappi" -> listOf("Rappi")
        "99" -> listOf("99")
        "Uber" -> listOf("Uber")
        else -> listOf("iFood", "Rappi", "99", "iFood", "iFood", "Rappi", "99", "iFood")
    }

    val baseEarnings = listOf(28.50, 42.00, 36.80, 54.20, 48.00, 62.50, 39.00, 45.30)
    val deliveries = listOf(2, 3, 2, 4, 3, 4, 2, 3)
    val kms = listOf(7.2, 11.5, 9.4, 14.8, 12.0, 16.5, 8.9, 10.4)

    for (i in 7 downTo 0) {
        var h = currentHour - i
        if (h < 0) h += 24
        val hourLabel = String.format("%02d:00", h)

        val idx = 7 - i
        val app = if (appFilter == "TODOS") appDistribution[idx % appDistribution.size] else appFilter
        val factor = if (appFilter == "TODOS") 1.0 else 0.75

        points.add(
            HourlyEarningsPoint(
                hourLabel = hourLabel,
                earnings = baseEarnings[idx] * factor,
                deliveriesCount = deliveries[idx],
                kmDriven = kms[idx],
                appLeading = app
            )
        )
    }

    return points
}
