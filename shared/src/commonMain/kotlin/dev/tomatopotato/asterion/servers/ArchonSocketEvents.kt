package dev.tomatopotato.asterion.servers

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** The Minecraft process state as the console socket reports it (`power-state` / `state` events). */
enum class ServerPowerState {
    Running, Starting, Stopping, Stopped, Crashed, Unknown;

    companion object {
        fun fromPowerState(raw: String?): ServerPowerState = when (raw) {
            "running" -> Running
            "starting" -> Starting
            "stopping" -> Stopping
            "stopped" -> Stopped
            "crashed" -> Crashed
            else -> Unknown
        }

        fun fromFlattened(raw: String?): ServerPowerState = when (raw) {
            "running" -> Running
            "starting" -> Starting
            "stopping" -> Stopping
            "idle" -> Stopped
            else -> Unknown
        }
    }
}

data class ServerStats(
    val cpuPercent: Double,
    val ramUsageBytes: Long,
    val ramTotalBytes: Long,
    val storageUsageBytes: Long,
    val storageTotalBytes: Long,
)

sealed interface ArchonSocketEvent {
    data object AuthOk : ArchonSocketEvent
    data object AuthExpiring : ArchonSocketEvent
    data object AuthIncorrect : ArchonSocketEvent
    data class Log(val lines: List<String>) : ArchonSocketEvent
    data class Stats(val stats: ServerStats) : ArchonSocketEvent
    data class Power(val state: ServerPowerState, val oomKilled: Boolean, val exitCode: Long?) : ArchonSocketEvent
    data class Uptime(val seconds: Long) : ArchonSocketEvent
    data class Other(val event: String) : ArchonSocketEvent
}

object ArchonSocketProtocol {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(frame: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): ArchonSocketEvent? {
        val obj = runCatching { json.parseToJsonElement(frame) as? JsonObject }.getOrNull() ?: return null
        val event = obj.string("event") ?: return null
        return when (event) {
            "auth-ok" -> ArchonSocketEvent.AuthOk
            "auth-expiring" -> ArchonSocketEvent.AuthExpiring
            "auth-incorrect" -> ArchonSocketEvent.AuthIncorrect
            "log" -> ArchonSocketEvent.Log(splitLines(obj.string("message").orEmpty()))
            "log4j" -> ArchonSocketEvent.Log(formatLog4j(obj, timeZone))
            "stats" -> ArchonSocketEvent.Stats(
                ServerStats(
                    cpuPercent = obj.double("cpu_percent") ?: 0.0,
                    ramUsageBytes = obj.long("ram_usage_bytes") ?: 0,
                    ramTotalBytes = obj.long("ram_total_bytes") ?: 0,
                    storageUsageBytes = obj.long("storage_usage_bytes") ?: 0,
                    storageTotalBytes = obj.long("storage_total_bytes") ?: 0,
                ),
            )
            "power-state" -> ArchonSocketEvent.Power(
                state = ServerPowerState.fromPowerState(obj.string("state")),
                oomKilled = obj.boolean("oom_killed") ?: false,
                exitCode = obj.long("exit_code"),
            )
            "state" -> ArchonSocketEvent.Power(
                state = ServerPowerState.fromFlattened(obj.string("power_variant")),
                oomKilled = obj.boolean("was_oom") ?: false,
                exitCode = obj.long("exit_code"),
            )
            "uptime" -> obj.long("uptime")?.let { ArchonSocketEvent.Uptime(it) } ?: ArchonSocketEvent.Other(event)
            else -> ArchonSocketEvent.Other(event)
        }
    }

    fun authMessage(jwt: String): String = buildJsonObject {
        put("event", "auth")
        put("jwt", jwt)
    }.toString()

    fun commandMessage(command: String): String = buildJsonObject {
        put("event", "command")
        put("cmd", command)
    }.toString()

    fun webSocketUrl(nodeUrl: String): String {
        val lower = nodeUrl.lowercase()
        return when {
            lower.startsWith("ws://") || lower.startsWith("wss://") -> nodeUrl
            lower.startsWith("https://") -> "wss://" + nodeUrl.substring(8)
            lower.startsWith("http://") -> "ws://" + nodeUrl.substring(7)
            else -> "wss://$nodeUrl"
        }
    }

    fun formatLog4j(obj: JsonObject, timeZone: TimeZone): List<String> {
        val time = obj.long("timestamp_millis")?.takeIf { it > 0 }?.let { millis ->
            val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
            "[${pad(t.hour)}:${pad(t.minute)}:${pad(t.second)}]"
        }
        val thread = obj.string("thread_name").orEmpty()
        val level = obj.string("level").orEmpty()
        val prefix = if (time != null) "$time [$thread/$level]: " else "[$thread/$level]: "

        val messageLines = obj.string("message").orEmpty().trim().split(LINE_BREAKS)
        val lines = mutableListOf(prefix + messageLines.first())
        messageLines.drop(1).filterTo(lines) { it.isNotEmpty() }
        obj.string("throwable")?.split(LINE_BREAKS)?.filterTo(lines) { it.isNotEmpty() }
        return lines
    }

    private fun splitLines(message: String): List<String> {
        val trimmed = message.trimEnd('\n', '\r')
        return if (trimmed.isEmpty()) emptyList() else trimmed.split(LINE_BREAKS)
    }

    private val LINE_BREAKS = Regex("\r?\n")

    private fun pad(value: Int) = value.toString().padStart(2, '0')

    private fun JsonObject.primitive(key: String): JsonPrimitive? =
        runCatching { this[key]?.jsonPrimitive }.getOrNull()

    private fun JsonObject.string(key: String): String? =
        primitive(key)?.takeIf { it.isString }?.content

    private fun JsonObject.long(key: String): Long? =
        primitive(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

    private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull

    private fun JsonObject.boolean(key: String): Boolean? = primitive(key)?.booleanOrNull
}
