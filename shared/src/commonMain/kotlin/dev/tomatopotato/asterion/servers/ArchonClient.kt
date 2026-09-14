package dev.tomatopotato.asterion.servers

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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class ArchonClient(
    private val tokenProvider: TokenProvider,
    private val http: HttpClient = asterionHttpClient(),
    private val baseUrl: String = ARCHON_BASE_URL,
) {
    companion object {
        const val ARCHON_BASE_URL: String = "https://archon.modrinth.com"
        const val PANEL_VERSION: String = "1"

        private const val V0 = "modrinth/v0"
    }

    suspend fun listServers(limit: Int? = null, offset: Int? = null): ServerListResponse =
        apiCall(
            block = {
                http.get("$baseUrl/$V0/servers") {
                    archonHeaders()
                    limit?.let { parameter("limit", it) }
                    offset?.let { parameter("offset", it) }
                }
            },
            parse = { it.body() },
        )

    suspend fun getServer(serverId: String): ArchonServer =
        apiCall(
            block = { http.get("$baseUrl/$V0/servers/$serverId") { archonHeaders() } },
            parse = { it.body() },
        )

    suspend fun getServerFull(serverId: String): ServerFull =
        apiCall(
            block = { http.get("$baseUrl/v1/servers/$serverId") { archonHeaders() } },
            parse = { it.body() },
        )

    suspend fun getAddons(serverId: String, worldId: String): AddonsResponse =
        apiCall(
            block = { http.get("$baseUrl/v1/servers/$serverId/worlds/$worldId/addons") { archonHeaders() } },
            parse = { it.body() },
        )

    suspend fun getInstalledContent(serverId: String): AddonsResponse {
        val world = getServerFull(serverId).activeWorld
            ?: throw ApiError.NotFound("This server has no world to read content from.")
        return getAddons(serverId, world.id)
    }

    suspend fun power(serverId: String, action: PowerAction) {
        apiCall(
            block = {
                http.post("$baseUrl/$V0/servers/$serverId/power") {
                    archonHeaders()
                    contentType(ContentType.Application.Json)
                    setBody(PowerRequest(action.name))
                }
            },
            parse = { },
        )
    }

    private suspend fun HttpRequestBuilder.archonHeaders() {
        val token = tokenProvider.token() ?: throw ApiError.Unauthorized("No Modrinth token stored")
        header("Authorization", "Bearer $token")
        header("X-Panel-Version", PANEL_VERSION)
    }

    @Serializable
    private data class PowerRequest(val action: String)
}
