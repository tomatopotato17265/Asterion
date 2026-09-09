import Foundation

struct StoredAccount: Codable, Identifiable, Equatable {
    let id: String
    var username: String
    var avatarURL: String?
    var bio: String?
}
