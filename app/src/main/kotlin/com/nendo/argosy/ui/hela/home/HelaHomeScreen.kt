package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nendo.argosy.domain.model.RequiredAction
import com.nendo.argosy.ui.components.GameCard
import com.nendo.argosy.ui.hela.theme.HelaWallpaperLayer
import com.nendo.argosy.ui.hela.theme.rememberHelaWallpaper
import com.nendo.argosy.ui.input.LocalInputDispatcher
import com.nendo.argosy.ui.navigation.Screen
import com.nendo.argosy.ui.screens.home.HomeEvent
import com.nendo.argosy.ui.screens.home.HomeRowItem
import com.nendo.argosy.ui.screens.home.HomeViewModel
import com.nendo.argosy.ui.screens.library.LibraryFilterArgs
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.util.clickableNoFocus

@Composable
fun HelaHomeScreen(
    onGameSelect: (Long) -> Unit,
    onNavigateToLibrary: (platformId: Long?, sourceFilter: String?, filters: String?) -> Unit,
    onNavigateToCollections: (collectionId: Long) -> Unit,
    onNavigateToDefault: () -> Unit,
    onDrawerToggle: () -> Unit,
    onChangelogAction: (RequiredAction) -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onNavigateToApps: () -> Unit,
    onPlayMedia: (itemId: String, startOver: Boolean) -> Unit,
    onMediaSelect: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HelaHomeEvents(
        viewModel = viewModel,
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateToCollections = onNavigateToCollections,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToApps = onNavigateToApps,
        onPlayMedia = onPlayMedia,
        onMediaSelect = onMediaSelect
    )
    HelaHomeLifecycle(
        viewModel = viewModel,
        onGameSelect = onGameSelect,
        onNavigateToDefault = onNavigateToDefault,
        onDrawerToggle = onDrawerToggle
    )

    val wallpaper = rememberHelaWallpaper()
    val items = uiState.currentItems
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.currentRow, uiState.focusedGameIndex, items.size) {
        if (uiState.focusedGameIndex in items.indices) {
            listState.animateScrollToItem(uiState.focusedGameIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            HelaWallpaperLayer(wallpaper = wallpaper, modifier = Modifier.fillMaxSize(), scrim = true)
        }
        LazyRow(
            state = listState,
            modifier = Modifier.align(Alignment.Center),
            contentPadding = PaddingValues(horizontal = Dimens.spacingXl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(items, key = { index, item -> item.stableKey(index) }) { index, item ->
                val isFocused = index == uiState.focusedGameIndex
                val tap = Modifier.clickableNoFocus { viewModel.handleItemTap(index, onGameSelect) }
                when (item) {
                    is HomeRowItem.Game -> GameCard(
                        game = item.game,
                        isFocused = isFocused,
                        modifier = Modifier.width(Dimens.gameCardWidth).then(tap)
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(Dimens.gameCardWidth, Dimens.gameCardHeight)
                            .then(tap)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
            }
        }
    }
}

private fun HomeRowItem.stableKey(index: Int): String = when (this) {
    is HomeRowItem.Game -> "game-${game.id}"
    is HomeRowItem.Media -> "media-$index"
    is HomeRowItem.ViewAll -> "all-${platformId ?: sourceFilter ?: index}"
}

@Composable
private fun HelaHomeEvents(
    viewModel: HomeViewModel,
    onNavigateToLibrary: (platformId: Long?, sourceFilter: String?, filters: String?) -> Unit,
    onNavigateToCollections: (collectionId: Long) -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onNavigateToApps: () -> Unit,
    onPlayMedia: (itemId: String, startOver: Boolean) -> Unit,
    onMediaSelect: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.LaunchIntent -> runCatching {
                    context.startActivity(event.intent, event.options)
                }.onFailure { android.util.Log.e("HelaHomeScreen", "Failed to start activity", it) }
                is HomeEvent.NavigateToLibrary -> onNavigateToLibrary(
                    event.platformId,
                    event.sourceFilter,
                    event.tileFilters?.let { LibraryFilterArgs.encode(it) }
                )
                is HomeEvent.NavigateToCollections -> onNavigateToCollections(event.collectionId)
                is HomeEvent.NavigateToSettings -> onNavigateToSettings(event.section)
                HomeEvent.NavigateToApps -> onNavigateToApps()
                is HomeEvent.PlayMedia -> onPlayMedia(event.itemId, event.startOver)
                is HomeEvent.NavigateToMediaDetail -> onMediaSelect(event.itemId)
            }
        }
    }
}

@Composable
private fun HelaHomeLifecycle(
    viewModel: HomeViewModel,
    onGameSelect: (Long) -> Unit,
    onNavigateToDefault: () -> Unit,
    onDrawerToggle: () -> Unit
) {
    val dockIsRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val inputDispatcher = LocalInputDispatcher.current
    val inputHandler = remember(onGameSelect, onDrawerToggle, dockIsRtl) {
        viewModel.createInputHandler(
            isDefaultView = true,
            onGameSelect = onGameSelect,
            onNavigateToDefault = onNavigateToDefault,
            onDrawerToggle = {
                viewModel.dismissAppBarTools()
                onDrawerToggle()
            },
            dockIsRtl = dockIsRtl
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, inputHandler) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                inputDispatcher.subscribeView(inputHandler, forRoute = Screen.ROUTE_HOME)
                viewModel.onResume()
                viewModel.refreshPlatforms()
                viewModel.refreshFavorites()
                viewModel.refreshRecentGames()
                viewModel.republishCompanionDetail()
                viewModel.onHomeShown()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        inputDispatcher.subscribeView(inputHandler, forRoute = Screen.ROUTE_HOME)
        viewModel.republishCompanionDetail()
        viewModel.onHomeShown()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.clearCompanionDetail()
            viewModel.dismissAppBarTools()
            viewModel.onHomeHidden()
        }
    }
}
