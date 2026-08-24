package com.zedgama.sudoku.ui.technique

import com.zedgama.sudoku.engine.Board
import com.zedgama.sudoku.engine.Cell
import com.zedgama.sudoku.engine.Technique

/** A hand-crafted diagram illustrating one technique. */
data class TechniqueExample(
    val board: Board,
    val hintCells: List<Int>,                     // cells forming the pattern (tertiary tint)
    val secondaryCells: List<Int> = emptyList(),  // supporting/eliminated cells (secondary tint)
    val highlightDigit: Int? = null,
    val caption: String
)

data class TechniqueInfo(
    val technique: Technique,
    val whatItIs: String,
    val howToSpot: String,
    val example: TechniqueExample
)

private fun exampleBoard(
    values: Map<Int, Int>,
    notes: Map<Int, List<Int>> = emptyMap()
): Board = Board(
    List(81) { i ->
        Cell(
            value = values[i] ?: 0,
            isGiven = values.containsKey(i),
            notes = notes[i]?.fold(0) { acc, d -> acc or (1 shl (d - 1)) } ?: 0
        )
    }
)

fun techniqueInfo(technique: Technique): TechniqueInfo = when (technique) {
    Technique.NAKED_SINGLE -> TechniqueInfo(
        technique = technique,
        whatItIs = "A cell where only one candidate digit remains. Every other digit already " +
            "appears somewhere in that cell's row, column, or box, so the last candidate must " +
            "go there.",
        howToSpot = "Pick an empty cell and cross off every digit you can see in its row, its " +
            "column, and its 3×3 box. If exactly one digit survives, you've found a naked " +
            "single. Pencil marks make these easy to see: look for cells with a single note.",
        example = TechniqueExample(
            board = exampleBoard(
                values = mapOf(0 to 5, 1 to 3, 3 to 6, 4 to 7, 5 to 8, 6 to 9, 7 to 1, 8 to 2),
                notes = mapOf(2 to listOf(4))
            ),
            hintCells = listOf(2),
            highlightDigit = 4,
            caption = "Every digit except 4 already appears in the top row, so the highlighted " +
                "cell's only remaining candidate is 4 — it must be a 4."
        )
    )

    Technique.HIDDEN_SINGLE -> TechniqueInfo(
        technique = technique,
        whatItIs = "A digit that fits in only one cell of a row, column, or box — even if that " +
            "cell still has other candidates. Since the digit must appear somewhere in the " +
            "house, it has to go in that one cell.",
        howToSpot = "Pick a digit and a house (row, column, or box). Mentally mark every cell " +
            "the digit cannot occupy because it already appears in a crossing row or column. " +
            "If only one cell in the house remains possible, the digit belongs there.",
        example = TechniqueExample(
            board = exampleBoard(
                values = mapOf(8 to 5, 14 to 5, 45 to 5, 55 to 5),
                notes = mapOf(20 to listOf(3, 5, 9))
            ),
            hintCells = listOf(20),
            secondaryCells = listOf(8, 14, 45, 55),
            highlightDigit = 5,
            caption = "The 5s in the shaded cells rule 5 out of the first two rows and the " +
                "first two columns. Inside the top-left box that leaves a single cell for 5 — " +
                "even though that cell still shows other pencil marks."
        )
    )

    Technique.LOCKED_CANDIDATES -> TechniqueInfo(
        technique = technique,
        whatItIs = "When all of a digit's possible cells within a box fall on one row or " +
            "column, the digit must be placed on that line inside the box. It can then be " +
            "removed as a candidate from the rest of that row or column (and vice versa: a " +
            "digit confined to one box within a line clears the rest of the box).",
        howToSpot = "For each box and digit, look at where the digit can still go. If every " +
            "spot lies in a single row or column, erase that digit's pencil marks elsewhere " +
            "on the same line outside the box.",
        example = TechniqueExample(
            board = exampleBoard(
                values = mapOf(0 to 1, 1 to 2, 2 to 3, 9 to 4, 10 to 5, 11 to 6),
                notes = mapOf(
                    18 to listOf(7, 8, 9),
                    19 to listOf(7, 8, 9),
                    20 to listOf(7, 8, 9),
                    21 to listOf(1, 7),
                    25 to listOf(7, 9)
                )
            ),
            hintCells = listOf(18, 19, 20),
            secondaryCells = listOf(21, 25),
            highlightDigit = 7,
            caption = "In the top-left box, 7 can only go in the highlighted cells — all in " +
                "row 3. Wherever it lands, row 3's 7 is used up, so 7 can be erased from the " +
                "shaded cells further along the row."
        )
    )

    Technique.NAKED_PAIR -> TechniqueInfo(
        technique = technique,
        whatItIs = "Two cells in the same row, column, or box that hold exactly the same two " +
            "candidates. Between them they claim both digits, so those digits can be removed " +
            "from every other cell in that house.",
        howToSpot = "Scan each house for two cells whose pencil marks are the same two " +
            "digits. Once found, erase those two digits from the rest of the house — often " +
            "this reveals a naked single elsewhere.",
        example = TechniqueExample(
            board = exampleBoard(
                values = mapOf(
                    3 to 1, 4 to 3, 5 to 4, 6 to 6, 7 to 7, 8 to 9,
                    36 to 8, 46 to 8
                ),
                notes = mapOf(
                    0 to listOf(2, 5),
                    1 to listOf(2, 5),
                    2 to listOf(2, 5, 8)
                )
            ),
            hintCells = listOf(0, 1),
            secondaryCells = listOf(2),
            caption = "The two highlighted cells can each only be 2 or 5 (the 8s lower down " +
                "block their columns). Between them they use up both digits, so 2 and 5 can " +
                "be erased from the shaded cell — leaving it a naked single 8."
        )
    )

    Technique.HIDDEN_PAIR -> TechniqueInfo(
        technique = technique,
        whatItIs = "Two digits that, within one row, column, or box, can only go in the same " +
            "two cells. Those two cells must hold that pair, so every other candidate in them " +
            "can be erased.",
        howToSpot = "For each house, check each digit's possible cells. When two digits share " +
            "exactly the same two cells (and fit nowhere else in the house), clear all other " +
            "pencil marks from those two cells.",
        example = TechniqueExample(
            board = exampleBoard(
                values = mapOf(
                    2 to 1, 11 to 2, 18 to 3,
                    15 to 4, 16 to 7,
                    24 to 4, 25 to 7
                ),
                notes = mapOf(
                    0 to listOf(4, 5, 7),
                    1 to listOf(4, 6, 7, 9),
                    9 to listOf(5, 6),
                    10 to listOf(5, 9),
                    19 to listOf(5, 9),
                    20 to listOf(6, 9)
                )
            ),
            hintCells = listOf(0, 1),
            secondaryCells = listOf(15, 16, 24, 25),
            caption = "The 4s and 7s in the shaded cells block rows 2 and 3, so inside the " +
                "top-left box, 4 and 7 fit only in the two highlighted cells. Those cells " +
                "must be the pair 4-7, and their other pencil marks can be erased."
        )
    )
}
