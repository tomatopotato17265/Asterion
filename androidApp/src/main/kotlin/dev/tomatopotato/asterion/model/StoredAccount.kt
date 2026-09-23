package dev.tomatopotato.asterion.model

data class StoredAccount(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
)
