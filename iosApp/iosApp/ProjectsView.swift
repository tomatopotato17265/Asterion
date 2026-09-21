import Shared
import SwiftUI

struct ProjectsView: View {
    @Environment(ProjectsStore.self) private var projects

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Projects")
                .navigationBarTitleDisplayMode(.inline)
        }
        .task { await projects.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch projects.state {
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)

        case let .failed(message):
            EmptyStateView(
                title: "Couldn't load your projects",
                message: message,
                actionTitle: "Try again"
            ) {
                Task { await projects.refresh() }
            }

        case let .loaded(list):
            if list.isEmpty {
                EmptyStateView(
                    title: "No projects yet",
                    message: "Projects you create on Modrinth will show up here.",
                    actionTitle: "Refresh"
                ) {
                    Task { await projects.refresh() }
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(list, id: \.id) { project in
                            NavigationLink {
                                ProjectDetailView(project: project)
                            } label: {
                                ProjectCard(project: project)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                }
                .background(Color(.systemGroupedBackground))
                .refreshable { await projects.refresh() }
            }
        }
    }
}

private struct ProjectCard: View {
    let project: ModrinthProject

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ProjectIcon(url: project.iconUrl, size: 56)

            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.inter(.semibold, size: 16, relativeTo: .headline))
                if !project.description_.isEmpty {
                    Text(project.description_)
                        .font(.inter(.regular, size: 14, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }
                ProjectStatusBadge(project: project)
                    .padding(.top, 2)
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.tertiary)
                .frame(maxHeight: .infinity)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct ProjectIcon: View {
    let url: String?
    let size: CGFloat

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { phase in
            switch phase {
            case let .success(image):
                image.resizable().scaledToFill()
            default:
                Image(systemName: "shippingbox")
                    .font(.system(size: size * 0.4))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .background(Color(.tertiarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: size * 0.21, style: .continuous))
        .accessibilityHidden(true)
    }
}

#Preview {
    ProjectsView()
        .environment(ProjectsStore())
}
