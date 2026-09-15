package dev.tomatopotato.asterion.servers

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchonServerDisplayTest {

    private fun server(
        status: String? = "available",
        net: ServerNet? = ServerNet(ip = null, port = 25565, domain = "my-server"),
        loaderRaw: String? = null,
        mcVersion: String? = null,
        suspensionReasonRaw: String? = null,
    ) = ArchonServer(
        serverId = "abc123",
        name = "My Server",
        net = net,
        statusRaw = status,
        loaderRaw = loaderRaw,
        mcVersion = mcVersion,
        suspensionReasonRaw = suspensionReasonRaw,
    )

    @Test
    fun connectAddressAppendsModrinthDomainSuffixWhenBare() {
        assertEquals("my-server.modrinth.gg", server().connectAddress)
    }

    @Test
    fun connectAddressOmitsDefaultPort() {
        val withPort = server(net = ServerNet(ip = null, port = 25566, domain = "my-server"))
        assertEquals("my-server.modrinth.gg:25566", withPort.connectAddress)
    }

    @Test
    fun connectionSummaryReflectsSuspendedStatus() {
        val suspended = server(status = "suspended", suspensionReasonRaw = "paymentfailed")
        assertEquals("Suspended — PaymentFailed", suspended.connectionSummary)
    }

    @Test
    fun connectionSummaryReflectsInstallingStatus() {
        assertEquals("Installing…", server(status = "installing").connectionSummary)
    }

    @Test
    fun subtitleJoinsAddressLoaderAndVersion() {
        val running = server(loaderRaw = "paper", mcVersion = "1.21")
        assertEquals("my-server · Paper · 1.21", running.subtitle)
    }
}
