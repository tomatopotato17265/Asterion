package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.TokenProvider

class ServersFacade(tokenProvider: () -> String?) {
    private val client = ArchonClient(TokenProvider { tokenProvider() })

    @Throws(Throwable::class)
    suspend fun listServers(): List<ArchonServer> = client.listServers().servers

    @Throws(Throwable::class)
    suspend fun getServer(serverId: String): ArchonServer = client.getServer(serverId)

    @Throws(Throwable::class)
    suspend fun power(serverId: String, action: PowerAction) = client.power(serverId, action)

    @Throws(Throwable::class)
    suspend fun getInstalledContent(serverId: String): AddonsResponse =
        client.getInstalledContent(serverId)

    @Throws(Throwable::class)
    suspend fun getServerFull(serverId: String): ServerFull = client.getServerFull(serverId)
}
