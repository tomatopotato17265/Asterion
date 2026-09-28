package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.R
import dev.tomatopotato.asterion.projects.ModrinthProject
import dev.tomatopotato.asterion.viewmodel.ProjectsState
import dev.tomatopotato.asterion.viewmodel.ProjectsViewModel

@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.load() }

    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val projects = (viewModel.state as? ProjectsState.Loaded)?.projects
    val selectedProject = selectedProjectId?.let { id -> projects?.firstOrNull { it.id == id } }

    if (selectedProject != null) {
        ProjectDetailScreen(
            project = selectedProject,
            onBack = { selectedProjectId = null },
            modifier = modifier,
        )
        return
    }

    ProjectsContent(
        state = viewModel.state,
        onRefresh = viewModel::refresh,
        onSelect = { selectedProjectId = it.id },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsContent(
    state: ProjectsState,
    onRefresh: () -> Unit,
    onSelect: (ModrinthProject) -> Unit,
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
                                ProjectCard(project, onClick = { onSelect(project) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ModrinthProject, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ProjectIcon(project = project, size = 56.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = project.title, style = MaterialTheme.typography.titleMedium)
                if (project.description.isNotEmpty()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProjectStatusBadge(project, modifier = Modifier.padding(top = 2.dp))
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ProjectIcon(project: ModrinthProject, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val icon = rememberRemoteImage(project.iconUrl)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.21f))
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
                        status = "approved",
                    ),
                ),
            ),
            onRefresh = {},
            onSelect = {},
        )
    }
}
