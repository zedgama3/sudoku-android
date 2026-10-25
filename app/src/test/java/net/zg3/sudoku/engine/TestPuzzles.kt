package net.zg3.sudoku.engine

object TestPuzzles {
    // Classic example puzzle (Wikipedia) and its unique solution
    const val KNOWN_PUZZLE =
        "530070000" + "600195000" + "098000060" +
        "800060003" + "400803001" + "700020006" +
        "060000280" + "000419005" + "000080079"

    const val KNOWN_SOLUTION =
        "534678912" + "672195348" + "198342567" +
        "859761423" + "426853791" + "713924856" +
        "961537284" + "287419635" + "345286179"

    fun parse(s: String): IntArray = IntArray(81) { s[it] - '0' }
}
