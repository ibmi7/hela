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
    private val onTab: (HelaHomeTab) -> Unit,
    private val onFolder: (HelaFolderTarget) -> Unit
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

    private fun folders(): List<HelaFolderTarget> = homeViewModel.uiState.value.helaFolders()

    private val onConsoles: Boolean get() = helaViewModel.uiState.value.onConsoles && folders().isNotEmpty()

    private val noPages: Boolean
        get() = homeViewModel.uiState.value.helaPages.isEmpty() && folders().isEmpty()

    private fun move(direction: HelaGridDirection): InputResult =
        if (onConsoles) moveInConsoles(direction) else moveInGrid(direction)

    override fun onUp(): InputResult = when {
        overlayOpen() -> home.onUp()
        onTabs -> InputResult.handled(SoundType.BOUNDARY)
        else -> move(HelaGridDirection.UP)
    }

    override fun onDown(): InputResult = when {
        overlayOpen() -> home.onDown()
        onTabs -> {
            helaViewModel.focusGrid()
            InputResult.handled(SoundType.SECTION_CHANGE)
        }
        else -> move(HelaGridDirection.DOWN)
    }

    override fun onLeft(): InputResult = when {
        overlayOpen() -> home.onLeft()
        onTabs -> {
            helaViewModel.moveTab(-1)
            InputResult.HANDLED
        }
        else -> move(HelaGridDirection.LEFT)
    }

    override fun onRight(): InputResult = when {
        overlayOpen() -> home.onRight()
        onTabs -> {
            helaViewModel.moveTab(1)
            InputResult.HANDLED
        }
        else -> move(HelaGridDirection.RIGHT)
    }

    override fun onConfirm(): InputResult = when {
        overlayOpen() -> home.onConfirm()
        onTabs -> {
            onTab(helaViewModel.uiState.value.focusedTab)
            InputResult.handled(SoundType.SELECT)
        }
        onConsoles -> {
            folders().getOrNull(helaViewModel.uiState.value.consoleIndex)?.let(onFolder)
            InputResult.handled(SoundType.SELECT)
        }
        noPages -> {
            homeViewModel.syncFromRomm()
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
        onConsoles && homeViewModel.uiState.value.helaPages.isNotEmpty() -> {
            helaViewModel.leaveConsoles()
            InputResult.handled(SoundType.BACK)
        }
        else -> home.onBack()
    }

    override fun onSecondaryAction(): InputResult =
        if (onConsoles && !overlayOpen()) InputResult.handled(SoundType.SILENT) else home.onSecondaryAction()

    override fun onContextMenu(): InputResult =
        if (onConsoles && !overlayOpen()) InputResult.handled(SoundType.SILENT) else home.onContextMenu()

    override fun onSelect(): InputResult =
        if (onConsoles && !overlayOpen()) InputResult.UNHANDLED else home.onSelect()

    override fun onPrevSection(): InputResult = if (overlayOpen()) home.onPrevSection() else changePage(-1)

    override fun onNextSection(): InputResult = if (overlayOpen()) home.onNextSection() else changePage(1)

    override fun onPrevTrigger(): InputResult = if (overlayOpen()) home.onPrevTrigger() else changePage(-1)

    override fun onNextTrigger(): InputResult = if (overlayOpen()) home.onNextTrigger() else changePage(1)

    private fun moveInGrid(direction: HelaGridDirection): InputResult {
        val state = homeViewModel.uiState.value
        val columns = helaViewModel.uiState.value.columns
        if (state.helaPages.isEmpty()) {
            if (direction == HelaGridDirection.UP) helaViewModel.focusTabs()
            return InputResult.handled(SoundType.BOUNDARY)
        }
        if (columns <= 0) return InputResult.handled(SoundType.BOUNDARY)
        val cells = HelaGridLayout.cells(
            count = state.currentItems.size,
            columns = columns,
            wideFirst = state.currentRow == HomeRow.Continue
        )
        return applyMove(HelaGridLayout.move(cells, state.focusedGameIndex, direction)) {
            homeViewModel.setFocusIndex(it)
        }
    }

    private fun moveInConsoles(direction: HelaGridDirection): InputResult {
        val columns = helaViewModel.uiState.value.columns
        if (columns <= 0) return InputResult.handled(SoundType.BOUNDARY)
        val cells = HelaGridLayout.cells(count = folders().size, columns = columns, wideFirst = false)
        return applyMove(HelaGridLayout.move(cells, helaViewModel.uiState.value.consoleIndex, direction)) {
            helaViewModel.setConsoleIndex(it)
        }
    }

    private fun applyMove(move: HelaGridMove, focus: (Int) -> Unit): InputResult = when (move) {
        is HelaGridMove.Focus -> {
            focus(move.index)
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

    private fun changePage(delta: Int, focusLast: Boolean = false): InputResult {
        val pages = homeViewModel.uiState.value.helaPages
        val folders = folders()
        val count = pages.size + if (folders.isEmpty()) 0 else 1
        if (count <= 1) return InputResult.handled(SoundType.BOUNDARY)
        val current = if (onConsoles) pages.size else pages.indexOf(homeViewModel.uiState.value.currentRow).coerceAtLeast(0)
        val target = (current + delta).mod(count)
        if (target == pages.size) {
            helaViewModel.enterConsoles(if (focusLast) folders.lastIndex else 0)
        } else {
            helaViewModel.leaveConsoles()
            homeViewModel.selectRow(pages[target])
            if (focusLast) {
                val last = homeViewModel.uiState.value.currentItems.lastIndex
                if (last > 0) homeViewModel.setFocusIndex(last)
            }
            helaViewModel.focusGrid()
        }
        return InputResult.handled(SoundType.SECTION_CHANGE)
    }
}
