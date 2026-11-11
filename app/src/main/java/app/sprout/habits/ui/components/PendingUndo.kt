package app.sprout.habits.ui.components

import android.os.SystemClock
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import app.sprout.habits.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The message after a change made anywhere ("Read archived · Undo"), shown by the next screen
 * with a [PendingUndoHost], which may be the screen the change sends the user back to. Undo runs
 * in the app's scope, so it still works after the screen that made the change has closed.
 */
class PendingUndo(
    private val scope: CoroutineScope,
    /** Milliseconds since boot; replaced in tests. */
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    class Item(
        val message: String,
        /** Null: a message without Undo. */
        val undo: (suspend () -> Unit)?,
        /** The host that must not show it: the screen that is closing as it offers it. */
        val notOn: String?,
        val at: Long,
    )

    private val _item = MutableStateFlow<Item?>(null)
    val item: StateFlow<Item?> = _item

    fun offer(message: String, notOn: String? = null, undo: (suspend () -> Unit)? = null) {
        _item.value = Item(message, undo, notOn, now())
    }

    /** Takes [item] for one host; false if another took it or it's too old to still make sense. */
    fun take(item: Item): Boolean =
        _item.compareAndSet(item, null) && now() - item.at < MAX_AGE_MS

    fun undo(item: Item) {
        item.undo?.let { scope.launch { it() } }
    }

    private companion object {
        const val MAX_AGE_MS = 5_000L
    }
}

val LocalPendingUndo = staticCompositionLocalOf<PendingUndo> { error("No PendingUndo") }

/** Shows [PendingUndo] messages in this screen's Undo snackbar while the screen is on top. */
@Composable
fun PendingUndoHost(snackbar: SnackbarHostState, name: String? = null) {
    val pending = LocalPendingUndo.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val undoLabel = stringResource(R.string.action_undo)
    // One collector for the screen's life (taking a message clears the queue, which must not
    // restart it and cancel the snackbar it just opened); a newer message replaces the shown one.
    LaunchedEffect(pending, lifecycle) {
        pending.item.filterNotNull().flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collectLatest { shown ->
            if (name != null && shown.notOn == name) return@collectLatest
            if (!pending.take(shown)) return@collectLatest
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(shown.message, actionLabel = shown.undo?.let { undoLabel }, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) pending.undo(shown)
        }
    }
}
