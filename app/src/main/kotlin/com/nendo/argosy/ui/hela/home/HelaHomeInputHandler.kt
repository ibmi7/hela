package com.nendo.argosy.ui.hela.home

import com.nendo.argosy.DualScreenManagerHolder
import com.nendo.argosy.core.input.SoundType
import com.nendo.argosy.ui.input.InputHandler
import com.nendo.argosy.ui.input.InputResult
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.screens.home.HomeViewModel

class HelaHomeInputHandler(
    private val home: InputHandler,
    private val homeViewModel: HomeViewModel,
    private val helaViewModel: HelaHomeViewModel,
    private val onTab: (HelaHomeTab) -> Unit
) : InputHandler by home {

    private fun overlayOpen(): Boolean {
        val state = homeViewModel.uiState.value
        return state.showGameMenu ||
            state.showAddToCollectionModal ||
            state.showCreateCollectionDialog ||
            state.appBarMenu != null ||
            state.appBarFocused ||
            state.mediaResumePrompt != null ||
            homeViewModel.siblingChoiceState.value != null ||
            DualScreenManagerHolder.instance?.focusPickerOpen?.value == true
    }

    private val onTabs: Boolean get() = helaViewModel.uiState.value.zone == HelaHomeZone.TABS

    override fun onUp(): InputResult = when {
        overlayOpen() -> home.onUp()
        onTabs -> InputResult.handled(SoundType.BOUNDARY)
        else -> moveInGrid(HelaGridDirection.UP)
    }

    override fun onDown(): InputResult = when {
        overlayOpen() -> home.onDown()
        onTabs -> {
            helaViewModel.focusGrid()
            InputResult.handled(SoundType.SECTION_CHANGE)
        }
        else -> moveInGrid(HelaGridDirection.DOWN)
    }

    override fun onLeft(): InputResult = when {
        overlayOpen() -> home.onLeft()
        onTabs -> {
            helaViewModel.moveTab(-1)
            InputResult.HANDLED
        }
        else -> moveInGrid(HelaGridDirection.LEFT)
    }

    override fun onRight(): InputResult = when {
        overlayOpen() -> home.onRight()
        onTabs -> {
            helaViewModel.moveTab(1)
            InputResult.HANDLED
        }
        else -> moveInGrid(HelaGridDirection.RIGHT)
    }

    override fun onConfirm(): InputResult = when {
        overlayOpen() -> home.onConfirm()
        onTabs -> {
            onTab(helaViewModel.uiState.value.focusedTab)
            InputResult.handled(SoundType.SELECT)
        }
        else -> home.onConfirm()
    }

    override fun onBack(): InputResult = when {
        overlayOpen() -> home.onBack()
        onTabs -> {
            helaViewModel.focusGrid()
            InputResult.handled(SoundType.BACK)
        }
        else -> home.onBack()
    }

    override fun onPrevSection(): InputResult = if (overlayOpen()) home.onPrevSection() else changePage(-1)

    override fun onNextSection(): InputResult = if (overlayOpen()) home.onNextSection() else changePage(1)

    override fun onPrevTrigger(): InputResult = if (overlayOpen()) home.onPrevTrigger() else changePage(-1)

    override fun onNextTrigger(): InputResult = if (overlayOpen()) home.onNextTrigger() else changePage(1)

    private fun moveInGrid(direction: HelaGridDirection): InputResult {
        val state = homeViewModel.uiState.value
        val columns = helaViewModel.uiState.value.columns
        if (columns <= 0) return InputResult.handled(SoundType.BOUNDARY)
        val cells = HelaGridLayout.cells(
            count = state.currentItems.size,
            columns = columns,
            wideFirst = state.currentRow == HomeRow.Continue
        )
        return when (val move = HelaGridLayout.move(cells, state.focusedGameIndex, direction)) {
            is HelaGridMove.Focus -> {
                homeViewModel.setFocusIndex(move.index)
                InputResult.HANDLED
            }
            HelaGridMove.PreviousPage -> changePage(-1, focusLast = true)
            HelaGridMove.NextPage -> changePage(1)
            HelaGridMove.Tabs -> {
                helaViewModel.focusTabs()
                InputResult.handled(SoundType.SECTION_CHANGE)
            }
            HelaGridMove.Boundary -> InputResult.handled(SoundType.BOUNDARY)
        }
    }

    private fun changePage(delta: Int, focusLast: Boolean = false): InputResult {
        val pages = homeViewModel.uiState.value.helaPages
        if (pages.size <= 1) return InputResult.handled(SoundType.BOUNDARY)
        val current = pages.indexOf(homeViewModel.uiState.value.currentRow).coerceAtLeast(0)
        homeViewModel.selectRow(pages[(current + delta).mod(pages.size)])
        if (focusLast) {
            val last = homeViewModel.uiState.value.currentItems.lastIndex
            if (last > 0) homeViewModel.setFocusIndex(last)
        }
        helaViewModel.focusGrid()
        return InputResult.handled(SoundType.SECTION_CHANGE)
    }
}
