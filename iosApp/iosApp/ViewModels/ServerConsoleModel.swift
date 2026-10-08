import Shared
import SwiftUI

@MainActor
@Observable
final class ServerConsoleModel {

    enum Filter: String, CaseIterable, Identifiable {
        case all = "All", error = "Error", warn = "Warn", info = "Info"

        var id: String { rawValue }

        var level: LogLevel? {
            switch self {
            case .all: return nil
            case .error: return .error
            case .warn: return .warn
            case .info: return .info
            }
        }
    }

    private struct ViewKey: Equatable {
        let total: Int64
        let cleared: Int64
        let filter: Filter
        let query: String
    }

    private(set) var snapshot: ConsoleSnapshot?
    private(set) var pendingAction: PowerAction?
    var actionError: String?
    var filter: Filter = .all
    var query = ""

    private let console: ServerConsole
    private var clearedAtTotal: Int64 = 0
    @ObservationIgnored private var viewKey: ViewKey?
    @ObservationIgnored private var viewLines: [ConsoleLine] = []

    init(serverId: String, facade: ServersFacade) {
        console = facade.console(serverId: serverId)
    }

    var powerState: ServerPowerState { snapshot?.powerState ?? .unknown }
    var isConnected: Bool { snapshot?.connection == .connected }
    var canSendCommands: Bool { isConnected && powerState == .running }

    func visibleLines() -> [ConsoleLine] {
        guard let snapshot else { return [] }
        let key = ViewKey(total: snapshot.totalLines, cleared: clearedAtTotal, filter: filter, query: query)
        if key == viewKey { return viewLines }

        let shown = min(Int(snapshot.totalLines - clearedAtTotal), snapshot.lines.count)
        let tail = Array(snapshot.lines.suffix(max(0, shown)))
        let classified = ConsoleLogs.shared.classify(lines: tail)
        viewLines = ConsoleLogs.shared.filter(lines: classified, level: filter.level, query: query)
        viewKey = key
        return viewLines
    }

    func clear() {
        clearedAtTotal = snapshot?.totalLines ?? 0
    }

    func start() {
        console.start { [weak self] snapshot in
            MainActor.assumeIsolated { self?.snapshot = snapshot }
        }
    }

    func stop() {
        console.stop()
    }

    func send(_ command: String) async {
        let trimmed = command.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        do {
            try await console.sendCommand(command: trimmed)
        } catch {
            actionError = ServersStore.message(for: error)
        }
    }

    func perform(_ action: PowerAction) async {
        guard pendingAction == nil else { return }
        pendingAction = action
        defer { pendingAction = nil }
        do {
            try await console.power(action: action)
        } catch {
            actionError = ServersStore.message(for: error)
        }
    }
}
