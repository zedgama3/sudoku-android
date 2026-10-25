package net.zg3.sudoku.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HintLoggerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun entry(time: Long) = HintLogEntry(
        time = time,
        version = "test",
        difficulty = "EASY",
        givens = "0".repeat(81),
        solution = "1".repeat(81),
        current = "0".repeat(81),
        notes = List(81) { 0 },
        selected = null,
        mistakeChecking = true,
        outcome = "Step",
        charged = false,
        message = "hello"
    )

    @Test
    fun appendsOneLinePerEntryAndFlagsLast() = runTest {
        val dir = tmp.newFolder("hintlog")
        val logger = HintLogger(dir)
        assertFalse(logger.flagLast(now = 1))
        logger.log(entry(10))
        logger.log(entry(20))
        assertTrue(logger.flagLast(now = 30))
        val lines = File(dir, "hintlog.jsonl").readLines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("\"type\":\"hint\""))
        assertEquals("""{"type":"flag","time":30,"hintTime":20}""", lines[2])
    }

    @Test
    fun rotatesIntoSingleBackup() = runTest {
        val dir = tmp.newFolder("hintlog")
        val logger = HintLogger(dir, maxFileBytes = 1000)
        repeat(20) { logger.log(entry(it.toLong())) }
        val live = File(dir, "hintlog.jsonl")
        val backup = File(dir, "hintlog.1.jsonl")
        assertTrue(backup.exists())
        assertEquals(setOf("hintlog.jsonl", "hintlog.1.jsonl"), dir.list()!!.toSet())
        // each file is at most one entry past the threshold
        val lineLength = live.readLines().first().length + 1
        assertTrue(live.length() < 1000 + lineLength)
        assertTrue(backup.length() < 1000 + lineLength)
        // newest entry is last in the live file
        assertTrue(live.readLines().last().contains("\"time\":19,"))
    }

    @Test
    fun clearRemovesBothFiles() = runTest {
        val dir = tmp.newFolder("hintlog")
        val logger = HintLogger(dir, maxFileBytes = 500)
        repeat(10) { logger.log(entry(it.toLong())) }
        logger.clear()
        assertEquals(0, dir.list()!!.size)
        assertFalse(logger.flagLast())
    }
}
