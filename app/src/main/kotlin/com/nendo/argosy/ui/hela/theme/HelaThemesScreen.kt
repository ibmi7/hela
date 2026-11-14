package com.nendo.argosy.ui.hela.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nendo.argosy.R
import com.nendo.argosy.data.model.hela.HelaThemeSelection
import com.nendo.argosy.data.model.hela.InstalledThemePack
import com.nendo.argosy.ui.components.ActionPreference
import com.nendo.argosy.ui.components.FocusedScroll
import com.nendo.argosy.ui.components.Modal
import com.nendo.argosy.ui.components.NavigationPreference
import com.nendo.argosy.ui.components.SwitchPreference
import com.nendo.argosy.ui.filebrowser.FileBrowserMode
import com.nendo.argosy.ui.filebrowser.FileBrowserScreen
import com.nendo.argosy.ui.filebrowser.FileFilter
import com.nendo.argosy.ui.input.ModalInputEffect
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.util.clickableNoFocus

private val PACK_ARCHIVE_FILTER = FileFilter(extensions = setOf("zip"))

@Composable
fun HelaThemesScreen(
    onDismiss: () -> Unit,
    viewModel: HelaThemesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val inputHandler = remember(viewModel, onDismiss) { HelaThemesInputHandler(viewModel, onDismiss) }
    ModalInputEffect(active = true, handler = inputHandler)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickableNoFocus {}
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HelaThemesHeader(onBack = onDismiss)
            HelaThemesList(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
        }

        val menuPack = state.menuPack
        if (menuPack != null) {
            PackActionsModal(
                pack = menuPack,
                actions = state.menuActions,
                focusedIndex = state.menuFocusIndex,
                isConfirmingDelete = state.isConfirmingDelete,
                onAction = { viewModel.runAction(menuPack, it) },
                onDismiss = { viewModel.dismissMenu() }
            )
        }
    }

    if (state.showBrowser) {
        FileBrowserScreen(
            mode = FileBrowserMode.FILE_SELECTION,
            title = stringResource(R.string.hela_themes_browser_title),
            fileFilter = PACK_ARCHIVE_FILTER,
            onPathSelected = { viewModel.importPack(it) },
            onDismiss = { viewModel.dismissBrowser() }
        )
    }
}

@Composable
private fun HelaThemesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.hela_themes_back_desc),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.iconLg).clickableNoFocus(onClick = onBack)
        )
        Spacer(modifier = Modifier.width(Dimens.spacingMd))
        Text(
            text = stringResource(R.string.hela_themes_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HelaThemesList(
    state: HelaThemesUiState,
    viewModel: HelaThemesViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    FocusedScroll(listState = listState, focusedIndex = state.focusedIndex)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        itemsIndexed(state.rows, key = { _, row -> row.key }) { index, row ->
            val isFocused = index == state.focusedIndex && state.menuPack == null
            val activate = {
                viewModel.focusRow(index)
                viewModel.activateRow(row)
            }
            when (row) {
                HelaThemesRow.Import -> ActionPreference(
                    title = stringResource(R.string.hela_themes_import_title),
                    subtitle = stringResource(R.string.hela_themes_import_subtitle),
                    isFocused = isFocused,
                    icon = Icons.Outlined.FileOpen,
                    isEnabled = !state.isImporting,
                    onClick = activate
                )
                HelaThemesRow.Animate -> SwitchPreference(
                    title = stringResource(R.string.hela_themes_animate_title),
                    subtitle = stringResource(R.string.hela_themes_animate_subtitle),
                    isEnabled = state.selection.animate,
                    isFocused = isFocused,
                    onToggle = {
                        viewModel.focusRow(index)
                        viewModel.setAnimate(it)
                    }
                )
                HelaThemesRow.Clear -> ActionPreference(
                    title = stringResource(R.string.hela_themes_clear_title),
                    subtitle = stringResource(R.string.hela_themes_clear_subtitle),
                    isFocused = isFocused,
                    icon = Icons.Outlined.HideImage,
                    onClick = activate
                )
                is HelaThemesRow.Pack -> NavigationPreference(
                    icon = Icons.Outlined.Palette,
                    title = row.pack.name,
                    subtitle = packSubtitle(row.pack, state.selection),
                    isFocused = isFocused,
                    onClick = activate
                )
            }
        }
        item(key = "status") {
            HelaThemesStatus(state)
        }
    }
}

@Composable
private fun HelaThemesStatus(state: HelaThemesUiState) {
    val messageRes = state.messageRes
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            state.isImporting -> {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.iconMd))
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Text(
                    text = stringResource(R.string.hela_themes_importing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            messageRes != null -> Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.messageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            !state.hasPacks -> Text(
                text = stringResource(R.string.hela_themes_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun packSubtitle(pack: InstalledThemePack, selection: HelaThemeSelection): String {
    val slots = buildList {
        if (pack.main != null && selection.mainPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_main))
        }
        if (pack.external != null && selection.externalPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_external))
        }
        if (pack.colors != null && selection.colorsPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_colors))
        }
        if (pack.sounds.isNotEmpty() && selection.soundsPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_sounds))
        }
        if (pack.music.isNotEmpty() && selection.musicPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_music))
        }
        if (pack.icons.isNotEmpty() && selection.iconsPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_icons))
        }
        if (!pack.folders.isEmpty && selection.foldersPackId == pack.id) {
            add(stringResource(R.string.hela_themes_slot_folders))
        }
    }
    val usage = if (slots.isEmpty()) {
        stringResource(R.string.hela_themes_pack_not_in_use)
    } else {
        stringResource(R.string.hela_themes_pack_in_use, slots.joinToString(", "))
    }
    val author = pack.author ?: return usage
    return stringResource(R.string.hela_themes_pack_subtitle, author, usage)
}

@Composable
private fun PackActionsModal(
    pack: InstalledThemePack,
    actions: List<HelaPackAction>,
    focusedIndex: Int,
    isConfirmingDelete: Boolean,
    onAction: (HelaPackAction) -> Unit,
    onDismiss: () -> Unit
) {
    Modal(
        title = pack.name,
        subtitle = if (isConfirmingDelete) stringResource(R.string.hela_themes_delete_confirm_prompt) else null,
        onDismiss = onDismiss
    ) {
        actions.forEachIndexed { index, action ->
            when (action) {
                HelaPackAction.CANCEL_DELETE -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_cancel_delete),
                    subtitle = stringResource(R.string.hela_themes_action_cancel_delete_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    onClick = { onAction(action) }
                )
                HelaPackAction.CONFIRM_DELETE -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_confirm_delete),
                    subtitle = stringResource(R.string.hela_themes_action_confirm_delete_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.DeleteOutline,
                    isDangerous = true,
                    onClick = { onAction(action) }
                )
                HelaPackAction.APPLY -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_apply),
                    subtitle = stringResource(R.string.hela_themes_action_apply_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Palette,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_MAIN -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_main),
                    subtitle = stringResource(R.string.hela_themes_action_use_main_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Image,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_EXTERNAL -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_external),
                    subtitle = stringResource(R.string.hela_themes_action_use_external_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Image,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_COLORS -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_colors),
                    subtitle = stringResource(R.string.hela_themes_action_use_colors_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Palette,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_SOUNDS -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_sounds),
                    subtitle = stringResource(R.string.hela_themes_action_use_sounds_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_MUSIC -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_music),
                    subtitle = stringResource(R.string.hela_themes_action_use_music_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.MusicNote,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_ICONS -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_icons),
                    subtitle = stringResource(R.string.hela_themes_action_use_icons_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Apps,
                    onClick = { onAction(action) }
                )
                HelaPackAction.USE_FOLDERS -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_use_folders),
                    subtitle = stringResource(R.string.hela_themes_action_use_folders_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.Folder,
                    onClick = { onAction(action) }
                )
                HelaPackAction.DELETE -> ActionPreference(
                    title = stringResource(R.string.hela_themes_action_delete),
                    subtitle = stringResource(R.string.hela_themes_action_delete_subtitle),
                    isFocused = index == focusedIndex,
                    icon = Icons.Outlined.DeleteOutline,
                    isDangerous = true,
                    onClick = { onAction(action) }
                )
            }
        }
    }
}
