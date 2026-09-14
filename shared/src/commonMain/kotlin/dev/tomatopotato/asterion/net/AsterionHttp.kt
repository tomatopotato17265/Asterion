package dev.tomatopotato.asterion.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

const val ASTERION_USER_AGENT: String = "Asterion/1.0 (+github.com/tomatopotato17265/Asterion)"

fun interface TokenProvider {
    suspend fun token(): String?
}

val AsterionJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

fun asterionHttpClient(
    tokenProvider: TokenProvider? = null,
): HttpClient = HttpClient {
    expectSuccess = false

    install(ContentNegotiation) { json(AsterionJson) }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }

    defaultRequest {
        header(HttpHeaders.UserAgent, ASTERION_USER_AGENT)
    }
}

suspend fun <T> apiCall(
    block: suspend () -> HttpResponse,
    parse: suspend (HttpResponse) -> T,
): T {
    val response = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiError.Network(e.message ?: "Network request failed", e)
    }

    if (!response.status.isSuccess()) throw response.toApiError()

    return try {
        parse(response)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiError.Network("Could not read the response from Modrinth", e)
    }
}

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

private suspend fun HttpResponse.toApiError(): ApiError = when (status.value) {
    401 -> ApiError.Unauthorized("HTTP 401: ${bodySnippet()}")
    403 -> ApiError.Forbidden("HTTP 403: ${bodySnippet()}")
    404 -> ApiError.NotFound()
    429 -> ApiError.RateLimited(
        headers[HttpHeaders.RetryAfter]?.toLongOrNull()
            ?: headers["X-Ratelimit-Reset"]?.toLongOrNull(),
    )
    in 500..599 -> ApiError.Server(status.value, "Modrinth returned ${status.value}")
    else -> ApiError.Network(
        "Unexpected response ${status.value}: ${runCatching { bodyAsText() }.getOrNull().orEmpty().take(200)}",
    )
}

private suspend fun HttpResponse.bodySnippet(): String =
    runCatching { bodyAsText() }.getOrNull()?.trim()?.take(300)?.ifBlank { null }
        ?: "(empty body) ${request.url.encodedPath}"
