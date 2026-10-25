package net.zg3.sudoku.data.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class InputMode { CELL_FIRST, DIGIT_FIRST }

/** What a plain tap does; a long-press always does the other. */
enum class TapAction { MARK, ENTER }

/** When arming a digit (digit-first) moves the selection to a cell holding that digit. */
enum class AutoSelect { ALWAYS, WHEN_FILLED, NEVER }

data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val inputMode: InputMode = InputMode.CELL_FIRST,
    val mistakeChecking: Boolean = true,
    val mistakeLimitEnabled: Boolean = false,
    val showTimer: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val tapAction: TapAction = TapAction.MARK,
    val allowOverwrite: Boolean = false,
    val tapRemovesMark: Boolean = true,
    val autoSelect: AutoSelect = AutoSelect.WHEN_FILLED,
    val hintLogging: Boolean = false
)
