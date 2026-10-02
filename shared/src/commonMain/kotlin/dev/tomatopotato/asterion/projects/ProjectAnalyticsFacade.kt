package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.TokenProvider
import kotlinx.datetime.Clock

private const val CURRENT_PERIOD_DAYS = 30
private const val TOTAL_FETCH_DAYS = CURRENT_PERIOD_DAYS * 2

class ProjectAnalyticsFacade(tokenProvider: () -> String?) {
    private val client = ModrinthAnalyticsClient(TokenProvider { tokenProvider() })

    @Throws(Throwable::class)
    suspend fun projectMetrics(projectId: String): List<ProjectMetricSeries> {
        val now = Clock.System.now()
        val core = client.dailyCoreMetrics(projectId, days = TOTAL_FETCH_DAYS, now = now)
        val revenue = runCatching { client.dailyRevenue(projectId, days = TOTAL_FETCH_DAYS, now = now) }

        return listOf(
            buildMetricSeries(ProjectMetricKind.Views, core.views, now, CURRENT_PERIOD_DAYS),
            buildMetricSeries(ProjectMetricKind.Downloads, core.downloads, now, CURRENT_PERIOD_DAYS),
            buildMetricSeries(ProjectMetricKind.Playtime, core.playtimeHours, now, CURRENT_PERIOD_DAYS),
            revenue.fold(
                onSuccess = { buildMetricSeries(ProjectMetricKind.Revenue, it, now, CURRENT_PERIOD_DAYS) },
                onFailure = {
                    buildMetricSeries(
                        ProjectMetricKind.Revenue,
                        List(TOTAL_FETCH_DAYS) { 0.0 },
                        now,
                        CURRENT_PERIOD_DAYS,
                        available = false,
                    )
                },
            ),
        )
    }
}
