package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tomatopotato.asterion.projects.ModrinthProject
import dev.tomatopotato.asterion.projects.ProjectStatusTone

@Composable
fun ProjectStatusBadge(project: ModrinthProject, modifier: Modifier = Modifier) {
    if (project.statusLabel.isEmpty()) return
    val color = project.statusColor
    Text(
        text = project.statusLabel,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

val ModrinthProject.statusColor: Color
    @Composable get() = when (statusTone) {
        ProjectStatusTone.Positive -> Color(0xFF34C759)
        ProjectStatusTone.Warning -> if (isSystemInDarkThemeApprox()) Color(0xFFFFD60A) else Color(0xFF9E7500)
        ProjectStatusTone.Negative -> Color(0xFFFF3B30)
        ProjectStatusTone.Neutral -> Color(0xFF8E8E93)
    }

@Composable
private fun isSystemInDarkThemeApprox(): Boolean {
    val background = MaterialTheme.colorScheme.background
    val luminance = (0.299f * background.red + 0.587f * background.green + 0.114f * background.blue)
    return luminance < 0.5f
}
