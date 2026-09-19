package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.ApiError

fun ApiError.projectsMessage(): String = when (this) {
    is ApiError.Unauthorized -> "Modrinth did not accept this account's sign-in."
    is ApiError.Forbidden -> "This account isn't allowed to view projects. ($message)"
    is ApiError.NotFound -> "Your projects couldn't be found."
    is ApiError.RateLimited -> retryAfterSeconds?.let { "Too many requests. Try again in ${it}s." }
        ?: "Too many requests. Try again shortly."
    is ApiError.Server -> "Modrinth is having trouble (error $status)."
    is ApiError.Network -> {
        val detail = cause?.message ?: message
        if (detail.isNullOrEmpty()) {
            "Couldn't reach Modrinth. Check your connection."
        } else {
            "Couldn't load from Modrinth: $detail"
        }
    }
}
