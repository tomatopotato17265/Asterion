package dev.tomatopotato.asterion.servers

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleLogsTest {
    private val sample = listOf(
        "WARNING: Restricted methods will be blocked",
        "[15:45:58] [Server thread/INFO]: Loading properties",
        "[15:45:59] [Server thread/WARN]: Can't keep up!",
        "[15:46:00] [Server thread/ERROR]: Failed to load",
        "java.lang.IllegalStateException: boom",
        "\tat net.minecraft.Foo.bar(Foo.java:1)",
        "Caused by: java.io.IOException",
        "stop",
        "[15:46:20] [Server thread/DEBUG]: noisy",
    )

    @Test
    fun classifiesLevelsAndKeepsStackTracesWithTheirError() {
        val levels = ConsoleLogs.classify(sample).map { it.level }
        assertEquals(
            listOf(
                LogLevel.Warn, LogLevel.Info, LogLevel.Warn, LogLevel.Error,
                LogLevel.Error, LogLevel.Error, LogLevel.Error, // headline, trace and cause stay with the error
                LogLevel.Other, // a bare command echo ("stop") is not part of the trace
                LogLevel.Other, // so DEBUG lines are only shown under All
            ),
            levels,
        )
    }

    @Test
    fun indentedTraceLinesInheritTheErrorAboveThem() {
        val lines = ConsoleLogs.classify(
            listOf("[1] [t/ERROR]: bad", "\tat a.B.c(B.java:1)", "Caused by: x", "[2] [t/INFO]: ok"),
        )
        assertEquals(
            listOf(LogLevel.Error, LogLevel.Error, LogLevel.Error, LogLevel.Info),
            lines.map { it.level },
        )
    }

    @Test
    fun filtersByLevelAndSearch() {
        val lines = ConsoleLogs.classify(sample)
        assertEquals(sample.size, ConsoleLogs.filter(lines, null, "").size)
        assertEquals(1, ConsoleLogs.filter(lines, LogLevel.Info, "").size)
        assertEquals(2, ConsoleLogs.filter(lines, LogLevel.Warn, "").size)
        assertEquals(listOf("[15:46:00] [Server thread/ERROR]: Failed to load"),
            ConsoleLogs.filter(lines, LogLevel.Error, "failed").map { it.text })
        assertEquals(1, ConsoleLogs.filter(lines, null, "  CAN'T keep ").size)
    }
}
