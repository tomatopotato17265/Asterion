package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.AccountViewModel
import dev.tomatopotato.asterion.AddAccountViewModel
import dev.tomatopotato.asterion.NotificationsViewModel
import dev.tomatopotato.asterion.ServersViewModel
import dev.tomatopotato.asterion.TabsViewModel
import dev.tomatopotato.asterion.tabs.AppTab

@Composable
fun MainTabScreen(
    account: AccountViewModel,
    addAccount: AddAccountViewModel,
    servers: ServersViewModel,
    tabs: TabsViewModel,
    inbox: NotificationsViewModel,
    onSignOut: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { account.load() }
    val visibleTabs = tabs.visibleTabs
    var selected by rememberSaveable { mutableStateOf(visibleTabs.first()) }
    if (selected !in visibleTabs) selected = visibleTabs.first()
    val avatar = account.avatar

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        label = { Text(tab.title) },
                        icon = {
                            if (tab == AppTab.ACCOUNT && avatar != null) {
                                Image(
                                    bitmap = avatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                )
                            } else {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selected) {
                AppTab.HOME -> HomeScreen()
                AppTab.PROJECTS -> ProjectsScreen()
                AppTab.SERVERS -> ServersScreen(viewModel = servers)
                AppTab.ANALYTICS -> AnalyticsScreen()
                AppTab.PAYOUTS -> PayoutsScreen()
                AppTab.INBOX -> InboxScreen(viewModel = inbox)
                AppTab.ACCOUNT -> AccountScreen(
                    account = account,
                    addAccount = addAccount,
                    tabs = tabs,
                    onSignOut = onSignOut,
                    onAddAccount = onAddAccount,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MainTabScreenPreview() {
    AsterionTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == AppTab.HOME,
                            onClick = {},
                            label = { Text(tab.title) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        )
                    }
                }
            },
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { HomeScreen() } }
    }
}
