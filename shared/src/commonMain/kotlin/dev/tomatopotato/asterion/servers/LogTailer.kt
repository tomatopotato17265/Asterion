package dev.tomatopotato.asterion.servers

sealed interface PollAction {
    data class Read(val offset: Long, val upTo: Long) : PollAction
    data object Reset : PollAction
    data object None : PollAction
}

class LogTailer(
    private val maxLines: Int = 1000,
    private val initialTailBytes: Long = 64_000,
) {
    private var offset: Long? = null
    private var pending: ByteArray = ByteArray(0)
    private var dropFirstPartialLine = false

    private val mutableLines = mutableListOf<String>()
    val lines: List<String> get() = mutableLines

    fun onPoll(size: Long): PollAction {
        val current = offset

        if (current == null) {
            val start = if (size > initialTailBytes) size - initialTailBytes else 0L
            offset = start
            dropFirstPartialLine = start > 0
            mutableLines.clear()
            return if (size > start) PollAction.Read(start, size) else PollAction.None
        }

        if (size < current) {
            offset = 0
            pending = ByteArray(0)
            dropFirstPartialLine = false
            mutableLines.clear()
            mutableLines.add(RESTART_MARKER)
            return if (size > 0) PollAction.Read(0, size) else PollAction.Reset
        }

        return if (size > current) PollAction.Read(current, size) else PollAction.None
    }

    fun onBytesRead(bytes: ByteArray) {
        offset = (offset ?: 0) + bytes.size
        pending += bytes
        extractCompleteLines()
    }

    private fun extractCompleteLines() {
        var lastNewline = -1
        for (i in pending.indices.reversed()) {
            if (pending[i] == NEWLINE) {
                lastNewline = i
                break
            }
        }
        if (lastNewline < 0) return

        var complete = pending.copyOfRange(0, lastNewline)
        pending = pending.copyOfRange(lastNewline + 1, pending.size)

        if (dropFirstPartialLine) {
            var firstNewline = -1
            for (i in complete.indices) {
                if (complete[i] == NEWLINE) {
                    firstNewline = i
                    break
                }
            }
            complete = if (firstNewline >= 0) complete.copyOfRange(firstNewline + 1, complete.size) else ByteArray(0)
            dropFirstPartialLine = false
        }

        if (complete.isEmpty()) return
        val text = complete.decodeToString()
        if (text.isEmpty()) return
        append(text.split("\n").map { it.trimEnd('\r') })
    }

    private fun append(new: List<String>) {
        mutableLines.addAll(new)
        if (mutableLines.size > maxLines) {
            repeat(mutableLines.size - maxLines) { mutableLines.removeAt(0) }
        }
    }

    companion object {
        const val RESTART_MARKER = "── server log restarted ──"
        private const val NEWLINE: Byte = 0x0A
    }
}
