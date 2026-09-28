package dev.tomatopotato.asterion.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.R
import dev.tomatopotato.asterion.projects.ModrinthProject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(project: ModrinthProject, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webUrl = project.webUrl

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(project.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
            actions = {
                if (webUrl != null) {
                    IconButton(onClick = {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(webUrl))
                    }) {
                        Icon(painterResource(R.drawable.ic_open_in_browser), contentDescription = "View on Modrinth")
                    }
                }
            },
        )

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Header(project)
            DetailsCard(project)
        }
    }
}

@Composable
private fun Header(project: ModrinthProject) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ProjectIcon(project = project, size = 80.dp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = project.title, style = MaterialTheme.typography.titleLarge)
            if (project.description.isNotEmpty()) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(project: ModrinthProject) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            if (project.statusLabel.isNotEmpty()) {
                DetailRow("Status") { ProjectStatusBadge(project) }
                HorizontalDivider()
            }
            if (project.projectTypeLabel.isNotEmpty()) {
                DetailRow("Type") { Text(project.projectTypeLabel) }
                HorizontalDivider()
            }
            DetailRow("Downloads") { Text(project.downloads.toString()) }
            HorizontalDivider()
            DetailRow("Followers") { Text(project.followers.toString()) }
            formattedDate(project.published)?.let {
                HorizontalDivider()
                DetailRow("Published") { Text(it) }
            }
            formattedDate(project.updated)?.let {
                HorizontalDivider()
                DetailRow("Updated") { Text(it) }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        value()
    }
}

private fun formattedDate(iso: String): String? {
    if (iso.isEmpty()) return null

    val datePart = iso.substringBefore('T').takeIf { it.length == 10 } ?: return null
    val (year, month, day) = datePart.split('-').takeIf { it.size == 3 } ?: return null
    val monthName = MONTH_NAMES.getOrNull(month.toIntOrNull()?.minus(1) ?: -1) ?: return null
    return "$monthName ${day.toIntOrNull() ?: return null}, $year"
}

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
