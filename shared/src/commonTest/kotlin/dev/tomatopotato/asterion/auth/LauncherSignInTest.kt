package dev.tomatopotato.asterion.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LauncherSignInTest {

    @Test
    fun buildsTheLauncherSignInUrl() {
        assertEquals(
            "https://modrinth.com/auth/sign-in?launcher=true&ipver=4&port=51234",
            LauncherSignIn.signInUrl(51234),
        )
    }

    @Test
    fun extractsTheSessionFromTheRedirect() {
        assertEquals("mra_abc123", LauncherSignIn.parseCallbackRequest("GET /?code=mra_abc123 HTTP/1.1"))
        assertEquals("mra_abc123", LauncherSignIn.parseCallbackRequest("GET ?code=mra_abc123 HTTP/1.1"))
        assertEquals("mra_x", LauncherSignIn.parseCallbackRequest("GET /?foo=1&code=mra_x HTTP/1.1"))
    }

    @Test
    fun ignoresAnythingThatIsNotASessionRedirect() {
        assertNull(LauncherSignIn.parseCallbackRequest("GET /favicon.ico HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("GET / HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("GET /?code= HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("GET /?code=mra_ HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("GET /?code=mro_oauth HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("GET /other?code=mra_abc HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("POST /?code=mra_abc HTTP/1.1"))
        assertNull(LauncherSignIn.parseCallbackRequest("garbage"))
    }

    @Test
    fun sessionRefreshPolicy() {
        val issued = 1_000_000L
        val expires = ModrinthSessionPolicy.expiresAt(issued)
        assertEquals(issued + 14 * 24 * 3600, expires)
        assertFalse(ModrinthSessionPolicy.needsRefresh(expires, issued + 12 * 24 * 3600))
        assertTrue(ModrinthSessionPolicy.needsRefresh(expires, issued + 13 * 24 * 3600))
        assertTrue(ModrinthSessionPolicy.needsRefresh(expires, expires + 1))
        assertTrue(ModrinthSessionPolicy.isSessionToken("mra_abc"))
        assertFalse(ModrinthSessionPolicy.isSessionToken("mro_abc"))
    }
}
