package dev.tomatopotato.asterion.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ProjectStatusTone { Positive, Warning, Negative, Neutral }

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String? = null,
    val title: String,
    val description: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("project_type") val projectType: String = "",
    val status: String = "",
    val downloads: Long = 0,
    val followers: Long = 0,
    val published: String = "",
    val updated: String = "",
) {
    val statusLabel: String get() = status.toLabel()

    val statusTone: ProjectStatusTone
        get() = when (status) {
            "approved", "unlisted", "scheduled" -> ProjectStatusTone.Positive
            "archived", "private", "processing" -> ProjectStatusTone.Warning
            "rejected", "withheld" -> ProjectStatusTone.Negative
            else -> ProjectStatusTone.Neutral
        }
    val projectTypeLabel: String get() = projectType.toLabel()

    val webUrl: String?
        get() = slug?.takeIf { it.isNotEmpty() && projectType.isNotEmpty() }
            ?.let { "https://modrinth.com/$projectType/$it" }
}

private fun String.toLabel(): String =
    replace('-', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() }
