package app.sprout.habits.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.sprout.habits.R
import app.sprout.habits.ui.components.ComponentTags
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.InfoRow
import app.sprout.habits.ui.components.MarkKind
import app.sprout.habits.ui.components.NoteCard
import app.sprout.habits.ui.components.NoteCardUi
import app.sprout.habits.ui.theme.WeftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Design 2026-11-11: the info row and the compact note card. */
@RunWith(AndroidJUnit4::class)
class CompactRowsTest {
    @get:Rule val rule = createComposeRule()

    private val read = NoteCardUi(
        id = 1, day = 0, habitName = "Read", icon = R.drawable.ic_sym_menu_book, hue = 215f,
        mark = DayMark(MarkKind.PARTIAL, 0.4f), outcome = "8 of 20 pages", time = "11:40 PM",
        text = "Only 8 pages, fell asleep.",
    )

    @Test fun infoRowShowsTitleLineAndItsButtonWorks() {
        var removed = 0
        rule.setContent {
            WeftTheme {
                InfoRow(R.drawable.ic_test_tube, "Sample habits", "Swipe or hold to try them", Modifier.testTag("row"), button = "Remove", onButton = { removed++ })
            }
        }
        rule.onNodeWithText("Sample habits").assertIsDisplayed()
        rule.onNodeWithText("Swipe or hold to try them").assertIsDisplayed()
        rule.onNodeWithTag("row").assertHeightIsAtLeast(64.dp)
        rule.onNodeWithText("Remove").performClick()
        assertEquals(1, removed)
    }

    @Test fun infoRowWithoutAButton() {
        rule.setContent { WeftTheme { InfoRow(R.drawable.ic_code, "Free and open source", "No ads · no tracking · GPL v3") } }
        rule.onNodeWithText("Free and open source").assertIsDisplayed()
    }

    @Test fun noteCardReadsNameOutcomeNoteAndTime() {
        rule.setContent { WeftTheme { NoteCard(read, onOpen = {}) } }
        // The habit row (name, outcome · time) and the note, read as one item.
        rule.onNodeWithContentDescription("Read, 8 of 20 pages, 11:40 PM. Only 8 pages, fell asleep.", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag(ComponentTags.HABIT_TILE, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun aLateNoteSaysWhenItWasWritten() {
        rule.setContent { WeftTheme { NoteCard(read.copy(mark = DayMark(MarkKind.SKIP), outcome = "Skipped", time = "written Tue, 12:23 AM"), onOpen = {}) } }
        rule.onNodeWithContentDescription("Read, Skipped, written Tue, 12:23 AM. Only 8 pages, fell asleep.", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun noteCardOnAHabitsPageHasNoTileAndNamesTheDay() {
        rule.setContent { WeftTheme { NoteCard(read.copy(mark = DayMark(MarkKind.SKIP), outcome = "Skipped"), onOpen = null, showHabit = false, dateLabel = "Yesterday") } }
        rule.onNodeWithContentDescription("Skipped, Yesterday, 11:40 PM. Only 8 pages, fell asleep.", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag(ComponentTags.HABIT_TILE, useUnmergedTree = true).assertDoesNotExist()
    }
}
