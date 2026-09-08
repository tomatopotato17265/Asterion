package dev.tomatopotato.asterion.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.AccountViewModel
import dev.tomatopotato.asterion.R

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Projects("Projects", R.drawable.ic_tab_projects),
    Analytics("Analytics", R.drawable.ic_tab_analytics),
    Payouts("Payouts", R.drawable.ic_tab_payouts),
    Inbox("Inbox", R.drawable.ic_tab_inbox),
    Account("Account", R.drawable.ic_tab_account),
}

@Composable
fun MainTabScreen(
    account: AccountViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { account.load() }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val avatar = account.avatar

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        label = { Text(tab.label) },
                        icon = {
                            if (tab == Tab.Account && avatar != null) {
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
                                    painter = painterResource(tab.icon),
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
            when (Tab.entries[selected]) {
                Tab.Projects -> ProjectsScreen()
                Tab.Analytics -> AnalyticsScreen()
                Tab.Payouts -> PayoutsScreen()
                Tab.Inbox -> InboxScreen()
                Tab.Account -> AccountScreen(account = account, onSignOut = onSignOut)
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
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = index == 0,
                            onClick = {},
                            label = { Text(tab.label) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        )
                    }
                }
            },
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { ProjectsScreen() } }
    }
}
