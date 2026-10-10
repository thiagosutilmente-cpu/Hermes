package com.example

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// PALETA DE CORES DE ALTO CONTRASTE (USO EXTERNO / SOB A LUZ SOLAR DIRETA)
// =============================================================================
// Projetada especificamente para entregadores em trânsito com celular no guidão.
// Sob sol forte e reflexos no capacete/visor, cores com baixo contraste ficam ilegíveis.
// Esta paleta utiliza contraste WCAG AAA (>7:1 até 18:1), pretos puros e brancos nítidos.

// Cores Base de Alto Contraste - Modo Sol (Daylight Sunlight Mode)
val SunlightPureWhite = Color(0xFFFFFFFF)
val SunlightOffWhite = Color(0xFFF4F6F8)
val SunlightCardSurface = Color(0xFFFFFFFF)
val SunlightTextPrimary = Color(0xFF000000)      // Preto absoluto para máxima nitidez sob luz solar
val SunlightTextSecondary = Color(0xFF1F2937)    // Cinza escuro de alto contraste (> 9:1)
val SunlightTextMuted = Color(0xFF374151)        // Legível mesmo com reflexo no vidro

// Cores de Ação e Segurança - Modo Sol
val SunlightEmeraldGreen = Color(0xFF007A3D)    // Verde escuro saturado legível no branco puro
val SunlightGreenContainer = Color(0xFFD1FADF)  // Fundo de destaque verde suave com borda escura
val SunlightAmberWarning = Color(0xFFB45309)    // Âmbar denso para alertas diurnos
val SunlightRedAlert = Color(0xFFC81E1E)        // Vermelho intenso para recusas/avisos críticos
val SunlightBorderHeavy = Color(0xFF111827)     // Borda escura destacada para delimitar cards sob claridade

// Cores de Alto Contraste - Modo Cockpit Noturno / OLED (Pitch Black Night Mode)
// Projetadas para telas AMOLED/OLED: pixels pretos puros (#000000) são completamente desligados,
// garantindo até 60% de economia de bateria na moto e zero reflexo ofuscante na viseira à noite.
val CockpitOledBlack = Color(0xFF000000)        // Preto absoluto (#000000) - Pixels desligados
val CockpitSurface = Color(0xFF050508)          // Fundo quase preto para preservar economia OLED
val CockpitSurfaceElevated = Color(0xFF0D0D14)  // Card escuro com consumo mínimo de corrente
val CockpitNeonGreen = Color(0xFF00FF88)        // Verde luminescente para leitura instantânea em alta velocidade
val CockpitNeonGreenDark = Color(0xFF00B35F)
val CockpitNeonCyan = Color(0xFF00F0FF)         // Ciano puro para rotas e telemetria noturna
val CockpitTextPrimary = Color(0xFFFFFFFF)      // Branco 100% puro para contraste máximo (> 21:1)
val CockpitTextSecondary = Color(0xFFD1D5DB)    // Cinza claro de alta nitidez
val CockpitBorderNeon = Color(0xFF00FF88)
val CockpitBorderSubtle = Color(0xFF1E1E2E)     // Borda sutil para contenção visual sem consumir energia
val CockpitOledCardBg = Color(0xFF000000)       // Fundo de card 100% preto para economia de bateria extrema

// Identidade dos Aplicativos de Entrega (Ajustados para Alto Contraste)
val HighContrastIFoodRed = Color(0xFFEA1D2C)
val HighContrastRappiOrange = Color(0xFFFF441F)
val HighContrastUberBlack = Color(0xFF000000)
val HighContrastUberWhite = Color(0xFFFFFFFF)
val HighContrast99Yellow = Color(0xFFFFB800)

// -----------------------------------------------------------------------------
// ESQUEMA MATERIAL 3 - MODO SOL (ALTO CONTRASTE PARA LUZ SOLAR DIRETA)
// -----------------------------------------------------------------------------
val SunlightHighContrastColorScheme: ColorScheme = lightColorScheme(
    primary = SunlightEmeraldGreen,
    onPrimary = SunlightPureWhite,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF003915),
    secondary = Color(0xFF1A56DB),
    onSecondary = SunlightPureWhite,
    secondaryContainer = Color(0xFFE1EFFE),
    onSecondaryContainer = Color(0xFF1E429F),
    tertiary = SunlightAmberWarning,
    onTertiary = SunlightPureWhite,
    background = SunlightOffWhite,
    onBackground = SunlightTextPrimary,
    surface = SunlightCardSurface,
    onSurface = SunlightTextPrimary,
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = SunlightTextSecondary,
    outline = SunlightBorderHeavy,
    outlineVariant = Color(0xFF4B5563),
    error = SunlightRedAlert,
    onError = SunlightPureWhite,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF9B1C1C)
)

// -----------------------------------------------------------------------------
// ESQUEMA MATERIAL 3 - MODO COCKPIT NOTURNO (ALTO CONTRASTE OLED)
// -----------------------------------------------------------------------------
val CockpitHighContrastColorScheme: ColorScheme = darkColorScheme(
    primary = CockpitNeonGreen,
    onPrimary = CockpitOledBlack,
    primaryContainer = CockpitNeonGreenDark,
    onPrimaryContainer = CockpitTextPrimary,
    secondary = Color(0xFF60A5FA),
    onSecondary = CockpitOledBlack,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFFFFD700),
    onTertiary = CockpitOledBlack,
    background = CockpitOledBlack,
    onBackground = CockpitTextPrimary,
    surface = CockpitSurface,
    onSurface = CockpitTextPrimary,
    surfaceVariant = CockpitSurfaceElevated,
    onSurfaceVariant = CockpitTextSecondary,
    outline = CockpitBorderNeon,
    outlineVariant = CockpitBorderSubtle,
    error = Color(0xFFFF3366),
    onError = CockpitOledBlack,
    errorContainer = Color(0xFF4A0018),
    onErrorContainer = Color(0xFFFFD1DC)
)

// =============================================================================
// TOKENS SEMÂNTICOS PERSONALIZADOS PARA ENTREGADORES
// =============================================================================
@Immutable
data class DeliverySemanticColors(
    val profitHigh: Color,
    val profitMedium: Color,
    val profitLow: Color,
    val ifoodBadge: Color,
    val rappiBadge: Color,
    val uberBadge: Color,
    val app99Badge: Color,
    val hudBorderHighContrast: Color,
    val cardBackgroundElevated: Color,
    val quickActionAccept: Color,
    val quickActionDecline: Color
)

val SunlightDeliveryColors = DeliverySemanticColors(
    profitHigh = Color(0xFF007A3D),
    profitMedium = Color(0xFFB45309),
    profitLow = Color(0xFFC81E1E),
    ifoodBadge = HighContrastIFoodRed,
    rappiBadge = HighContrastRappiOrange,
    uberBadge = HighContrastUberBlack,
    app99Badge = Color(0xFFD97706),
    hudBorderHighContrast = SunlightBorderHeavy,
    cardBackgroundElevated = SunlightPureWhite,
    quickActionAccept = Color(0xFF007A3D),
    quickActionDecline = Color(0xFFC81E1E)
)

val CockpitDeliveryColors = DeliverySemanticColors(
    profitHigh = CockpitNeonGreen,
    profitMedium = Color(0xFFFFB800),
    profitLow = Color(0xFFFF3366),
    ifoodBadge = HighContrastIFoodRed,
    rappiBadge = HighContrastRappiOrange,
    uberBadge = HighContrastUberWhite,
    app99Badge = HighContrast99Yellow,
    hudBorderHighContrast = CockpitNeonGreen,
    cardBackgroundElevated = CockpitSurfaceElevated,
    quickActionAccept = CockpitNeonGreen,
    quickActionDecline = Color(0xFFFF3366)
)

val LocalDeliveryColors = staticCompositionLocalOf { CockpitDeliveryColors }

// =============================================================================
// TIPOGRAFIA MATERIAL 3 (PESOS BOLD E CONTRASTE REFORÇADO PARA VISIBILIDADE)
// =============================================================================
val DeliveryHighContrastTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // Negrito reforçado para leitura rápida
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

// =============================================================================
// FORMAS (BORDAS BEM DEFINIDAS PARA FACILITAR TOQUE COM LUVAS DE MOTO)
// =============================================================================
val DeliveryHighContrastShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// =============================================================================
// TEMA COMPOSABLE PRINCIPAL: RADAR HIGH CONTRAST THEME
// =============================================================================
/**
 * Tema Material 3 otimizado para entregadores de aplicativo.
 *
 * @param sunlightMode Se ativado, utiliza a paleta de alto contraste para luz solar direta
 *                     (fundo ultra claro, textos e bordas em preto absoluto).
 *                     Se desativado, utiliza o modo cockpit noturno (preto OLED + verde neon).
 * @param darkTheme Se sunlightMode for false, permite alternar entre modo claro ou escuro padrão.
 */
@Composable
fun DeliveryHighContrastTheme(
    sunlightMode: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        sunlightMode -> SunlightHighContrastColorScheme
        darkTheme -> CockpitHighContrastColorScheme
        else -> SunlightHighContrastColorScheme
    }

    val semanticColors = if (sunlightMode) SunlightDeliveryColors else CockpitDeliveryColors

    CompositionLocalProvider(
        LocalDeliveryColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DeliveryHighContrastTypography,
            shapes = DeliveryHighContrastShapes,
            content = content
        )
    }
}

// =============================================================================
// MODOS DE PREFERÊNCIA DE TEMA & DETECÇÃO AUTOMÁTICA NOTURNA
// =============================================================================
enum class ThemeModePreference(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: String
) {
    AUTO_INTELLIGENT(
        id = "auto_intelligent",
        title = "Auto Inteligente",
        subtitle = "Horário (18h-06h) + Sistema",
        description = "Ativa o tema escuro automaticamente entre 18:00 e 06:00 OU quando o Android estiver em tema escuro.",
        icon = "⚡"
    ),
    AUTO_SCHEDULE(
        id = "auto_schedule",
        title = "Horário do Turno",
        subtitle = "Programado no Relógio",
        description = "Ativa o Modo Cockpit Noturno no período de entregas noturnas (padrão: 18:00 às 06:00).",
        icon = "⏰"
    ),
    AUTO_SYSTEM(
        id = "auto_system",
        title = "Seguir Sistema",
        subtitle = "Configuração do Android",
        description = "Sincroniza automaticamente com o modo claro ou escuro configurado no sistema operacional.",
        icon = "📱"
    ),
    MANUAL_DARK(
        id = "manual_dark",
        title = "Noturno Fixo",
        subtitle = "Preto OLED Anti-Reflexo",
        description = "Mantém a interface sempre no modo escuro para zero ofuscamento e máxima economia de bateria.",
        icon = "🌙"
    ),
    MANUAL_LIGHT(
        id = "manual_light",
        title = "Modo Sol Fixo",
        subtitle = "Alto Contraste WCAG AAA",
        description = "Mantém a interface sempre no modo ultra-claro para leitura sob luz solar direta.",
        icon = "☀️"
    );

    companion object {
        fun fromId(id: String?): ThemeModePreference {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO_INTELLIGENT
        }
    }
}

// =============================================================================
// GERENCIADOR DE PREFERÊNCIAS DE TEMA & DETECÇÃO AUTOMÁTICA NOTURNA
// =============================================================================
object ThemePreferencesManager {
    private const val PREFS_NAME = "radar_theme_preferences"
    private const val KEY_NIGHT_MODE = "radar_night_mode_active"
    private const val KEY_THEME_MODE = "radar_theme_mode_preference"
    private const val KEY_NIGHT_START_HOUR = "radar_night_start_hour"
    private const val KEY_NIGHT_END_HOUR = "radar_night_end_hour"
    private const val KEY_EYE_FATIGUE_ENABLED = "radar_eye_fatigue_reduction_enabled"

    fun getThemeMode(context: Context): ThemeModePreference {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_THEME_MODE, ThemeModePreference.AUTO_INTELLIGENT.id)
        return ThemeModePreference.fromId(id)
    }

    fun setThemeMode(context: Context, mode: ThemeModePreference) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
    }

    fun getNightStartHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_START_HOUR, 18) // 18:00
    }

    fun setNightStartHour(context: Context, hour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_START_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun getNightEndHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_END_HOUR, 6) // 06:00
    }

    fun setNightEndHour(context: Context, hour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_END_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun isEyeFatigueProtectionEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_EYE_FATIGUE_ENABLED, true)
    }

    fun setEyeFatigueProtectionEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EYE_FATIGUE_ENABLED, enabled).apply()
    }

    /**
     * Determina se o momento atual se encontra dentro do intervalo de horário noturno.
     */
    fun isNightTimeNow(startHour: Int = 18, endHour: Int = 6): Boolean {
        val cal = java.util.Calendar.getInstance()
        val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(java.util.Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60
        val endMinutes = endHour * 60

        return if (startMinutes > endMinutes) {
            // Turno noturno atravessa meia-noite (ex: 18h às 06h)
            currentTimeMinutes >= startMinutes || currentTimeMinutes < endMinutes
        } else {
            // Turno no mesmo dia
            currentTimeMinutes in startMinutes until endMinutes
        }
    }

    /**
     * Resolve em tempo real se o tema deve ser escuro (Modo Cockpit Noturno)
     * avaliando a preferência do entregador, o horário solar do relógio e o sistema Android.
     */
    fun resolveEffectiveNightMode(context: Context, isSystemDark: Boolean): Boolean {
        val mode = getThemeMode(context)
        val startHour = getNightStartHour(context)
        val endHour = getNightEndHour(context)
        val isNightBySchedule = isNightTimeNow(startHour, endHour)

        return when (mode) {
            ThemeModePreference.MANUAL_DARK -> true
            ThemeModePreference.MANUAL_LIGHT -> false
            ThemeModePreference.AUTO_SYSTEM -> isSystemDark
            ThemeModePreference.AUTO_SCHEDULE -> isNightBySchedule
            ThemeModePreference.AUTO_INTELLIGENT -> isNightBySchedule || isSystemDark
        }
    }

    /**
     * Descrição amigável do status de detecção para feedback visual ao entregador.
     */
    fun getThemeStatusDescription(context: Context, isSystemDark: Boolean): String {
        val mode = getThemeMode(context)
        val isEffectiveNight = resolveEffectiveNightMode(context, isSystemDark)
        val startHour = getNightStartHour(context)
        val endHour = getNightEndHour(context)
        val isNightBySchedule = isNightTimeNow(startHour, endHour)

        return when (mode) {
            ThemeModePreference.AUTO_INTELLIGENT -> {
                if (isEffectiveNight) {
                    val trigger = if (isNightBySchedule) "Horário Noturno ($startHour:00 às 0$endHour:00)" else "Sistema Android"
                    "🌙 Cockpit Noturno Ativo: Detectado via $trigger. Zero reflexo na viseira e anti-fadiga ocular."
                } else {
                    "☀️ Modo Sol Ativo: Período diurno detectado. Alto contraste WCAG AAA para luz solar direta."
                }
            }
            ThemeModePreference.AUTO_SCHEDULE -> {
                if (isEffectiveNight) {
                    "🌙 Cockpit Noturno Ativo: Turno noturno programado ($startHour:00 às 0$endHour:00)."
                } else {
                    "☀️ Modo Sol Ativo: Fora do turno noturno ($startHour:00 às 0$endHour:00)."
                }
            }
            ThemeModePreference.AUTO_SYSTEM -> {
                if (isEffectiveNight) {
                    "🌙 Cockpit Noturno Ativo: Sincronizado com o tema escuro do Android."
                } else {
                    "☀️ Modo Sol Ativo: Sincronizado com o tema claro do Android."
                }
            }
            ThemeModePreference.MANUAL_DARK -> "🌙 Cockpit Noturno Fixo: Modo escuro OLED forçado permanentemente."
            ThemeModePreference.MANUAL_LIGHT -> "☀️ Modo Sol Fixo: Modo claro de alta claridade forçado permanentemente."
        }
    }

    /**
     * Retorna se o Modo Noturno está ativado (compatibilidade com chamadas legadas).
     */
    fun isNightMode(context: Context): Boolean {
        return resolveEffectiveNightMode(context, isNightTimeNow())
    }

    /**
     * Alternância rápida manual. Se o entregador tocar no botão de alternância rápida,
     * alterna entre MANUAL_DARK e MANUAL_LIGHT ou volta para AUTO.
     */
    fun setNightMode(context: Context, isNight: Boolean) {
        setThemeMode(context, if (isNight) ThemeModePreference.MANUAL_DARK else ThemeModePreference.MANUAL_LIGHT)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NIGHT_MODE, isNight).apply()
    }
}

