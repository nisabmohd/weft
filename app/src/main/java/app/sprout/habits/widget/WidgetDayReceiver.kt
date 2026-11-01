package app.sprout.habits.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.sprout.habits.WeftApp
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Redraws every widget just after midnight. Widgets only change with the data, so without this
 * one would keep showing yesterday ("7/7", "All done") until something was logged.
 */
class WidgetDayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WeftApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.widgets.updateAll()
                scheduleNextDay(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** An alarm a few seconds after the next local midnight; replaces any earlier one. */
        fun scheduleNextDay(context: Context) {
            val at = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 5_000
            val intent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WidgetDayReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            // Inexact is fine: Doze may hold it to its next window, which still beats waiting for a log.
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, at, intent)
        }
    }
}
