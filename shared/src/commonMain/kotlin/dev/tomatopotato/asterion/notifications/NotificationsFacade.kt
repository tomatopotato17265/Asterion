package dev.tomatopotato.asterion.notifications

import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.TokenProvider

class NotificationsFacade(
    tokenProvider: () -> String?,
    private val userIdProvider: () -> String?,
) {
    private val client = ModrinthNotificationsClient(TokenProvider { tokenProvider() })

    @Throws(Throwable::class)
    suspend fun listNotifications(): List<ModrinthNotification> {
        val userId = userIdProvider() ?: throw ApiError.Unauthorized("No Modrinth user id stored")
        return client.listNotifications(userId).sortedByDescending { it.created }
    }

    @Throws(Throwable::class)
    suspend fun markRead(id: String) = client.markRead(id)

    @Throws(Throwable::class)
    suspend fun markRead(ids: List<String>) = client.markRead(ids)
}
