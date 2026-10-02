package dev.tomatopotato.asterion.projects

enum class ProjectMetricKind {
    Views, Downloads, Playtime, Revenue;

    val title: String
        get() = when (this) {
            Views -> "Views"
            Downloads -> "Downloads"
            Playtime -> "Playtime"
            Revenue -> "Revenue"
        }


     // Whether this metric's period-over-period change reads better as a percentage or as a raw amount
    val usesPercentDelta: Boolean
        get() = this == Views || this == Playtime
}

data class DailyMetricValue(val date: String, val value: Double)

data class ProjectMetricSeries(
    val kind: ProjectMetricKind,
    val currentTotal: Double,
    val previousTotal: Double,
    val dailyValues: List<DailyMetricValue>,
    val available: Boolean,
) {
    val percentDelta: Double
        get() = when {
            previousTotal == 0.0 -> if (currentTotal == 0.0) 0.0 else 100.0
            else -> (currentTotal - previousTotal) / previousTotal * 100.0
        }

    val absoluteDelta: Double
        get() = currentTotal - previousTotal
}
