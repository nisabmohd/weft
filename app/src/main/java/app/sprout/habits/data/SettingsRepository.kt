package app.sprout.habits.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.DEFAULT_ACCENT_HUE
import app.sprout.habits.ui.theme.ThemeMode
import app.sprout.habits.ui.theme.ThemeSettings
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** How often Drive backup runs by itself (overnight, while charging). */
enum class DriveFrequency { DAILY, WEEKLY, MONTHLY, OFF }

/** The file "Export now" writes (foss build's Backup & restore → File format). */
enum class ExportFormat { JSON, CSV }

@Immutable
data class Settings(
    val theme: ThemeSettings = ThemeSettings(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    /** Minutes after midnight used as the default for new habit reminders. */
    val defaultReminderMinutes: Int = 9 * 60,
    /** The Google account Drive backup uses (play and github builds); null = not connected. */
    val driveAccount: String? = null,
    val driveFrequency: DriveFrequency = DriveFrequency.DAILY,
    /** Scheduled Drive backups wait for Wi-Fi (an unmetered network). */
    val driveWifiOnly: Boolean = true,
    /** Epoch millis of the last successful Drive backup, or null if never. */
    val lastBackupAt: Long? = null,
    /**
     * Why the last Drive backup (manual or scheduled) failed, a `DriveResult` name; null when it
     * worked. Backup & restore says so until one works.
     */
    val lastBackupError: String? = null,
    /** Backups in the connected Drive as last seen (after a backup or listing them); null = not known. */
    val driveBackupCount: Int? = null,
    /** Epoch millis of the last Export to file (JSON or CSV), or null if never. */
    val lastExportAt: Long? = null,
    val exportFormat: ExportFormat = ExportFormat.JSON,
    /** False until the welcome screen has been passed once. */
    val onboarded: Boolean = false,
    /** Epoch millis of the first launch; null until then. */
    val firstOpenAt: Long? = null,
    /** Days logged as done or partly done, counted for the support prompt. */
    val checkInCount: Int = 0,
    val supportPromptShownAt: Long? = null,
    val supportPromptDismissCount: Int = 0,
    /** A newer release found on GitHub (github build), for the "Update available" pill. */
    val availableUpdate: String? = null,
    /** Epoch millis of the last automatic update check. */
    val lastUpdateCheckAt: Long? = null,
    /** The release the user answered "Later" to; its dialog doesn't come back on its own. */
    val dismissedUpdate: String? = null,
    /** Ids of the sample habits a fresh install starts with; empty once removed or never added. */
    val sampleHabitIds: Set<Long> = emptySet(),
    /** True once the user logged or changed a habit: the Today banner goes, the sample habits stay. */
    val sampleBannerHidden: Boolean = false,
    /** Starter phrases, past notes and sentence completion in New note (More → Journal). */
    val writingSuggestions: Boolean = true,
    /** Epoch millis of "Clear suggestion history": notes last written before it aren't learned from. */
    val suggestionsClearedAt: Long = 0,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store: DataStore<Preferences> = context.dataStore

    val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setAccentHue(hue: Float) = store.edit { it[ACCENT_HUE] = hue }
    suspend fun setFont(font: BodyFont) = store.edit { it[FONT] = font.name }
    suspend fun setTextScale(scale: Float) = store.edit { it[TEXT_SCALE] = scale }
    suspend fun setWeekStart(day: DayOfWeek) = store.edit { it[WEEK_START] = day.name }
    suspend fun setDefaultReminderMinutes(minutes: Int) = store.edit { it[DEFAULT_REMINDER] = minutes }
    /** Connects (an email) or disconnects (null) Drive backup. */
    suspend fun setDriveAccount(email: String?) = store.edit {
        it.remove(LAST_BACKUP_ERROR)
        it.remove(DRIVE_BACKUP_COUNT)
        if (email != null) it[DRIVE_ACCOUNT] = email else it.remove(DRIVE_ACCOUNT)
    }
    suspend fun setDriveFrequency(frequency: DriveFrequency) = store.edit { it[DRIVE_FREQUENCY] = frequency.name }
    suspend fun setDriveWifiOnly(on: Boolean) = store.edit { it[DRIVE_WIFI_ONLY] = on }
    /** A Drive backup worked at [at], leaving [count] backups in Drive (null = not known). */
    suspend fun setBackupDone(at: Long, count: Int?) = store.edit {
        it[LAST_BACKUP_AT] = at
        it.remove(LAST_BACKUP_ERROR)
        if (count != null) it[DRIVE_BACKUP_COUNT] = count
    }
    /** A Drive backup failed; [reason] is a `DriveResult` name. */
    suspend fun setBackupFailed(reason: String) = store.edit { it[LAST_BACKUP_ERROR] = reason }
    suspend fun setDriveBackupCount(count: Int) = store.edit { it[DRIVE_BACKUP_COUNT] = count }
    suspend fun setExported(at: Long = System.currentTimeMillis()) = store.edit { it[LAST_EXPORT_AT] = at }
    suspend fun setExportFormat(format: ExportFormat) = store.edit { it[EXPORT_FORMAT] = format.name }
    suspend fun setOnboarded() = store.edit { it[ONBOARDED] = true }
    /** Remembers the newest release found (or null when up to date) and when the check ran. */
    suspend fun setUpdateCheck(available: String?, at: Long) = store.edit {
        if (available != null) it[AVAILABLE_UPDATE] = available else it.remove(AVAILABLE_UPDATE)
        it[LAST_UPDATE_CHECK] = at
    }
    suspend fun dismissUpdate(version: String) = store.edit { it[DISMISSED_UPDATE] = version }
    /**
     * The user logged or changed a habit: hide the sample banner for good. Does nothing before the
     * sample habits are in, so seeding them doesn't hide it.
     */
    suspend fun hideSampleBanner() {
        if (store.data.first().let { it[SAMPLE_HABITS] == null || it[SAMPLE_BANNER_HIDDEN] == true }) return
        store.edit { it[SAMPLE_BANNER_HIDDEN] = true }
    }

    suspend fun setSampleHabitIds(ids: Set<Long>) = store.edit {
        it.remove(SAMPLE_BANNER_HIDDEN)
        if (ids.isEmpty()) it.remove(SAMPLE_HABITS) else it[SAMPLE_HABITS] = ids.mapTo(HashSet()) { id -> id.toString() }
    }
    /**
     * Also keeps the look of installs from before 1.2: they followed the wallpaper with a green
     * fallback by default, and the new defaults (amber, dynamic color off) are for new installs.
     */
    suspend fun markFirstOpen(now: Long = System.currentTimeMillis()) = store.edit {
        if (it[FIRST_OPEN_AT] == null) {
            it[FIRST_OPEN_AT] = now
            // Written down so the next launch doesn't take this for an older install.
            if (it[DYNAMIC_COLOR] == null) it[DYNAMIC_COLOR] = false
            if (it[ACCENT_HUE] == null) it[ACCENT_HUE] = DEFAULT_ACCENT_HUE
        } else {
            if (it[DYNAMIC_COLOR] == null) it[DYNAMIC_COLOR] = true
            if (it[ACCENT_HUE] == null) it[ACCENT_HUE] = 150f
        }
    }
    suspend fun setWritingSuggestions(enabled: Boolean) = store.edit { it[WRITING_SUGGESTIONS] = enabled }
    suspend fun clearSuggestionHistory(now: Long = System.currentTimeMillis()) = store.edit { it[SUGGESTIONS_CLEARED_AT] = now }
    suspend fun addCheckIn() = store.edit { it[CHECK_INS] = (it[CHECK_INS] ?: 0) + 1 }
    suspend fun supportPromptShown(dismissed: Boolean, now: Long = System.currentTimeMillis()) = store.edit {
        it[PROMPT_SHOWN_AT] = now
        if (dismissed) it[PROMPT_DISMISSED] = (it[PROMPT_DISMISSED] ?: 0) + 1
    }

    /** Replaces the user's preferences in one write, e.g. when restoring a backup. */
    suspend fun restore(settings: Settings) = store.edit {
        it[THEME] = settings.theme.mode.name
        it[DYNAMIC_COLOR] = settings.theme.dynamicColor
        it[ACCENT_HUE] = settings.theme.accentHue
        it[FONT] = settings.theme.font.name
        it[TEXT_SCALE] = settings.theme.textScale
        it[WEEK_START] = settings.weekStart.name
        it[DEFAULT_REMINDER] = settings.defaultReminderMinutes
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        val theme = defaults.theme
        return Settings(
            theme = ThemeSettings(
                mode = enumOrDefault(this[THEME], theme.mode),
                dynamicColor = this[DYNAMIC_COLOR] ?: theme.dynamicColor,
                accentHue = this[ACCENT_HUE] ?: DEFAULT_ACCENT_HUE,
                font = enumOrDefault(this[FONT], theme.font),
                textScale = this[TEXT_SCALE] ?: theme.textScale,
            ),
            weekStart = enumOrDefault(this[WEEK_START], defaults.weekStart),
            defaultReminderMinutes = this[DEFAULT_REMINDER] ?: defaults.defaultReminderMinutes,
            driveAccount = this[DRIVE_ACCOUNT],
            driveFrequency = this[DRIVE_FREQUENCY]?.let { name -> DriveFrequency.entries.firstOrNull { it.name == name } } ?: defaults.driveFrequency,
            driveWifiOnly = this[DRIVE_WIFI_ONLY] ?: defaults.driveWifiOnly,
            lastBackupAt = this[LAST_BACKUP_AT],
            lastBackupError = this[LAST_BACKUP_ERROR],
            driveBackupCount = this[DRIVE_BACKUP_COUNT],
            lastExportAt = this[LAST_EXPORT_AT],
            exportFormat = enumOrDefault(this[EXPORT_FORMAT], defaults.exportFormat),
            onboarded = this[ONBOARDED] ?: false,
            firstOpenAt = this[FIRST_OPEN_AT],
            checkInCount = this[CHECK_INS] ?: 0,
            supportPromptShownAt = this[PROMPT_SHOWN_AT],
            supportPromptDismissCount = this[PROMPT_DISMISSED] ?: 0,
            availableUpdate = this[AVAILABLE_UPDATE],
            lastUpdateCheckAt = this[LAST_UPDATE_CHECK],
            dismissedUpdate = this[DISMISSED_UPDATE],
            sampleHabitIds = this[SAMPLE_HABITS].orEmpty().mapNotNullTo(HashSet()) { it.toLongOrNull() },
            sampleBannerHidden = this[SAMPLE_BANNER_HIDDEN] == true,
            writingSuggestions = this[WRITING_SUGGESTIONS] ?: defaults.writingSuggestions,
            suggestionsClearedAt = this[SUGGESTIONS_CLEARED_AT] ?: 0,
        )
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamicColor")
        val ACCENT_HUE = floatPreferencesKey("accentHue")
        val FONT = stringPreferencesKey("font")
        val TEXT_SCALE = floatPreferencesKey("textScale")
        val WEEK_START = stringPreferencesKey("weekStart")
        val DEFAULT_REMINDER = intPreferencesKey("defaultReminderMinutes")
        val DRIVE_ACCOUNT = stringPreferencesKey("driveAccount")
        val DRIVE_FREQUENCY = stringPreferencesKey("driveFrequency")
        val DRIVE_WIFI_ONLY = booleanPreferencesKey("driveWifiOnly")
        val LAST_BACKUP_AT = longPreferencesKey("lastBackupAt")
        val LAST_BACKUP_ERROR = stringPreferencesKey("lastBackupError")
        val DRIVE_BACKUP_COUNT = intPreferencesKey("driveBackupCount")
        val LAST_EXPORT_AT = longPreferencesKey("lastExportAt")
        val EXPORT_FORMAT = stringPreferencesKey("exportFormat")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val FIRST_OPEN_AT = longPreferencesKey("firstOpenAt")
        val CHECK_INS = intPreferencesKey("checkInCount")
        val PROMPT_SHOWN_AT = longPreferencesKey("supportPromptShownAt")
        val PROMPT_DISMISSED = intPreferencesKey("supportPromptDismissCount")
        val AVAILABLE_UPDATE = stringPreferencesKey("availableUpdateVersion")
        val LAST_UPDATE_CHECK = longPreferencesKey("lastUpdateCheckAt")
        val DISMISSED_UPDATE = stringPreferencesKey("dismissedUpdateVersion")
        val SAMPLE_HABITS = stringSetPreferencesKey("sampleHabitIds")
        val SAMPLE_BANNER_HIDDEN = booleanPreferencesKey("sampleBannerHidden")
        val WRITING_SUGGESTIONS = booleanPreferencesKey("writingSuggestions")
        val SUGGESTIONS_CLEARED_AT = longPreferencesKey("suggestionsClearedAt")

        inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
    }
}
