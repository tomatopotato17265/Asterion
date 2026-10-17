import CryptoKit
import Foundation

struct LauncherSealKey {
    static let hkdfInfo = Data("modrinth-app-auth-v2".utf8)

    private let privateKey: P256.KeyAgreement.PrivateKey
    let nonce: String

    init() {
        var bytes = [UInt8](repeating: 0, count: 16)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        self.init(privateKey: P256.KeyAgreement.PrivateKey(), nonce: Self.base64URL(Data(bytes)))
    }

    init(privateKey: P256.KeyAgreement.PrivateKey, nonce: String) {
        self.privateKey = privateKey
        self.nonce = nonce
    }

    var publicKeySPKI: Data { privateKey.publicKey.derRepresentation }

    func open(ephemeralPublicKeySPKI: Data, iv: Data, ciphertext: Data, tag: Data) -> String? {
        guard let ephemeral = try? P256.KeyAgreement.PublicKey(derRepresentation: ephemeralPublicKeySPKI),
              let shared = try? privateKey.sharedSecretFromKeyAgreement(with: ephemeral),
              let gcmNonce = try? AES.GCM.Nonce(data: iv),
              let box = try? AES.GCM.SealedBox(nonce: gcmNonce, ciphertext: ciphertext, tag: tag)
        else { return nil }
        let key = shared.hkdfDerivedSymmetricKey(
            using: SHA256.self,
            salt: Data(),
            sharedInfo: Self.hkdfInfo,
            outputByteCount: 32
        )
        guard let plaintext = try? AES.GCM.open(box, using: key) else { return nil }
        return String(data: plaintext, encoding: .utf8)
    }

    static func base64URL(_ data: Data) -> String {
        data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
