package dev.tomatopotato.asterion.tabs

enum class AppTab(val id: String) {
    HOME("home"),
    PROJECTS("projects"),
    SERVERS("servers"),
    ANALYTICS("analytics"),
    PAYOUTS("payouts"),
    INBOX("inbox"),
    ACCOUNT("account");

    companion object {
        fun fromId(id: String): AppTab? = entries.firstOrNull { it.id == id }
    }
}
