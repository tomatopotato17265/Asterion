import Citadel
import Foundation
import NIOCore
import Observation
import os
import Shared

@MainActor
@Observable
final class SftpLogTail {

    private(set) var lines: [String] = []
    private(set) var errorText: String?

    private let log = Logger(subsystem: "dev.tomatopotato.asterion", category: "SftpLogTail")
    private let host: String
    private let port: Int
    private let username: String
    private let password: String
    private let path = "/logs/latest.log"
    private let maxChunkBytes: UInt64 = 1_000_000
    private let pollInterval: Duration = .seconds(1)

    // Offset tracking, restart detection, and line-splitting live in the shared KMP module
    // (dev.tomatopotato.asterion.servers.LogTailer) so a future Android implementation can
    // reuse them behind its own SSH transport instead of reimplementing this logic.
    private let tailer = LogTailer(maxLines: 1000, initialTailBytes: 64_000)

    private var task: Task<Void, Never>?

    init(host rawHost: String, username: String, password: String) {
        let endpoint = SftpEndpoint.companion.parse(raw: rawHost)
        host = endpoint.host
        port = Int(endpoint.port)
        self.username = username
        self.password = password
    }

    func start() {
        guard task == nil else { return }
        task = Task { await run() }
    }

    func stop() {
        task?.cancel()
        task = nil
    }

    private func run() async {
        while !Task.isCancelled {
            do {
                try await tailSession()
            } catch is CancellationError {
                return
            } catch {
                if Task.isCancelled { return }
                log.error("tail session ended: \(String(describing: error), privacy: .public)")
                errorText = "Can't read server logs over SFTP: \(error.localizedDescription)"
            }
            try? await Task.sleep(for: .seconds(5))
        }
    }

    private func tailSession() async throws {
        let client = try await SSHClient.connect(
            host: host,
            port: port,
            authenticationMethod: .passwordBased(username: username, password: password),
            hostKeyValidator: .acceptAnything(),
            reconnect: .never
        )
        do {
            let sftp = try await client.openSFTP()
            do {
                try await poll(sftp)
            } catch {
                try? await sftp.close()
                throw error
            }
            try? await sftp.close()
        } catch {
            try? await client.close()
            throw error
        }
        try? await client.close()
    }

    private func poll(_ sftp: SFTPClient) async throws {
        var file: SFTPFile?

        do {
            while !Task.isCancelled {
                let size = Int64(try await sftp.getAttributes(at: path).size ?? 0)
                let action = tailer.onPoll(size: size)
                lines = tailer.lines

                if action is PollActionReset {
                    try? await file?.close()
                    file = nil
                } else if let read = action as? PollActionRead {
                    if file == nil {
                        file = try await sftp.openFile(filePath: path, flags: .read)
                    }
                    var offset = UInt64(read.offset)
                    let target = UInt64(read.upTo)
                    while offset < target {
                        let length = UInt32(min(target - offset, maxChunkBytes))
                        var buffer = try await file!.read(from: offset, length: length)
                        let count = buffer.readableBytes
                        if count == 0 { break }
                        let bytes = buffer.readBytes(length: count) ?? []
                        tailer.onBytesRead(bytes: toKotlinByteArray(bytes))
                        lines = tailer.lines
                        offset += UInt64(count)
                    }
                }

                errorText = nil
                try await Task.sleep(for: pollInterval)
            }
        } catch {
            try? await file?.close()
            throw error
        }
        try? await file?.close()
    }

    private func toKotlinByteArray(_ bytes: [UInt8]) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}
