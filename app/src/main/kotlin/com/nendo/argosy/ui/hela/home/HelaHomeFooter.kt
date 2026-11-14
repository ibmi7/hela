package com.nendo.argosy.ui.hela.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource
import com.nendo.argosy.R
import com.nendo.argosy.ui.components.FooterHints
import com.nendo.argosy.ui.components.FooterVariant
import com.nendo.argosy.ui.components.InputButton
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import com.nendo.argosy.ui.screens.home.HomeRowItem
import com.nendo.argosy.ui.screens.home.HomeUiState
import com.nendo.argosy.ui.screens.home.HomeViewModel

@Composable
fun HelaHomeFooter(
    uiState: HomeUiState,
    onTabs: Boolean,
    pageCount: Int,
    onConsoles: Boolean,
    onOpenFolder: () -> Unit,
    downloadIndicators: State<Map<Long, GameDownloadIndicator>>,
    viewModel: HomeViewModel,
    onGameSelect: (Long) -> Unit
) {
    val pagesLabel = stringResource(R.string.hela_home_footer_pages)
    val focusedGame = uiState.focusedGame.takeIf { !onTabs && !onConsoles && !uiState.showGameMenu }
    val focusedViewAll = (uiState.focusedItem as? HomeRowItem.ViewAll).takeIf { !onTabs && !onConsoles }
    val onFolder = onConsoles && !onTabs
    val confirmLabel = when {
        onFolder -> stringResource(R.string.hela_home_footer_open)
        focusedGame == null -> if (focusedViewAll != null) stringResource(R.string.hela_home_footer_library) else null
        focusedGame.needsInstall -> stringResource(R.string.hela_home_footer_install)
        focusedGame.isDownloaded -> stringResource(R.string.hela_home_footer_play)
        downloadIndicators.value[focusedGame.id]?.isPaused == true -> stringResource(R.string.hela_home_footer_resume)
        else -> stringResource(R.string.hela_home_footer_download)
    }
    val favoriteLabel = focusedGame?.let {
        if (it.isFavorite) stringResource(R.string.hela_home_footer_unfavorite)
        else stringResource(R.string.hela_home_footer_favorite)
    }
    val detailsLabel = focusedGame?.let { stringResource(R.string.hela_home_footer_details) }

    FooterHints(
        hints = listOfNotNull(
            (InputButton.LB_RB to pagesLabel).takeIf { pageCount > 1 },
            confirmLabel?.let { InputButton.A to it },
            favoriteLabel?.let { InputButton.Y to it },
            detailsLabel?.let { InputButton.X to it }
        ),
        variant = FooterVariant.SUBTLE,
        onHintClick = { button ->
            when (button) {
                InputButton.A -> when {
                    onFolder -> onOpenFolder()
                    focusedGame != null -> viewModel.activateFocusedGame(focusedGame)
                    focusedViewAll != null -> viewModel.handleItemTap(uiState.focusedGameIndex, onGameSelect)
                }
                InputButton.Y -> focusedGame?.let { viewModel.toggleFavorite(it.id) }
                InputButton.X -> focusedGame?.let { onGameSelect(it.id) }
                else -> Unit
            }
        }
    )
}
