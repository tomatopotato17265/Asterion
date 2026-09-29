package dev.tomatopotato.asterion.projects

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectDownloadHistoryTest {

    @Test
    fun startDateIsInclusiveOfTodayGoingBackNMinusOneDays() {
        val now = Instant.parse("2026-09-28T15:30:00Z")

        assertEquals(LocalDate(2026, 8, 30), startDateFor(now, days = 30))
        assertEquals(LocalDate(2026, 9, 28), startDateFor(now, days = 1))
    }

    @Test
    fun startDateHandlesMonthAndYearBoundaries() {
        val now = Instant.parse("2026-01-05T00:00:00Z")

        assertEquals(LocalDate(2025, 12, 27), startDateFor(now, days = 10))
    }

    @Test
    fun buildsOneEntryPerSliceInDateOrderStartingAtStartDate() {
        val startDate = LocalDate(2026, 9, 1)

        val result = buildDailyDownloads(startDate, downloadsPerSlice = listOf(5L, null, 0L, 12L))

        assertEquals(
            listOf(
                DailyDownloads("2026-09-01", 5),
                DailyDownloads("2026-09-02", 0),
                DailyDownloads("2026-09-03", 0),
                DailyDownloads("2026-09-04", 12),
            ),
            result,
        )
    }

    @Test
    fun emptySliceListProducesEmptyHistory() {
        assertEquals(emptyList(), buildDailyDownloads(LocalDate(2026, 9, 1), emptyList()))
    }
}
