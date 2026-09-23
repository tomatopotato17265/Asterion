package dev.tomatopotato.asterion.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import dev.tomatopotato.asterion.servers.ArchonServer
import dev.tomatopotato.asterion.servers.DefaultServerIcon
import dev.tomatopotato.asterion.servers.connectionSummary
import dev.tomatopotato.asterion.servers.loadServerIcon
import dev.tomatopotato.asterion.viewmodel.ServersState
import dev.tomatopotato.asterion.viewmodel.ServersViewModel

private val defaultServerIconBitmap: Bitmap by lazy {
    val bytes = Base64.decode(DefaultServerIcon.base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(viewModel: ServersViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.load() }

    var selectedServerId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedServer = selectedServerId?.let { viewModel.server(it) }

    if (selectedServer != null) {
        ServerDetailScreen(
            server = selectedServer,
            onBack = { selectedServerId = null },
            modifier = modifier,
        )
        return
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = viewModel.state) {
            ServersState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is ServersState.NotPermitted -> EmptyState(
                title = "Hosting isn't available here yet",
                message = "Modrinth Hosting didn't accept this app's sign-in. (${state.detail})",
                actionTitle = "Try again",
                onAction = { viewModel.refresh() },
            )

            is ServersState.Failed -> EmptyState(
                title = "Couldn't load your servers",
                message = state.message,
                actionTitle = "Try again",
                onAction = { viewModel.refresh() },
            )

            is ServersState.Loaded -> {
                if (state.servers.isEmpty()) {
                    EmptyState(
                        title = "No servers yet",
                        message = "Servers you own or have been invited to will show up here.",
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
                            items(state.servers, key = { it.serverId }) { server ->
                                ServerRow(server = server, onClick = { selectedServerId = server.serverId })
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
private fun ServerRow(server: ArchonServer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServerIcon(server = server, size = 36.dp)

        Column {
            Text(
                text = server.name.ifEmpty { server.serverId },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = server.connectionSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ServerIcon(server: ArchonServer, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, server.serverId) {
        val host = server.sftpHost
        val user = server.sftpUsername
        val pass = server.sftpPassword
        value = if (host != null && user != null && pass != null) {
            loadServerIcon(host, user, pass)
        } else {
            null
        }
    }

    Image(
        bitmap = (bitmap ?: defaultServerIconBitmap).asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (actionTitle != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
                Text(actionTitle)
            }
        }
    }
}
