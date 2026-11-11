package app.sprout.habits.ui.more

import app.sprout.habits.ui.theme.AboutLogoIconSize
import app.sprout.habits.ui.theme.AboutLogoSize
import app.sprout.habits.ui.theme.CardShape
import app.sprout.habits.ui.theme.IconSizeSmall
import app.sprout.habits.ui.theme.PillShape
import app.sprout.habits.ui.theme.RowPadding
import app.sprout.habits.ui.theme.SectionGap
import app.sprout.habits.ui.theme.Space12
import app.sprout.habits.ui.theme.Space16
import app.sprout.habits.ui.theme.Space24
import app.sprout.habits.ui.theme.Space8
import app.sprout.habits.ui.components.TitleLineGap
import app.sprout.habits.ui.theme.TopBarHeight
import app.sprout.habits.ui.theme.TopBarTitleGap
import app.sprout.habits.ui.components.HabitTileSize
import app.sprout.habits.ui.theme.WeftType
import androidx.compose.foundation.layout.Spacer
import app.sprout.habits.ui.update.CheckForUpdates
import app.sprout.habits.data.update.PlayUpdates
import app.sprout.habits.data.update.UpdateManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.openEmail
import app.sprout.habits.ui.openUrl
import app.sprout.habits.ui.theme.habitColors
@Composable
fun AboutScreen(updates: UpdateManager, playUpdates: PlayUpdates, onBack: () -> Unit, onOpenLicences: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.about), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            Box(Modifier.padding(top = Space16).size(AboutLogoSize).background(colors.primaryContainer, CardShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_weft), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(AboutLogoIconSize))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = WeftType.screenTitle, color = colors.onBackground)
                Text(stringResource(R.string.version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = WeftType.supporting, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(Space12))
                CheckForUpdates(updates, playUpdates)
            }
            // One row (design 2026-11-11); the licence itself is under Source code and Open-source licences.
            app.sprout.habits.ui.components.InfoRow(R.drawable.ic_code, stringResource(R.string.free_open_source), stringResource(R.string.about_open_source_line))
            // Not in the play build: Google Play doesn't allow asking for money outside its own billing.
            if (!BuildConfig.PLAY_STORE) SettingsCard { SupportRow { openUrl(context, BuildConfig.SPONSOR_URL) } }
            val repo = BuildConfig.REPO_URL
            SettingsGroup(
                if (BuildConfig.PLAY_LISTED) {
                    { LinkRow(stringResource(R.string.rate_on_play), stringResource(R.string.rate_desc)) { openUrl(context, BuildConfig.PLAY_STORE_URL) } }
                } else null,
                { LinkRow(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_desc)) { openUrl(context, BuildConfig.PRIVACY_URL) } },
                { LinkRow(stringResource(R.string.source_code), repo.removePrefix("https://")) { openUrl(context, repo) } },
                { LinkRow(stringResource(R.string.report_bug), stringResource(R.string.github_issues)) { openUrl(context, "$repo/issues") } },
                { LinkRow(stringResource(R.string.contribute), stringResource(R.string.contribute_desc)) { openUrl(context, "$repo/blob/main/CONTRIBUTING.md") } },
                {
                    SettingsRow(stringResource(R.string.licences), stringResource(R.string.licences_desc), onClick = onOpenLicences) {
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(IconSizeSmall))
                    }
                },
            )
            // The developer, mid apps, with the contact address; tapping writes an email.
            SettingsCard { MidAppsRow { openEmail(context, BuildConfig.CONTACT_EMAIL, context.getString(R.string.app_name)) } }
            Text(stringResource(R.string.made_by), style = WeftType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = Space24))
        }
    }
}

/** "Support Weft" with a pink heart and a small Sponsor pill. */
@Composable
private fun SupportRow(onClick: () -> Unit) {
    val pink = habitColors(330f)
    SettingsRow(stringResource(R.string.support_weft), stringResource(R.string.support_desc), onClick = onClick) {
        Text(
            stringResource(R.string.sponsor),
            style = WeftType.label,
            color = pink.ink,
            modifier = Modifier.background(pink.soft, PillShape).padding(horizontal = Space12, vertical = Space8),
        )
    }
}

/** "mid apps" with its mark and "Contact: hello.midapps@gmail.com", like Burrow's Made by row. */
@Composable
private fun MidAppsRow(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space12),
    ) {
        Image(painterResource(R.drawable.ic_mid_apps), contentDescription = null, modifier = Modifier.size(HabitTileSize))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(app.sprout.habits.ui.components.TitleLineGap)) {
            Text(stringResource(R.string.mid_apps), style = WeftType.cardTitle, color = colors.onSurface)
            Text(stringResource(R.string.about_contact, BuildConfig.CONTACT_EMAIL), style = WeftType.supporting, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_sym_mail), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(IconSizeSmall))
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) =
    SettingsRow(title, subtitle, onClick = onClick) {
        Icon(painterResource(R.drawable.ic_open_external), contentDescription = null, modifier = Modifier.size(IconSizeSmall))
    }

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(TopBarHeight).padding(start = Space16, top = Space8), verticalAlignment = Alignment.CenterVertically) {
        HeaderIconButton(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onClick = onBack)
        Text(title, style = WeftType.title, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = TopBarTitleGap))
    }
}
