package dev.tomatopotato.asterion.servers

object ServerContentListing {
    fun kinds(addons: List<Addon>): List<String> = addons.map { it.kind }.distinct().sorted()

    fun filter(addons: List<Addon>, kind: String?, query: String): List<Addon> {
        val needle = query.trim()
        return addons
            .filter { kind == null || it.kind == kind }
            .filter {
                needle.isEmpty() ||
                    it.displayName.contains(needle, ignoreCase = true) ||
                    it.filename.contains(needle, ignoreCase = true)
            }
            .sortedBy { it.displayName.lowercase() }
    }
}
