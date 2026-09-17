package dev.tomatopotato.asterion.ui

import androidx.annotation.DrawableRes
import dev.tomatopotato.asterion.R
import dev.tomatopotato.asterion.tabs.AppTab

val AppTab.title: String
    get() = when (this) {
        AppTab.HOME -> "Home"
        AppTab.PROJECTS -> "Projects"
        AppTab.SERVERS -> "Servers"
        AppTab.ANALYTICS -> "Analytics"
        AppTab.PAYOUTS -> "Payouts"
        AppTab.INBOX -> "Inbox"
        AppTab.ACCOUNT -> "Account"
    }

@get:DrawableRes
val AppTab.iconRes: Int
    get() = when (this) {
        AppTab.HOME -> R.drawable.ic_tab_home
        AppTab.PROJECTS -> R.drawable.ic_tab_projects
        AppTab.SERVERS -> R.drawable.ic_tab_servers
        AppTab.ANALYTICS -> R.drawable.ic_tab_analytics
        AppTab.PAYOUTS -> R.drawable.ic_tab_payouts
        AppTab.INBOX -> R.drawable.ic_tab_inbox
        AppTab.ACCOUNT -> R.drawable.ic_tab_account
    }
