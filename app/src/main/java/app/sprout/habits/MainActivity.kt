package app.sprout.habits

import app.sprout.habits.ui.update.UpdatePrompt
import app.sprout.habits.notify.Notifications
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.ui.WelcomeScreen
import app.sprout.habits.ui.drawEdgeToEdge
import app.sprout.habits.ui.nav.WeftNavHost
import app.sprout.habits.ui.theme.WeftTheme
import app.sprout.habits.ui.theme.isDark
import app.sprout.habits.ui.nav.AppLink
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Where an outside tap should land (widget, notification, shortcut, weft:// link); consumed by the nav host. */
    private val pendingLink = MutableStateFlow<AppLink?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After a language change the activity is recreated but view models survive, holding
        // text formatted in the old language ("Monday, 28 September"). Start them afresh.
        if (savedInstanceState != null && savedInstanceState.getString(KEY_LANGUAGE) != language()) viewModelStore.clear()
        // Channel names follow the app language, which may have just changed.
        Notifications.createChannels(this)
        if (savedInstanceState == null) handleIntent(intent)
        drawEdgeToEdge(dark = false)
        val container = (application as WeftApp).container
        setContent {
            // Null until DataStore has loaded, so the first real frame already has the right theme.
            // Until then draw an empty frame over the window background (light or night), so the
            // launch still produces a frame for the system and startup tooling.
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: run {
                Spacer(Modifier.fillMaxSize())
                return@setContent
            }
            val dark = current.theme.isDark()
            LaunchedEffect(dark) { drawEdgeToEdge(dark) }
            WeftTheme(current.theme) {
                if (!current.onboarded) {
                    WelcomeScreen(container, onDone = { container.settings.setOnboarded() })
                } else {
                    // Held until Get started is done, then opened.
                    val link by pendingLink.collectAsStateWithLifecycle()
                    androidx.compose.runtime.CompositionLocalProvider(
                        app.sprout.habits.ui.components.LocalPendingUndo provides container.pendingUndo,
                        app.sprout.habits.ui.LocalUndoableActions provides container.actions,
                    ) {
                        WeftNavHost(container, current, link = link, onLinkHandled = { pendingLink.value = null })
                    }
                    UpdatePrompt(container.updates, container.playUpdates)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LANGUAGE, language())
    }

    private fun language() = resources.configuration.locales.get(0).toLanguageTag()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        AppLink.from(intent)?.let { pendingLink.value = it }
    }

    companion object {
        const val ACTION_OPEN_HABIT = AppLink.ACTION_OPEN_HABIT
        const val EXTRA_HABIT_ID = AppLink.EXTRA_HABIT_ID
        private const val KEY_LANGUAGE = "language"
    }
}
