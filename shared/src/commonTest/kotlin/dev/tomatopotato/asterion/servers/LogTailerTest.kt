package dev.tomatopotato.asterion.servers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogTailerTest {

    @Test
    fun smallFileIsReadFromTheStart() {
        val tailer = LogTailer(initialTailBytes = 64_000)
        val action = tailer.onPoll(size = 10)
        assertEquals(PollAction.Read(0, 10), action)
    }

    @Test
    fun largeFileSeeksToTailAndDropsTheFirstPartialLine() {
        val tailer = LogTailer(initialTailBytes = 10)
        val action = tailer.onPoll(size = 100)
        assertEquals(PollAction.Read(90, 100), action)
        tailer.onBytesRead("garbage\nfirst\nsecond\n".encodeToByteArray())
        assertEquals(listOf("first", "second"), tailer.lines)
    }

    @Test
    fun completeLinesAreExtractedAndPartialLineCarriesOver() {
        val tailer = LogTailer()
        tailer.onPoll(size = 0)
        tailer.onBytesRead("one\ntwo\nthree-par".encodeToByteArray())
        assertEquals(listOf("one", "two"), tailer.lines)

        tailer.onBytesRead("tial\nfour\n".encodeToByteArray())
        assertEquals(listOf("one", "two", "three-partial", "four"), tailer.lines)
    }

    @Test
    fun carriageReturnsAreTrimmed() {
        val tailer = LogTailer()
        tailer.onPoll(size = 0)
        tailer.onBytesRead("one\r\ntwo\r\n".encodeToByteArray())
        assertEquals(listOf("one", "two"), tailer.lines)
    }

    @Test
    fun shrinkingFileResetsAndEmitsRestartMarker() {
        val tailer = LogTailer()
        tailer.onPoll(size = 20)
        tailer.onBytesRead("old line one\nold line two\n".encodeToByteArray())
        assertTrue(tailer.lines.contains("old line one"))

        val action = tailer.onPoll(size = 5)
        assertEquals(listOf(LogTailer.RESTART_MARKER), tailer.lines)
        assertEquals(PollAction.Read(0, 5), action)

        tailer.onBytesRead("new\n".encodeToByteArray())
        assertEquals(listOf(LogTailer.RESTART_MARKER, "new"), tailer.lines)
    }

    @Test
    fun clearingToEmptyFileResetsWithoutARead() {
        val tailer = LogTailer()
        tailer.onPoll(size = 20)
        tailer.onBytesRead("old line\n".encodeToByteArray())

        val action = tailer.onPoll(size = 0)
        assertEquals(listOf(LogTailer.RESTART_MARKER), tailer.lines)
        assertEquals(PollAction.Reset, action)
    }

    @Test
    fun noNewBytesYieldsNoAction() {
        val tailer = LogTailer()
        tailer.onPoll(size = 10)
        tailer.onBytesRead("0123456789".encodeToByteArray())

        assertEquals(PollAction.None, tailer.onPoll(size = 10))
    }

    @Test
    fun lineCountIsCappedDroppingOldestFirst() {
        val tailer = LogTailer(maxLines = 3)
        tailer.onPoll(size = 0)
        tailer.onBytesRead("a\nb\nc\nd\ne\n".encodeToByteArray())
        assertEquals(listOf("c", "d", "e"), tailer.lines)
    }
}
