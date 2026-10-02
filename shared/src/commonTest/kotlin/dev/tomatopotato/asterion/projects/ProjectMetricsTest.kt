package dev.tomatopotato.asterion.projects

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectMetricsTest {

    @Test
    fun startDateIsInclusiveOfTodayGoingBackNMinusOneDays() {
        val now = Instant.parse("2026-09-28T15:30:00Z")

        assertEquals(LocalDate(2026, 8, 30), startDateFor(now, days = 30))
        assertEquals(LocalDate(2026, 9, 28), startDateFor(now, days = 1))
    }

    @Test
    fun buildMetricSeriesSplitsPreviousAndCurrentPeriodsAndSumsEach() {
        val now = Instant.parse("2026-09-28T12:00:00Z")
        // 4 "previous" days then 3 "current" days.
        val daily = listOf(1.0, 2.0, 3.0, 4.0, 10.0, 20.0, 30.0)

        val series = buildMetricSeries(ProjectMetricKind.Downloads, daily, now, currentDays = 3)

        assertEquals(10.0, series.previousTotal)
        assertEquals(60.0, series.currentTotal)
        assertEquals(
            listOf(
                DailyMetricValue("2026-09-26", 10.0),
                DailyMetricValue("2026-09-27", 20.0),
                DailyMetricValue("2026-09-28", 30.0),
            ),
            series.dailyValues,
        )
        assertTrue(series.available)
    }

    @Test
    fun absoluteDeltaIsCurrentMinusPrevious() {
        val series = seriesWithTotals(previous = 10.0, current = 15.0)
        assertEquals(5.0, series.absoluteDelta)
    }

    @Test
    fun percentDeltaComputesRelativeChange() {
        val series = seriesWithTotals(previous = 66.0, current = 33.0)
        assertEquals(-50.0, series.percentDelta, absoluteTolerance = 0.001)
    }

    @Test
    fun percentDeltaFromZeroToNonZeroIsAHundredPercent() {
        assertEquals(100.0, seriesWithTotals(previous = 0.0, current = 5.0).percentDelta)
    }

    @Test
    fun percentDeltaStaysZeroWhenBothPeriodsAreZero() {
        assertEquals(0.0, seriesWithTotals(previous = 0.0, current = 0.0).percentDelta)
    }

    @Test
    fun onlyViewsAndPlaytimeUsePercentDeltas() {
        assertTrue(ProjectMetricKind.Views.usesPercentDelta)
        assertTrue(ProjectMetricKind.Playtime.usesPercentDelta)
        assertFalse(ProjectMetricKind.Downloads.usesPercentDelta)
        assertFalse(ProjectMetricKind.Revenue.usesPercentDelta)
    }

    private fun seriesWithTotals(previous: Double, current: Double) = ProjectMetricSeries(
        kind = ProjectMetricKind.Views,
        currentTotal = current,
        previousTotal = previous,
        dailyValues = emptyList(),
        available = true,
    )
}
