package dev.tomatopotato.asterion.servers

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultServerIconTest {

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun decodesToAValidPngSignature() {
        val bytes = Base64.decode(DefaultServerIcon.base64)
        assertTrue(bytes.size > 1000, "expected a real image, got ${bytes.size} bytes")

        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )
        assertEquals(pngSignature.toList(), bytes.take(8))
    }
}
