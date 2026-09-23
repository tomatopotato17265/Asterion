import Shared
import SwiftUI

@MainActor
@Observable
final class InboxStore {

    enum State {
        case loading
        case loaded([ModrinthNotification])
        case failed(String)
        case notPermitted(String)
    }

    private(set) var state: State = .loading

    let facade = NotificationsFacade(
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
        state = .loading
        do {
            let notifications = try await facade.listNotifications().filter { !$0.read }
            state = .loaded(notifications)
        } catch {
            if Self.isPermissionError(error) {
                let detail = (error as NSError).kotlinError?.message ?? "Rejected by Modrinth"
                state = .notPermitted(detail)
            } else {
                state = .failed(Self.message(for: error))
            }
        }
    }

    func markRead(_ notification: ModrinthNotification) async {
        guard case let .loaded(notifications) = state else { return }
        state = .loaded(notifications.filter { $0.id != notification.id })
        do {
            try await facade.markRead(id: notification.id)
        } catch {
            state = .loaded(notifications)
        }
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

    private static func message(for error: Error) -> String {
        guard let kotlinError = (error as NSError).kotlinError else {
            return error.localizedDescription
        }

        switch kotlinError {
        case is ApiError.Unauthorized:
            return "Modrinth did not accept this account's sign-in."
        case is ApiError.Forbidden:
            return "This account isn't allowed to view notifications. (\(kotlinError.message ?? "HTTP 403"))"
        case is ApiError.NotFound:
            return "Notifications couldn't be found."
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
