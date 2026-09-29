package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.TokenProvider
import dev.tomatopotato.asterion.net.apiCall
import dev.tomatopotato.asterion.net.asterionHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MINUTES_PER_DAY = 1440L

class ModrinthAnalyticsClient(
    private val tokenProvider: TokenProvider,
    private val http: HttpClient = asterionHttpClient(),
    private val baseUrl: String = BASE_URL,
) {
    companion object {
        const val BASE_URL: String = "https://api.modrinth.com/v3"
    }

    @Throws(Throwable::class)
    suspend fun dailyDownloads(
        projectId: String,
        days: Int,
        now: Instant = Clock.System.now(),
    ): List<DailyDownloads> {
        val token = tokenProvider.token() ?: throw ApiError.Unauthorized("No Modrinth token stored")
        val startDate = startDateFor(now, days)
        val startInstant = startDate.atStartOfDayIn(TimeZone.UTC)

        val request = AnalyticsRequestDto(
            timeRange = TimeRangeDto(
                start = startInstant.toString(),
                end = now.toString(),
                resolution = ResolutionDto(minutes = MINUTES_PER_DAY),
            ),
            returnMetrics = ReturnMetricsDto(),
            projectIds = listOf(projectId),
        )

        val response = apiCall(
            block = {
                http.post("$baseUrl/analytics") {
                    header("Authorization", token)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            },
            parse = { it.body<AnalyticsResponseDto>() },
        )

        return buildDailyDownloads(startDate, response.metrics.map { it.firstOrNull()?.downloads })
    }
}

internal fun startDateFor(now: Instant, days: Int) =
    now.toLocalDateTime(TimeZone.UTC).date.minus(DatePeriod(days = days - 1))

internal fun buildDailyDownloads(startDate: LocalDate, downloadsPerSlice: List<Long?>) =
    downloadsPerSlice.mapIndexed { index, downloads ->
        DailyDownloads(date = startDate.plus(DatePeriod(days = index)).toString(), downloads = downloads ?: 0)
    }

@Serializable
private data class AnalyticsRequestDto(
    @SerialName("time_range") val timeRange: TimeRangeDto,
    @SerialName("return_metrics") val returnMetrics: ReturnMetricsDto,
    @SerialName("project_ids") val projectIds: List<String>,
)

@Serializable
private data class TimeRangeDto(val start: String, val end: String, val resolution: ResolutionDto)

@Serializable
private data class ResolutionDto(val minutes: Long)

@Serializable
private data class ReturnMetricsDto(
    @SerialName("project_downloads") val projectDownloads: EmptyAnalyticsMetricDto = EmptyAnalyticsMetricDto(),
)

@Serializable
private class EmptyAnalyticsMetricDto

@Serializable
private data class AnalyticsResponseDto(val metrics: List<List<AnalyticsEntryDto>> = emptyList())

@Serializable
private data class AnalyticsEntryDto(val downloads: Long? = null)
