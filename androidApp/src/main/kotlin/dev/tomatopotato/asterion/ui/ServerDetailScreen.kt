package dev.tomatopotato.asterion.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tomatopotato.asterion.R
import dev.tomatopotato.asterion.servers.ArchonServer
import dev.tomatopotato.asterion.servers.ServerStatus
import dev.tomatopotato.asterion.servers.SftpLogTail
import dev.tomatopotato.asterion.servers.connectionSummary
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(server: ArchonServer, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val logTail = remember(server.serverId) {
        val host = server.sftpHost
        val user = server.sftpUsername
        val pass = server.sftpPassword
        if (host != null && user != null && pass != null) SftpLogTail(host, user, pass) else null
    }

    DisposableEffect(logTail) {
        logTail?.start(scope)
        onDispose { logTail?.stop() }
    }

    val emptyLines = remember { MutableStateFlow<List<String>>(emptyList()) }
    val noError = remember { MutableStateFlow<String?>(null) }
    val lines by (logTail?.lines ?: emptyLines).collectAsState()
    val errorText by (logTail?.errorText ?: noError).collectAsState()

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(server.name.ifEmpty { server.serverId }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, Uri.parse("https://modrinth.com/hosting/manage/${server.serverId}"))
                }) {
                    Icon(painterResource(R.drawable.ic_open_in_browser), contentDescription = "Open on Modrinth")
                }
            },
        )

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Header(server = server)

            if (server.status == ServerStatus.Suspended) {
                NoticeBanner(
                    message = "This server is suspended" +
                        (server.suspensionReason?.let { " (${it.name})" } ?: "") + ".",
                )
                Spacer(Modifier.padding(top = 8.dp))
            }

            ConsoleOutput(
                lines = lines,
                errorText = errorText,
                modifier = Modifier.weight(1f).padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Header(server: ArchonServer) {
    val versionLine = remember(server) {
        if (server.status == ServerStatus.Suspended ||
            server.status == ServerStatus.Installing ||
            server.status == ServerStatus.Broken
        ) {
            null
        } else {
            listOfNotNull(server.loader?.display, server.mcVersion).takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ServerIcon(server = server, size = 64.dp)

        Column {
            Text(text = server.connectionSummary, style = MaterialTheme.typography.titleMedium)
            if (versionLine != null) {
                Text(
                    text = versionLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoticeBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ConsoleOutput(lines: List<String>, errorText: String?, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (lines.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = errorText ?: "No console output yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(lines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
