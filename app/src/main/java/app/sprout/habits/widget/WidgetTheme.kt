package app.sprout.habits.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.glance.material3.ColorProviders
import app.sprout.habits.MainActivity
import app.sprout.habits.WeftApp
import app.sprout.habits.ui.nav.AppLink
import app.sprout.habits.ui.theme.LOGO_HUE
import app.sprout.habits.ui.theme.seedColorScheme

/**
 * Widgets live on the home screen, so they follow the system: wallpaper colors on Android 12+
 * and the logo's blue below that. The in-app theme and accent only change the app.
 */
@Composable
fun WeftGlanceTheme(content: @Composable () -> Unit) {
    // Glance keeps a widget's session alive across updates, with the context it started with: after
    // a language change (in Weft or in the phone's settings) its text would stay in the old language
    // until the process restarts. Text is read from a context in the current configuration instead.
    val base = LocalContext.current
    val config by WidgetConfig.current.collectAsState()
    val context = remember(config) { config?.let { base.createConfigurationContext(it) } ?: base }
    CompositionLocalProvider(LocalContext provides context) { WeftGlanceColors(content) }
}

@Composable
private fun WeftGlanceColors(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceTheme.colors
        } else {
            ColorProviders(light = seedColorScheme(LOGO_HUE, false), dark = seedColorScheme(LOGO_HUE, true))
        },
        content = content,
    )
}

/**
 * The app's configuration after its last language or dark-mode change (`WeftApp.onConfigurationChanged`),
 * or null before any. Widgets redraw their text and reload their data when it changes.
 */
object WidgetConfig {
    val current = MutableStateFlow<Configuration?>(null)

    /**
     * Bumped by every `WidgetUpdater.updateAll()` (midnight, a clock or time zone change, a language
     * or dark-mode change). A widget's live session reloads its data on it: "today" may have moved
     * without any habit or entry changing.
     */
    val refresh = MutableStateFlow(0L)
}

val Context.container get() = (applicationContext as WeftApp).container

/**
 * Opens Weft on Today (the ring, the strip, the week's header). MainActivity is singleTop, so a
 * running app gets this in onNewIntent: without the link it would stay on whatever tab was open.
 */
fun Context.openAppIntent(): Intent = Intent(this, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    .setAction(Intent.ACTION_VIEW)
    .setData(android.net.Uri.parse("${AppLink.SCHEME}://today"))

fun Context.openHabitIntent(habitId: Long): Intent = openAppIntent()
    .setAction(MainActivity.ACTION_OPEN_HABIT)
    .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
    // Distinct data so each habit gets its own PendingIntent.
    .setData(android.net.Uri.parse("weft://habit/$habitId"))
