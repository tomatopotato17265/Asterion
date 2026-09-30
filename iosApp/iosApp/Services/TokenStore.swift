import Foundation
import Shared

enum TokenStore {
    private static let tokenKey = "modrinth.accessToken"
    private static let expiresAtKey = "modrinth.expiresAt"
    private static let userIDKey = "modrinth.userId"

    static func save(session: String) {
        Keychain.set(session, for: tokenKey)
        let now = Int64(Date().timeIntervalSince1970)
        let expiresAt = ModrinthSessionPolicy.shared.expiresAt(issuedAtEpochSeconds: now)
        Keychain.set(String(expiresAt), for: expiresAtKey)
    }

    static func accessToken() -> String? {
        guard let token = Keychain.get(tokenKey),
              ModrinthSessionPolicy.shared.isSessionToken(token: token)
        else { return nil }
        return token
    }

    static func hasLegacyToken() -> Bool {
        guard let token = Keychain.get(tokenKey) else { return false }
        return !ModrinthSessionPolicy.shared.isSessionToken(token: token)
    }

    static func needsRefresh() -> Bool {
        guard let raw = Keychain.get(expiresAtKey), let expiresAt = Int64(raw) else { return true }
        let now = Int64(Date().timeIntervalSince1970)
        return ModrinthSessionPolicy.shared.needsRefresh(expiresAtEpochSeconds: expiresAt, nowEpochSeconds: now)
    }

    static func saveUserID(_ id: String) { Keychain.set(id, for: userIDKey) }
    static func userID() -> String? { Keychain.get(userIDKey) }

    static func clear() {
        Keychain.delete(tokenKey)
        Keychain.delete(expiresAtKey)
        Keychain.delete(userIDKey)
    }
}
