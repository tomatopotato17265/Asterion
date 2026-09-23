import Citadel
import Foundation
import NIOCore
import Observation
import Shared
import UIKit

@MainActor
@Observable
final class ServerIconLoader {
    private(set) var image: UIImage?

    private let host: String
    private let port: Int
    private let username: String
    private let password: String
    private var task: Task<Void, Never>?

    init(host rawHost: String, username: String, password: String) {
        let endpoint = SftpEndpoint.companion.parse(raw: rawHost)
        host = endpoint.host
        port = Int(endpoint.port)
        self.username = username
        self.password = password
    }

    func load() {
        guard image == nil, task == nil else { return }
        task = Task {
            image = await Self.fetchIcon(host: host, port: port, username: username, password: password)
        }
    }

    private static func fetchIcon(host: String, port: Int, username: String, password: String) async -> UIImage? {
        guard let client = try? await SSHClient.connect(
            host: host,
            port: port,
            authenticationMethod: .passwordBased(username: username, password: password),
            hostKeyValidator: .acceptAnything(),
            reconnect: .never
        ) else { return nil }

        let image: UIImage?
        if let sftp = try? await client.openSFTP() {
            image = await firstReadableIcon(sftp)
            try? await sftp.close()
        } else {
            image = nil
        }

        try? await client.close()
        return image
    }

    private static func firstReadableIcon(_ sftp: SFTPClient) async -> UIImage? {
        for path in ServerIconPaths.shared.candidates {
            guard let file = try? await sftp.openFile(filePath: path, flags: .read) else { continue }
            var buffer = try? await file.readAll()
            try? await file.close()

            guard var body = buffer,
                  let bytes = body.readBytes(length: body.readableBytes),
                  let image = UIImage(data: Data(bytes))
            else { continue }
            return image
        }
        return nil
    }
}
