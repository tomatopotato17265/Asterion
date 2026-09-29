import Shared
import SwiftUI

@MainActor
@Observable
final class ProjectAnalyticsStore {

    enum State {
        case loading
        case loaded([DailyDownloads])
        case failed(String)
    }

    private(set) var state: State = .loading

    private let facade = ProjectAnalyticsFacade(tokenProvider: { TokenStore.accessToken() })

    private var didLoad = false

    func load(projectId: String) async {
        guard !didLoad else { return }
        didLoad = true
        await refresh(projectId: projectId)
    }

    func refresh(projectId: String) async {
        state = .loading
        do {
            state = .loaded(try await facade.downloadsOverLast30Days(projectId: projectId))
        } catch {
            state = .failed(Self.message(for: error))
        }
    }

    private static func message(for error: Error) -> String {
        guard let kotlinError = (error as NSError).kotlinError else {
            return error.localizedDescription
        }

        switch kotlinError {
        case is ApiError.Unauthorized, is ApiError.Forbidden:
            return "Couldn't load download analytics for this project."
        case is ApiError.NotFound:
            return "Download analytics couldn't be found for this project."
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
