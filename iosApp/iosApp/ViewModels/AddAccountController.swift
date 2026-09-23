import AuthenticationServices
import Shared
import SwiftUI
import UIKit

@MainActor
@Observable
final class AddAccountController: NSObject, ASWebAuthenticationPresentationContextProviding {

    enum State: Equatable {
        case idle
        case working
        case failed(String)
    }

    private(set) var state: State = .idle
    var isWorking: Bool { state == .working }

    private let auth = ModrinthAuthClient()
    private let userClient = ModrinthUserClient()
    private var session: ASWebAuthenticationSession?
    private var expectedState = ""
    private weak var store: AddedAccountsStore?

    func start(into store: AddedAccountsStore) {
        guard !isWorking else { return }
        self.store = store

        expectedState = AuthConfig.shared.generateState()
        guard let url = URL(string: AuthConfig.shared.buildAuthorizeUrl(state: expectedState)) else {
            state = .failed("Could not build the sign-in URL")
            return
        }
        let callback: ASWebAuthenticationSession.Callback =
            AuthConfig.shared.USE_VERIFIED_HTTPS_CALLBACK
            ? .https(host: AuthConfig.shared.CALLBACK_HOST, path: AuthConfig.shared.CALLBACK_PATH)
            : .customScheme(AuthConfig.shared.CALLBACK_SCHEME)
        let session = ASWebAuthenticationSession(
            url: url,
            callback: callback
        ) { [weak self] callbackURL, error in
            self?.handleCallback(url: callbackURL, error: error)
        }
        session.presentationContextProvider = self
        session.prefersEphemeralWebBrowserSession = true
        self.session = session
        state = .working
        if !session.start() {
            self.session = nil
            state = .failed("iOS couldn't start the sign-in session.")
        }
    }

    func acknowledge() {
        if case .failed = state { state = .idle }
    }

    private func handleCallback(url callbackURL: URL?, error: Error?) {
        session = nil

        if let error {
            if let asError = error as? ASWebAuthenticationSessionError,
               asError.code == .canceledLogin {
                state = .idle
            } else {
                state = .failed(error.localizedDescription)
            }
            return
        }

        guard let callbackURL,
              let items = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)?.queryItems
        else {
            state = .failed("Sign-in returned no data")
            return
        }
        func value(_ name: String) -> String? { items.first { $0.name == name }?.value }

        if let oauthError = value("error") {
            state = .failed("Authorization denied: \(oauthError)")
            return
        }
        guard value("state") == expectedState else {
            state = .failed("Sign-in could not be verified")
            return
        }
        guard let code = value("code") else {
            state = .failed("Sign-in returned no code")
            return
        }

        Task {
            do {
                let token = try await auth.exchangeCode(code: code)
                let newUser = try await userClient.fetchCurrentUser(accessToken: token.accessToken)
                let known = try await knownAccountIDs()
                guard !known.contains(newUser.id) else {
                    state = .failed("\(newUser.username) is already added.")
                    return
                }
                store?.add(
                    StoredAccount(
                        id: newUser.id,
                        username: newUser.username,
                        avatarURL: newUser.avatarUrl,
                        bio: newUser.bio
                    ),
                    token: token.accessToken
                )
                state = .idle
            } catch {
                state = .failed(error.localizedDescription)
            }
        }
    }

    private func knownAccountIDs() async throws -> Set<String> {
        var ids = Set(store?.accounts.map(\.id) ?? [])
        if let activeID = TokenStore.userID() {
            ids.insert(activeID)
        } else if let activeToken = TokenStore.accessToken() {
            let primary = try await userClient.fetchCurrentUser(accessToken: activeToken)
            TokenStore.saveUserID(primary.id)
            ids.insert(primary.id)
        }
        return ids
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?
            .keyWindow
        return window ?? ASPresentationAnchor()
    }
}
