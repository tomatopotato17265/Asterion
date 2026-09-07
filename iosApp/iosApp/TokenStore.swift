import Foundation

enum TokenStore {
    private static let tokenKey = "modrinth.accessToken"
    private static let expiresAtKey = "modrinth.expiresAt"

    static func save(accessToken: String, expiresIn: Int64) {
        Keychain.set(accessToken, for: tokenKey)
        let expiresAt = Date().addingTimeInterval(TimeInterval(expiresIn))
        Keychain.set(String(expiresAt.timeIntervalSince1970), for: expiresAtKey)
    }

    static func accessToken() -> String? { Keychain.get(tokenKey) }

    static func clear() {
        Keychain.delete(tokenKey)
        Keychain.delete(expiresAtKey)
    }
}
