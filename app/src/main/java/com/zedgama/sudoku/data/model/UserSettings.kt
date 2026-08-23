package com.zedgama.sudoku.data.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class InputMode { CELL_FIRST, DIGIT_FIRST }

data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val mistakeChecking: Boolean = true,
    val mistakeLimitEnabled: Boolean = false,
    val showTimer: Boolean = true,
    val autoRemoveNotes: Boolean = true
)
