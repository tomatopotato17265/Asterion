package dev.tomatopotato.asterion

object AuthConfig {
    const val CLIENT_ID: String = "xAS89yPM"
    const val TOKEN_ENDPOINT: String = "https://asterion.tomatopotato17265.workers.dev/token"

    const val AUTHORIZE_URL: String = "https://modrinth.com/auth/authorize"
    const val REDIRECT_URI: String = "dev.tomatopotato.asterion://oauth/callback"
    const val SCOPES: String = "USER_READ"
}
