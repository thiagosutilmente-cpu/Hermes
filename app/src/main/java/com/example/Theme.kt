package com.example

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
val CockpitOledBlack = Color(0xFF000000)        // Preto absoluto para evitar reflexos noturnos
val CockpitSurface = Color(0xFF0A0A0F)          // Superfície quase preta
val CockpitSurfaceElevated = Color(0xFF13131D)  // Superfície de card elevada
val CockpitNeonGreen = Color(0xFF00FF88)        // Verde luminescente para leitura instantânea
val CockpitNeonGreenDark = Color(0xFF00B35F)
val CockpitTextPrimary = Color(0xFFFFFFFF)      // Branco 100% puro
val CockpitTextSecondary = Color(0xFFCCCCCC)
val CockpitBorderNeon = Color(0xFF00FF88)
val CockpitBorderSubtle = Color(0xFF2A2A3E)

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
