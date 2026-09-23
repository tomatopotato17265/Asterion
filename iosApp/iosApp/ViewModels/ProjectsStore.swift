import Shared
import SwiftUI

@MainActor
@Observable
final class ProjectsStore {

    enum State {
        case loading
        case loaded([ModrinthProject])
        case failed(String)
    }

    private(set) var state: State = .loading

    let facade = ProjectsFacade(
        tokenProvider: { TokenStore.accessToken() },
        userIdProvider: { TokenStore.userID() }
    )

    private var didLoad = false

    func load() async {
        guard !didLoad else { return }
        didLoad = true
        await refresh()
    }

    func refresh() async {
        if case .loaded = state {} else { state = .loading }
        do {
            state = .loaded(try await facade.listProjects())
        } catch {
            state = .failed(Self.message(for: error))
        }
    }

    func reset() {
        state = .loading
        didLoad = false
    }

    private static func message(for error: Error) -> String {
        guard let kotlinError = (error as NSError).kotlinError else {
            return error.localizedDescription
        }

        switch kotlinError {
        case is ApiError.Unauthorized:
            return "Modrinth did not accept this account's sign-in."
        case is ApiError.Forbidden:
            return "This account isn't allowed to view projects. (\(kotlinError.message ?? "HTTP 403"))"
        case is ApiError.NotFound:
            return "Your projects couldn't be found."
        case let rateLimited as ApiError.RateLimited:
            if let seconds = rateLimited.retryAfterSeconds {
                return "Too many requests. Try again in \(seconds.int64Value)s."
            }
            return "Too many requests. Try again shortly."
        case let serverError as ApiError.Server:
            return "Modrinth is having trouble (error \(serverError.status))."
        case let network as ApiError.Network:
            let detail = network.cause?.message ?? network.message ?? ""
            return detail.isEmpty
                ? "Couldn't reach Modrinth. Check your connection."
                : "Couldn't load from Modrinth: \(detail)"
        default:
            return kotlinError.message ?? error.localizedDescription
        }
    }
}
