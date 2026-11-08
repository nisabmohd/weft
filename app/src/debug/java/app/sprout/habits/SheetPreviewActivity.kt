package app.sprout.habits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.data.update.AppUpdate
import app.sprout.habits.support.SupportPromptSheet
import app.sprout.habits.ui.drawEdgeToEdge
import app.sprout.habits.ui.theme.WeftTheme
import app.sprout.habits.ui.update.UpdateSheet

/**
 * Debug builds only: shows a sheet that is hard to reach on demand over whatever is on screen
 * (the activity is see-through), for design captures and checks:
 * `adb shell am start -n app.sprout.habits/.SheetPreviewActivity --es sheet update|support`
 * The sheet's buttons only close it.
 */
class SheetPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawEdgeToEdge(dark = false)
        val container = (application as WeftApp).container
        val sheet = intent.getStringExtra("sheet")
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: return@setContent
            WeftTheme(current.theme) {
                when (sheet) {
                    "update" -> UpdateSheet(
                        update = AppUpdate(
                            version = "1.1.0",
                            notes = listOf("Language picker in More", "Edit any past day from the Week view", "Smoother swipes on older phones"),
                            apkUrl = null,
                            apkName = null,
                            checksumsUrl = null,
                            pageUrl = BuildConfig.REPO_URL,
                        ),
                        updates = container.updates,
                        onLater = ::finish,
                        onDismiss = ::finish,
                        currentVersionOverride = "1.0.0",
                    )
                    "support" -> SupportPromptSheet(
                        showRate = true,
                        showSponsor = true,
                        onRate = ::finish,
                        onStar = ::finish,
                        onSponsor = ::finish,
                        onLater = ::finish,
                    )
                    else -> finish()
                }
            }
        }
    }
}
