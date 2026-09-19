package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.ModrinthUserClient
import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.TokenProvider

class ProjectsFacade(
    private val tokenProvider: () -> String?,
    private val userIdProvider: () -> String?,
) {
    private val client = ModrinthProjectsClient(TokenProvider { tokenProvider() })
    private val userClient = ModrinthUserClient()

    @Throws(Throwable::class)
    suspend fun listProjects(): List<ModrinthProject> {
        val userId = userIdProvider() ?: fetchUserId()
        return client.listUserProjects(userId).sortedByRecentlyUpdated()
    }

    private suspend fun fetchUserId(): String {
        val token = tokenProvider() ?: throw ApiError.Unauthorized("No Modrinth token stored")
        return userClient.fetchCurrentUser(token).id
    }
}

internal fun List<ModrinthProject>.sortedByRecentlyUpdated(): List<ModrinthProject> =
    sortedByDescending { it.updated }
