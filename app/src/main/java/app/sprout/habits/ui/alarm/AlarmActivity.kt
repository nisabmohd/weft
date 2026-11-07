package app.sprout.habits.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.sprout.habits.R
import app.sprout.habits.WeftApp
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.notify.ReminderActionReceiver
import app.sprout.habits.notify.ReminderActions
import app.sprout.habits.notify.ReminderScheduler
import app.sprout.habits.ui.components.ButtonPair
import app.sprout.habits.ui.components.HabitTile
import app.sprout.habits.ui.components.SheetSidePadding
import app.sprout.habits.ui.drawEdgeToEdge
import app.sprout.habits.ui.theme.WeftTheme
import app.sprout.habits.ui.theme.WeftType
import app.sprout.habits.ui.theme.habitColors
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The alarm ringing screen (`design/alarm-3-ringing.png`): opened full screen by an alarm
 * reminder, over the lock screen. Slide to mark done, Snooze 10 min or Dismiss; the power or a
 * volume button silences it like Dismiss. Every answer goes through [ReminderActionReceiver], so it
 * logs and stops the sound exactly like the notification's buttons.
 */
class AlarmActivity : ComponentActivity() {
    private var habitId = 0L
    private var answered = false

    /** The power button turns the screen off: that silences the alarm, like Dismiss. */
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = answer(ReminderActions.ACTION_DISMISS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        drawEdgeToEdge(dark = false)
        habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        val container = (application as WeftApp).container
        // Back doesn't close a ringing alarm; one of its three answers does.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = Unit })
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val ringing by container.alarmRinger.ringing.collectAsStateWithLifecycle()
            var habit by remember { mutableStateOf<Habit?>(null) }
            var seenRinging by remember { mutableStateOf(false) }
            LaunchedEffect(habitId) { habit = container.repository.getHabit(habitId) }
            // Stopped elsewhere (a notification button, the 10-minute limit): close.
            LaunchedEffect(ringing) {
                if (ringing?.habitId == habitId) seenRinging = true else if (seenRinging) finish()
            }
            val current = settings ?: return@setContent
            val h = habit ?: return@setContent
            val minutes = ringing?.takeIf { it.habitId == habitId }?.minutes ?: LocalTime.now().let { it.hour * 60 + it.minute }
            WeftTheme(current.theme) {
                AlarmScreen(
                    habit = h,
                    time = clockTime(this@AlarmActivity, minutes),
                    onDone = { answer(ReminderActions.ACTION_DONE) },
                    onSnooze = { answer(ReminderActions.ACTION_SNOOZE) },
                    onDismiss = { answer(ReminderActions.ACTION_DISMISS) },
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            answer(ReminderActions.ACTION_DISMISS)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        unregisterReceiver(screenOff)
        super.onDestroy()
    }

    private fun answer(action: String) {
        if (answered) return
        answered = true
        // Stop the sound now; the receiver logs (Done), reschedules (Snooze) and refreshes widgets.
        (application as WeftApp).container.alarmRinger.stop(habitId)
        sendBroadcast(
            Intent(this, ReminderActionReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habitId)
                .putExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay()),
        )
        lifecycleScope.launch { finish() }
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val EXTRA_HABIT_ID = "habitId"

        fun intent(context: Context, habitId: Long): Intent = Intent(context, AlarmActivity::class.java)
            .putExtra(EXTRA_HABIT_ID, habitId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

/** "6:30" (or "18:30" on a 24-hour phone): the big time, without AM / PM. */
private fun clockTime(context: Context, minutes: Int): String {
    val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
    return LocalTime.of(minutes / 60, minutes % 60).format(java.time.format.DateTimeFormatter.ofPattern(pattern))
}

/** Size of the habit tile on the ringing screen, and its corners (about 0.3 × size, like every tile). */
private val AlarmTileSize = 64.dp
private val AlarmTileCorner = 20.dp
private val AlarmTileIconSize = 30.dp

/** The slide bar: 64 dp tall, with a thumb 8 dp inside it. */
private val SlideHeight = 64.dp
private val SlideInset = 8.dp

@Composable
private fun AlarmScreen(habit: Habit, time: String, onDone: () -> Unit, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val hc = habitColors(habit.colorHue.toFloat())
    Column(
        Modifier
            .fillMaxSize()
            .background(hc.soft)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = SheetSidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        HabitTile(hc.mid, Modifier.size(AlarmTileSize), RoundedCornerShape(AlarmTileCorner)) {
            Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(AlarmTileIconSize))
        }
        Text(habit.name, style = WeftType.title, color = hc.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        Text(time, style = WeftType.alarmTime, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(2f))
        SlideToDone(track = hc.mid, thumb = hc.solid, label = stringResource(R.string.alarm_slide), labelColor = hc.ink, onDone = onDone)
        Spacer(Modifier.height(16.dp))
        ButtonPair(
            secondaryLabel = stringResource(R.string.notify_snooze_minutes, ReminderActions.SNOOZE_MINUTES),
            onSecondary = onSnooze,
            primaryLabel = stringResource(R.string.alarm_dismiss),
            onPrimary = onDismiss,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** A pill track with a round thumb; dragging the thumb to the end marks the habit done. */
@Composable
private fun SlideToDone(track: Color, thumb: Color, label: String, labelColor: Color, onDone: () -> Unit) {
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(SlideHeight)
            .background(track, CircleShape)
            // TalkBack: a double tap marks it done, as sliding does.
            .semantics { contentDescription = label; onClick(label) { onDone(); true } },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbSize = SlideHeight - SlideInset * 2
        val max = with(density) { (maxWidth - thumbSize - SlideInset * 2).toPx() }
        var offset by remember { mutableFloatStateOf(0f) }
        var done by remember { mutableStateOf(false) }
        Text(label, style = WeftType.cardTitle, color = labelColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = thumbSize))
        Box(
            Modifier
                .padding(start = SlideInset)
                .offset { IntOffset(offset.roundToInt(), 0) }
                .size(thumbSize)
                .background(thumb, CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> if (!done) offset = (offset + delta).coerceIn(0f, max) },
                    onDragStopped = {
                        if (offset >= max * 0.9f) { done = true; offset = max; onDone() } else offset = 0f
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
