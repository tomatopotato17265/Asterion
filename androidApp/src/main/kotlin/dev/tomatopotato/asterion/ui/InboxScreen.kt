package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.notifications.ModrinthNotification
import dev.tomatopotato.asterion.viewmodel.InboxState
import dev.tomatopotato.asterion.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(viewModel: NotificationsViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = viewModel.state) {
            InboxState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is InboxState.NotPermitted -> EmptyState(
                title = "Notifications aren't available here yet",
                message = "Modrinth didn't accept this app's sign-in. (${state.detail})",
                actionTitle = "Try again",
                onAction = { viewModel.refresh() },
            )

            is InboxState.Failed -> EmptyState(
                title = "Couldn't load your notifications",
                message = state.message,
                actionTitle = "Try again",
                onAction = { viewModel.refresh() },
            )

            is InboxState.Loaded -> {
                if (state.notifications.isEmpty()) {
                    EmptyState(
                        title = "You're all caught up",
                        message = "New notifications from Modrinth will show up here.",
                        actionTitle = "Refresh",
                        onAction = { viewModel.refresh() },
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.notifications, key = { it.id }) { notification ->
                                NotificationRow(
                                    notification = notification,
                                    onClick = { viewModel.markRead(notification) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: ModrinthNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )

        Column {
            Text(text = notification.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
