package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.AsterionJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerPermissionsTest {

    private fun permsFrom(rawJsonValue: String): ServerPermissions =
        AsterionJson.decodeFromString<ArchonServer>(
            """{"server_id":"s","current_user_permissions":$rawJsonValue}""",
        ).permissions

    @Test
    fun serverAdminAsSignedNumberGrantsEverything() {
        val perms = permsFrom("-32768")
        assertTrue(perms.canUsePowerActions)
        assertTrue(perms.canExecuteCommands)
        assertEquals(listOf("SERVER_ADMIN"), perms.names)
    }

    @Test
    fun serverAdminAsUnsignedStringGrantsEverything() {
        assertTrue(permsFrom("\"18446744073709518848\"").canUsePowerActions)
    }

    @Test
    fun viewerScopesAllowPowerButNotCommands() {
        val perms = permsFrom((Long.MIN_VALUE or (1L shl 62)).toString())
        assertTrue(perms.canUsePowerActions)
        assertFalse(perms.canExecuteCommands)
        assertEquals(listOf("BASE_READ", "POWER_ACTIONS"), perms.names)
    }

    @Test
    fun readOnlyHasNoPowerActions() {
        val perms = permsFrom(Long.MIN_VALUE.toString())
        assertFalse(perms.canUsePowerActions)
        assertEquals(listOf("BASE_READ"), perms.names)
    }

    @Test
    fun scopeNameListIsAccepted() {
        val perms = permsFrom("\"BASE_READ|EXEC_COMMANDS\"")
        assertFalse(perms.canUsePowerActions)
        assertTrue(perms.canExecuteCommands)
    }

    @Test
    fun missingOrZeroMeansNoPermissions() {
        assertFalse(permsFrom("0").canUsePowerActions)
        assertFalse(permsFrom("null").canUsePowerActions)
        assertEquals(emptyList(), permsFrom("0").names)
        val absent = AsterionJson.decodeFromString<ArchonServer>("""{"server_id":"s"}""")
        assertFalse(absent.permissions.canUsePowerActions)
    }
}
