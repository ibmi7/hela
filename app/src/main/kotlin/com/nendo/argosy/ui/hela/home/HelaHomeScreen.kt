package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nendo.argosy.domain.model.RequiredAction
import com.nendo.argosy.ui.hela.theme.HelaWallpaperLayer
import com.nendo.argosy.ui.hela.theme.rememberHelaWallpaper
import com.nendo.argosy.ui.input.LocalInputDispatcher
import com.nendo.argosy.ui.navigation.Screen
import com.nendo.argosy.ui.components.FooterSpacer
import com.nendo.argosy.ui.screens.home.EmptyState
import com.nendo.argosy.ui.screens.home.HomeEvent
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.screens.home.SplashOverlay
import com.nendo.argosy.ui.screens.home.HomeViewModel
import com.nendo.argosy.ui.screens.library.LibraryFilterArgs
import com.nendo.argosy.ui.theme.Dimens

@Composable
fun HelaHomeScreen(
    onGameSelect: (Long) -> Unit,
    onNavigateToLibrary: (platformId: Long?, sourceFilter: String?, filters: String?) -> Unit,
    onNavigateToCollections: (collectionId: Long) -> Unit,
    onNavigateToDefault: () -> Unit,
    onDrawerToggle: () -> Unit,
    onChangelogAction: (RequiredAction) -> Unit,
    onNavigateToSettings: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToApps: () -> Unit,
    onPlayMedia: (itemId: String, startOver: Boolean) -> Unit,
    onMediaSelect: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    helaViewModel: HelaHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val helaState by helaViewModel.uiState.collectAsState()
    val downloadIndicators = viewModel.downloadIndicators.collectAsState()
    val pages = uiState.helaPages

    val folderTargets = remember(uiState.platforms, pages) { uiState.helaFolders() }
    val folderPack = rememberHelaFolders()
    val onConsoles = helaState.onConsoles && folderTargets.isNotEmpty()

    LaunchedEffect(pages, uiState.currentRow) {
        if (pages.isNotEmpty() && uiState.currentRow !in pages) viewModel.selectRow(pages.first())
    }
    LaunchedEffect(pages.isEmpty(), folderTargets.isEmpty(), uiState.isLoading) {
        if (!uiState.isLoading && pages.isEmpty() && folderTargets.isNotEmpty()) helaViewModel.enterConsoles()
    }

    val openFolder: (HelaFolderTarget) -> Unit = remember(onNavigateToLibrary, viewModel, helaViewModel) {
        { target ->
            when (target) {
                is HelaFolderTarget.Platform -> onNavigateToLibrary(target.platform.id, null, null)
                is HelaFolderTarget.Special -> {
                    helaViewModel.leaveConsoles()
                    viewModel.selectRow(target.row)
                }
            }
        }
    }

    val focusedFolder = folderTargets.getOrNull(helaState.consoleIndex).takeIf { onConsoles }
    val folderDetail = focusedFolder?.let { helaFolderDetail(it.label(), it.art(folderPack)) }
    LaunchedEffect(onConsoles, folderDetail) {
        if (onConsoles) {
            viewModel.clearCompanionDetail()
            helaViewModel.publishFolder(folderDetail)
        } else {
            helaViewModel.publishFolder(null)
            viewModel.republishCompanionDetail()
        }
    }
    DisposableEffect(helaViewModel) {
        onDispose { helaViewModel.publishFolder(null) }
    }

    val openTab: (HelaHomeTab) -> Unit = remember(
        onNavigateToLibrary, onNavigateToCollections, onNavigateToApps, onOpenSettings, helaViewModel
    ) {
        { tab ->
            when (tab) {
                HelaHomeTab.LIBRARY -> onNavigateToLibrary(null, null, null)
                HelaHomeTab.COLLECTIONS -> onNavigateToCollections(0)
                HelaHomeTab.APPS -> onNavigateToApps()
                HelaHomeTab.SETTINGS -> onOpenSettings()
            }
            helaViewModel.focusGrid()
        }
    }

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
        helaViewModel = helaViewModel,
        openTab = openTab,
        openFolder = openFolder,
        onGameSelect = onGameSelect,
        onNavigateToDefault = onNavigateToDefault,
        onDrawerToggle = onDrawerToggle
    )

    val wallpaper = rememberHelaWallpaper()
    val onTabs = helaState.zone == HelaHomeZone.TABS

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            HelaWallpaperLayer(wallpaper = wallpaper, modifier = Modifier.fillMaxSize(), scrim = true)
        }
        if (uiState.isLoading) {
            SplashOverlay()
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HelaHomeTabBar(
                focusedTab = helaState.focusedTab.takeIf { onTabs },
                onTab = { tab ->
                    helaViewModel.focusTab(tab)
                    openTab(tab)
                }
            )
            if (pages.isEmpty() && folderTargets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        isRommConfigured = uiState.isRommConfigured,
                        currentRow = HomeRow.Continue,
                        isPinnedLoading = false,
                        onSync = viewModel::syncFromRomm
                    )
                }
            } else {
                if (onConsoles) HelaFoldersGrid(
                    targets = folderTargets,
                    folders = folderPack,
                    focusedIndex = helaState.consoleIndex.takeIf { !onTabs },
                    onColumns = helaViewModel::setColumns,
                    onTap = { index ->
                        helaViewModel.focusGrid()
                        if (index == helaState.consoleIndex) {
                            folderTargets.getOrNull(index)?.let(openFolder)
                        } else {
                            helaViewModel.setConsoleIndex(index)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) else HelaHomeGrid(
                    items = if (uiState.currentRow in pages) uiState.currentItems else emptyList(),
                    page = uiState.currentRow,
                    focusedIndex = uiState.focusedGameIndex.takeIf { !onTabs },
                    downloadIndicators = downloadIndicators,
                    onColumns = helaViewModel::setColumns,
                    onTap = { index ->
                        helaViewModel.focusGrid()
                        viewModel.handleItemTap(index, onGameSelect)
                    },
                    onLongPress = { index ->
                        helaViewModel.focusGrid()
                        viewModel.handleItemLongPress(index)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                HelaHomePageDots(
                    pages = pages,
                    current = uiState.currentRow,
                    onSelect = { row ->
                        helaViewModel.focusGrid()
                        helaViewModel.leaveConsoles()
                        viewModel.selectRow(row)
                    },
                    hasConsoles = folderTargets.isNotEmpty(),
                    onConsoles = onConsoles,
                    onSelectConsoles = { helaViewModel.enterConsoles() }
                )
                HelaHomeFooter(
                    uiState = uiState,
                    onTabs = onTabs,
                    pageCount = pages.size + if (folderTargets.isEmpty()) 0 else 1,
                    onConsoles = onConsoles,
                    onOpenFolder = { focusedFolder?.let(openFolder) },
                    downloadIndicators = downloadIndicators,
                    viewModel = viewModel,
                    onGameSelect = onGameSelect
                )
            }
            FooterSpacer()
        }
        HelaHomeOverlays(
            viewModel = viewModel,
            uiState = uiState,
            downloadIndicators = downloadIndicators,
            onGameSelect = onGameSelect,
            onChangelogAction = onChangelogAction
        )
    }
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
    helaViewModel: HelaHomeViewModel,
    openTab: (HelaHomeTab) -> Unit,
    openFolder: (HelaFolderTarget) -> Unit,
    onGameSelect: (Long) -> Unit,
    onNavigateToDefault: () -> Unit,
    onDrawerToggle: () -> Unit
) {
    val dockIsRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val inputDispatcher = LocalInputDispatcher.current
    val currentOpenTab by rememberUpdatedState(openTab)
    val currentOpenFolder by rememberUpdatedState(openFolder)
    val inputHandler = remember(onGameSelect, onDrawerToggle, dockIsRtl) {
        HelaHomeInputHandler(
            home = viewModel.createInputHandler(
                isDefaultView = true,
                onGameSelect = onGameSelect,
                onNavigateToDefault = onNavigateToDefault,
                onDrawerToggle = {
                    viewModel.dismissAppBarTools()
                    onDrawerToggle()
                },
                dockIsRtl = dockIsRtl
            ),
            homeViewModel = viewModel,
            helaViewModel = helaViewModel,
            onTab = { tab -> currentOpenTab(tab) },
            onFolder = { target -> currentOpenFolder(target) }
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
                if (helaViewModel.uiState.value.onConsoles) {
                    viewModel.clearCompanionDetail()
                    helaViewModel.republishFolder()
                }
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
