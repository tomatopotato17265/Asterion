package dev.tomatopotato.asterion.auth

import dev.tomatopotato.asterion.ModrinthUser
import dev.tomatopotato.asterion.ModrinthUserClient
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readLine
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LauncherSignIn {
    private val selector = SelectorManager(Dispatchers.Default)
    private var server: ServerSocket? = null

    @Throws(Throwable::class)
    suspend fun start(): String {
        val socket = aSocket(selector).tcp().bind(LOOPBACK_HOST, 0)
        server = socket
        return signInUrl((socket.localAddress as InetSocketAddress).port)
    }

    @Throws(Throwable::class)
    suspend fun awaitSession(): SignedInSession {
        val socket = server ?: error("Sign-in wasn't started")
        val users = ModrinthUserClient()
        try {
            return withTimeout(TIMEOUT) { firstVerifiedSession(socket, users) }
        } finally {
            cancel()
        }
    }

    private suspend fun firstVerifiedSession(socket: ServerSocket, users: ModrinthUserClient): SignedInSession =
        coroutineScope {
            val result = CompletableDeferred<SignedInSession>()
            val acceptor = launch {
                while (isActive) {
                    val connection = socket.accept()
                    launch {
                        val token = serve(connection) ?: return@launch
                        val user = runCatching { users.fetchCurrentUser(token) }.getOrNull() ?: return@launch
                        result.complete(SignedInSession(token, user))
                    }
                }
            }
            result.await().also { acceptor.cancel() }
        }

    fun cancel() {
        server?.close()
        server = null
    }

    private suspend fun serve(connection: Socket): String? = try {
        withTimeoutOrNull(CONNECTION_TIMEOUT) {
            val input = connection.openReadChannel()
            val requestLine = input.readLine() ?: return@withTimeoutOrNull null
            while (!input.readLine().isNullOrEmpty()) { /* drain headers */ }

            val token = parseCallbackRequest(requestLine)
            val output = connection.openWriteChannel(autoFlush = true)
            output.writeStringUtf8(if (token != null) SUCCESS_RESPONSE else NOT_FOUND_RESPONSE)
            token
        }
    } finally {
        connection.close()
    }

    companion object {
        const val SIGN_IN_URL: String = "https://modrinth.com/auth/sign-in"
        private const val LOOPBACK_HOST = "127.0.0.1"
        private val TIMEOUT = 10.minutes
        private val CONNECTION_TIMEOUT = 15.seconds

        fun signInUrl(port: Int): String = "$SIGN_IN_URL?launcher=true&ipver=4&port=$port"

        fun parseCallbackRequest(requestLine: String): String? {
            val parts = requestLine.split(' ')
            if (parts.size < 2 || parts[0] != "GET") return null
            val target = parts[1]
            val path = target.substringBefore('?')
            if (path != "/" && path.isNotEmpty()) return null
            val query = target.substringAfter('?', "")
            val code = query.split('&')
                .firstOrNull { it.startsWith("code=") }
                ?.removePrefix("code=")
                ?: return null
            return code.takeIf { it.startsWith(SESSION_PREFIX) && it.length > SESSION_PREFIX.length }
        }

        private const val SESSION_PREFIX = "mra_"

        private val SUCCESS_BODY = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width">
            <title>Asterion</title></head>
            <body style="font-family:-apple-system,sans-serif;text-align:center;padding:48px">
            <h2>Signed in</h2><p>You can return to Asterion.</p></body></html>
        """.trimIndent()

        private val SUCCESS_RESPONSE = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Content-Length: ${SUCCESS_BODY.encodeToByteArray().size}\r\n" +
            "Connection: close\r\n\r\n" + SUCCESS_BODY

        private const val NOT_FOUND_RESPONSE =
            "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
    }
}

data class SignedInSession(val token: String, val user: ModrinthUser)
