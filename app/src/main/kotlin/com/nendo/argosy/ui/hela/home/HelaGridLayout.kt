package com.nendo.argosy.ui.hela.home

data class HelaGridCell(val row: Int, val column: Int, val span: Int)

enum class HelaGridDirection { UP, DOWN, LEFT, RIGHT }

sealed interface HelaGridMove {
    data class Focus(val index: Int) : HelaGridMove
    data object PreviousPage : HelaGridMove
    data object NextPage : HelaGridMove
    data object Tabs : HelaGridMove
    data object Boundary : HelaGridMove
}

object HelaGridLayout {

    /**
     * Row-major placement of [count] tiles on [columns] columns, the first tile two columns wide
     * when [wideFirst] holds and the grid has room for it. A tile that does not fit the rest of a
     * row starts the next one.
     */
    fun cells(count: Int, columns: Int, wideFirst: Boolean): List<HelaGridCell> {
        if (count <= 0 || columns <= 0) return emptyList()
        val cells = ArrayList<HelaGridCell>(count)
        var row = 0
        var column = 0
        repeat(count) { index ->
            val span = if (index == 0 && wideFirst && columns >= 2) 2 else 1
            if (column + span > columns) {
                row++
                column = 0
            }
            cells += HelaGridCell(row, column, span)
            column += span
            if (column >= columns) {
                row++
                column = 0
            }
        }
        return cells
    }

    fun move(cells: List<HelaGridCell>, index: Int, direction: HelaGridDirection): HelaGridMove {
        val cell = cells.getOrNull(index) ?: return if (cells.isEmpty()) {
            if (direction == HelaGridDirection.UP) HelaGridMove.Tabs else HelaGridMove.Boundary
        } else {
            HelaGridMove.Focus(0)
        }
        return when (direction) {
            HelaGridDirection.LEFT ->
                if (cell.column == 0) HelaGridMove.PreviousPage else HelaGridMove.Focus(index - 1)
            HelaGridDirection.RIGHT -> {
                val next = cells.getOrNull(index + 1)
                if (next != null && next.row == cell.row) HelaGridMove.Focus(index + 1) else HelaGridMove.NextPage
            }
            HelaGridDirection.UP ->
                closestInRow(cells, cell.row - 1, cell.column) ?: HelaGridMove.Tabs
            HelaGridDirection.DOWN ->
                closestInRow(cells, cell.row + 1, cell.column) ?: HelaGridMove.Boundary
        }
    }

    private fun closestInRow(cells: List<HelaGridCell>, row: Int, column: Int): HelaGridMove.Focus? {
        val inRow = cells.withIndex().filter { it.value.row == row }
        if (inRow.isEmpty()) return null
        val covering = inRow.firstOrNull { column in it.value.column until it.value.column + it.value.span }
        val target = covering ?: inRow.minBy { kotlin.math.abs(it.value.column - column) }
        return HelaGridMove.Focus(target.index)
    }
}
