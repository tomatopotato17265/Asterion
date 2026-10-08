package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.isPermissionError
import dev.tomatopotato.asterion.net.userMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

enum class ConsoleConnection { Connecting, Connected, Reconnecting, Failed }

data class ConsoleSnapshot(
    val lines: List<String>,
    val totalLines: Long,
    val powerState: ServerPowerState,
    val stats: ServerStats?,
    val uptimeSeconds: Long?,
    val oomKilled: Boolean,
    val connection: ConsoleConnection,
    val errorMessage: String?,
)

class ConsoleState(private val maxLines: Int = 1000) {
    private val lines = ArrayDeque<String>()
    private var totalLines = 0L
    var powerState: ServerPowerState = ServerPowerState.Unknown; private set
    var stats: ServerStats? = null; private set
    var uptimeSeconds: Long? = null; private set
    var oomKilled: Boolean = false; private set
    var connection: ConsoleConnection = ConsoleConnection.Connecting
    var errorMessage: String? = null

    fun apply(event: ArchonSocketEvent) {
        when (event) {
            is ArchonSocketEvent.Log -> {
                lines.addAll(event.lines)
                totalLines += event.lines.size
                while (lines.size > maxLines) lines.removeFirst()
            }
            is ArchonSocketEvent.Stats -> stats = event.stats
            is ArchonSocketEvent.Power -> {
                powerState = event.state
                oomKilled = event.oomKilled
                if (event.state != ServerPowerState.Running) uptimeSeconds = null
            }
            is ArchonSocketEvent.Uptime -> uptimeSeconds = event.seconds
            ArchonSocketEvent.AuthOk -> {
                connection = ConsoleConnection.Connected
                errorMessage = null
            }
            ArchonSocketEvent.AuthExpiring, ArchonSocketEvent.AuthIncorrect, is ArchonSocketEvent.Other -> Unit
        }
    }

    fun snapshot(): ConsoleSnapshot = ConsoleSnapshot(
        lines = lines.toList(),
        totalLines = totalLines,
        powerState = powerState,
        stats = stats,
        uptimeSeconds = uptimeSeconds,
        oomKilled = oomKilled,
        connection = connection,
        errorMessage = errorMessage,
    )
}

class ServerConsole internal constructor(
    private val serverId: String,
    private val archon: ArchonClient,
    private val sockets: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val state = ConsoleState()
    private val snapshots = MutableStateFlow(state.snapshot())
    private var session: DefaultClientWebSocketSession? = null
    private var loop: Job? = null

    fun start(onUpdate: (ConsoleSnapshot) -> Unit) {
        if (loop != null) return
        scope.launch(Dispatchers.Main) { snapshots.collect { onUpdate(it) } }
        loop = scope.launch { run() }
    }

    fun stop() {
        loop = null
        scope.cancel()
    }

    @Throws(Throwable::class)
    suspend fun sendCommand(command: String) {
        val live = session ?: throw ApiError.Network("The console isn't connected yet.")
        live.send(Frame.Text(ArchonSocketProtocol.commandMessage(command)))
    }

    @Throws(Throwable::class)
    suspend fun power(action: PowerAction) = archon.power(serverId, action)

    private suspend fun run() {
        var attempt = 0
        while (scope.isActive) {
            publish(if (attempt == 0) ConsoleConnection.Connecting else ConsoleConnection.Reconnecting)
            try {
                val auth = archon.getWebSocketAuth(serverId)
                sockets.webSocket(urlString = ArchonSocketProtocol.webSocketUrl(auth.url)) {
                    session = this
                    send(Frame.Text(ArchonSocketProtocol.authMessage(auth.token)))
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val event = ArchonSocketProtocol.parse(frame.readText()) ?: continue
                        when (event) {
                            ArchonSocketEvent.AuthOk -> attempt = 0
                            ArchonSocketEvent.AuthExpiring -> {
                                val fresh = archon.getWebSocketAuth(serverId)
                                send(Frame.Text(ArchonSocketProtocol.authMessage(fresh.token)))
                            }

                            ArchonSocketEvent.AuthIncorrect -> break
                            else -> Unit
                        }
                        state.apply(event)
                        snapshots.value = state.snapshot()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                if (e.isPermissionError) {
                    state.errorMessage = e.userMessage()
                    publish(ConsoleConnection.Failed)
                    return
                }
                state.errorMessage = e.userMessage()
            } catch (e: Throwable) {
                state.errorMessage = "Console connection lost: ${e.message ?: "unknown error"}"
            } finally {
                session = null
            }
            attempt++
            delay(min(30_000L, 1_000L shl min(attempt, 5)))
        }
    }

    private fun publish(connection: ConsoleConnection) {
        state.connection = connection
        snapshots.value = state.snapshot()
    }
}
