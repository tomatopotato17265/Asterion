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
