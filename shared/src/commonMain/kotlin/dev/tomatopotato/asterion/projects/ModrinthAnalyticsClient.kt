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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_HOUR = 3600.0

internal class DailyCoreMetrics(
    val views: List<Double>,
    val downloads: List<Double>,
    val playtimeHours: List<Double>,
)

class ModrinthAnalyticsClient(
    private val tokenProvider: TokenProvider,
    private val http: HttpClient = asterionHttpClient(),
    private val baseUrl: String = BASE_URL,
) {
    companion object {
        const val BASE_URL: String = "https://api.modrinth.com/v3"
    }

    @Throws(Throwable::class)
    internal suspend fun dailyCoreMetrics(projectId: String, days: Int, now: Instant): DailyCoreMetrics {
        val response = request(
            projectId = projectId,
            days = days,
            now = now,
            returnMetrics = ReturnMetricsDto(
                projectViews = EmptyAnalyticsMetricDto(),
                projectDownloads = EmptyAnalyticsMetricDto(),
                projectPlaytime = EmptyAnalyticsMetricDto(),
            ),
        )
        return DailyCoreMetrics(
            views = response.metrics.map { slice -> slice.valueFor("views") { it.views } },
            downloads = response.metrics.map { slice -> slice.valueFor("downloads") { it.downloads } },
            playtimeHours = response.metrics.map { slice ->
                slice.valueFor("playtime") { it.seconds } / SECONDS_PER_HOUR
            },
        )
    }

    @Throws(Throwable::class)
    internal suspend fun dailyRevenue(projectId: String, days: Int, now: Instant): List<Double> {
        val response = request(
            projectId = projectId,
            days = days,
            now = now,
            returnMetrics = ReturnMetricsDto(projectRevenue = EmptyAnalyticsMetricDto()),
        )
        return response.metrics.map { slice -> slice.valueFor("revenue") { it.revenue } }
    }

    private suspend fun request(
        projectId: String,
        days: Int,
        now: Instant,
        returnMetrics: ReturnMetricsDto,
    ): AnalyticsResponseDto {
        val token = tokenProvider.token() ?: throw ApiError.Unauthorized("No Modrinth token stored")
        val startInstant = startDateFor(now, days).atStartOfDayIn(TimeZone.UTC)

        val body = AnalyticsRequestDto(
            timeRange = TimeRangeDto(
                start = startInstant.toString(),
                end = now.toString(),
                resolution = ResolutionDto(minutes = MINUTES_PER_DAY),
            ),
            returnMetrics = returnMetrics,
            projectIds = listOf(projectId),
        )

        return apiCall(
            block = {
                http.post("$baseUrl/analytics") {
                    header("Authorization", token)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
            parse = { it.body() },
        )
    }
}

internal fun startDateFor(now: Instant, days: Int): LocalDate =
    now.toLocalDateTime(TimeZone.UTC).date.minus(DatePeriod(days = days - 1))

internal fun buildMetricSeries(
    kind: ProjectMetricKind,
    dailyValues: List<Double>,
    now: Instant,
    currentDays: Int,
    available: Boolean = true,
): ProjectMetricSeries {
    val currentStart = startDateFor(now, currentDays)
    val previous = dailyValues.take(dailyValues.size - currentDays)
    val current = dailyValues.takeLast(currentDays)
    return ProjectMetricSeries(
        kind = kind,
        currentTotal = current.sum(),
        previousTotal = previous.sum(),
        dailyValues = current.mapIndexed { index, value ->
            DailyMetricValue(date = currentStart.plus(DatePeriod(days = index)).toString(), value = value)
        },
        available = available,
    )
}

private inline fun List<AnalyticsEntryDto>.valueFor(metricKind: String, field: (AnalyticsEntryDto) -> Double?): Double =
    firstOrNull { it.metricKind == metricKind }?.let(field) ?: 0.0

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
    @SerialName("project_views") val projectViews: EmptyAnalyticsMetricDto? = null,
    @SerialName("project_downloads") val projectDownloads: EmptyAnalyticsMetricDto? = null,
    @SerialName("project_playtime") val projectPlaytime: EmptyAnalyticsMetricDto? = null,
    @SerialName("project_revenue") val projectRevenue: EmptyAnalyticsMetricDto? = null,
)

@Serializable
private class EmptyAnalyticsMetricDto

@Serializable
private data class AnalyticsResponseDto(val metrics: List<List<AnalyticsEntryDto>> = emptyList())

@Serializable
private data class AnalyticsEntryDto(
    @SerialName("metric_kind") val metricKind: String? = null,
    val views: Double? = null,
    val downloads: Double? = null,
    val seconds: Double? = null,
    @Serializable(with = LenientDecimalSerializer::class) val revenue: Double? = null,
)

private object LenientDecimalSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDecimal", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val element = jsonDecoder.decodeJsonElement()
        return (element as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
    }
}
