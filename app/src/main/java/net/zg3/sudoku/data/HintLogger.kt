package net.zg3.sudoku.data

import net.zg3.sudoku.engine.HintPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LoggedStep(
    val text: String,
    val primary: List<Int>,
    val secondary: List<Int>,
    val digit: Int? = null
)

@Serializable
data class LoggedPlan(
    val cell: Int,
    val digit: Int,
    val techniques: List<String>,
    val isReveal: Boolean,
    val steps: List<LoggedStep>
)

/** One hint press: the full board it was asked on, and everything the hint said. */
@Serializable
data class HintLogEntry(
    val type: String = "hint",
    val time: Long,
    val version: String,
    val difficulty: String,
    val givens: String,
    val solution: String,
    val current: String,
    val notes: List<Int>,
    val selected: Int?,
    val mistakeChecking: Boolean,
    val outcome: String,
    val charged: Boolean,
    val message: String?,          // card text shown after the press
    val stepIndex: Int? = null,
    val plan: LoggedPlan? = null
)

/** Marks the hint entry logged at [hintTime] as suspicious. Appended, never rewritten. */
@Serializable
data class HintFlagEntry(
    val type: String = "flag",
    val time: Long,
    val hintTime: Long
)

fun HintPlan.toLogged(): LoggedPlan = LoggedPlan(
    cell = cellIndex,
    digit = digit,
    techniques = techniques.map { it.name },
    isReveal = isReveal,
    steps = steps.map { LoggedStep(it.text, it.highlight.primary, it.highlight.secondary, it.highlight.digit) }
)

/**
 * Debug-only running history of hint presses, as JSON lines under [dir]. When the live file
 * passes [maxFileBytes] it replaces the single backup, so disk use stays under twice that.
 * Pull with `task hintlog`.
 */
class HintLogger(
    private val dir: File,
    private val maxFileBytes: Long = 5L * 1024 * 1024
) {
    private val json = Json { encodeDefaults = true }
    private val mutex = Mutex()
    private val live get() = File(dir, "hintlog.jsonl")
    private val backup get() = File(dir, "hintlog.1.jsonl")

    /** Time of the most recent hint entry, for [flagLast]. */
    @Volatile
    var lastHintTime: Long? = null
        private set

    suspend fun log(entry: HintLogEntry) {
        lastHintTime = entry.time
        append(json.encodeToString(HintLogEntry.serializer(), entry))
    }

    /** Flags the most recent hint entry; false when there is none this session. */
    suspend fun flagLast(now: Long = System.currentTimeMillis()): Boolean {
        val hintTime = lastHintTime ?: return false
        append(json.encodeToString(HintFlagEntry.serializer(), HintFlagEntry(time = now, hintTime = hintTime)))
        return true
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            live.delete()
            backup.delete()
            lastHintTime = null
        }
    }

    private suspend fun append(line: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                dir.mkdirs()
                if (live.length() >= maxFileBytes) {
                    backup.delete()
                    live.renameTo(backup)
                }
                live.appendText(line + "\n")
            }
        }
    }
}
