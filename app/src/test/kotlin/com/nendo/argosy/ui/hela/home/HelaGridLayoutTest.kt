package com.nendo.argosy.ui.hela.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelaGridLayoutTest {

    @Test
    fun `a wide first tile takes two columns and the row wraps after the last column`() {
        val cells = HelaGridLayout.cells(count = 6, columns = 4, wideFirst = true)
        assertEquals(
            listOf(
                HelaGridCell(0, 0, 2), HelaGridCell(0, 2, 1), HelaGridCell(0, 3, 1),
                HelaGridCell(1, 0, 1), HelaGridCell(1, 1, 1), HelaGridCell(1, 2, 1)
            ),
            cells
        )
    }

    @Test
    fun `without a wide tile every tile spans one column`() {
        val cells = HelaGridLayout.cells(count = 5, columns = 3, wideFirst = false)
        assertTrue(cells.all { it.span == 1 })
        assertEquals(HelaGridCell(1, 1, 1), cells[4])
    }

    @Test
    fun `a single column grid keeps the first tile narrow`() {
        val cells = HelaGridLayout.cells(count = 2, columns = 1, wideFirst = true)
        assertEquals(listOf(HelaGridCell(0, 0, 1), HelaGridCell(1, 0, 1)), cells)
    }

    @Test
    fun `an empty grid has no cells`() {
        assertTrue(HelaGridLayout.cells(count = 0, columns = 4, wideFirst = true).isEmpty())
    }

    @Test
    fun `left on the first column and right on the last tile of a row change page`() {
        val cells = HelaGridLayout.cells(count = 6, columns = 4, wideFirst = true)
        assertEquals(HelaGridMove.PreviousPage, HelaGridLayout.move(cells, 3, HelaGridDirection.LEFT))
        assertEquals(HelaGridMove.NextPage, HelaGridLayout.move(cells, 2, HelaGridDirection.RIGHT))
        assertEquals(HelaGridMove.NextPage, HelaGridLayout.move(cells, 5, HelaGridDirection.RIGHT))
        assertEquals(HelaGridMove.Focus(1), HelaGridLayout.move(cells, 0, HelaGridDirection.RIGHT))
        assertEquals(HelaGridMove.Focus(4), HelaGridLayout.move(cells, 5, HelaGridDirection.LEFT))
    }

    @Test
    fun `up from the first row reaches the tabs`() {
        val cells = HelaGridLayout.cells(count = 6, columns = 4, wideFirst = true)
        assertEquals(HelaGridMove.Tabs, HelaGridLayout.move(cells, 2, HelaGridDirection.UP))
    }

    @Test
    fun `vertical moves land on the tile covering the same column`() {
        val cells = HelaGridLayout.cells(count = 6, columns = 4, wideFirst = true)
        assertEquals(HelaGridMove.Focus(0), HelaGridLayout.move(cells, 4, HelaGridDirection.UP))
        assertEquals(HelaGridMove.Focus(3), HelaGridLayout.move(cells, 0, HelaGridDirection.DOWN))
        assertEquals(HelaGridMove.Focus(5), HelaGridLayout.move(cells, 1, HelaGridDirection.DOWN))
    }

    @Test
    fun `down into a shorter row lands on its nearest tile and down from the last row is a boundary`() {
        val cells = HelaGridLayout.cells(count = 6, columns = 4, wideFirst = true)
        assertEquals(HelaGridMove.Focus(5), HelaGridLayout.move(cells, 2, HelaGridDirection.DOWN))
        assertEquals(HelaGridMove.Boundary, HelaGridLayout.move(cells, 4, HelaGridDirection.DOWN))
    }

    @Test
    fun `an out of range index refocuses the first tile and an empty grid only reaches the tabs`() {
        val cells = HelaGridLayout.cells(count = 3, columns = 4, wideFirst = false)
        assertEquals(HelaGridMove.Focus(0), HelaGridLayout.move(cells, 7, HelaGridDirection.RIGHT))
        assertEquals(HelaGridMove.Tabs, HelaGridLayout.move(emptyList(), 0, HelaGridDirection.UP))
        assertEquals(HelaGridMove.Boundary, HelaGridLayout.move(emptyList(), 0, HelaGridDirection.LEFT))
    }
}
