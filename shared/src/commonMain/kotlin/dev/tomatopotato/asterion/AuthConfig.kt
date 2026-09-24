package dev.tomatopotato.asterion

import io.ktor.http.URLBuilder
import kotlin.random.Random

object AuthConfig {
    const val CLIENT_ID: String = "xAS89yPM"
    const val TOKEN_ENDPOINT: String = "https://asterion.tomatopotato17265.workers.dev/token"
    const val USER_ENDPOINT: String = "https://api.modrinth.com/v2/user"
    const val AUTHORIZE_URL: String = "https://modrinth.com/auth/authorize"
    const val SCOPES: String = "USER_READ+PROJECT_READ+NOTIFICATION_READ+NOTIFICATION_WRITE"
    const val CALLBACK_HOST: String = "asterion.tomatopotato17265.workers.dev"
    const val CALLBACK_PATH: String = "/callback"
    const val REDIRECT_URI: String = "https://asterion.tomatopotato17265.workers.dev/callback"

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
