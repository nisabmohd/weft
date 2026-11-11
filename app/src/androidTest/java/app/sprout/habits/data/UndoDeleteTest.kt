package app.sprout.habits.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Deleting habits (Delete habit, Remove sample data) and Undo putting them back exactly. */
@RunWith(AndroidJUnit4::class)
class UndoDeleteTest {
    private lateinit var db: WeftDatabase
    private lateinit var repository: HabitRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WeftDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = HabitRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun undoBringsBackTheHabitWithItsDaysAndNotes() = runBlocking {
        val read = repository.getHabit(repository.saveHabit(Habit(name = "Read", icon = "book", colorHue = 215)))!!
        val walk = repository.getHabit(repository.saveHabit(Habit(name = "Walk", icon = "walk", colorHue = 150)))!!
        repository.setEntry(Entry(read.id, 100, EntryStatus.DONE, loggedAt = 5))
        repository.setEntry(Entry(read.id, 101, EntryStatus.PARTIAL, 0.5, loggedAt = 6))
        repository.setEntry(Entry(walk.id, 100, EntryStatus.SKIP, loggedAt = 7))
        val noteId = repository.saveNote(Note(habitId = read.id, date = 101, text = "Half a chapter"))
        val note = repository.getNote(noteId)!!

        val removed = repository.deleteHabits(listOf(read))
        assertNull(repository.getHabit(read.id))
        assertNull(repository.getNote(noteId))
        assertEquals(listOf("Walk"), repository.observeHabits().first().map { it.name })

        repository.restore(removed)
        assertEquals(read, repository.getHabit(read.id))
        assertEquals(Entry(read.id, 100, EntryStatus.DONE, loggedAt = 5), repository.getEntry(read.id, 100))
        assertEquals(Entry(read.id, 101, EntryStatus.PARTIAL, 0.5, loggedAt = 6), repository.getEntry(read.id, 101))
        assertEquals(note, repository.getNote(noteId))
        // The other habit was never touched.
        assertEquals(Entry(walk.id, 100, EntryStatus.SKIP, loggedAt = 7), repository.getEntry(walk.id, 100))
    }

    @Test fun deletingSeveralAtOnceRestoresThemAllInTheirOrder() = runBlocking {
        val ids = listOf("Wake", "Read", "Water").map { repository.saveHabit(Habit(name = it, icon = "leaf", colorHue = 150)) }
        val habits = ids.map { repository.getHabit(it)!! }
        val removed = repository.deleteHabits(habits.take(2))
        assertEquals(listOf("Water"), repository.observeHabits().first().map { it.name })
        repository.restore(removed)
        assertEquals(listOf("Wake", "Read", "Water"), repository.observeHabits().first().map { it.name })
    }

    @Test fun archiveAndItsUndo() = runBlocking {
        val id = repository.saveHabit(Habit(name = "Read", icon = "book", colorHue = 215))
        repository.setArchived(id, true)
        assertTrue(repository.observeHabits().first().isEmpty())
        repository.setArchived(id, false)
        assertEquals(listOf("Read"), repository.observeHabits().first().map { it.name })
    }
}
