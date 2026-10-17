package dev.tomatopotato.asterion.auth

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class LauncherSealKey internal constructor(
    private val privateKey: PrivateKey,
    val publicKeySpki: ByteArray,
    val nonce: String,
) {
    constructor() : this(generateKeyPair(), randomNonce())

    private constructor(pair: KeyPair, nonce: String) : this(pair.private, pair.public.encoded, nonce)

    val signInUrl: String get() = LauncherAuth.signInUrl(publicKeySpki, nonce)

    // the session token from a `modrinth://auth` hand-off, or null if it doesn't decrypt
    fun open(handoff: LauncherHandoff): String? {
        if (handoff.nonce != nonce) return null
        val sealed = LauncherAuth.parseSealed(handoff.code) ?: return null
        val plaintext = runCatching { decrypt(sealed) }.getOrNull() ?: return null
        return LauncherAuth.tokenFromPlaintext(plaintext, nonce)
    }

    private fun decrypt(sealed: SealedSession): String {
        val ephemeral = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(sealed.ephemeralPublicKeySpki))
        val shared = KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(ephemeral, true)
            generateSecret()
        }
        val aesKey = hkdfSha256(shared, LauncherAuth.HKDF_INFO.encodeToByteArray())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, sealed.iv))
        return cipher.doFinal(sealed.ciphertextWithTag).decodeToString()
    }

    internal companion object {
        fun fromEncoded(privateKeyPkcs8: ByteArray, publicKeySpki: ByteArray, nonce: String): LauncherSealKey =
            LauncherSealKey(KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(privateKeyPkcs8)), publicKeySpki, nonce)

        private fun generateKeyPair(): KeyPair =
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

        private fun randomNonce(): String =
            LauncherAuth.nonceFrom(ByteArray(LauncherAuth.NONCE_BYTES).also(SecureRandom()::nextBytes))

        private fun hkdfSha256(inputKey: ByteArray, info: ByteArray): ByteArray {
            val extract = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(ByteArray(32), "HmacSHA256")) }
            val prk = extract.doFinal(inputKey)
            val expand = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(prk, "HmacSHA256")) }
            return expand.doFinal(info + byteArrayOf(1))
        }
    }
}
