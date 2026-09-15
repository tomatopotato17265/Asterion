package dev.tomatopotato.asterion.net

sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unauthorized(message: String = "HTTP 401: signed out or token rejected") : ApiError(message)
    class Forbidden(message: String = "HTTP 403: not permitted") : ApiError(message)
    class NotFound(message: String = "Not found") : ApiError(message)
    class RateLimited(val retryAfterSeconds: Long?) :
        ApiError("Rate limited by Modrinth")
    class Server(val status: Int, message: String) : ApiError(message)
    class Network(message: String, cause: Throwable? = null) : ApiError(message, cause)
}

val ApiError.isPermissionError: Boolean
    get() = this is ApiError.Unauthorized || this is ApiError.Forbidden

fun ApiError.userMessage(): String = when (this) {
    is ApiError.Unauthorized -> "Modrinth Hosting did not accept this account's sign-in."
    is ApiError.Forbidden -> "This account isn't allowed to do that on this server. ($message)"
    is ApiError.NotFound -> "That server no longer exists."
    is ApiError.RateLimited -> retryAfterSeconds?.let { "Too many requests. Try again in ${it}s." }
        ?: "Too many requests. Try again shortly."
    is ApiError.Server -> "Modrinth Hosting is having trouble (error $status)."
    is ApiError.Network -> {
        val detail = cause?.message ?: message
        if (detail.isNullOrEmpty()) {
            "Couldn't reach Modrinth Hosting. Check your connection."
        } else {
            "Couldn't load from Modrinth Hosting: $detail"
        }
    }
}
