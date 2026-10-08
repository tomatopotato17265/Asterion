package dev.tomatopotato.asterion.servers

import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ArchonSocketProtocolTest {
    private fun parse(frame: String) = ArchonSocketProtocol.parse(frame, TimeZone.UTC)

    @Test
    fun parsesAuthEvents() {
        assertEquals(ArchonSocketEvent.AuthOk, parse("""{"event":"auth-ok"}"""))
        assertEquals(ArchonSocketEvent.AuthExpiring, parse("""{"event":"auth-expiring"}"""))
        assertEquals(ArchonSocketEvent.AuthIncorrect, parse("""{"event":"auth-incorrect"}"""))
    }

    @Test
    fun parsesLogsIntoLines() {
        assertEquals(
            ArchonSocketEvent.Log(listOf("first", "second")),
            parse("""{"event":"log","stream":"stdout","message":"first\r\nsecond\n"}"""),
        )
        assertEquals(ArchonSocketEvent.Log(emptyList()), parse("""{"event":"log","message":""}"""))
    }

    @Test
    fun formatsLog4jLikeThePanel() {
        val event = parse(
            """{"event":"log4j","timestamp_millis":3723000,"thread_name":"Server thread","level":"INFO",
               "message":"Done (0.3s)!\nsecond line","throwable":"java.lang.Error: boom\n\tat x"}""",
        )
        assertEquals(
            ArchonSocketEvent.Log(
                listOf(
                    "[01:02:03] [Server thread/INFO]: Done (0.3s)!",
                    "second line",
                    "java.lang.Error: boom",
                    "\tat x",
                ),
            ),
            event,
        )
        assertEquals(
            ArchonSocketEvent.Log(listOf("[main/WARN]: hi")),
            parse("""{"event":"log4j","thread_name":"main","level":"WARN","message":"hi"}"""),
        )
    }

    @Test
    fun parsesStats() {
        val event = parse(
            """{"event":"stats","cpu_percent":12.5,"ram_usage_bytes":1024,"ram_total_bytes":4096,
               "storage_usage_bytes":10,"storage_total_bytes":100,"net_tx_bytes":1,"net_rx_bytes":2}""",
        )
        assertEquals(ArchonSocketEvent.Stats(ServerStats(12.5, 1024, 4096, 10, 100)), event)
    }

    @Test
    fun parsesPowerStateAndFlattenedState() {
        assertEquals(
            ArchonSocketEvent.Power(ServerPowerState.Crashed, oomKilled = true, exitCode = 137),
            parse("""{"event":"power-state","state":"crashed","oom_killed":true,"exit_code":137}"""),
        )
        assertEquals(
            ArchonSocketEvent.Power(ServerPowerState.Stopped, oomKilled = false, exitCode = null),
            parse("""{"event":"state","power_variant":"idle","uptime":0,"target":null}"""),
        )
        assertEquals(ArchonSocketEvent.Uptime(42), parse("""{"event":"uptime","uptime":42}"""))
    }

    @Test
    fun toleratesUnknownAndMalformedFrames() {
        assertIs<ArchonSocketEvent.Other>(parse("""{"event":"backup-progress","id":"x"}"""))
        assertNull(parse("not json"))
        assertNull(parse("""["event","log"]"""))
        assertNull(parse("""{"message":"no event"}"""))
        assertEquals(
            ArchonSocketEvent.Stats(ServerStats(0.0, 0, 0, 0, 0)),
            parse("""{"event":"stats","cpu_percent":"oops"}"""),
        )
    }

    @Test
    fun encodesOutgoingMessagesWithTheirEventField() {
        val auth = Json.parseToJsonElement(ArchonSocketProtocol.authMessage("jwt1")).jsonObject
        assertEquals("auth", auth["event"]?.jsonPrimitive?.content)
        assertEquals("jwt1", auth["jwt"]?.jsonPrimitive?.content)

        val cmd = Json.parseToJsonElement(ArchonSocketProtocol.commandMessage("say \"hi\"")).jsonObject
        assertEquals("command", cmd["event"]?.jsonPrimitive?.content)
        assertEquals("say \"hi\"", cmd["cmd"]?.jsonPrimitive?.content)
    }

    @Test
    fun normalisesNodeWebSocketUrls() {
        assertEquals("wss://node.modrinth.com/ws", ArchonSocketProtocol.webSocketUrl("node.modrinth.com/ws"))
        assertEquals("wss://node/ws", ArchonSocketProtocol.webSocketUrl("https://node/ws"))
        assertEquals("ws://node/ws", ArchonSocketProtocol.webSocketUrl("http://node/ws"))
        assertEquals("wss://node/ws", ArchonSocketProtocol.webSocketUrl("wss://node/ws"))
    }

    @Test
    fun nodeAuthBaseUrlMatchesThePanel() {
        assertEquals("https://node-1.modrinth.com", NodeAuth("node-1.modrinth.com/modrinth/v0/fs", "t").baseUrl)
        assertEquals("https://node-1.modrinth.com", NodeAuth("https://node-1.modrinth.com/modrinth/v0/fs/", "t").baseUrl)
        assertEquals("http://localhost:8080", NodeAuth("http://localhost:8080", "t").baseUrl)
    }

    @Test
    fun consoleStateFoldsEvents() {
        val state = ConsoleState(maxLines = 3)
        state.apply(ArchonSocketEvent.Log(listOf("a", "b")))
        state.apply(ArchonSocketEvent.Log(listOf("c", "d")))
        state.apply(ArchonSocketEvent.Power(ServerPowerState.Running, false, null))
        state.apply(ArchonSocketEvent.Uptime(10))
        state.apply(ArchonSocketEvent.Stats(ServerStats(1.0, 2, 3, 4, 5)))
        state.apply(ArchonSocketEvent.AuthOk)

        val snap = state.snapshot()
        assertEquals(listOf("b", "c", "d"), snap.lines)
        assertEquals(ServerPowerState.Running, snap.powerState)
        assertEquals(10, snap.uptimeSeconds)
        assertEquals(ServerStats(1.0, 2, 3, 4, 5), snap.stats)
        assertEquals(ConsoleConnection.Connected, snap.connection)

        state.apply(ArchonSocketEvent.Power(ServerPowerState.Stopped, false, 0))
        assertNull(state.snapshot().uptimeSeconds)
    }
}
