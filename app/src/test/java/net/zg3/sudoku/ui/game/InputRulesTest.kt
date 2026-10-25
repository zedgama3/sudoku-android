package net.zg3.sudoku.ui.game

import net.zg3.sudoku.data.model.AutoSelect
import net.zg3.sudoku.engine.Board
import net.zg3.sudoku.engine.Cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputRulesTest {

    private fun enter(cell: Cell, digit: Int, overwrite: Boolean = false) =
        resolveEntry(cell, digit, asMark = false, allowOverwrite = overwrite, tapRemovesMark = true)

    private fun mark(cell: Cell, digit: Int, removes: Boolean = true) =
        resolveEntry(cell, digit, asMark = true, allowOverwrite = false, tapRemovesMark = removes)

    @Test
    fun enteringSameDigitNeverClears() {
        assertNull(enter(Cell(value = 5), 5))
        assertNull(enter(Cell(value = 5), 5, overwrite = true))
    }

    @Test
    fun overwriteRespectsSetting() {
        assertNull(enter(Cell(value = 5), 3))
        assertEquals(Cell(value = 3), enter(Cell(value = 5), 3, overwrite = true))
    }

    @Test
    fun enteringIntoEmptyCellClearsItsMarks() {
        assertEquals(Cell(value = 4), enter(Cell(notes = 0b1010), 4))
    }

    @Test
    fun givensAreNeverChanged() {
        assertNull(enter(Cell(value = 5, isGiven = true), 3, overwrite = true))
        assertNull(mark(Cell(value = 5, isGiven = true), 3))
    }

    @Test
    fun markAddsAndTogglesPerSetting() {
        assertEquals(Cell(notes = 0b100), mark(Cell(), 3))
        assertEquals(Cell(notes = 0), mark(Cell(notes = 0b100), 3, removes = true))
        assertNull(mark(Cell(notes = 0b100), 3, removes = false))
    }

    @Test
    fun markOnFilledCellDoesNothing() {
        assertNull(mark(Cell(value = 7), 3))
    }

    // --- auto-select ---

    /** Board with 5s at r0c0 (0), r4c4 (40) and r8c8 (80); a 2 at r4c0 (36). */
    private val board = Board.empty()
        .with(0, Cell(value = 5))
        .with(40, Cell(value = 5))
        .with(80, Cell(value = 5))
        .with(36, Cell(value = 2))

    @Test
    fun whenFilledLeavesBlankSelectionAlone() {
        assertNull(pickAutoSelectCell(board, 37, 5, AutoSelect.WHEN_FILLED))
        assertNull(pickAutoSelectCell(board.with(37, Cell(notes = 0b10000)), 37, 5, AutoSelect.WHEN_FILLED))
        assertNull(pickAutoSelectCell(board, null, 5, AutoSelect.WHEN_FILLED))
    }

    @Test
    fun whenFilledMovesToNearestMatch() {
        // from r4c0, r4c4 is 4 away and r0c0 is 4 away; tie goes to reading order (index 0)
        assertEquals(0, pickAutoSelectCell(board, 36, 5, AutoSelect.WHEN_FILLED))
        // from r7c7 (a filled cell), r8c8 is nearest
        assertEquals(80, pickAutoSelectCell(board.with(70, Cell(value = 1)), 70, 5, AutoSelect.WHEN_FILLED))
    }

    @Test
    fun noMoveWhenAlreadyOnDigitOrNoMatch() {
        assertNull(pickAutoSelectCell(board, 40, 5, AutoSelect.ALWAYS))
        assertNull(pickAutoSelectCell(board, 36, 9, AutoSelect.ALWAYS))
    }

    @Test
    fun alwaysMovesFromBlankOrNothing() {
        assertEquals(40, pickAutoSelectCell(board, 41, 5, AutoSelect.ALWAYS))
        assertEquals(0, pickAutoSelectCell(board, null, 5, AutoSelect.ALWAYS))
    }

    @Test
    fun neverMoves() {
        assertNull(pickAutoSelectCell(board, 36, 5, AutoSelect.NEVER))
    }
}
