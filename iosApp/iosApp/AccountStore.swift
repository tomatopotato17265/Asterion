import Shared
import SwiftUI
import UIKit

@MainActor
@Observable
final class AccountStore {

    private(set) var username: String?
    private(set) var avatarTabImage: UIImage?

    private let client = ModrinthUserClient()
    private var didLoad = false

    func load() async {
        guard !didLoad else { return }
        guard let token = TokenStore.accessToken() else { return }

        do {
            let user = try await client.fetchCurrentUser(accessToken: token)
            username = user.username
            if let urlString = user.avatarUrl, let url = URL(string: urlString) {
                let (data, _) = try await URLSession.shared.data(from: url)
                if let image = UIImage(data: data) {
                    avatarTabImage = Self.circularTabIcon(from: image)
                }
            }
            didLoad = true
        } catch {}
    }

    func reset() {
        username = nil
        avatarTabImage = nil
        didLoad = false
    }

    private static func circularTabIcon(from image: UIImage) -> UIImage {
        let side: CGFloat = 25
        let size = CGSize(width: side, height: side)
        let format = UIGraphicsImageRendererFormat.preferred()
        format.opaque = false
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let rendered = renderer.image { _ in
            let rect = CGRect(origin: .zero, size: size)
            UIBezierPath(ovalIn: rect).addClip()
            image.draw(in: rect)
        }
        return rendered.withRenderingMode(.alwaysOriginal)
    }
}
