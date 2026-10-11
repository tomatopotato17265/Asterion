import SwiftUI

enum ServerTab: String, CaseIterable, Identifiable {
    case overview = "Overview"
    case content = "Content"
    case files = "Files"
    case backups = "Backups"
    case access = "Access"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .overview: return "square.grid.2x2"
        case .content: return "square.stack.3d.up"
        case .files: return "folder"
        case .backups: return "clock.arrow.circlepath"
        case .access: return "person.2"
        }
    }
}

struct ServerTabBar: View {
    @Binding var selection: ServerTab
    @Namespace private var pill

    var body: some View {
        HStack(spacing: 2) {
            ForEach(ServerTab.allCases) { tab in
                pillButton(tab)
            }
        }
        .padding(4)
        .background(Color(.tertiarySystemBackground), in: Capsule())
    }

    /// the selected tab shows its label. the rest are icons sharing the leftover width, so all 5 tabs fit on a phone without scrolling
    private func pillButton(_ tab: ServerTab) -> some View {
        let selected = selection == tab
        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) { selection = tab }
        } label: {
            Group {
                if selected {
                    Label(tab.rawValue, systemImage: tab.systemImage)
                        .fixedSize()
                } else {
                    Image(systemName: tab.systemImage)
                }
            }
            .font(.inter(.semibold, size: 14, relativeTo: .subheadline))
            .foregroundStyle(selected ? Color.accentColor : Color.primary)
            .lineLimit(1)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .frame(maxWidth: selected ? nil : .infinity)
            .background {
                if selected {
                    Capsule()
                        .fill(Color.accentColor.opacity(0.18))
                        .matchedGeometryEffect(id: "selected", in: pill)
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .layoutPriority(selected ? 1 : 0)
        .accessibilityLabel(tab.rawValue)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
