package dev.tomatopotato.asterion.servers

val ArchonServer.connectAddress: String
    get() {
        val n = net ?: return "Address unavailable"
        val ip = n.ip
        if (!ip.isNullOrEmpty()) {
            return if (n.port == 25565) ip else "$ip:${n.port}"
        }
        if (n.domain.isEmpty()) return "Address unavailable"
        val host = if (n.domain.contains(".")) n.domain else "${n.domain}.modrinth.gg"
        return if (n.port == 25565) host else "$host:${n.port}"
    }

val ArchonServer.connectionSummary: String
    get() {
        if (status == ServerStatus.Suspended) {
            val reason = suspensionReason?.let { " — ${it.name}" } ?: ""
            return "Suspended$reason"
        }
        if (status == ServerStatus.Installing) return "Installing…"
        if (status == ServerStatus.Broken) return "Needs attention"
        return connectAddress
    }

val ArchonServer.subtitle: String
    get() {
        if (status == ServerStatus.Suspended) {
            val reason = suspensionReason?.let { " — ${it.name}" } ?: ""
            return "Suspended$reason"
        }
        if (status == ServerStatus.Installing) return "Installing…"
        if (status == ServerStatus.Broken) return "Needs attention"

        val parts = listOfNotNull(address, loader?.display, mcVersion)
        return if (parts.isEmpty()) "Server" else parts.joinToString(" · ")
    }
