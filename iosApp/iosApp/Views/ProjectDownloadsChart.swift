import Charts
import Shared
import SwiftUI

struct ProjectDownloadsChart: View {
    let state: ProjectAnalyticsStore.State

    @State private var kind: ChartKind = .line

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
            VStack(alignment: .trailing, spacing: 8) {
                ChartKindToggle(selection: $kind)

                Chart {
                    ForEach(Self.chartPoints(points)) { point in
                        marks(for: point)
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 7))
                }
                .frame(height: 160)
            }
            .padding(.vertical, 4)
        }
    }

    @ChartContentBuilder
    private func marks(for point: ChartPoint) -> some ChartContent {
        switch kind {
        case .line:
            LineMark(
                x: .value("Date", point.date, unit: .day),
                y: .value("Downloads", point.downloads)
            )
            .interpolationMethod(.monotone)

        case .area:
            AreaMark(
                x: .value("Date", point.date, unit: .day),
                y: .value("Downloads", point.downloads)
            )
            .interpolationMethod(.monotone)
            .foregroundStyle(
                .linearGradient(
                    colors: [Color.accentColor.opacity(0.35), Color.accentColor.opacity(0.02)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            LineMark(
                x: .value("Date", point.date, unit: .day),
                y: .value("Downloads", point.downloads)
            )
            .interpolationMethod(.monotone)

        case .bar:
            BarMark(
                x: .value("Date", point.date, unit: .day),
                y: .value("Downloads", point.downloads)
            )
            .cornerRadius(2)
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

private enum ChartKind: CaseIterable, Hashable {
    case line, area, bar

    var title: String {
        switch self {
        case .line: return "Line"
        case .area: return "Area"
        case .bar: return "Bar"
        }
    }
}

private struct ChartKindToggle: View {
    @Binding var selection: ChartKind

    @Namespace private var glassNamespace

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: 4) {
                HStack(spacing: 4) {
                    ForEach(ChartKind.allCases, id: \.self, content: glassSegment)
                }
            }
        } else {
            Picker("Chart type", selection: $selection) {
                ForEach(ChartKind.allCases, id: \.self) { kind in
                    Text(kind.title).tag(kind)
                }
            }
            .pickerStyle(.segmented)
            .frame(maxWidth: 180)
        }
    }

    @available(iOS 26.0, *)
    private func glassSegment(for kind: ChartKind) -> some View {
        let isSelected = selection == kind
        return Button {
            withAnimation(.snappy) { selection = kind }
        } label: {
            Text(kind.title)
                .font(.inter(.medium, size: 13, relativeTo: .footnote))
                .frame(minWidth: 44, minHeight: 30)
                .padding(.horizontal, 4)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(kind.title)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
        .glassEffect(
            isSelected ? .regular.tint(.accentColor).interactive() : .regular.interactive(),
            in: .capsule
        )
        .glassEffectID(kind, in: glassNamespace)
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
