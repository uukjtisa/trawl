package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.NoteBlockView
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitchWithContainer
import com.junkfood.seal.ui.page.UpdateDialog
import com.junkfood.seal.ui.theme.FrauncesFamily
import com.junkfood.seal.util.AUTO_UPDATE
import com.junkfood.seal.util.PRE_RELEASE
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.NoteBlock
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.ReleaseNotes
import com.junkfood.seal.util.STABLE
import com.junkfood.seal.util.UPDATE_CHANNEL
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    var autoUpdate by remember { mutableStateOf(PreferenceUtil.isAutoUpdateEnabled()) }
    var updateChannel by UPDATE_CHANNEL.intState
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var release by remember { mutableStateOf(UpdateUtil.Release()) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUnavailableDialog by remember { mutableStateOf(App.isFDroidBuild()) }

    // Notes for the newest release on this channel, shown whether or not it is an upgrade. Keyed
    // on the channel so flipping to pre-release swaps the notes instead of leaving the stable ones
    // on screen under a pre-release heading.
    var latest by remember { mutableStateOf<UpdateUtil.Release?>(null) }
    var notes by remember { mutableStateOf<List<NoteBlock>>(emptyList()) }
    var notesLoading by remember { mutableStateOf(!App.isFDroidBuild()) }

    LaunchedEffect(updateChannel) {
        if (App.isFDroidBuild()) return@LaunchedEffect
        notesLoading = true
        val fetched = UpdateUtil.latestRelease()
        latest = fetched
        notes = fetched?.body?.let { ReleaseNotes.parse(it) }.orEmpty()
        notesLoading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.auto_update))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddings ->
            LazyColumn(modifier = Modifier.padding(paddings)) {
                item {
                    PreferenceSwitchWithContainer(
                        title = stringResource(id = R.string.enable_auto_update),
                        icon = null,
                        isChecked = autoUpdate,
                    ) {
                        autoUpdate = !autoUpdate
                        AUTO_UPDATE.updateBoolean(autoUpdate)
                    }
                }
                item {
                    PreferenceSubtitle(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.update_channel),
                    )
                }
                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(id = R.string.stable_channel),
                        selected = updateChannel == STABLE,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        updateChannel = STABLE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                }

                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(id = R.string.pre_release_channel),
                        selected = updateChannel == PRE_RELEASE,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        updateChannel = PRE_RELEASE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                }
                item {
                    var isLoading by remember { mutableStateOf(false) }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ProgressIndicatorButton(
                            modifier =
                                Modifier.padding(horizontal = 24.dp)
                                    .padding(top = 6.dp)
                                    .padding(bottom = 12.dp),
                            text = stringResource(id = R.string.check_for_updates),
                            icon = Icons.Outlined.Update,
                            isLoading = isLoading,
                        ) {
                            if (!isLoading)
                                scope.launch {
                                    runCatching {
                                            isLoading = true
                                            withContext(Dispatchers.IO) {
                                                UpdateUtil.checkForUpdate()?.let {
                                                    release = it
                                                    showUpdateDialog = true
                                                }
                                                    ?: App.applicationScope.launch(Dispatchers.Main) {
                                                        context.makeToast(R.string.app_up_to_date)
                                                    }
                                            }
                                            isLoading = false
                                        }
                                        .onFailure {
                                            it.printStackTrace()
                                            App.applicationScope.launch(Dispatchers.Main) {
                                                context.makeToast(R.string.app_update_failed)
                                            }
                                            isLoading = false
                                        }
                                }
                        }
                    }
                    androidx.compose.material3.HorizontalDivider()
                }
                item {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.update_channel_desc),
                    )
                }

                // ------------------------------------------------------- what's new
                if (!App.isFDroidBuild()) {
                    item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }
                    item {
                        WhatsNewHeader(
                            release = latest,
                            loading = notesLoading,
                            updateAvailable =
                                latest?.let { UpdateUtil.isNewerThanInstalled(it) } == true,
                        )
                    }
                    // One item per block, so a long body costs only what is on screen.
                    items(notes) { block ->
                        NoteBlockView(block, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        },
    )
    if (showUpdateDialog)
        UpdateDialog(onDismissRequest = { showUpdateDialog = false }, release = release)

    if (showUnavailableDialog) {
        AutoUpdateUnavailableDialog {
            showUnavailableDialog = false
            onNavigateBack()
        }
    }
}

/**
 * The band above the release notes: which release this is, and whether it is an upgrade or
 * the build already running.
 *
 * The pill matters more than it looks. Without it the notes are ambiguous -- a reader cannot
 * tell whether they are looking at what they just got or at what they are missing.
 */
@Composable
private fun WhatsNewHeader(
    release: UpdateUtil.Release?,
    loading: Boolean,
    updateAvailable: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            text = stringResource(R.string.whats_new).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 0.09.em,
            color = scheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                    when {
                        release != null ->
                            release.name.orEmpty().ifBlank { release.tagName.orEmpty() }
                        loading -> stringResource(R.string.check_for_updates)
                        else -> stringResource(R.string.app_update_failed)
                    },
                fontFamily = FrauncesFamily,
                fontSize = 21.sp,
                fontWeight = FontWeight.W700,
                color = scheme.onSurface,
            )
            if (release != null) {
                Spacer(Modifier.width(10.dp))
                val label =
                    if (updateAvailable) stringResource(R.string.update_available)
                    else stringResource(R.string.app_up_to_date)
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.04.em,
                    color = if (updateAvailable) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier =
                        Modifier.clip(RoundedCornerShape(50))
                            .then(
                                if (updateAvailable) Modifier.background(scheme.primary)
                                else Modifier.border(1.dp, scheme.outline, RoundedCornerShape(50))
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (loading) {
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (isLoading)
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    strokeWidth = 3.dp,
                )
            }
        else Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
