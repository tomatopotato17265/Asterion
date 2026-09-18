package dev.tomatopotato.asterion.notifications

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthNotification(
    val id: String,
    val type: String? = null,
    val title: String,
    val text: String,
    val link: String? = null,
    val read: Boolean = false,
    val created: String,
)
