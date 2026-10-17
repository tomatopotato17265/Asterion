package dev.tomatopotato.asterion.auth

import dev.tomatopotato.asterion.ModrinthUserClient
import io.ktor.http.Url
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// where Modrinth hands the sealed app session back: `modrinth://auth?code=<sealed>&nonce=<nonce>`.
data class LauncherHandoff(val code: String, val nonce: String)

class SealedSession(
    val ephemeralPublicKeySpki: ByteArray,
    val iv: ByteArray,
    val ciphertextWithTag: ByteArray,
) {
    val ciphertext: ByteArray get() = ciphertextWithTag.copyOfRange(0, ciphertextWithTag.size - TAG_LENGTH)
    val tag: ByteArray get() = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - TAG_LENGTH, ciphertextWithTag.size)

    internal companion object {
        const val TAG_LENGTH = 16
    }
}

@OptIn(ExperimentalEncodingApi::class)
object LauncherAuth {
    const val PROTOCOL: String = "2"
    const val HKDF_INFO: String = "modrinth-app-auth-v2"
    const val NONCE_BYTES: Int = 16
    const val HANDOFF_SCHEME: String = "modrinth"

    private const val SIGN_IN_URL = "https://modrinth.com/auth/sign-in"
    private const val SEAL_VERSION: Byte = 0x02
    private const val IV_LENGTH = 12
    private const val MAX_SEALED_BYTES = 4096
    private const val MAX_SPKI_BYTES = 256
    private val SESSION_TOKEN = Regex("^mra_[A-Za-z0-9]{32,128}$")
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun signInUrl(publicKeySpki: ByteArray, nonce: String): String =
        signInUrl(publicKeySpkiBase64Url = encode(publicKeySpki), nonce = nonce)

    fun signInUrl(publicKeySpkiBase64Url: String, nonce: String): String =
        "$SIGN_IN_URL?launcher=true&protocol=$PROTOCOL&key=$publicKeySpkiBase64Url&nonce=$nonce"

    // follows the "Open Modrinth App" hand-off link as soon as the page shows it so no button press is needed
    const val FOLLOW_HANDOFF_SCRIPT: String = """
(function () {
  if (window.__asterionFollowHandoff) { return; }
  window.__asterionFollowHandoff = true;
  function follow() {
    var link = document.querySelector('a[href^="modrinth://auth"]');
    if (link) { location.href = link.href; return true; }
    return false;
  }
  if (follow()) { return; }
  var observer = new MutationObserver(function () { if (follow()) { observer.disconnect(); } });
  observer.observe(document.documentElement, { childList: true, subtree: true });
})();
"""

    fun nonceFrom(randomBytes: ByteArray): String = encode(randomBytes)

    fun parseHandoff(url: String): LauncherHandoff? {
        val parsed = runCatching { Url(url) }.getOrNull() ?: return null
        if (parsed.protocol.name != HANDOFF_SCHEME || parsed.host != "auth") return null
        val code = parsed.parameters["code"]?.takeIf { it.isNotEmpty() } ?: return null
        val nonce = parsed.parameters["nonce"]?.takeIf { it.isNotEmpty() } ?: return null
        return LauncherHandoff(code, nonce)
    }

    fun parseSealed(code: String): SealedSession? {
        val bytes = decode(code) ?: return null
        if (bytes.size < 3 || bytes.size > MAX_SEALED_BYTES || bytes[0] != SEAL_VERSION) return null
        val spkiLength = ((bytes[1].toInt() and 0xff) shl 8) or (bytes[2].toInt() and 0xff)
        if (spkiLength !in 1..MAX_SPKI_BYTES) return null
        val ivStart = 3 + spkiLength
        val cipherStart = ivStart + IV_LENGTH
        if (bytes.size <= cipherStart + SealedSession.TAG_LENGTH) return null
        return SealedSession(
            ephemeralPublicKeySpki = bytes.copyOfRange(3, ivStart),
            iv = bytes.copyOfRange(ivStart, cipherStart),
            ciphertextWithTag = bytes.copyOfRange(cipherStart, bytes.size),
        )
    }

    fun tokenFromPlaintext(plaintext: String, expectedNonce: String): String? {
        val dot = plaintext.indexOf('.')
        if (dot < 0 || plaintext.substring(0, dot) != expectedNonce) return null
        return plaintext.substring(dot + 1).takeIf(SESSION_TOKEN::matches)
    }

    @Throws(Throwable::class)
    suspend fun verify(token: String): SignedInSession =
        SignedInSession(token, ModrinthUserClient().fetchCurrentUser(token))

    fun encode(bytes: ByteArray): String = base64Url.encode(bytes)

    fun decode(value: String): ByteArray? = runCatching { base64Url.decode(value) }.getOrNull()
}
