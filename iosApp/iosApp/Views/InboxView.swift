import Shared
import SwiftUI

struct InboxView: View {
    @Environment(InboxStore.self) private var inbox

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Inbox")
                .navigationBarTitleDisplayMode(.inline)
        }
        .task { await inbox.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch inbox.state {
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)

        case let .notPermitted(detail):
            EmptyStateView(
                title: "Notifications aren't available here yet",
                message: "Modrinth didn't accept this app's sign-in. (\(detail))",
                actionTitle: "Try again"
            ) {
                Task { await inbox.refresh() }
            }

        case let .failed(message):
            EmptyStateView(
                title: "Couldn't load your notifications",
                message: message,
                actionTitle: "Try again"
            ) {
                Task { await inbox.refresh() }
            }

        case let .loaded(notifications):
            if notifications.isEmpty {
                EmptyStateView(
                    title: "You're all caught up",
                    message: "New notifications from Modrinth will show up here.",
                    actionTitle: "Refresh"
                ) {
                    Task { await inbox.refresh() }
                }
            } else {
                List(notifications, id: \.id) { notification in
                    NotificationRow(notification: notification)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            Task { await inbox.markRead(notification) }
                        }
                        .swipeActions {
                            Button("Mark as Read") {
                                Task { await inbox.markRead(notification) }
                            }
                            .tint(.accentColor)
                        }
                }
                .listStyle(.insetGrouped)
                .refreshable { await inbox.refresh() }
            }
        }
    }
}

private struct NotificationRow: View {
    let notification: ModrinthNotification

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(Color.accentColor)
                .frame(width: 8, height: 8)
                .padding(.top, 6)

            VStack(alignment: .leading, spacing: 2) {
                Text(notification.title)
                    .font(.inter(.semibold, size: 16, relativeTo: .headline))
                Text(notification.text)
                    .font(.inter(.regular, size: 14, relativeTo: .subheadline))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    InboxView()
        .environment(InboxStore())
}
