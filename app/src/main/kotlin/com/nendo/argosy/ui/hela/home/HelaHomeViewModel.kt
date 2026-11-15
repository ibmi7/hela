package com.nendo.argosy.ui.hela.home

import androidx.lifecycle.ViewModel
import com.nendo.argosy.DualScreenManagerHolder
import com.nendo.argosy.data.hela.home.HelaHomeStore
import com.nendo.argosy.data.hela.theme.CocoonSmartFolders
import com.nendo.argosy.data.model.SortOption
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import com.nendo.argosy.ui.dualscreen.SlotOwner
import com.nendo.argosy.ui.screens.home.HomePlatformUi
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.screens.home.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class HelaHomeZone { TABS, GRID }

enum class HelaHomeTab { LIBRARY, COLLECTIONS, APPS, SETTINGS }

data class HelaHomeUiState(
    val zone: HelaHomeZone = HelaHomeZone.GRID,
    val tabIndex: Int = 0,
    val columns: Int = 0,
    val onConsoles: Boolean = false,
    val consoleIndex: Int = 0
) {
    val focusedTab: HelaHomeTab get() = HelaHomeTab.entries[tabIndex]
}

@HiltViewModel
class HelaHomeViewModel @Inject constructor(
    private val store: HelaHomeStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        store.consolesPosition
            ?.let { HelaHomeUiState(onConsoles = true, consoleIndex = it) }
            ?: HelaHomeUiState()
    )
    val uiState: StateFlow<HelaHomeUiState> = _uiState.asStateFlow()

    fun focusTabs() = _uiState.update { it.copy(zone = HelaHomeZone.TABS) }

    fun focusGrid() = _uiState.update { it.copy(zone = HelaHomeZone.GRID) }

    fun moveTab(delta: Int) = _uiState.update {
        it.copy(tabIndex = (it.tabIndex + delta).mod(HelaHomeTab.entries.size))
    }

    fun focusTab(tab: HelaHomeTab) = _uiState.update {
        it.copy(zone = HelaHomeZone.TABS, tabIndex = tab.ordinal)
    }

    fun setColumns(columns: Int) = _uiState.update { it.copy(columns = columns) }

    fun enterConsoles(index: Int = 0) {
        val focused = index.coerceAtLeast(0)
        store.consolesPosition = focused
        _uiState.update { it.copy(onConsoles = true, consoleIndex = focused, zone = HelaHomeZone.GRID) }
    }

    fun leaveConsoles() {
        store.consolesPosition = null
        _uiState.update { it.copy(onConsoles = false) }
    }

    fun setConsoleIndex(index: Int) {
        if (_uiState.value.onConsoles) store.consolesPosition = index
        _uiState.update { it.copy(consoleIndex = index) }
    }

    private val folderOwner = SlotOwner.of("hela.home", this)
    private var folderDetail: CompanionDetail? = null

    fun publishFolder(detail: CompanionDetail?) {
        folderDetail = detail
        DualScreenManagerHolder.instance?.setCompanionDetail(folderOwner, detail)
    }

    fun republishFolder() = publishFolder(folderDetail)

    override fun onCleared() {
        publishFolder(null)
    }
}

sealed interface HelaFolderTarget {
    data class Special(val row: HomeRow, val artKey: String) : HelaFolderTarget
    data class Sorted(val sort: SortOption, val artKey: String) : HelaFolderTarget
    data class Platform(val platform: HomePlatformUi) : HelaFolderTarget
}

const val HELA_FOLDER_SLUG = "hela:folder"

/**
 * The tiles of the consoles page: the favorites and recent pages when they exist, the library
 * sorted by play time and by date added, then one tile per platform. Empty when the library
 * has no platform.
 */
fun HomeUiState.helaFolders(): List<HelaFolderTarget> {
    if (platforms.isEmpty()) return emptyList()
    val pages = helaPages
    return buildList {
        if (HomeRow.Favorites in pages) add(HelaFolderTarget.Special(HomeRow.Favorites, CocoonSmartFolders.FAVORITES))
        if (HomeRow.Continue in pages) add(HelaFolderTarget.Special(HomeRow.Continue, CocoonSmartFolders.RECENT))
        add(HelaFolderTarget.Sorted(SortOption.PLAY_TIME, CocoonSmartFolders.MOST_PLAYED))
        add(HelaFolderTarget.Sorted(SortOption.RECENTLY_ADDED, CocoonSmartFolders.NEWLY_ADDED))
        platforms.forEach { add(HelaFolderTarget.Platform(it)) }
    }
}

private val HELA_PAGE_ROWS = listOf(HomeRow.Continue, HomeRow.Favorites, HomeRow.Recommendations)

/**
 * The Argosy rows the Hela home shows as pages, in Argosy's own order.
 */
val HomeUiState.helaPages: List<HomeRow>
    get() = availableRows.filter { it in HELA_PAGE_ROWS }
