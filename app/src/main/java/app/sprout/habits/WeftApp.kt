package app.sprout.habits

import app.sprout.habits.widget.WidgetConfig
import android.content.Context
import android.app.Application
import android.content.res.Configuration
import app.sprout.habits.data.SampleData
import app.sprout.habits.notify.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeftApp : Application() {
    private var lastUiMode = 0
    private var lastLocales = ""
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch(Dispatchers.IO) {
            // Sample data first: it only goes in while the first-open time isn't set yet.
            SampleData.seedOnFirstLaunch(container.repository, container.settings, container.strings)
            container.settings.markFirstOpen()
            // Drive backup (play, github): make sure the next scheduled backup is set.
            container.drive.ensureScheduled()
        }
        container.reminders.start(container.appScope)
        container.reminderNotifier.start(container.appScope)
        container.alarmRinger.start(container.appScope)
        container.widgets.start(container.appScope)
        lastUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        lastLocales = resources.configuration.locales.toLanguageTags()
    }

    /**
     * Widgets draw their rings and marks into bitmaps in light or dark colors, and take their text
     * from resources: after a dark-mode or language change (per-app language included) they are
     * redrawn, or they would keep the old look until the next log.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val uiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val locales = newConfig.locales.toLanguageTags()
        if (uiMode != lastUiMode || locales != lastLocales) {
            lastUiMode = uiMode
            lastLocales = locales
            WidgetConfig.current.value = Configuration(newConfig)
            container.appScope.launch { container.widgets.updateAll() }
        }
    }
}
