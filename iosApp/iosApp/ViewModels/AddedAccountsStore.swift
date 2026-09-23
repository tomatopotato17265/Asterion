import Foundation
import SwiftUI

@MainActor
@Observable
final class AddedAccountsStore {

    private static let listKey = "modrinth.addedAccounts"
    private static func tokenKey(_ id: String) -> String { "modrinth.token.\(id)" }

    private(set) var accounts: [StoredAccount] = []

    init() {
        if let json = Keychain.get(Self.listKey),
           let data = json.data(using: .utf8),
           let decoded = try? JSONDecoder().decode([StoredAccount].self, from: data) {
            accounts = decoded
        }
    }

    func contains(id: String) -> Bool {
        accounts.contains { $0.id == id }
    }

    func add(_ account: StoredAccount, token: String) {
        guard !contains(id: account.id) else { return }
        Keychain.set(token, for: Self.tokenKey(account.id))
        accounts.append(account)
        persist()
    }

    func token(for id: String) -> String? {
        Keychain.get(Self.tokenKey(id))
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(accounts),
              let json = String(data: data, encoding: .utf8) else { return }
        Keychain.set(json, for: Self.listKey)
    }
}
