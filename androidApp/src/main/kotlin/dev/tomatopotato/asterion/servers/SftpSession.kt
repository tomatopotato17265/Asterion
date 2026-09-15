package dev.tomatopotato.asterion.servers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

private val ensureBouncyCastleProvider: Unit by lazy {
    Security.removeProvider("BC")
    Security.insertProviderAt(BouncyCastleProvider(), 1)
    Unit
}

suspend fun <T> withSftpSession(
    rawHost: String,
    username: String,
    password: String,
    block: suspend (SFTPClient) -> T,
): T = withContext(Dispatchers.IO) {
    ensureBouncyCastleProvider
    val endpoint = SftpEndpoint.parse(rawHost)
    val client = SSHClient()
    client.addHostKeyVerifier(PromiscuousVerifier())
    client.connect(endpoint.host, endpoint.port)
    try {
        client.authPassword(username, password)
        client.newSFTPClient().use { sftp ->
            block(sftp)
        }
    } finally {
        client.disconnect()
    }
}
