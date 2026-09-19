package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.ProjectsState
import dev.tomatopotato.asterion.ProjectsViewModel
import dev.tomatopotato.asterion.projects.ModrinthProject

@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.load() }
    ProjectsContent(state = viewModel.state, onRefresh = viewModel::refresh, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsContent(
    state: ProjectsState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            ProjectsState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is ProjectsState.Failed -> EmptyState(
                title = "Couldn't load your projects",
                message = state.message,
                actionTitle = "Try again",
                onAction = onRefresh,
            )

            is ProjectsState.Loaded -> {
                if (state.projects.isEmpty()) {
                    EmptyState(
                        title = "No projects yet",
                        message = "Projects you create on Modrinth will show up here.",
                        actionTitle = "Refresh",
                        onAction = onRefresh,
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.projects, key = { it.id }) { project ->
                                ProjectCard(project)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ModrinthProject, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = rememberRemoteImage(project.iconUrl)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = project.title, style = MaterialTheme.typography.titleMedium)
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
}

@Preview
@Composable
private fun ProjectsScreenPreview() {
    AsterionTheme {
        ProjectsContent(
            state = ProjectsState.Loaded(
                listOf(
                    ModrinthProject(
                        id = "p1",
                        title = "Waypoint Manager",
                        description = "A simple, chat-based waypoint mod for servers.",
                    ),
                ),
            ),
            onRefresh = {},
        )
    }
}
