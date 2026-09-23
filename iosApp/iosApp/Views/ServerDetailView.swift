import Shared
import SwiftUI
import UIKit

struct ServerDetailView: View {
    let server: ArchonServer

    @State private var logTail: SftpLogTail?
    @State private var iconLoader: ServerIconLoader?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header

            if server.status == ServerStatus.suspended {
                NoticeBanner(
                    message: "This server is suspended"
                        + (server.suspensionReason.map { " (\($0.name))" } ?? "")
                        + "."
                )
            }

            consoleOutput
                .padding(.bottom, 12)
        }
        .padding(.horizontal, 16)
        .navigationTitle(server.name.isEmpty ? server.serverId : server.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    openOnModrinth()
                } label: {
                    Image(systemName: "safari")
                }
            }
        }
        .onAppear {
            if logTail == nil,
               let host = server.sftpHost, let user = server.sftpUsername, let pass = server.sftpPassword {
                logTail = SftpLogTail(host: host, username: user, password: pass)
            }
            logTail?.start()
        }
        .task {
            guard iconLoader == nil,
                  let host = server.sftpHost, let user = server.sftpUsername, let pass = server.sftpPassword
            else { return }
            let loader = ServerIconLoader(host: host, username: user, password: pass)
            iconLoader = loader
            loader.load()
        }
        .onDisappear {
            logTail?.stop()
            logTail = nil
        }
    }

    private func openOnModrinth() {
        guard let url = URL(string: "https://modrinth.com/hosting/manage/\(server.serverId)") else { return }
        UIApplication.shared.open(url)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 16) {
            icon

            VStack(alignment: .leading, spacing: 6) {
                Text(server.connectionSummary)
                    .font(.inter(.semibold, size: 18, relativeTo: .title3))
                    .foregroundStyle(.primary)

                if let versionLine {
                    Text(versionLine)
                        .font(.inter(.regular, size: 15, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 4)
    }

    private var versionLine: String? {
        guard server.status != ServerStatus.suspended,
              server.status != ServerStatus.installing,
              server.status != ServerStatus.broken
        else { return nil }
        let parts = [server.loader?.display, server.mcVersion].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    @ViewBuilder
    private var icon: some View {
        Group {
            if let image = iconLoader?.image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(uiImage: .defaultServerIcon)
                    .resizable()
                    .scaledToFill()
            }
        }
        .frame(width: 64, height: 64)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }


    private var consoleLines: [String] { logTail?.lines ?? [] }

    private var consoleOutput: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(consoleLines.enumerated()), id: \.offset) { index, line in
                        Text(line)
                            .font(.system(size: 11, design: .monospaced))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .id(index)
                    }

                    Color.clear
                        .frame(height: 1)
                        .id("bottom")
                }
                .padding(12)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
            .overlay {
                if consoleLines.isEmpty {
                    Text(logTail?.errorText ?? "No console output yet")
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                        .font(.inter(.regular, size: 13, relativeTo: .caption))
                        .foregroundStyle(.secondary)
                }
            }

            .task(id: "initial-scroll") {
                guard !consoleLines.isEmpty else { return }
                try? await Task.sleep(for: .milliseconds(50))
                proxy.scrollTo("bottom", anchor: .bottom)
            }
            .onChange(of: consoleLines.count) { _, count in
                guard count > 0 else { return }
                withAnimation { proxy.scrollTo("bottom", anchor: .bottom) }
            }
        }
    }

}

struct NoticeBanner: View {
    let message: String
    var onDismiss: (() -> Void)?

    var body: some View {
        HStack {
            Text(message)
                .font(.inter(.regular, size: 13, relativeTo: .caption))
                .foregroundStyle(.primary)
            Spacer()
            if let onDismiss {
                Button("Dismiss", action: onDismiss)
                    .font(.inter(.regular, size: 13, relativeTo: .caption))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.red.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
    }
}
