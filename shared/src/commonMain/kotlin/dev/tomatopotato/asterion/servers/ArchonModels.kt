package dev.tomatopotato.asterion.servers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ArchonServer(
    @SerialName("server_id") val serverId: String,
    val name: String = "",
    @SerialName("owner_id") val ownerId: String? = null,
    val net: ServerNet? = null,
    val game: String? = null,
    @SerialName("status") val statusRaw: String? = null,
    @SerialName("suspension_reason") val suspensionReasonRaw: String? = null,
    @SerialName("loader") val loaderRaw: String? = null,
    @SerialName("loader_version") val loaderVersion: String? = null,
    @SerialName("mc_version") val mcVersion: String? = null,
    @SerialName("backup_quota") val backupQuota: Long? = null,
    @SerialName("used_backup_quota") val usedBackupQuota: Long? = null,
    @SerialName("sftp_host") val sftpHost: String? = null,
    @SerialName("sftp_username") val sftpUsername: String? = null,
    @SerialName("sftp_password") val sftpPassword: String? = null,
    val datacenter: String? = null,
    val notices: List<ServerNotice> = emptyList(),
    @SerialName("is_medal") val isMedal: Boolean = false,
    @SerialName("current_user_permissions") val currentUserPermissionsRaw: JsonElement? = null,
) {
    val status: ServerStatus get() = ServerStatus.fromWire(statusRaw)
    val permissions: ServerPermissions get() = ServerPermissions.parse(currentUserPermissionsRaw)
    val suspensionReason: SuspensionReason?
        get() = suspensionReasonRaw?.let { SuspensionReason.fromWire(it) }
    val loader: ServerLoader? get() = loaderRaw?.let { ServerLoader.fromWire(it) }
    val sftpUrl: String? get() =
        if (sftpHost != null && sftpUsername != null) "sftp://$sftpUsername@$sftpHost" else null
    val address: String?
        get() = net?.let { n ->
            val host = n.domain.takeIf { it.isNotBlank() } ?: n.ip ?: return null
            if (n.port == 25565) host else "$host:${n.port}"
        }
}

object ServerIconPaths {
    val candidates: List<String> = listOf("/server-icon.png", "/server-icon-original.png")
}

@Serializable
data class ServerNet(
    val ip: String? = null,
    val port: Int = 25565,
    val domain: String = "",
)

@Serializable
data class ServerNotice(
    val id: Int,
    val title: String = "",
    val message: String = "",
    val level: String = "info",
    val dismissable: Boolean = false,
    val announced: String? = null,
)

@Serializable
data class ServerListResponse(
    val servers: List<ArchonServer> = emptyList(),
    val pagination: Pagination? = null,
    val users: Map<String, ServerOwner> = emptyMap(),
)

@Serializable
data class Pagination(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_items") val totalItems: Int = 0,
)

@Serializable
data class ServerOwner(
    val id: String,
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

enum class ServerStatus {
    Installing, Broken, Available, Suspended, Unknown;

    companion object {
        fun fromWire(raw: String?): ServerStatus = when (raw?.lowercase()) {
            "installing" -> Installing
            "broken" -> Broken
            "available" -> Available
            "suspended" -> Suspended
            else -> Unknown
        }
    }
}

enum class SuspensionReason {
    Moderated, PaymentFailed, Cancelled, Upgrading, Other;

    companion object {
        fun fromWire(raw: String): SuspensionReason = when (raw.lowercase()) {
            "moderated" -> Moderated
            "paymentfailed" -> PaymentFailed
            "cancelled" -> Cancelled
            "upgrading" -> Upgrading
            else -> Other
        }
    }
}

enum class ServerLoader(val display: String) {
    Forge("Forge"),
    NeoForge("NeoForge"),
    Fabric("Fabric"),
    Quilt("Quilt"),
    Purpur("Purpur"),
    Spigot("Spigot"),
    Vanilla("Vanilla"),
    Paper("Paper"),
    Unknown("Unknown");

    companion object {
        fun fromWire(raw: String): ServerLoader =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Unknown
    }
}

enum class PowerAction { Start, Stop, Restart, Kill }

@Serializable
data class ServerFull(
    val id: String,
    val name: String = "",
    val subdomain: String? = null,
    val specs: ServerSpecs? = null,
    val worlds: List<WorldFull> = emptyList(),
) {
    val activeWorld: WorldFull? get() = worlds.firstOrNull { it.isActive } ?: worlds.firstOrNull()
}

@Serializable
data class ServerSpecs(
    val cpu: Double? = null,
    @SerialName("memory_mb") val memoryMb: Long? = null,
    @SerialName("storage_mb") val storageMb: Long? = null,
    @SerialName("swap_mb") val swapMb: Long? = null,
)

@Serializable
data class WorldFull(
    val id: String,
    val name: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
    val content: WorldContentInfo? = null,
)

@Serializable
data class WorldContentInfo(
    val modloader: String? = null,
    @SerialName("modloader_version") val modloaderVersion: String? = null,
    @SerialName("game_version") val gameVersion: String? = null,
)

@Serializable
data class AddonsResponse(
    val modloader: String? = null,
    @SerialName("modloader_version") val modloaderVersion: String? = null,
    @SerialName("game_version") val gameVersion: String? = null,
    val installing: String? = null,
    val addons: List<Addon> = emptyList(),
)

@Serializable
data class Addon(
    val id: String? = null,
    val filename: String = "",
    val filesize: Long = 0,
    val disabled: Boolean = false,
    val kind: String = "mod",
    @SerialName("from_modpack") val fromModpack: Boolean = false,
    @SerialName("has_update") val hasUpdate: String? = null,
    val name: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
) {
    val key: String get() = "$kind:$filename"
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: filename
}
