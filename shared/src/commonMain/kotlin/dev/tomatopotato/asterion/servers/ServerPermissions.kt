package dev.tomatopotato.asterion.servers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class ServerPermissions internal constructor(private val mask: ULong) {

    fun has(permission: ServerPermission): Boolean {
        if (permission == ServerPermission.None) return true
        if (permission != ServerPermission.ServerAdmin && hasBits(ServerPermission.ServerAdmin)) return true
        return hasBits(permission)
    }

    private fun hasBits(permission: ServerPermission): Boolean =
        permission.bits != 0UL && (mask and permission.bits) == permission.bits

    val canUsePowerActions: Boolean get() = has(ServerPermission.PowerActions)
    val canExecuteCommands: Boolean get() = has(ServerPermission.ExecCommands)
    val names: List<String>
        get() = if (hasBits(ServerPermission.ServerAdmin)) {
            listOf(ServerPermission.ServerAdmin.wireName)
        } else {
            ServerPermission.entries
                .filter { it != ServerPermission.None && it != ServerPermission.ServerAdmin && hasBits(it) }
                .map { it.wireName }
        }

    companion object {
        val NONE: ServerPermissions = ServerPermissions(0UL)

        fun parse(raw: JsonElement?): ServerPermissions {
            val primitive = raw as? JsonPrimitive ?: return NONE
            if (primitive is JsonNull) return NONE
            val content = primitive.content.trim()
            if (content.isEmpty()) return NONE

            content.toLongOrNull()?.let { return ServerPermissions(it.toULong()) }
            content.toULongOrNull()?.let { return ServerPermissions(it) }
            if (!primitive.isString) return NONE

            val mask = content.split('|')
                .mapNotNull { name -> ServerPermission.entries.firstOrNull { it.wireName == name.trim() } }
                .fold(0UL) { acc, permission -> acc or permission.bits }
            return ServerPermissions(mask)
        }
    }
}

enum class ServerPermission(val wireName: String, internal val bits: ULong) {
    None("NONE", 0UL),
    BaseRead("BASE_READ", 1UL shl 63),
    PowerActions("POWER_ACTIONS", 1UL shl 62),
    ExecCommands("EXEC_COMMANDS", 1UL shl 61),
    FilesWrite("FILES_WRITE", 1UL shl 60),
    Setup("SETUP", 1UL shl 59),
    Backups("BACKUPS", 1UL shl 58),
    Advanced("ADVANCED", 1UL shl 57),
    ResetServer("RESET_SERVER", 1UL shl 56),
    ManageUsers("MANAGE_USERS", 1UL shl 55),
    SupportAgent("SUPPORT_AGENT", 1UL),
    InfraManager("INFRA_MANAGER", 1UL shl 1),
    InfraManagerRead("INFRA_MANAGER_READ", 1UL shl 2),
    InfraServersXfer("INFRA_SERVERS_XFER", 1UL shl 3),
    InfraUsers("INFRA_USERS", 1UL shl 4),
    ServerAdmin("SERVER_ADMIN", ULong.MAX_VALUE xor ((1UL shl 15) - 1UL)),
}
