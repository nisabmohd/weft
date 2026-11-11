package app.sprout.habits.ui

import app.sprout.habits.ui.components.PendingUndo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "… · Undo" message a change leaves for the next screen. */
class PendingUndoTest {
    private var clock = 0L
    private val pending = PendingUndo(CoroutineScope(Dispatchers.Unconfined)) { clock }

    @Test fun oneScreenTakesAMessageOnce() {
        pending.offer("Read archived")
        val item = pending.item.value!!
        assertTrue(pending.take(item))
        assertFalse(pending.take(item))
        assertNull(pending.item.value)
    }

    @Test fun aNewerMessageReplacesAnOlderOne() {
        pending.offer("Read archived")
        val old = pending.item.value!!
        pending.offer("Read restored")
        assertFalse(pending.take(old))
        assertEquals("Read restored", pending.item.value!!.message)
    }

    @Test fun aMessageNobodyShowedInTimeIsDropped() {
        pending.offer("Read deleted")
        val item = pending.item.value!!
        clock += 6_000
        assertFalse(pending.take(item))
    }

    @Test fun theClosingScreenIsNamed() {
        pending.offer("Read archived", notOn = "detail")
        assertEquals("detail", pending.item.value!!.notOn)
    }

    @Test fun undoRunsTheChangeBack() {
        var archived = true
        pending.offer("Read archived") { archived = false }
        val item = pending.item.value!!
        pending.take(item)
        pending.undo(item)
        assertFalse(archived)
    }

    @Test fun aMessageWithoutUndoDoesNothingOnUndo() {
        pending.offer("Disconnected from Google Drive")
        val item = pending.item.value!!
        assertNull(item.undo)
        pending.undo(item)
    }
}
