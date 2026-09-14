package dev.tomatopotato.asterion.servers

import kotlin.test.Test
import kotlin.test.assertEquals

class SftpEndpointTest {

    @Test
    fun parsesHostAndPort() {
        val endpoint = SftpEndpoint.parse("node-1.modrinth.com:2022")
        assertEquals("node-1.modrinth.com", endpoint.host)
        assertEquals(2022, endpoint.port)
    }

    @Test
    fun fallsBackToPort22WhenNoColonPresent() {
        val endpoint = SftpEndpoint.parse("node-1.modrinth.com")
        assertEquals("node-1.modrinth.com", endpoint.host)
        assertEquals(22, endpoint.port)
    }

    @Test
    fun fallsBackToPort22WhenSuffixIsNotANumber() {
        val endpoint = SftpEndpoint.parse("node-1.modrinth.com:notaport")
        assertEquals("node-1.modrinth.com:notaport", endpoint.host)
        assertEquals(22, endpoint.port)
    }

    @Test
    fun usesTheLastColonWhenMultiplePresent() {
        val endpoint = SftpEndpoint.parse("::1:2022")
        assertEquals("::1", endpoint.host)
        assertEquals(2022, endpoint.port)
    }
}
