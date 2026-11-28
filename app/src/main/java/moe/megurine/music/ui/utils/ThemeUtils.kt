package moe.megurine.music.ui.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext



internal const val ThemePrefsFile = "app_theme_prefs"
internal const val ThemeModeKey = "theme_mode"
internal const val ThemeModeSystem = "system"
internal const val ThemeModeDark = "dark"
internal const val ThemeModeLight = "light"

internal fun Context.themePreferences(): SharedPreferences =
    getSharedPreferences(ThemePrefsFile, Context.MODE_PRIVATE)

internal fun SharedPreferences.savedThemeMode(): String =
    getString(ThemeModeKey, ThemeModeSystem) ?: ThemeModeSystem


internal fun resolveDarkTheme(themeMode: String, systemDark: Boolean): Boolean = when (themeMode) {
    ThemeModeDark -> true
    ThemeModeLight -> false
    else -> systemDark
}

private fun Context.systemDarkTheme(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES


@Composable
fun isAppDarkTheme(): Boolean = rememberAppSettingDark()

@Composable
fun rememberAppSettingDark(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.themePreferences() }
    var themeMode by remember(prefs) { mutableStateOf(prefs.savedThemeMode()) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, key: String? ->
            if (key == ThemeModeKey) themeMode = prefs.savedThemeMode()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return resolveDarkTheme(themeMode, isSystemInDarkTheme())
}


fun Activity.applyThemeAwareSystemBars() {
    applyThemeAwareSystemBars(resolveDarkTheme(themePreferences().savedThemeMode(), systemDarkTheme()))
}

fun Activity.applyThemeAwareSystemBars(isDark: Boolean) {
    applySystemBarIconMode(isDark, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
}

fun Activity.applyNavigationBarIsDark(isDark: Boolean) {
    applySystemBarIconMode(isDark, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
}


private fun Activity.applySystemBarIconMode(isDark: Boolean, mask: Int) {
    window.decorView.post {
        window.insetsController?.setSystemBarsAppearance(if (isDark) 0 else mask, mask)
    }
}
