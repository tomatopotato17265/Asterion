import Charts
import Shared
import SwiftUI

struct ProjectDownloadsChart: View {
    let state: ProjectAnalyticsStore.State

    var body: some View {
        switch state {
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, minHeight: 120)

        case let .failed(message):
            Text(message)
                .font(.inter(.regular, size: 14, relativeTo: .footnote))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

        case let .loaded(points):
            Chart(Self.chartPoints(points)) { point in
                LineMark(
                    x: .value("Date", point.date, unit: .day),
                    y: .value("Downloads", point.downloads)
                )
                .interpolationMethod(.monotone)
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: 7))
            }
            .frame(height: 160)
            .padding(.vertical, 4)
        }
    }

    private struct ChartPoint: Identifiable {
        let id: Date
        let date: Date
        let downloads: Int64
    }

    private static func chartPoints(_ points: [DailyDownloads]) -> [ChartPoint] {
        points.map { ChartPoint(id: $0.swiftDate, date: $0.swiftDate, downloads: $0.downloads) }
    }
}

private extension DailyDownloads {
    var swiftDate: Date {
        Self.dateFormatter.date(from: date) ?? Date()
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}

#Preview {
    List {
        Section("Analytics") {
            ProjectDownloadsChart(state: .loaded(
                (1...30).map { day in
                    DailyDownloads(
                        date: "2026-09-\(String(format: "%02d", day))",
                        downloads: Int64(Int.random(in: 0...50))
                    )
                }
            ))
        }
    }
}
