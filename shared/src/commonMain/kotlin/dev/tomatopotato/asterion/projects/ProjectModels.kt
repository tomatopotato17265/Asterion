package dev.tomatopotato.asterion.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String? = null,
    val title: String,
    val description: String = "",
    @SerialName("icon_url") val iconUrl: String? = null,
    val updated: String = "",
)
