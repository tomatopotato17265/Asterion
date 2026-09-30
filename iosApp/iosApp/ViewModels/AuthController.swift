import Shared
import SwiftUI

@MainActor
@Observable
final class AuthController {

    enum Phase: Equatable {
        case signedOut
        case authenticating
        case signedIn
        case failed(String)
    }

    private(set) var phase: Phase
    private(set) var needsReSignIn: Bool

    private let signInFlow = ModrinthSignIn()
    private let sessions = ModrinthSessionClient()
    private var isRefreshing = false

    init() {
        let legacy = TokenStore.hasLegacyToken()
        if legacy { TokenStore.clear() }
        needsReSignIn = legacy
        phase = TokenStore.accessToken() != nil ? .signedIn : .signedOut
    }

    func signIn() {
        guard phase != .authenticating else { return }
        phase = .authenticating
        Task {
            do {
                guard let result = try await signInFlow.run() else {
                    phase = .signedOut
                    return
                }
                TokenStore.save(session: result.token)
                TokenStore.saveUserID(result.user.id)
                needsReSignIn = false
                phase = .signedIn
            } catch {
                phase = .failed(error.localizedDescription)
            }
        }
    }

    func refreshSessionIfNeeded() async {
        guard phase == .signedIn, !isRefreshing, TokenStore.needsRefresh(),
              let current = TokenStore.accessToken() else { return }
        isRefreshing = true
        defer { isRefreshing = false }
        do {
            TokenStore.save(session: try await sessions.refresh(session: current))
        } catch {
            if (error as NSError).kotlinError is ApiError.Unauthorized { signOut() }
        }
    }

    func signOut() {
        signInFlow.cancel()
        TokenStore.clear()
        phase = .signedOut
    }

    func dismissError() {
        if case .failed = phase { phase = .signedOut }
    }
}
