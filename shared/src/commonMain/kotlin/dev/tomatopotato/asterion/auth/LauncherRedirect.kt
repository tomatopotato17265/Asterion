package dev.tomatopotato.asterion.auth

import dev.tomatopotato.asterion.ModrinthUserClient
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object LauncherRedirect {
    const val SIGN_IN_URL: String = "${LauncherSignIn.SIGN_IN_URL}?launcher=true"
    const val REDIRECT_HOST: String = "launcher-files.modrinth.com"

    fun parse(url: String): String? {
        val parsed = runCatching { Url(url) }.getOrNull() ?: return null
        if (parsed.protocol != URLProtocol.HTTPS || parsed.host != REDIRECT_HOST) return null
        return parsed.parameters["code"]?.takeIf(ModrinthSessionPolicy::isSessionToken)
            ?.takeIf { it.length > SESSION_PREFIX_LENGTH }
    }

    @Throws(Throwable::class)
    suspend fun verify(token: String): SignedInSession =
        SignedInSession(token, ModrinthUserClient().fetchCurrentUser(token))

    private const val SESSION_PREFIX_LENGTH = 4
}
