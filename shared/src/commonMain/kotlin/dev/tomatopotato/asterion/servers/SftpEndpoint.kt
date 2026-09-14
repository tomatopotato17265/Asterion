package dev.tomatopotato.asterion.servers

data class SftpEndpoint(val host: String, val port: Int) {
    companion object {
        const val DEFAULT_PORT: Int = 22

        fun parse(raw: String): SftpEndpoint {
            val colon = raw.lastIndexOf(':')
            if (colon < 0) return SftpEndpoint(raw, DEFAULT_PORT)
            val port = raw.substring(colon + 1).toIntOrNull() ?: return SftpEndpoint(raw, DEFAULT_PORT)
            return SftpEndpoint(raw.substring(0, colon), port)
        }
    }
}
