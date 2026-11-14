package com.nendo.argosy.ui.hela.theme

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nendo.argosy.R
import com.nendo.argosy.data.hela.theme.HelaThemeStore
import com.nendo.argosy.data.hela.theme.PackImportFailure
import com.nendo.argosy.data.hela.theme.PackImportResult
import com.nendo.argosy.data.model.hela.HelaThemeSelection
import com.nendo.argosy.data.model.hela.InstalledThemePack
import com.nendo.argosy.data.model.hela.WallpaperSlot
import com.nendo.argosy.ui.input.InputHandler
import com.nendo.argosy.ui.input.InputResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HelaThemesRow(val key: String) {
    data object Import : HelaThemesRow("import")
    data object Animate : HelaThemesRow("animate")
    data object Clear : HelaThemesRow("clear")
    data class Pack(val pack: InstalledThemePack) : HelaThemesRow("pack:${pack.id}")
}

enum class HelaPackAction {
    APPLY, USE_MAIN, USE_EXTERNAL, USE_COLORS, USE_SOUNDS, USE_MUSIC, USE_ICONS, USE_FOLDERS, DELETE, CANCEL_DELETE, CONFIRM_DELETE
}

data class HelaThemesUiState(
    val rows: List<HelaThemesRow> = listOf(HelaThemesRow.Import, HelaThemesRow.Animate),
    val selection: HelaThemeSelection = HelaThemeSelection(),
    val focusedIndex: Int = 0,
    val isImporting: Boolean = false,
    val showBrowser: Boolean = false,
    val menuPack: InstalledThemePack? = null,
    val menuActions: List<HelaPackAction> = emptyList(),
    val menuFocusIndex: Int = 0,
    val isConfirmingDelete: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val messageIsError: Boolean = false
) {
    val hasPacks: Boolean get() = rows.any { it is HelaThemesRow.Pack }
}

private data class HelaThemesLocalState(
    val focusedIndex: Int = 0,
    val isImporting: Boolean = false,
    val showBrowser: Boolean = false,
    val menuPackId: String? = null,
    val menuFocusIndex: Int = 0,
    val confirmingDelete: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val messageIsError: Boolean = false
)

@HiltViewModel
class HelaThemesViewModel @Inject constructor(
    private val store: HelaThemeStore
) : ViewModel() {

    private val local = MutableStateFlow(HelaThemesLocalState())

    val uiState: StateFlow<HelaThemesUiState> =
        combine(store.packs, store.selection, local) { packs, selection, local ->
            val rows = buildList {
                add(HelaThemesRow.Import)
                add(HelaThemesRow.Animate)
                if (!selection.isEmpty) add(HelaThemesRow.Clear)
                packs.forEach { add(HelaThemesRow.Pack(it)) }
            }
            val menuPack = packs.firstOrNull { it.id == local.menuPackId }
            val confirmingDelete = menuPack != null && local.confirmingDelete
            val menuActions = when {
                menuPack == null -> emptyList()
                confirmingDelete -> listOf(HelaPackAction.CANCEL_DELETE, HelaPackAction.CONFIRM_DELETE)
                else -> actionsFor(menuPack)
            }
            HelaThemesUiState(
                rows = rows,
                selection = selection,
                focusedIndex = local.focusedIndex.coerceIn(0, rows.lastIndex),
                isImporting = local.isImporting,
                showBrowser = local.showBrowser,
                menuPack = menuPack,
                menuActions = menuActions,
                menuFocusIndex = local.menuFocusIndex.coerceIn(0, (menuActions.size - 1).coerceAtLeast(0)),
                isConfirmingDelete = confirmingDelete,
                messageRes = local.messageRes,
                messageIsError = local.messageIsError
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HelaThemesUiState())

    fun moveFocus(delta: Int) {
        val state = uiState.value
        if (state.menuPack != null) {
            if (state.menuActions.isEmpty()) return
            local.update { it.copy(menuFocusIndex = (state.menuFocusIndex + delta).mod(state.menuActions.size)) }
        } else {
            local.update { it.copy(focusedIndex = (state.focusedIndex + delta).mod(state.rows.size)) }
        }
    }

    fun focusRow(index: Int) {
        local.update { it.copy(focusedIndex = index) }
    }

    fun confirmFocused() {
        val state = uiState.value
        if (state.isImporting) return
        if (state.menuPack != null) {
            state.menuActions.getOrNull(state.menuFocusIndex)?.let { runAction(state.menuPack, it) }
            return
        }
        activateRow(state.rows.getOrNull(state.focusedIndex) ?: return)
    }

    fun activateRow(row: HelaThemesRow) {
        if (uiState.value.isImporting) return
        when (row) {
            HelaThemesRow.Import -> local.update { it.copy(showBrowser = true, messageRes = null) }
            HelaThemesRow.Animate -> setAnimate(!uiState.value.selection.animate)
            HelaThemesRow.Clear -> viewModelScope.launch { store.clearSelection() }
            is HelaThemesRow.Pack -> local.update {
                it.copy(menuPackId = row.pack.id, menuFocusIndex = 0, confirmingDelete = false)
            }
        }
    }

    fun adjustFocused(direction: Int): Boolean {
        val state = uiState.value
        if (state.menuPack != null) return false
        if (state.rows.getOrNull(state.focusedIndex) != HelaThemesRow.Animate) return false
        setAnimate(direction > 0)
        return true
    }

    fun setAnimate(animate: Boolean) {
        viewModelScope.launch { store.setAnimate(animate) }
    }

    fun runAction(pack: InstalledThemePack, action: HelaPackAction) {
        when (action) {
            HelaPackAction.DELETE -> {
                local.update { it.copy(confirmingDelete = true, menuFocusIndex = 0) }
                return
            }
            HelaPackAction.CANCEL_DELETE -> {
                local.update { it.copy(confirmingDelete = false, menuFocusIndex = 0) }
                return
            }
            else -> Unit
        }
        local.update { it.copy(menuPackId = null, confirmingDelete = false) }
        viewModelScope.launch {
            when (action) {
                HelaPackAction.APPLY -> store.applyPack(pack.id)
                HelaPackAction.USE_MAIN -> store.setSlot(WallpaperSlot.MAIN, pack.id)
                HelaPackAction.USE_EXTERNAL -> store.setSlot(WallpaperSlot.EXTERNAL, pack.id)
                HelaPackAction.USE_COLORS -> store.setColorsPack(pack.id)
                HelaPackAction.USE_SOUNDS -> store.setSoundsPack(pack.id)
                HelaPackAction.USE_MUSIC -> store.setMusicPack(pack.id)
                HelaPackAction.USE_ICONS -> store.setIconsPack(pack.id)
                HelaPackAction.USE_FOLDERS -> store.setFoldersPack(pack.id)
                HelaPackAction.CONFIRM_DELETE -> store.deletePack(pack.id)
                HelaPackAction.DELETE, HelaPackAction.CANCEL_DELETE -> Unit
            }
        }
    }

    fun dismissMenu(): Boolean {
        val state = uiState.value
        if (state.menuPack == null) return false
        if (state.isConfirmingDelete) {
            local.update { it.copy(confirmingDelete = false, menuFocusIndex = 0) }
        } else {
            local.update { it.copy(menuPackId = null) }
        }
        return true
    }

    fun dismissBrowser() {
        local.update { it.copy(showBrowser = false) }
    }

    fun importPack(zipPath: String) {
        local.update { it.copy(showBrowser = false, isImporting = true, messageRes = null) }
        viewModelScope.launch {
            val result = store.importPack(zipPath)
            local.update {
                it.copy(
                    isImporting = false,
                    messageRes = messageFor(result),
                    messageIsError = result is PackImportResult.Failure
                )
            }
        }
    }

    private fun actionsFor(pack: InstalledThemePack): List<HelaPackAction> = buildList {
        val slots = listOf(
            pack.main != null,
            pack.external != null,
            pack.colors != null,
            pack.sounds.isNotEmpty(),
            pack.music.isNotEmpty(),
            pack.icons.isNotEmpty(),
            !pack.folders.isEmpty
        )
        if (slots.count { it } > 1) add(HelaPackAction.APPLY)
        if (pack.main != null) add(HelaPackAction.USE_MAIN)
        if (pack.external != null) add(HelaPackAction.USE_EXTERNAL)
        if (pack.colors != null) add(HelaPackAction.USE_COLORS)
        if (pack.sounds.isNotEmpty()) add(HelaPackAction.USE_SOUNDS)
        if (pack.music.isNotEmpty()) add(HelaPackAction.USE_MUSIC)
        if (pack.icons.isNotEmpty()) add(HelaPackAction.USE_ICONS)
        if (!pack.folders.isEmpty) add(HelaPackAction.USE_FOLDERS)
        add(HelaPackAction.DELETE)
    }

    @StringRes
    private fun messageFor(result: PackImportResult): Int = when (result) {
        is PackImportResult.Success -> R.string.hela_themes_import_done
        is PackImportResult.Failure -> when (result.reason) {
            PackImportFailure.NOT_A_ZIP -> R.string.hela_themes_import_error_not_zip
            PackImportFailure.NO_THEME -> R.string.hela_themes_import_error_no_theme
            PackImportFailure.UNSAFE_ENTRY -> R.string.hela_themes_import_error_unsafe
            PackImportFailure.TOO_LARGE -> R.string.hela_themes_import_error_too_large
            PackImportFailure.IO -> R.string.hela_themes_import_error_io
        }
    }
}

class HelaThemesInputHandler(
    private val viewModel: HelaThemesViewModel,
    private val onDismiss: () -> Unit
) : InputHandler {

    override fun onUp(): InputResult {
        viewModel.moveFocus(-1)
        return InputResult.HANDLED
    }

    override fun onDown(): InputResult {
        viewModel.moveFocus(1)
        return InputResult.HANDLED
    }

    override fun onLeft(): InputResult {
        viewModel.adjustFocused(-1)
        return InputResult.HANDLED
    }

    override fun onRight(): InputResult {
        viewModel.adjustFocused(1)
        return InputResult.HANDLED
    }

    override fun onConfirm(): InputResult {
        viewModel.confirmFocused()
        return InputResult.HANDLED
    }

    override fun onBack(): InputResult {
        if (!viewModel.dismissMenu()) onDismiss()
        return InputResult.HANDLED
    }

    override fun onMenu(): InputResult = InputResult.HANDLED

    override fun onSecondaryAction(): InputResult = InputResult.HANDLED

    override fun onContextMenu(): InputResult = InputResult.HANDLED

    override fun onPrevSection(): InputResult = InputResult.HANDLED

    override fun onNextSection(): InputResult = InputResult.HANDLED

    override fun onPrevTrigger(): InputResult = InputResult.HANDLED

    override fun onNextTrigger(): InputResult = InputResult.HANDLED

    override fun onSelect(): InputResult = InputResult.HANDLED

    override fun onLongSelect(): InputResult = InputResult.HANDLED
}
