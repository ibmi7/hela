package com.nendo.argosy.ui.hela.home

import androidx.lifecycle.ViewModel
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
    val columns: Int = 0
) {
    val focusedTab: HelaHomeTab get() = HelaHomeTab.entries[tabIndex]
}

@HiltViewModel
class HelaHomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HelaHomeUiState())
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
}

private val HELA_PAGE_ROWS = listOf(HomeRow.Continue, HomeRow.Favorites, HomeRow.Recommendations)

/**
 * The Argosy rows the Hela home shows as pages, in Argosy's own order.
 */
val HomeUiState.helaPages: List<HomeRow>
    get() = availableRows.filter { it in HELA_PAGE_ROWS }
