package dev.tomatopotato.asterion

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModrinthUser(
    val id: String,
    val username: String,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

class ModrinthUserClient {
    private val http: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun fetchCurrentUser(accessToken: String): ModrinthUser =
        http.get(AuthConfig.USER_ENDPOINT) {
            header("Authorization", accessToken)
        }.body()
}
