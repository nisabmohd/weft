package app.sprout.habits.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.update.AppUpdate
import app.sprout.habits.data.update.UpdateManager
import app.sprout.habits.ui.theme.WeftTheme
import app.sprout.habits.ui.update.UpdateDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Long release notes: Update and Later stay on screen, and every note can be scrolled to in full. */
@RunWith(AndroidJUnit4::class)
class UpdateDialogTest {
    @get:Rule val rule = createComposeRule()

    private val long = "Google Drive backup. ".repeat(20).trim()
    private val notes = List(5) { "$it $long END$it" }

    @Test fun longNotesScrollAndButtonsStayVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val updates = UpdateManager(context, SettingsRepository(context))
        val update = AppUpdate("9.9.9", notes, "https://example.com/a.apk", "a.apk", null, "https://example.com")
        rule.setContent { WeftTheme { UpdateDialog(update, updates, onLater = {}, onDismiss = {}) } }

        rule.onNodeWithText("Later").assertIsDisplayed()
        rule.onNodeWithText("Update").assertIsDisplayed()
        // The last note, shown whole (no "…"), can be scrolled into view.
        rule.onNodeWithText(notes.last()).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Later").assertIsDisplayed()
    }
}
