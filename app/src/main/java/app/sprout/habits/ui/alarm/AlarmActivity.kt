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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
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
import app.sprout.habits.ui.today.LogSheet
import app.sprout.habits.ui.today.DayLogger
import app.sprout.habits.ui.components.ButtonHeight
import app.sprout.habits.domain.measure
import app.sprout.habits.data.TrackType
import app.sprout.habits.ResourceStrings
import app.sprout.habits.MainActivity
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import android.app.KeyguardManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.toArgb
import app.sprout.habits.ui.theme.ThemeMode

/**
 * The alarm ringing screen (`design/alarm-3-ringing.png`): opened full screen by an alarm
 * reminder, over the lock screen. Slide to mark done, Snooze 10 min or Dismiss; the power or a
 * volume button silences it like Dismiss. Every answer goes through [ReminderActionReceiver], so it
 * logs and stops the sound exactly like the notification's buttons.
 */
class AlarmActivity : ComponentActivity() {
    /** The habit on screen: the first alarm in the queue. */
    private var habitId = 0L
    /** The habit last answered, so a double tap doesn't answer the next alarm too. */
    private var answeredId = 0L

    /** The power button turns the screen off: that silences the alarm, like Dismiss. */
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = silenceAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opens at once, already drawn, and fades out (user, 2026-11-08: the default slide looked
        // like a glitch). No fade in: over the lock screen Android paints the theme's page colour
        // behind a fading window, which flashed white before the alarm (2026-11-09).
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        showOverLockScreen()
        drawEdgeToEdge(dark = false)
        habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        val container = (application as WeftApp).container
        // Read the settings and the habit before the first frame (a few ms), and paint the window
        // in the habit's colour, so the screen opens already drawn: no blank or page-coloured flash.
        val first = runBlocking {
            withTimeoutOrNull(FIRST_LOAD_MS) { container.settings.settings.first() to container.repository.getHabit(habitId) }
        }
        val firstSettings = first?.first
        val firstHabit = first?.second
        if (firstSettings != null && firstHabit != null) {
            val dark = when (firstSettings.theme.mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            window.setBackgroundDrawable(ColorDrawable(habitColors(firstHabit.colorHue.toFloat(), dark).soft.toArgb()))
        } else {
            window.setBackgroundDrawableResource(R.color.weft_bg)
        }
        // The theme starts see-through on Android 11+ so Android shows no splash screen (values-v30);
        // make it opaque now, because only an opaque screen covers the lock screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setTranslucent(false)
        // On the lock screen Back does nothing (a pocket press mustn't silence it). Opened over an
        // app on an unlocked phone, Back is Dismiss and returns to that app (design 2026-11-08, 12).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) answer(ReminderActions.ACTION_DISMISS)
            }
        })
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(firstSettings)
            val queue by container.alarmRinger.queue.collectAsStateWithLifecycle()
            val ringing = queue.firstOrNull()
            val answered by container.alarmRinger.answered.collectAsStateWithLifecycle()
            var habit by remember { mutableStateOf(firstHabit) }
            var seenRinging by remember { mutableStateOf(false) }
            // Always the alarm that is sounding: after one is answered, the next takes the screen.
            // Every alarm stopped (answered, a log anywhere, the 10-minute limit): close.
            LaunchedEffect(ringing?.habitId) {
                val id = ringing?.habitId
                if (id != null) {
                    seenRinging = true
                    habitId = id
                    if (habit?.id != id) habit = container.repository.getHabit(id)
                } else if (seenRinging) {
                    finish()
                }
            }
            val current = settings ?: return@setContent
            val h = habit?.takeIf { it.id == habitId } ?: return@setContent
            val minutes = ringing?.takeIf { it.habitId == habitId }?.minutes ?: LocalTime.now().let { it.hour * 60 + it.minute }
            val sheet by logger.sheet.collectAsStateWithLifecycle()
            WeftTheme(current.theme) {
                AlarmScreen(
                    habit = h,
                    time = clockTime(this@AlarmActivity, minutes),
                    // "1 of 2", then "2 of 2" once the first is answered.
                    position = (answered + queue.size).takeIf { it > 1 }?.let { answered + 1 to it },
                    next = queue.getOrNull(1)?.name,
                    onDone = { answer(ReminderActions.ACTION_DONE) },
                    onSnooze = { answer(ReminderActions.ACTION_SNOOZE) },
                    onDismiss = { answer(ReminderActions.ACTION_DISMISS) },
                    onSkip = { answer(ReminderActions.ACTION_SKIP) },
                    onLogAmount = {
                        // The sound stops; the screen stays under the app's own amount sheet.
                        container.alarmRinger.hush(h.id)
                        logger.open(h.id, LocalDate.now())
                    },
                    onOpenWeft = { openWeft(h.id, several = queue.size > 1) },
                )
                // Save logs it, and the alarm moves on (AlarmRinger sees the log); Cancel or a
                // swipe down comes back here, still quiet.
                sheet?.let { s ->
                    LogSheet(
                        s,
                        onSave = { status, amount, note -> logger.save(s, status, amount, note) },
                        onUndo = { logger.clear(s) },
                        onDismiss = logger::dismiss,
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < 34) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // A volume button only stops the sound; the screen stays to answer it (user, 2026-11-09).
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0) (application as WeftApp).container.alarmRinger.hush(habitId)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // "Showing" means in front of the user. A started screen can still sit behind the lock screen,
    // and then the Show over other apps fallback must still step in.
    override fun onResume() {
        super.onResume()
        showing = true
    }

    override fun onPause() {
        showing = false
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(screenOff)
        super.onDestroy()
    }

    /** The app's own logging, for Log amount: the same sheet and rules as on Today. */
    private val logger by lazy {
        val app = application as WeftApp
        DayLogger(app.container.repository, app.container.settings, lifecycleScope, ResourceStrings(this))
    }

    /**
     * Open Weft: stops this alarm, asks to unlock, then opens the habit (or Today when other
     * alarms were waiting; they stay as notifications, unanswered).
     */
    private fun openWeft(id: Long, several: Boolean) {
        (application as WeftApp).container.alarmRinger.leave(id)
        val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (!several) open.setAction(MainActivity.ACTION_OPEN_HABIT).putExtra(MainActivity.EXTRA_HABIT_ID, id)
        val go = { startActivity(open); finish() }
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = go()
                override fun onDismissCancelled() = finish()
                override fun onDismissError() = go()
            })
        } else {
            go()
        }
    }

    /** Answers the alarm on screen; the screen stays if another alarm is waiting. */
    private fun answer(action: String) {
        val id = habitId
        if (id == 0L || id == answeredId) return
        answeredId = id
        // Stop the sound now (the next alarm, if any, starts); the receiver logs (Done),
        // reschedules (Snooze) and refreshes widgets.
        (application as WeftApp).container.alarmRinger.stop(id)
        send(action, id)
    }

    /** The power button: every ringing alarm stops, like Dismiss on each. */
    private fun silenceAll() {
        val ringer = (application as WeftApp).container.alarmRinger
        ringer.queue.value.map { it.habitId }.forEach { id ->
            ringer.stop(id)
            send(ReminderActions.ACTION_DISMISS, id)
        }
    }

    private fun send(action: String, id: Long) = sendBroadcast(
        Intent(this, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderScheduler.EXTRA_HABIT_ID, id)
            .putExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay()),
    )

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

        /** How long the first frame may wait for the habit; past it the screen draws when it loads. */
        private const val FIRST_LOAD_MS = 400L

        /** True while the ringing screen is on screen: the overlay fallback leaves it alone. */
        @Volatile
        var showing = false
            private set

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
private fun AlarmScreen(
    habit: Habit,
    time: String,
    /** "1 of 2" when alarms share a time; null for one alarm. */
    position: Pair<Int, Int>?,
    next: String?,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onLogAmount: () -> Unit,
    onOpenWeft: () -> Unit,
) {
    val hc = habitColors(habit.colorHue.toFloat())
    val measured = habit.trackType == TrackType.AMOUNT
    Column(
        Modifier
            .fillMaxSize()
            .background(hc.soft)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = SheetSidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Open Weft: a text button with a chevron (alarm-4/5).
        TextButton(onClick = onOpenWeft, modifier = Modifier.padding(top = 16.dp).heightIn(min = ButtonHeight)) {
            Text(stringResource(R.string.alarm_open_weft), style = WeftType.label, color = hc.ink)
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = hc.ink, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
        Spacer(Modifier.weight(1f))
        if (position != null) {
            Text(
                stringResource(R.string.alarm_position, position.first, position.second),
                style = WeftType.captionStrong,
                color = hc.ink,
                modifier = Modifier.padding(bottom = 16.dp).background(hc.mid, CircleShape).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        HabitTile(hc.mid, Modifier.size(AlarmTileSize), RoundedCornerShape(AlarmTileCorner)) {
            Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(AlarmTileIconSize))
        }
        Text(habit.name, style = WeftType.title, color = hc.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        if (measured) {
            Text(stringResource(R.string.alarm_goal, habit.measure(habit.target)), style = WeftType.supporting, color = hc.ink.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
        }
        Text(time, style = WeftType.alarmTime, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
        if (next != null) {
            Text(stringResource(R.string.widget_next, next), style = WeftType.supporting, color = hc.ink.copy(alpha = 0.75f), modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(2f))
        // Keyed by habit: when the next alarm takes the screen ("2 of 2") its slider starts at the left.
        key(habit.id) {
            SlideToDone(track = hc.mid, thumb = hc.solid, label = stringResource(R.string.alarm_slide), labelColor = hc.ink, onDone = onDone)
        }
        // Log amount (measured) or Skip today (check-off), between the slide bar and the pair.
        TextButton(
            onClick = if (measured) onLogAmount else onSkip,
            modifier = Modifier.padding(vertical = 8.dp).heightIn(min = ButtonHeight),
        ) {
            Text(stringResource(if (measured) R.string.log_amount else R.string.alarm_skip_today), style = WeftType.cardTitle, color = hc.ink)
        }
        ButtonPair(
            secondaryLabel = stringResource(R.string.notify_snooze_minutes, ReminderActions.SNOOZE_MINUTES),
            onSecondary = onSnooze,
            primaryLabel = stringResource(R.string.alarm_dismiss),
            onPrimary = onDismiss,
        )
        Spacer(Modifier.height(40.dp))
    }
}

/** A pill track with a round thumb; dragging the thumb to the end marks the habit done. */
@Composable
private fun SlideToDone(track: Color, thumb: Color, label: String, labelColor: Color, onDone: () -> Unit) {
    val density = LocalDensity.current
    // The track's width, measured: the thumb travels from the left inset to the right one.
    var width by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(SlideHeight)
            .onSizeChanged { width = it.width }
            .background(track, CircleShape)
            // TalkBack: a double tap marks it done, as sliding does.
            .semantics { contentDescription = label; onClick(label) { onDone(); true } },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbSize = SlideHeight - SlideInset * 2
        val max = (width - with(density) { (thumbSize + SlideInset * 2).toPx() }).coerceAtLeast(0f)
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
