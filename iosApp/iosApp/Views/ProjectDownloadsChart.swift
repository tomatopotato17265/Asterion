import Charts
import Shared
import SwiftUI

struct ProjectDownloadsChart: View {
    let state: ProjectAnalyticsStore.State

    @State private var metric: ProjectMetricKind = .views
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

        case let .loaded(series):
            VStack(alignment: .trailing, spacing: 12) {
                MetricCardRow(series: series, selection: $metric)

                if let selected = series.first(where: { $0.kind == metric }), selected.available {
                    ChartKindToggle(selection: $kind)

                    Chart {
                        ForEach(Self.chartPoints(selected.dailyValues)) { point in
                            marks(for: point)
                        }
                    }
                    .chartXAxis {
                        AxisMarks(values: .stride(by: .day, count: 7))
                    }
                    .frame(height: 160)
                } else {
                    Text("\(metric.title) data isn't available for this account yet.")
                        .font(.inter(.regular, size: 14, relativeTo: .footnote))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 8)
                }
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
                y: .value(metric.title, point.value)
            )
            .interpolationMethod(.monotone)

        case .area:
            AreaMark(
                x: .value("Date", point.date, unit: .day),
                y: .value(metric.title, point.value)
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
                y: .value(metric.title, point.value)
            )
            .interpolationMethod(.monotone)

        case .bar:
            BarMark(
                x: .value("Date", point.date, unit: .day),
                y: .value(metric.title, point.value)
            )
            .cornerRadius(2)
        }
    }

    private struct ChartPoint: Identifiable {
        let id: Date
        let date: Date
        let value: Double
    }

    private static func chartPoints(_ points: [DailyMetricValue]) -> [ChartPoint] {
        points.map { ChartPoint(id: $0.swiftDate, date: $0.swiftDate, value: $0.value) }
    }
}


// metric cards
private struct MetricCardRow: View {
    let series: [ProjectMetricSeries]
    @Binding var selection: ProjectMetricKind

    var body: some View {
        HStack(spacing: 8) {
            ForEach(orderedKinds, id: \.self) { kind in
                if let item = series.first(where: { $0.kind == kind }) {
                    MetricCard(series: item, isSelected: selection == kind) {
                        withAnimation(.snappy) { selection = kind }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private let orderedKinds: [ProjectMetricKind] = [.views, .downloads, .revenue, .playtime]
}

private struct MetricCard: View {
    let series: ProjectMetricSeries
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(series.kind.title)
                        .font(.inter(.medium, size: 12, relativeTo: .caption))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                    Spacer(minLength: 0)
                }

                Text(series.available ? series.kind.formattedTotal(series.currentTotal) : "—")
                    .font(.inter(.bold, size: 20, relativeTo: .title3))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)

                if series.available {
                    deltaLabel
                }
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isSelected ? Color.accentColor.opacity(0.14) : Color(.tertiarySystemGroupedBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(isSelected ? Color.accentColor : .clear, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    private var deltaLabel: some View {
        let delta = series.kind.usesPercentDelta ? series.percentDelta : series.absoluteDelta
        let color: Color = delta > 0 ? .green : (delta < 0 ? .red : .secondary)

        return HStack(spacing: 2) {
            if delta != 0 {
                Image(systemName: delta > 0 ? "arrow.up.right" : "arrow.down.right")
                    .font(.system(size: 9, weight: .bold))
            }
            Text(series.kind.formattedDelta(series))
        }
        .font(.inter(.medium, size: 11, relativeTo: .caption2))
        .foregroundStyle(color)
    }
}

private extension ProjectMetricKind {
    func formattedTotal(_ total: Double) -> String {
        switch self {
        case .views, .downloads:
            return Int(total).formatted(.number)
        case .revenue:
            return total.formatted(.currency(code: "USD"))
        case .playtime:
            return total < 10 ? "\(total.formatted(.number.precision(.fractionLength(1)))) hrs" : "\(Int(total.rounded())) hrs"
        default:
            return total.formatted(.number)
        }
    }

    func formattedDelta(_ series: ProjectMetricSeries) -> String {
        let value = usesPercentDelta ? series.percentDelta : series.absoluteDelta
        let sign = value > 0 ? "+" : (value < 0 ? "-" : "")

        let magnitude: String
        if usesPercentDelta {
            magnitude = value == 0 ? "0%" : "\(abs(value).formatted(.number.precision(.fractionLength(1))))%"
        } else if self == .revenue {
            magnitude = abs(value).formatted(.currency(code: "USD"))
        } else {
            magnitude = Int(abs(value)).formatted(.number)
        }
        return sign + magnitude
    }
}

// Chart kind toggle
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

private extension DailyMetricValue {
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
            ProjectDownloadsChart(state: .loaded([
                previewSeries(.views, current: 33, previous: 66),
                previewSeries(.downloads, current: 5, previous: 0),
                previewSeries(.revenue, current: 0.03, previous: 0),
                previewSeries(.playtime, current: 0, previous: 0),
            ]))
        }
    }
}

private func previewSeries(_ kind: ProjectMetricKind, current: Double, previous: Double) -> ProjectMetricSeries {
    ProjectMetricSeries(
        kind: kind,
        currentTotal: current,
        previousTotal: previous,
        dailyValues: (1...30).map { day in
            DailyMetricValue(date: "2026-09-\(String(format: "%02d", day))", value: Double.random(in: 0...(current / 5 + 1)))
        },
        available: true
    )
}
