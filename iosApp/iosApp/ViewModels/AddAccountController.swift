import Shared
import SwiftUI

@MainActor
@Observable
final class AddAccountController {

    enum State: Equatable {
        case idle
        case working
        case failed(String)
    }

    private(set) var state: State = .idle
    var isWorking: Bool { state == .working }

    private let signInFlow = ModrinthSignIn()
    private let userClient = ModrinthUserClient()
    private weak var store: AddedAccountsStore?

    func start(into store: AddedAccountsStore) {
        guard !isWorking else { return }
        self.store = store
        state = .working

        Task {
            do {
                guard let result = try await signInFlow.run() else {
                    state = .idle
                    return
                }
                let known = try await knownAccountIDs()
                guard !known.contains(result.user.id) else {
                    state = .failed("\(result.user.username) is already added.")
                    return
                }
                store.add(
                    StoredAccount(
                        id: result.user.id,
                        username: result.user.username,
                        avatarURL: result.user.avatarUrl,
                        bio: result.user.bio
                    ),
                    token: result.token
                )
                state = .idle
            } catch {
                state = .failed(error.localizedDescription)
            }
        }
    }

    func acknowledge() {
        if case .failed = state { state = .idle }
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
}
