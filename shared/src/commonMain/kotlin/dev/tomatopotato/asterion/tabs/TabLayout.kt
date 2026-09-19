package dev.tomatopotato.asterion.tabs

object TabLayout {
    val freeCandidates: List<AppTab> = listOf(
        AppTab.HOME, AppTab.PROJECTS, AppTab.SERVERS, AppTab.ANALYTICS, AppTab.PAYOUTS
    )

    val defaultFreeOrder: List<AppTab> = listOf(
        AppTab.HOME, AppTab.PROJECTS, AppTab.SERVERS
    )

    val trailingLocked: List<AppTab> = listOf(AppTab.INBOX, AppTab.ACCOUNT)

    const val freeSlotCount: Int = 3

    fun resolveVisibleTabs(persistedFreeOrderIds: List<String>): List<AppTab> {
        val resolved = LinkedHashSet<AppTab>()

        for (id in persistedFreeOrderIds) {
            val tab = AppTab.fromId(id) ?: continue
            if (tab in freeCandidates) resolved.add(tab)
        }
        for (tab in defaultFreeOrder) {
            if (resolved.size >= freeSlotCount) break
            resolved.add(tab)
        }
        for (tab in freeCandidates) {
            if (resolved.size >= freeSlotCount) break
            resolved.add(tab)
        }

        return resolved.toList().take(freeSlotCount) + trailingLocked
    }
}
