package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.TokenProvider
import kotlinx.datetime.Clock

class ProjectAnalyticsFacade(tokenProvider: () -> String?) {
    private val client = ModrinthAnalyticsClient(TokenProvider { tokenProvider() })

    @Throws(Throwable::class)
    suspend fun downloadsOverLast30Days(projectId: String): List<DailyDownloads> =
        client.dailyDownloads(projectId = projectId, days = 30, now = Clock.System.now())
}
