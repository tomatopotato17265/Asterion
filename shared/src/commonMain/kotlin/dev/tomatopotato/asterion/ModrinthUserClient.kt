package dev.tomatopotato.asterion

import dev.tomatopotato.asterion.net.apiCall
import dev.tomatopotato.asterion.net.asterionHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthUser(
    val id: String,
    val username: String,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

class ModrinthUserClient {
    private val http: HttpClient = asterionHttpClient()

    @Throws(Throwable::class)
    suspend fun fetchCurrentUser(accessToken: String): ModrinthUser =
        apiCall(
            block = { http.get(AuthConfig.USER_ENDPOINT) { header("Authorization", accessToken) } },
            parse = { it.body() },
        )
}
