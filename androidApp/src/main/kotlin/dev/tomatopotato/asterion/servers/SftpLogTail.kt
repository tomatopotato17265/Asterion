package dev.tomatopotato.asterion.servers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.schmizz.sshj.sftp.OpenMode
import java.util.EnumSet
import kotlin.math.min

class SftpLogTail(
    private val host: String,
    private val username: String,
    private val password: String,
) {
    private val path = "/logs/latest.log"
    private val maxChunkBytes = 1_000_000
    private val pollIntervalMs = 1_000L
    private val retryDelayMs = 5_000L

    private val tailer = LogTailer(maxLines = 1000, initialTailBytes = 64_000)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun run() {
        while (true) {
            try {
                tailSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorText.value = "Can't read server logs: ${e.message}"
            }
            delay(retryDelayMs)
        }
    }

    private suspend fun tailSession() {
        withSftpSession(host, username, password) { sftp ->
            var file = sftp.open(path, EnumSet.of(OpenMode.READ))
            try {
                while (true) {
                    val size = sftp.stat(path).size
                    when (val action = tailer.onPoll(size)) {
                        is PollAction.Read -> {
                            var offset = action.offset
                            val target = action.upTo
                            val buffer = ByteArray(maxChunkBytes)
                            while (offset < target) {
                                val length = min(target - offset, maxChunkBytes.toLong()).toInt()
                                val count = file.read(offset, buffer, 0, length)
                                if (count <= 0) break
                                tailer.onBytesRead(buffer.copyOf(count))
                                offset += count
                            }
                        }
                        PollAction.Reset -> Unit
                        PollAction.None -> Unit
                    }
                    _lines.value = tailer.lines
                    _errorText.value = null
                    delay(pollIntervalMs)
                }
            } finally {
                runCatching { file.close() }
            }
        }
    }
}
