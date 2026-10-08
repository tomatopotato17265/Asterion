package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.TokenProvider
import dev.tomatopotato.asterion.net.asterionSocketClient
import kotlinx.coroutines.CancellationException

class ServersFacade(tokenProvider: () -> String?) {
    private val client = ArchonClient(TokenProvider { tokenProvider() })
    private val sockets by lazy { asterionSocketClient() }

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

    fun console(serverId: String): ServerConsole = ServerConsole(serverId, client, sockets)

    @Throws(Throwable::class)
    suspend fun downloadServerIcon(serverId: String): ByteArray? {
        val auth = try {
            client.getFilesystemAuth(serverId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            return null
        }
        for (path in ServerIconPaths.candidates) {
            try {
                return client.downloadNodeFile(auth, path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                continue
            }
        }
        return null
    }
}
