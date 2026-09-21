import Shared
import SwiftUI

struct ProjectDetailView: View {
    let project: ModrinthProject

    var body: some View {
        List {
            Section {
                header
                    .listRowInsets(EdgeInsets(top: 20, leading: 16, bottom: 20, trailing: 16))
            }

            Section {
                if !project.statusLabel.isEmpty {
                    LabeledContent {
                        Text(project.statusLabel)
                            .font(.inter(.regular, size: 17, relativeTo: .body))
                            .foregroundStyle(project.statusColor)
                    } label: {
                        Text("Status").font(.inter(.regular, size: 17, relativeTo: .body))
                    }
                }
                row("Type", project.projectTypeLabel)
                row("Downloads", Self.count(project.downloads))
                row("Followers", Self.count(project.followers))
                row("Published", Self.date(project.published))
                row("Updated", Self.date(project.updated))
            }
        }
        .navigationTitle(project.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let webUrl = project.webUrl, let url = URL(string: webUrl) {
                ToolbarItem(placement: .topBarTrailing) {
                    Link(destination: url) {
                        Image(systemName: "safari")
                    }
                    .accessibilityLabel("View on Modrinth")
                }
            }
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 16) {
            ProjectIcon(url: project.iconUrl, size: 80)

            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.inter(.bold, size: 20, relativeTo: .title3))
                if !project.description_.isEmpty {
                    Text(project.description_)
                        .font(.inter(.regular, size: 15, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    @ViewBuilder
    private func row(_ title: String, _ value: String?) -> some View {
        if let value, !value.isEmpty {
            LabeledContent {
                Text(value).font(.inter(.regular, size: 17, relativeTo: .body))
            } label: {
                Text(title).font(.inter(.regular, size: 17, relativeTo: .body))
            }
        }
    }

    private static func count(_ value: Int64) -> String {
        value.formatted(.number)
    }

    private static func date(_ iso: String) -> String? {
        guard !iso.isEmpty else { return nil }
        let withFraction = ISO8601DateFormatter()
        withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let plain = ISO8601DateFormatter()
        guard let date = withFraction.date(from: iso) ?? plain.date(from: iso) else { return nil }
        return date.formatted(date: .abbreviated, time: .omitted)
    }
}

#Preview {
    NavigationStack {
        ProjectDetailView(
            project: ModrinthProject(
                id: "p1",
                slug: "waypoint-manager",
                title: "Waypoint Manager",
                description: "A simple, chat-based waypoint mod for servers.",
                iconUrl: nil,
                projectType: "mod",
                status: "approved",
                downloads: 1234,
                followers: 56,
                published: "2025-01-01T00:00:00.000000Z",
                updated: "2026-09-01T12:00:00.000000Z"
            )
        )
    }
}
