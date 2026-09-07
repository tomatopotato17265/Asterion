package dev.tomatopotato.asterion

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class TokenRequest(
    val code: String,
    @SerialName("redirect_uri") val redirectUri: String,
)

@Serializable
data class ModrinthToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 0,
)

class ModrinthAuthClient {
    private val http: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun exchangeCode(code: String): ModrinthToken =
        http.post(AuthConfig.TOKEN_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(code = code, redirectUri = AuthConfig.REDIRECT_URI))
        }.body()
}

sealed interface AuthUiState {
    data object SignedOut : AuthUiState
    data object Authorizing : AuthUiState
    data object ExchangingToken : AuthUiState
    data class SignedIn(val token: ModrinthToken) : AuthUiState
    data class Failed(val message: String) : AuthUiState
}
