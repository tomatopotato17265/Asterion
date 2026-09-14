import Shared
import SwiftUI

@MainActor
@Observable
final class ServersStore {

    enum State {
        case loading
        case loaded([ArchonServer])
        case failed(String)
        case notPermitted(String)
    }

    private(set) var state: State = .loading

    let facade = ServersFacade(tokenProvider: { TokenStore.accessToken() })

    private var didLoad = false

    func load() async {
        guard !didLoad else { return }
        didLoad = true
        await refresh()
    }

    func refresh() async {
        state = .loading
        do {
            state = .loaded(try await facade.listServers())
        } catch {
            if Self.isPermissionError(error) {
                let detail = (error as NSError).kotlinError?.message ?? "Rejected by Modrinth Hosting"
                state = .notPermitted(detail)
            } else {
                state = .failed(Self.message(for: error))
            }
        }
    }

    func server(id: String) -> ArchonServer? {
        guard case let .loaded(servers) = state else { return nil }
        return servers.first { $0.serverId == id }
    }

    func reset() {
        state = .loading
        didLoad = false
    }

    private static func isPermissionError(_ error: Error) -> Bool {
        switch (error as NSError).kotlinError {
        case is ApiError.Unauthorized, is ApiError.Forbidden: return true
        default: return false
        }
    }

    static func message(for error: Error) -> String {
        guard let kotlinError = (error as NSError).kotlinError else {
            return error.localizedDescription
        }

        switch kotlinError {
        case is ApiError.Unauthorized:
            return "Modrinth Hosting did not accept this account's sign-in."
        case is ApiError.Forbidden:
            return "This account isn't allowed to do that on this server. (\(kotlinError.message ?? "HTTP 403"))"
        case is ApiError.NotFound:
            return "That server no longer exists."
        case let rateLimited as ApiError.RateLimited:
            if let seconds = rateLimited.retryAfterSeconds {
                return "Too many requests. Try again in \(seconds.int64Value)s."
            }
            return "Too many requests. Try again shortly."
        case let serverError as ApiError.Server:
            return "Modrinth Hosting is having trouble (error \(serverError.status))."
        case let network as ApiError.Network:
            let detail = network.cause?.message ?? network.message ?? ""
            return detail.isEmpty
                ? "Couldn't reach Modrinth Hosting. Check your connection."
                : "Couldn't load from Modrinth Hosting: \(detail)"
        default:
            return kotlinError.message ?? error.localizedDescription
        }
    }
}

extension NSError {
    var kotlinError: ApiError? { userInfo["KotlinException"] as? ApiError }
}

extension ArchonServer {
    var subtitle: String {
        if status == ServerStatus.suspended {
            let reason = suspensionReason.map { " — \($0.name)" } ?? ""
            return "Suspended\(reason)"
        }
        if status == ServerStatus.installing { return "Installing…" }
        if status == ServerStatus.broken { return "Needs attention" }

        let parts = [address, loader?.display, mcVersion].compactMap { $0 }
        return parts.isEmpty ? "Server" : parts.joined(separator: " · ")
    }

    var connectionSummary: String {
        if status == ServerStatus.suspended {
            let reason = suspensionReason.map { " — \($0.name)" } ?? ""
            return "Suspended\(reason)"
        }
        if status == ServerStatus.installing { return "Installing…" }
        if status == ServerStatus.broken { return "Needs attention" }
        return connectAddress
    }

    var connectAddress: String {
        guard let net else { return "Address unavailable" }
        if let ip = net.ip, !ip.isEmpty {
            return net.port == 25565 ? ip : "\(ip):\(net.port)"
        }
        guard !net.domain.isEmpty else { return "Address unavailable" }
        let host = net.domain.contains(".") ? net.domain : "\(net.domain).modrinth.gg"
        return net.port == 25565 ? host : "\(host):\(net.port)"
    }
}
