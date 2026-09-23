import Foundation

enum TokenStore {
    private static let tokenKey = "modrinth.accessToken"
    private static let expiresAtKey = "modrinth.expiresAt"
    private static let userIDKey = "modrinth.userId"

    static func save(accessToken: String, expiresIn: Int64) {
        Keychain.set(accessToken, for: tokenKey)
        let expiresAt = Date().addingTimeInterval(TimeInterval(expiresIn))
        Keychain.set(String(expiresAt.timeIntervalSince1970), for: expiresAtKey)
    }

    static func accessToken() -> String? { Keychain.get(tokenKey) }
    static func saveUserID(_ id: String) { Keychain.set(id, for: userIDKey) }
    static func userID() -> String? { Keychain.get(userIDKey) }

    static func clear() {
        Keychain.delete(tokenKey)
        Keychain.delete(expiresAtKey)
        Keychain.delete(userIDKey)
    }
}
