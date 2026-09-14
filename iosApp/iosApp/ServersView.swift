import Shared
import SwiftUI
import UIKit

struct ServersView: View {
    @Environment(ServersStore.self) private var servers

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Servers")
                .navigationBarTitleDisplayMode(.inline)
        }
        .task { await servers.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch servers.state {
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)

        case let .notPermitted(detail):
            EmptyStateView(
                title: "Hosting isn't available here yet",
                message: "Modrinth Hosting didn't accept this app's sign-in. (\(detail))",
                actionTitle: "Try again"
            ) {
                Task { await servers.refresh() }
            }

        case let .failed(message):
            EmptyStateView(
                title: "Couldn't load your servers",
                message: message,
                actionTitle: "Try again"
            ) {
                Task { await servers.refresh() }
            }

        case let .loaded(list):
            if list.isEmpty {
                EmptyStateView(
                    title: "No servers yet",
                    message: "Servers you own or have been invited to will show up here.",
                    actionTitle: "Refresh"
                ) {
                    Task { await servers.refresh() }
                }
            } else {
                List(list, id: \.serverId) { server in
                    NavigationLink {
                        ServerDetailView(server: server)
                    } label: {
                        ServerRow(server: server)
                    }
                }
                .listStyle(.insetGrouped)
                .refreshable { await servers.refresh() }
            }
        }
    }
}

private struct ServerRow: View {
    let server: ArchonServer

    @State private var iconLoader: ServerIconLoader?

    var body: some View {
        HStack(spacing: 12) {
            icon

            VStack(alignment: .leading, spacing: 2) {
                Text(server.name.isEmpty ? server.serverId : server.name)
                    .font(.inter(.semibold, size: 16, relativeTo: .headline))
                Text(server.connectionSummary)
                    .font(.inter(.regular, size: 13, relativeTo: .caption))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
        .task {
            guard iconLoader == nil,
                  let host = server.sftpHost, let user = server.sftpUsername, let pass = server.sftpPassword
            else { return }
            let loader = ServerIconLoader(host: host, username: user, password: pass)
            iconLoader = loader
            loader.load()
        }
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
        .frame(width: 36, height: 36)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct EmptyStateView: View {
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.inter(.semibold, size: 17, relativeTo: .headline))
                .multilineTextAlignment(.center)
            Text(message)
                .font(.inter(.regular, size: 15, relativeTo: .body))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .padding(.top, 4)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    ServersView()
        .environment(ServersStore())
}
