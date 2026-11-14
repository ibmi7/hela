package com.nendo.argosy.ui.hela.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.nendo.argosy.domain.model.RequiredAction
import com.nendo.argosy.ui.common.GameConfirmHost
import com.nendo.argosy.ui.components.AddToCollectionModal
import com.nendo.argosy.ui.components.ChangelogModal
import com.nendo.argosy.ui.components.CollectionItem
import com.nendo.argosy.ui.components.DiscPickerModal
import com.nendo.argosy.ui.components.MemcardPickerModal
import com.nendo.argosy.ui.input.ChangelogInputHandler
import com.nendo.argosy.ui.input.DiscPickerInputHandler
import com.nendo.argosy.ui.input.LocalInputDispatcher
import com.nendo.argosy.ui.input.MemcardPickerInputHandler
import com.nendo.argosy.ui.input.ModalInputEffect
import com.nendo.argosy.ui.screens.collections.dialogs.CreateCollectionDialog
import com.nendo.argosy.ui.screens.common.SiblingChoiceModalHost
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import com.nendo.argosy.ui.screens.home.GameSelectOverlay
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.screens.home.HomeUiState
import com.nendo.argosy.ui.screens.home.HomeViewModel
import com.nendo.argosy.ui.screens.media.modals.MediaResumeModalHost

@Composable
fun HelaHomeOverlays(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    downloadIndicators: State<Map<Long, GameDownloadIndicator>>,
    onGameSelect: (Long) -> Unit,
    onChangelogAction: (RequiredAction) -> Unit
) {
    HelaHomeModalInput(viewModel = viewModel, uiState = uiState, onChangelogAction = onChangelogAction)

    AnimatedVisibility(visible = uiState.showGameMenu, enter = fadeIn(), exit = fadeOut()) {
        val focusedGame = uiState.focusedGame
        if (focusedGame != null) {
            GameSelectOverlay(
                game = focusedGame,
                isDownloadPaused = downloadIndicators.value[focusedGame.id]?.isPaused == true,
                isPlatformRow = uiState.currentRow is HomeRow.Platform,
                focusIndex = uiState.gameMenuFocusIndex,
                onDismiss = { viewModel.toggleGameMenu() },
                onPrimaryAction = {
                    viewModel.toggleGameMenu()
                    viewModel.activateFocusedGame(focusedGame)
                },
                playDisplays = uiState.gameMenuDisplays,
                onPlayOnDisplay = { index ->
                    viewModel.moveGameMenuFocus(index + 1 - uiState.gameMenuFocusIndex)
                    viewModel.confirmGameMenuSelection(onGameSelect)
                },
                onFavorite = { viewModel.toggleFavorite(focusedGame.id) },
                onDetails = {
                    viewModel.toggleGameMenu()
                    onGameSelect(focusedGame.id)
                },
                onAddToCollection = {
                    viewModel.toggleGameMenu()
                    viewModel.showAddToCollectionModal(focusedGame.id)
                },
                hasSiblingGroup = uiState.gameMenuHasSiblingGroup,
                onActiveVariant = { viewModel.openActiveVariant(focusedGame.id) },
                onRefresh = { viewModel.refreshGameData(focusedGame.id) },
                onResyncPlatform = {
                    viewModel.toggleGameMenu()
                    viewModel.syncPlatform(focusedGame.platformId, focusedGame.platformDisplayName)
                },
                onDelete = {
                    viewModel.toggleGameMenu()
                    viewModel.deleteLocalFile(focusedGame.id)
                },
                onRemoveFromHome = {
                    viewModel.toggleGameMenu()
                    viewModel.removeFromHome(focusedGame.id)
                },
                onRemoveFromLibrary = {
                    viewModel.toggleGameMenu()
                    viewModel.removeFromLibrary(focusedGame.id)
                },
                onHide = {
                    viewModel.toggleGameMenu()
                    viewModel.hideGame(focusedGame.id)
                }
            )
        }
    }

    GameConfirmHost(viewModel.gameConfirm)

    val siblingChoiceState by viewModel.siblingChoiceState.collectAsState()
    SiblingChoiceModalHost(
        state = siblingChoiceState,
        onMove = viewModel::moveSiblingChoiceFocus,
        onFocus = viewModel::setSiblingChoiceFocus,
        onConfirm = viewModel::confirmSiblingChoice,
        onDismiss = viewModel::dismissSiblingChoice
    )

    AnimatedVisibility(visible = uiState.showAddToCollectionModal, enter = fadeIn(), exit = fadeOut()) {
        AddToCollectionModal(
            collections = uiState.collections.map { c -> CollectionItem(c.id, c.name, c.isInCollection) },
            focusIndex = uiState.collectionModalFocusIndex,
            showCreateOption = true,
            onToggleCollection = viewModel::toggleGameInCollection,
            onCreate = viewModel::showCreateCollectionFromModal,
            onDismiss = viewModel::dismissAddToCollectionModal
        )
    }

    if (uiState.showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = viewModel::hideCreateCollectionDialog,
            onCreate = { name -> viewModel.createCollectionFromModal(name) }
        )
    }

    uiState.discPickerState?.let { pickerState ->
        DiscPickerModal(
            discs = pickerState.discs,
            focusIndex = uiState.discPickerFocusIndex,
            onSelectDisc = viewModel::selectDisc,
            onDismiss = viewModel::dismissDiscPicker
        )
    }

    uiState.memcardPickerState?.let { pickerState ->
        MemcardPickerModal(
            cards = pickerState.cards,
            focusIndex = uiState.memcardPickerFocusIndex,
            selectedCardPath = null,
            onSelectCard = viewModel::selectMemcard,
            onDismiss = viewModel::dismissMemcardPicker
        )
    }

    MediaResumeModalHost(
        prompt = uiState.mediaResumePrompt,
        onStartOver = viewModel::startMediaOver,
        onResume = viewModel::resumeMedia,
        onDismiss = viewModel::dismissMediaResumePrompt
    )

    uiState.changelogEntry?.let { entry ->
        ChangelogModal(
            entry = entry,
            onDismiss = { viewModel.dismissChangelog() },
            onAction = { action -> onChangelogAction(viewModel.handleChangelogAction(action)) }
        )
    }
}

@Composable
private fun HelaHomeModalInput(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    onChangelogAction: (RequiredAction) -> Unit
) {
    val inputDispatcher = LocalInputDispatcher.current
    val changelogInputHandler = remember(viewModel) {
        ChangelogInputHandler(
            getEntry = { viewModel.uiState.value.changelogEntry },
            onDismiss = { viewModel.dismissChangelog() },
            onAction = { action -> onChangelogAction(viewModel.handleChangelogAction(action)) }
        )
    }
    val discPickerInputHandler = remember(viewModel) {
        DiscPickerInputHandler(
            getDiscs = { viewModel.uiState.value.discPickerState?.discs ?: emptyList() },
            getFocusIndex = { viewModel.uiState.value.discPickerFocusIndex },
            onFocusChange = { viewModel.setDiscPickerFocusIndex(it) },
            onSelect = { viewModel.selectDisc(it) },
            onDismiss = { viewModel.dismissDiscPicker() },
            wrapMode = { viewModel.menuWrapMode }
        )
    }
    val memcardPickerInputHandler = remember(viewModel) {
        MemcardPickerInputHandler(
            getCards = { viewModel.uiState.value.memcardPickerState?.cards ?: emptyList() },
            getFocusIndex = { viewModel.uiState.value.memcardPickerFocusIndex },
            onFocusChange = { viewModel.setMemcardPickerFocusIndex(it) },
            onSelect = { viewModel.selectMemcard(it) },
            onDismiss = { viewModel.dismissMemcardPicker() },
            wrapMode = { viewModel.menuWrapMode }
        )
    }

    ModalInputEffect(
        active = uiState.changelogEntry != null,
        handler = changelogInputHandler,
        inputDispatcher = inputDispatcher
    )

    val showDiscPicker = uiState.discPickerState != null
    LaunchedEffect(showDiscPicker) {
        if (showDiscPicker) viewModel.setDiscPickerFocusIndex(0)
    }
    ModalInputEffect(active = showDiscPicker, handler = discPickerInputHandler, inputDispatcher = inputDispatcher)

    val showMemcardPicker = uiState.memcardPickerState != null
    LaunchedEffect(showMemcardPicker) {
        if (showMemcardPicker) viewModel.setMemcardPickerFocusIndex(0)
    }
    ModalInputEffect(
        active = showMemcardPicker,
        handler = memcardPickerInputHandler,
        inputDispatcher = inputDispatcher
    )
}
