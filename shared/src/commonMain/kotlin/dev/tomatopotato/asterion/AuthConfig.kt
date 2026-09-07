package dev.tomatopotato.asterion

import io.ktor.http.URLBuilder
import kotlin.random.Random

object AuthConfig {
    const val CLIENT_ID: String = "xAS89yPM"
    const val TOKEN_ENDPOINT: String = "https://asterion.tomatopotato17265.workers.dev/token"
    const val AUTHORIZE_URL: String = "https://modrinth.com/auth/authorize"
    const val SCOPES: String = "USER_READ"
    const val USE_VERIFIED_HTTPS_CALLBACK: Boolean = false
    const val HTTPS_REDIRECT_URI: String = "https://asterion.tomatopotato17265.workers.dev/callback"
    const val CALLBACK_HOST: String = "asterion.tomatopotato17265.workers.dev"
    const val CALLBACK_PATH: String = "/callback"
    const val SCHEME_REDIRECT_URI: String = "dev.tomatopotato.asterion://oauth/callback"
    const val CALLBACK_SCHEME: String = "dev.tomatopotato.asterion"
    val REDIRECT_URI: String =
        if (USE_VERIFIED_HTTPS_CALLBACK) HTTPS_REDIRECT_URI else SCHEME_REDIRECT_URI

    fun buildAuthorizeUrl(state: String): String =
        URLBuilder(AUTHORIZE_URL).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", CLIENT_ID)
            parameters.append("redirect_uri", REDIRECT_URI)
            parameters.append("scope", SCOPES)
            parameters.append("state", state)
        }.buildString()

    fun generateState(): String {
        val hex = "0123456789abcdef"
        return buildString { repeat(32) { append(hex[Random.nextInt(16)]) } }
    }
}
