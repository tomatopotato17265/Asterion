package dev.tomatopotato.asterion.notifications

import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.TokenProvider
import dev.tomatopotato.asterion.net.apiCall
import dev.tomatopotato.asterion.net.asterionHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch

class ModrinthNotificationsClient(
    private val tokenProvider: TokenProvider,
    private val http: HttpClient = asterionHttpClient(),
    private val baseUrl: String = BASE_URL,
) {
    companion object {
        const val BASE_URL: String = "https://api.modrinth.com/v2"
    }

    suspend fun listNotifications(userId: String): List<ModrinthNotification> =
        apiCall(
            block = { http.get("$baseUrl/user/$userId/notifications") { authHeader() } },
            parse = { it.body() },
        )

    suspend fun markRead(id: String) {
        apiCall(
            block = { http.patch("$baseUrl/notification/$id") { authHeader() } },
            parse = { },
        )
    }

    suspend fun markRead(ids: List<String>) {
        if (ids.isEmpty()) return
        apiCall(
            block = {
                http.patch("$baseUrl/notifications") {
                    authHeader()
                    parameter("ids", ids.joinToString(prefix = "[", postfix = "]") { "\"$it\"" })
                }
            },
            parse = { },
        )
    }

    private suspend fun HttpRequestBuilder.authHeader() {
        val token = tokenProvider.token() ?: throw ApiError.Unauthorized("No Modrinth token stored")
        header("Authorization", token)
    }
}
