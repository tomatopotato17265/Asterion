package dev.tomatopotato.asterion.auth

import dev.tomatopotato.asterion.net.apiCall
import dev.tomatopotato.asterion.net.asterionHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlinx.serialization.Serializable

object ModrinthSessionPolicy {
    const val LIFETIME_SECONDS: Long = 14L * 24 * 60 * 60
    const val REFRESH_WINDOW_SECONDS: Long = 24L * 60 * 60

    fun expiresAt(issuedAtEpochSeconds: Long): Long = issuedAtEpochSeconds + LIFETIME_SECONDS

    fun needsRefresh(expiresAtEpochSeconds: Long, nowEpochSeconds: Long): Boolean =
        nowEpochSeconds >= expiresAtEpochSeconds - REFRESH_WINDOW_SECONDS

    fun isSessionToken(token: String): Boolean = token.startsWith("mra_")
}

class ModrinthSessionClient {
    private val http = asterionHttpClient()

    @Throws(Throwable::class)
    suspend fun refresh(session: String): String =
        apiCall(
            block = {
                http.post(REFRESH_URL) { header("Authorization", session) }
            },
            parse = { it.body<RefreshResponse>().session },
        )

    @Serializable
    private data class RefreshResponse(val session: String)

    companion object {
        const val REFRESH_URL: String = "https://api.modrinth.com/v2/session/refresh"
    }
}
