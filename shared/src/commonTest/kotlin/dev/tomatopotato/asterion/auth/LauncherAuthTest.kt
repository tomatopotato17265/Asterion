package dev.tomatopotato.asterion.auth

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LauncherAuthTest {
    private val token = "mra_" + "a1B2c3D4e5".repeat(6)

    @Test
    fun buildsTheProtocolTwoSignInUrl() {
        assertEquals(
            "https://modrinth.com/auth/sign-in?launcher=true&protocol=2&key=AQID_w&nonce=n0nce",
            LauncherAuth.signInUrl(byteArrayOf(1, 2, 3, -1), "n0nce"),
        )
    }

    @Test
    fun parsesTheAppHandoff() {
        assertEquals(LauncherHandoff("abc_-", "n0"), LauncherAuth.parseHandoff("modrinth://auth?code=abc_-&nonce=n0"))
        assertNull(LauncherAuth.parseHandoff("modrinth://auth?code=abc"))
        assertNull(LauncherAuth.parseHandoff("modrinth://other?code=abc&nonce=n0"))
        assertNull(LauncherAuth.parseHandoff("https://auth/?code=abc&nonce=n0"))
        assertNull(LauncherAuth.parseHandoff("https://launcher-files.modrinth.com/?code=mra_abc"))
        assertNull(LauncherAuth.parseHandoff("not a url"))
    }

    @Test
    fun splitsTheSealedLayout() {
        val spki = ByteArray(91) { it.toByte() }
        val iv = ByteArray(12) { 7 }
        val body = ByteArray(40) { 9 }
        val sealed = byteArrayOf(2, 0, 91) + spki + iv + body
        val parts = assertNotNull(LauncherAuth.parseSealed(LauncherAuth.encode(sealed)))
        assertContentEquals(spki, parts.ephemeralPublicKeySpki)
        assertContentEquals(iv, parts.iv)
        assertContentEquals(body, parts.ciphertextWithTag)
        assertEquals(24, parts.ciphertext.size)
        assertEquals(16, parts.tag.size)
    }

    @Test
    fun rejectsMalformedSeals() {
        val ok = byteArrayOf(2, 0, 4) + ByteArray(4) + ByteArray(12) + ByteArray(17)
        assertNotNull(LauncherAuth.parseSealed(LauncherAuth.encode(ok)))
        assertNull(LauncherAuth.parseSealed(LauncherAuth.encode(byteArrayOf(1) + ok.copyOfRange(1, ok.size))))
        assertNull(LauncherAuth.parseSealed(LauncherAuth.encode(byteArrayOf(2, 0, 0) + ByteArray(40))))
        assertNull(LauncherAuth.parseSealed(LauncherAuth.encode(byteArrayOf(2, 1, 1) + ByteArray(40))))
        assertNull(LauncherAuth.parseSealed(LauncherAuth.encode(byteArrayOf(2, 0, 4) + ByteArray(4) + ByteArray(12) + ByteArray(16))))
        assertNull(LauncherAuth.parseSealed(LauncherAuth.encode(ByteArray(5000) { 2 })))
        assertNull(LauncherAuth.parseSealed("%%%"))
    }

    @Test
    fun acceptsOnlyOurNonceAndRealSessions() {
        assertEquals(token, LauncherAuth.tokenFromPlaintext("n0nce.$token", "n0nce"))
        assertNull(LauncherAuth.tokenFromPlaintext("other.$token", "n0nce"))
        assertNull(LauncherAuth.tokenFromPlaintext(token, "n0nce"))
        assertNull(LauncherAuth.tokenFromPlaintext("n0nce.mro_" + "a".repeat(40), "n0nce"))
        assertNull(LauncherAuth.tokenFromPlaintext("n0nce.mra_short", "n0nce"))
        assertNull(LauncherAuth.tokenFromPlaintext("n0nce.mra_" + "a".repeat(40) + "!", "n0nce"))
    }
}
