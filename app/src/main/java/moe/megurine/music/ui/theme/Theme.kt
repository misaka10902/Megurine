package moe.megurine.music.ui.theme

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import moe.megurine.music.ui.utils.ThemeModeKey
import moe.megurine.music.ui.utils.resolveDarkTheme
import moe.megurine.music.ui.utils.savedThemeMode
import moe.megurine.music.ui.utils.themePreferences
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private const val THEME_COLOR_KEY = "theme_color"

private const val THEME_COLOR_MIKU = "miku"

private const val THEME_COLOR_HYPEROS = "hyperos"

private const val THEME_COLOR_LUKA = "luka"

private const val THEME_COLOR_KAGAMINE = "kagamine"

private fun hyperOsLightColors(): Colors = lightColorScheme(
    primary = Color(0xFF3482FF),
    primaryVariant = Color(0xFF3482FF),
    disabledPrimary = Color(0xFFC2D9FF),
    disabledOnPrimary = Color(0xFFF3F8FF),
    disabledPrimaryButton = Color(0xFFC2D9FF),
    disabledOnPrimaryButton = Color(0xFFFFFFFF),
    disabledPrimarySlider = Color(0xFFB8CFF5),
    primaryContainer = Color(0xFF5D9BFF),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFFEAF2FF),
    onTertiaryContainer = Color(0xFF3482FF),
    sliderKeyPointForeground = Color(0xFF6EB5FF),
)

private fun hyperOsDarkColors(): Colors = darkColorScheme(
    primary = Color(0xFF277AF7),
    primaryVariant = Color(0xFF0073DD),
    disabledPrimary = Color(0xFF253E64),
    disabledOnPrimary = Color(0xFF677993),
    disabledPrimaryButton = Color(0xFF253E64),
    disabledOnPrimaryButton = Color(0xFF677893),
    disabledPrimarySlider = Color(0xFF44587C),
    primaryContainer = Color(0xFF338FE4),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFF2B3B54),
    onTertiaryContainer = Color(0xFF4788ff),
    sliderKeyPointForeground = Color(0xFF5DAAFF),
)

private fun mikuLightColors(): Colors = lightColorScheme(
    primary = Color(0xFF39C5BB),
    primaryVariant = Color(0xFF39C5BB),
    disabledPrimary = Color(0xFFB2E6E1),
    disabledOnPrimary = Color(0xFFE6F8F5),
    disabledPrimaryButton = Color(0xFFB2E6E1),
    disabledOnPrimaryButton = Color(0xFFFFFFFF),
    disabledPrimarySlider = Color(0xFF9BDAD3),
    primaryContainer = Color(0xFF5FD6CD),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFFE6F8F5),
    onTertiaryContainer = Color(0xFF39C5BB),
    sliderKeyPointForeground = Color(0xFF52CFC4),
)

private fun mikuDarkColors(): Colors = darkColorScheme(
    primary = Color(0xFF2FB8AE),
    primaryVariant = Color(0xFF1AA89E),
    disabledPrimary = Color(0xFF1F4A47),
    disabledOnPrimary = Color(0xFF6FA8A3),
    disabledPrimaryButton = Color(0xFF1F4A47),
    disabledOnPrimaryButton = Color(0xFF6FA8A3),
    disabledPrimarySlider = Color(0xFF2E5F5B),
    primaryContainer = Color(0xFF3AC4BA),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFF1F4A47),
    onTertiaryContainer = Color(0xFF52CFC4),
    sliderKeyPointForeground = Color(0xFF40C9BE),
)

private const val THEME_COLOR_LUKA_BASE = 0xFFFAAFBE

private const val THEME_COLOR_KAGAMINE_BASE = 0xFFFFA500

private fun accentLightColors(accent: Color): Colors = lightColorScheme(
    primary = accent,
    primaryVariant = accent,
    disabledPrimary = lerp(accent, Color.White, 0.70f),
    disabledOnPrimary = lerp(accent, Color.White, 0.90f),
    disabledPrimaryButton = lerp(accent, Color.White, 0.70f),
    disabledOnPrimaryButton = Color.White,
    disabledPrimarySlider = lerp(accent, Color.White, 0.55f),
    primaryContainer = lerp(accent, Color.White, 0.22f),
    onPrimaryContainer = Color.White,
    tertiaryContainer = lerp(accent, Color.White, 0.88f),
    onTertiaryContainer = accent,
    sliderKeyPointForeground = lerp(accent, Color.White, 0.14f),
)

private fun accentDarkColors(accent: Color): Colors = darkColorScheme(
    primary = accent,
    primaryVariant = lerp(accent, Color.Black, 0.16f),
    disabledPrimary = lerp(accent, Color.Black, 0.72f),
    disabledOnPrimary = lerp(accent, Color.Black, 0.45f),
    disabledPrimaryButton = lerp(accent, Color.Black, 0.72f),
    disabledOnPrimaryButton = lerp(accent, Color.Black, 0.45f),
    disabledPrimarySlider = lerp(accent, Color.Black, 0.62f),
    primaryContainer = lerp(accent, Color.Black, 0.10f),
    onPrimaryContainer = Color.White,
    tertiaryContainer = lerp(accent, Color.Black, 0.76f),
    onTertiaryContainer = lerp(accent, Color.White, 0.12f),
    sliderKeyPointForeground = lerp(accent, Color.White, 0.08f),
)

@Composable
fun HyperTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.themePreferences() }
    val themeMode = remember { mutableStateOf(prefs.savedThemeMode()) }
    val themeColor = remember {
        mutableStateOf(prefs.getString(THEME_COLOR_KEY, THEME_COLOR_HYPEROS) ?: THEME_COLOR_HYPEROS)
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ThemeModeKey -> {
                    themeMode.value = prefs.savedThemeMode()
                }
                THEME_COLOR_KEY -> {
                    themeColor.value = prefs.getString(THEME_COLOR_KEY, THEME_COLOR_HYPEROS)
                        ?: THEME_COLOR_HYPEROS
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val targetColors = remember(themeMode.value, themeColor.value, systemDark) {
        val dark = resolveDarkTheme(themeMode.value, systemDark)
        when (themeColor.value) {
            THEME_COLOR_MIKU -> if (dark) mikuDarkColors() else mikuLightColors()
            THEME_COLOR_LUKA -> {
                val accent = Color(THEME_COLOR_LUKA_BASE)
                if (dark) accentDarkColors(accent) else accentLightColors(accent)
            }
            THEME_COLOR_KAGAMINE -> {
                val accent = Color(THEME_COLOR_KAGAMINE_BASE)
                if (dark) accentDarkColors(accent) else accentLightColors(accent)
            }
            else -> if (dark) hyperOsDarkColors() else hyperOsLightColors()
        }
    }

    val themeKey = "${themeMode.value}|${themeColor.value}|${targetColors.primary.toArgb()}"
    var displayColors by remember { mutableStateOf(targetColors) }
    LaunchedEffect(themeKey) {
        val start = displayColors
        val target = targetColors
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) {
            displayColors = lerpColors(start, target, value)
        }
    }

    MiuixTheme(
        colors = displayColors,
        content = content
    )
}
