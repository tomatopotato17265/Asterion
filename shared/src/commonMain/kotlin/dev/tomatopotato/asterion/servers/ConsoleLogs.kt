package dev.tomatopotato.asterion.servers

enum class LogLevel { Error, Warn, Info, Other }

data class ConsoleLine(val text: String, val level: LogLevel)

object ConsoleLogs {
    private val LEVEL_MARKER = Regex("/(INFO|WARN|ERROR|FATAL|DEBUG|TRACE)]")
    private val EXCEPTION_HEADLINE = Regex("^[\\w.$]+(Exception|Error)(:|$)")

    fun classify(lines: List<String>): List<ConsoleLine> {
        var previous = LogLevel.Other
        return lines.map { text ->
            val level = levelOf(text) ?: if (isContinuation(text)) previous else LogLevel.Other
            previous = level
            ConsoleLine(text, level)
        }
    }

    fun filter(lines: List<ConsoleLine>, level: LogLevel?, query: String): List<ConsoleLine> {
        val needle = query.trim()
        if (level == null && needle.isEmpty()) return lines
        return lines.filter { line ->
            (level == null || line.level == level) && line.text.contains(needle, ignoreCase = true)
        }
    }

    private fun levelOf(text: String): LogLevel? {
        val marker = LEVEL_MARKER.find(text)?.groupValues?.get(1)
        return when (marker) {
            "ERROR", "FATAL" -> LogLevel.Error
            "WARN" -> LogLevel.Warn
            "INFO" -> LogLevel.Info
            "DEBUG", "TRACE" -> LogLevel.Other
            else -> if (text.startsWith("WARNING:")) LogLevel.Warn else null
        }
    }

    private fun isContinuation(text: String): Boolean =
        text.startsWith(" ") || text.startsWith("\t") || text.startsWith("at ") ||
            text.startsWith("Caused by") || text.startsWith("Suppressed:") || text.startsWith("... ") ||
            EXCEPTION_HEADLINE.containsMatchIn(text)
}
