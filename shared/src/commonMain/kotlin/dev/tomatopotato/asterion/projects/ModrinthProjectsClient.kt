package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.TokenProvider
import dev.tomatopotato.asterion.net.apiCall
import dev.tomatopotato.asterion.net.asterionHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

class ModrinthProjectsClient(
    private val tokenProvider: TokenProvider,
    private val http: HttpClient = asterionHttpClient(),
    private val baseUrl: String = BASE_URL,
) {
    companion object {
        const val BASE_URL: String = "https://api.modrinth.com/v2"
    }

    suspend fun listUserProjects(userId: String): List<ModrinthProject> =
        apiCall(
            block = {
                http.get("$baseUrl/user/$userId/projects") {
                    tokenProvider.token()?.let { header("Authorization", it) }
                }
            },
            parse = { it.body() },
        )
}
