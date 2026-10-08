import Shared
import SwiftUI
import UIKit

struct ServerDetailView: View {
    let server: ArchonServer

    @Environment(ServersStore.self) private var servers
    @State private var console: ServerConsoleModel?
    @State private var confirmingKill = false
    @State private var expandedConsole = false
    @State private var copiedAddress = false

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header

            if server.status == ServerStatus.suspended {
                NoticeBanner(
                    message: "This server is suspended"
                        + (server.suspensionReason.map { " (\($0.name))" } ?? "")
                        + "."
                )
            }

            powerControls
            statCards

            if let console {
                ConsolePanel(model: console, isExpanded: false) { expandedConsole = true }
                    .padding(.bottom, 8)
            } else {
                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .navigationTitle(server.name.isEmpty ? server.serverId : server.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    openOnModrinth()
                } label: {
                    Image(systemName: "safari")
                }
            }
        }
        .task { await servers.loadIcon(for: server.serverId) }
        .onAppear {
            let model = console ?? ServerConsoleModel(serverId: server.serverId, facade: servers.facade)
            console = model
            model.start()
        }
        .onDisappear {
            console?.stop()
            console = nil
        }
        .fullScreenCover(isPresented: $expandedConsole) {
            if let console {
                ConsolePanel(model: console, isExpanded: true) { expandedConsole = false }
                    .padding(16)
                    .background(Color(.systemBackground))
            }
        }
        .confirmationDialog("Kill the server?", isPresented: $confirmingKill, titleVisibility: .visible) {
            Button("Kill", role: .destructive) { run(.kill) }
        } message: {
            Text("Stops the process immediately without saving. Only use this if the server won't stop.")
        }
        .alert("Couldn't do that", isPresented: actionErrorBinding) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(console?.actionError ?? "")
        }
    }

    private func run(_ action: PowerAction) {
        Task { await console?.perform(action) }
    }

    private var actionErrorBinding: Binding<Bool> {
        Binding(
            get: { console?.actionError != nil },
            set: { presenting in if !presenting { console?.actionError = nil } }
        )
    }

    private func openOnModrinth() {
        guard let url = URL(string: "https://modrinth.com/hosting/manage/\(server.serverId)") else { return }
        UIApplication.shared.open(url)
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .center, spacing: 14) {
            icon

            VStack(alignment: .leading, spacing: 6) {
                if isNormalStatus {
                    addressButton
                } else {
                    Text(server.connectionSummary)
                        .font(.inter(.medium, size: 17, relativeTo: .headline))
                        .foregroundStyle(.primary)
                }

                if let versionLine {
                    headerLine(versionLine, systemImage: "shippingbox")
                }
            }

            Spacer(minLength: 0)
        }
    }

    /// address and server type
    private func headerLine(_ text: String, systemImage: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: systemImage)
                .frame(width: 22)
            Text(text)
        }
        .font(.inter(.medium, size: 17, relativeTo: .headline))
        .foregroundStyle(.primary)
        .lineLimit(1)
        .minimumScaleFactor(0.8)
    }

    private var addressButton: some View {
        Button {
            UIPasteboard.general.string = server.connectAddress
            copiedAddress = true
        } label: {
            HStack(spacing: 10) {
                headerLine(server.connectAddress, systemImage: "link")

                Image(systemName: copiedAddress ? "checkmark" : "doc.on.doc")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(copiedAddress ? Color.accentColor : Color.secondary)
                    .contentTransition(.symbolEffect(.replace))
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.success, trigger: copiedAddress) { _, copied in copied }
        .task(id: copiedAddress) {
            guard copiedAddress else { return }
            try? await Task.sleep(for: .seconds(1.5))
            copiedAddress = false
        }
        .accessibilityLabel("Copy server address")
        .accessibilityValue(server.connectAddress)
    }

    private var isNormalStatus: Bool {
        server.status != ServerStatus.suspended
            && server.status != ServerStatus.installing
            && server.status != ServerStatus.broken
    }

    private var versionLine: String? {
        guard isNormalStatus else { return nil }
        let parts = [server.loader?.display, server.mcVersion].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }

    @ViewBuilder
    private var icon: some View {
        Group {
            if let image = servers.icons[server.serverId] {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(uiImage: .defaultServerIcon)
                    .resizable()
                    .scaledToFill()
            }
        }
        .frame(width: 64, height: 64)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: Power

    private var powerState: ServerPowerState { console?.powerState ?? .unknown }

    @ViewBuilder
    private var powerControls: some View {
        let busy = console?.pendingAction != nil || console?.isConnected != true
        HStack(spacing: 8) {
            switch powerState {
            case .running:
                powerButton("Restart", systemImage: "arrow.clockwise", action: .restart, prominent: true)
                powerButton("Stop", systemImage: "stop.fill", action: .stop)
            case .starting:
                powerButton("Stop", systemImage: "stop.fill", action: .stop)
                killButton
            case .stopping:
                killButton
            default:
                powerButton("Start", systemImage: "play.fill", action: .start, prominent: true)
            }
        }
        .disabled(busy || server.status == ServerStatus.suspended)
    }

    private func powerButton(
        _ title: String,
        systemImage: String,
        action: PowerAction,
        prominent: Bool = false
    ) -> some View {
        Button {
            run(action)
        } label: {
            HStack(spacing: 6) {
                if console?.pendingAction == action {
                    ProgressView().controlSize(.small)
                } else {
                    Image(systemName: systemImage)
                }
                Text(title)
            }
            .font(.inter(.semibold, size: 15, relativeTo: .body))
            .foregroundStyle(prominent ? Color.black : Color.primary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                prominent ? Color.accentColor : Color(.tertiarySystemBackground),
                in: RoundedRectangle(cornerRadius: 14)
            )
        }
        .buttonStyle(.plain)
    }

    private var killButton: some View {
        Button {
            confirmingKill = true
        } label: {
            Label("Kill", systemImage: "xmark.octagon.fill")
                .font(.inter(.semibold, size: 15, relativeTo: .body))
                .foregroundStyle(Color.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.red.opacity(0.14), in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    // MARK: Stats

    private var statCards: some View {
        let stats = powerState == .running ? console?.snapshot?.stats : nil
        let lastKnown = console?.snapshot?.stats
        return HStack(spacing: 8) {
            StatCard(
                title: "CPU",
                systemImage: "cpu",
                value: String(format: "%.2f%%", stats?.cpuPercent ?? 0),
                detail: nil,
                progress: (stats?.cpuPercent ?? 0) / 100
            )
            StatCard(
                title: "Memory",
                systemImage: "memorychip",
                value: String(format: "%.2f%%", Self.fraction(stats?.ramUsageBytes, of: stats?.ramTotalBytes) * 100),
                detail: stats.map { Self.formatBytes($0.ramUsageBytes) },
                progress: Self.fraction(stats?.ramUsageBytes, of: stats?.ramTotalBytes)
            )
            StatCard(
                title: "Storage",
                systemImage: "folder",
                value: lastKnown.map { Self.formatBytes($0.storageUsageBytes) } ?? "—",
                detail: lastKnown.flatMap { $0.storageTotalBytes > 0 ? "of \(Self.formatBytes($0.storageTotalBytes))" : nil },
                progress: Self.fraction(lastKnown?.storageUsageBytes, of: lastKnown?.storageTotalBytes)
            )
        }
    }

    private static func fraction(_ used: Int64?, of total: Int64?) -> Double {
        guard let used, let total, total > 0 else { return 0 }
        return min(1, max(0, Double(used) / Double(total)))
    }

    fileprivate static func formatBytes(_ bytes: Int64) -> String {
        let units = ["B", "KiB", "MiB", "GiB", "TiB"]
        var value = Double(bytes)
        var unit = 0
        while value >= 1024, unit < units.count - 1 {
            value /= 1024
            unit += 1
        }
        return unit == 0 ? "\(bytes) B" : String(format: "%.1f %@", value, units[unit])
    }
}

// MARK: - Stat card

private struct StatCard: View {
    let title: String
    let systemImage: String
    let value: String
    let detail: String?
    let progress: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.inter(.medium, size: 12, relativeTo: .caption))
                    .foregroundStyle(.secondary)
                Spacer(minLength: 0)
                Image(systemName: systemImage)
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }

            Text(value)
                .font(.inter(.bold, size: 19, relativeTo: .title3))
                .lineLimit(1)
                .minimumScaleFactor(0.6)

            Text(detail ?? " ")
                .font(.inter(.regular, size: 11, relativeTo: .caption2))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.top, 10)
        .padding(.bottom, 13)
        .background(Color(.tertiarySystemBackground))
        .overlay(alignment: .bottom) { progressBar }
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var progressBar: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Rectangle().fill(Color.accentColor.opacity(0.18))
                Rectangle()
                    .fill(Color.accentColor)
                    .frame(width: geometry.size.width * min(1, max(0, progress)))
            }
        }
        .frame(height: 3)
    }
}

// MARK: - Console

private struct ConsolePanel: View {
    @Bindable var model: ServerConsoleModel
    let isExpanded: Bool
    let onToggleExpand: () -> Void

    @State private var command = ""

    var body: some View {
        let lines = model.visibleLines()
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Console")
                    .font(.inter(.bold, size: 20, relativeTo: .title3))
                Spacer()
                Button(action: onToggleExpand) {
                    Image(systemName: isExpanded
                        ? "arrow.down.right.and.arrow.up.left"
                        : "arrow.up.left.and.arrow.down.right")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(.secondary)
                        .frame(width: 32, height: 32)
                }
                .accessibilityLabel(isExpanded ? "Collapse console" : "Expand console")
            }

            searchField
            filterRow(lines)
            logCard(lines)
        }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Search logs", text: $model.query)
                .font(.inter(.regular, size: 14, relativeTo: .body))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
            if !model.query.isEmpty {
                Button {
                    model.query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private func filterRow(_ lines: [ConsoleLine]) -> some View {
        HStack(spacing: 8) {
            filterButton

            Spacer(minLength: 0)

            Button {
                model.clear()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .medium))
                    .frame(width: 30, height: 30)
            }
            .accessibilityLabel("Clear console")

            ShareLink(item: lines.map(\.text).joined(separator: "\n")) {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 14, weight: .medium))
                    .frame(width: 30, height: 30)
            }
            .disabled(lines.isEmpty)
            .accessibilityLabel("Share logs")
        }
        .foregroundStyle(.secondary)
    }

    private var filterButton: some View {
        let active = model.filter != .all
        let tint = active ? Color.accentColor : Color.secondary
        return Menu {
            Picker("Level", selection: $model.filter) {
                ForEach(ServerConsoleModel.Filter.allCases) { filter in
                    Label(filter.rawValue, systemImage: filter.systemImage).tag(filter)
                }
            }
        } label: {
            HStack(spacing: 14) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(active ? Color.accentColor : Color.primary)
                Text(active ? model.filter.rawValue : "Filter")
                    .font(.inter(.medium, size: 15, relativeTo: .body))
                    .foregroundStyle(tint)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .glassCapsule()
        }
        .menuOrder(.fixed)
        .buttonStyle(.plain)
        .accessibilityLabel("Filter by level")
        .accessibilityValue(model.filter.rawValue)
    }

    private func logCard(_ lines: [ConsoleLine]) -> some View {
        VStack(spacing: 0) {
            logScroll(lines)
            Divider()
            commandBar
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
    }

    private func logScroll(_ lines: [ConsoleLine]) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 4) {
                    ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                        Text(line.text)
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundStyle(color(for: line.level))
                            .textSelection(.enabled)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    Color.clear
                        .frame(height: 1)
                        .id("bottom")
                }
                .padding(12)
            }
            .overlay {
                if lines.isEmpty {
                    Text(placeholder)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                        .font(.inter(.regular, size: 13, relativeTo: .caption))
                        .foregroundStyle(.secondary)
                }
            }
            .defaultScrollAnchor(.bottom)
            .onChange(of: model.snapshot?.totalLines) { scrollToBottom(proxy) }
            .onChange(of: model.filter) { scrollToBottom(proxy) }
            .onChange(of: model.query) { scrollToBottom(proxy) }
        }
    }

    private func scrollToBottom(_ proxy: ScrollViewProxy) {
        proxy.scrollTo("bottom", anchor: .bottom)
    }

    private func color(for level: LogLevel) -> Color {
        switch level {
        case .error: return .red
        case .warn: return .orange
        default: return .primary
        }
    }

    private var placeholder: String {
        if let error = model.snapshot?.errorMessage, !model.isConnected { return error }
        guard model.isConnected else { return "Connecting to the console…" }
        if model.filter != .all || !model.query.trimmingCharacters(in: .whitespaces).isEmpty {
            return "No matching lines"
        }
        return "No console output yet"
    }

    private var commandBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "terminal")
                .font(.system(size: 14))
                .foregroundStyle(.secondary)

            TextField(commandPlaceholder, text: $command)
                .font(.system(size: 13, design: .monospaced))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.send)
                .onSubmit(sendCommand)
                .disabled(!model.canSendCommands)

            if model.canSendCommands {
                Button(action: sendCommand) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 24))
                        .foregroundStyle(
                            command.trimmingCharacters(in: .whitespaces).isEmpty
                                ? Color.secondary
                                : Color.accentColor
                        )
                }
                .disabled(command.trimmingCharacters(in: .whitespaces).isEmpty)
                .accessibilityLabel("Send command")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
    }

    private var commandPlaceholder: String {
        if !model.isConnected { return "Connecting…" }
        return model.canSendCommands ? "Send a command" : "Server is not running"
    }

    private func sendCommand() {
        let text = command
        command = ""
        Task { await model.send(text) }
    }
}

private extension ServerConsoleModel.Filter {
    var systemImage: String {
        switch self {
        case .all: return "list.bullet"
        case .error: return "xmark.octagon"
        case .warn: return "exclamationmark.triangle"
        case .info: return "info.circle"
        }
    }
}

private extension View {
    @ViewBuilder
    func glassCapsule() -> some View {
        if #available(iOS 26, *) {
            self.glassEffect(.regular.interactive(), in: Capsule())
        } else {
            self
                .background(.ultraThinMaterial, in: Capsule())
                .overlay(Capsule().strokeBorder(Color.white.opacity(0.1), lineWidth: 0.5))
        }
    }
}

struct NoticeBanner: View {
    let message: String
    var onDismiss: (() -> Void)?

    var body: some View {
        HStack {
            Text(message)
                .font(.inter(.regular, size: 13, relativeTo: .caption))
                .foregroundStyle(.primary)
            Spacer()
            if let onDismiss {
                Button("Dismiss", action: onDismiss)
                    .font(.inter(.regular, size: 13, relativeTo: .caption))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.red.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
    }
}
