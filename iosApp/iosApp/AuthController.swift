import AuthenticationServices
import Shared
import SwiftUI
import UIKit

@MainActor
@Observable
final class AuthController: NSObject, ASWebAuthenticationPresentationContextProviding {

    enum Phase: Equatable {
        case signedOut
        case authenticating
        case exchanging
        case signedIn
        case failed(String)
    }

    private(set) var phase: Phase

    private let auth = ModrinthAuthClient()
    private var session: ASWebAuthenticationSession?
    private var expectedState = ""

    override init() {
        phase = TokenStore.accessToken() != nil ? .signedIn : .signedOut
        super.init()
    }

    func signIn() {
        expectedState = AuthConfig.shared.generateState()
        guard let url = URL(string: AuthConfig.shared.buildAuthorizeUrl(state: expectedState)) else {
            phase = .failed("Could not build the sign-in URL")
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
        self.session = session
        phase = .authenticating
        if !session.start() {
            self.session = nil
            phase = .failed("iOS couldn't start the sign-in session.")
        }
    }

    private func handleCallback(url callbackURL: URL?, error: Error?) {
        session = nil

        if let error {
            if let asError = error as? ASWebAuthenticationSessionError,
               asError.code == .canceledLogin {
                phase = .signedOut
            } else {
                phase = .failed(error.localizedDescription)
            }
            return
        }

        guard let callbackURL,
              let items = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)?.queryItems
        else {
            phase = .failed("Sign-in returned no data")
            return
        }
        func value(_ name: String) -> String? { items.first { $0.name == name }?.value }

        if let oauthError = value("error") {
            phase = .failed("Authorization denied: \(oauthError)")
            return
        }
        guard value("state") == expectedState else {
            phase = .failed("Sign-in could not be verified")
            return
        }
        guard let code = value("code") else {
            phase = .failed("Sign-in returned no code")
            return
        }

        phase = .exchanging
        Task {
            do {
                let token = try await auth.exchangeCode(code: code)
                TokenStore.save(accessToken: token.accessToken, expiresIn: token.expiresIn)
                phase = .signedIn
            } catch {
                phase = .failed(error.localizedDescription)
            }
        }
    }

    func signOut() {
        TokenStore.clear()
        phase = .signedOut
    }

    func dismissError() {
        if case .failed = phase { phase = .signedOut }
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?
            .keyWindow
        return window ?? ASPresentationAnchor()
    }
}
