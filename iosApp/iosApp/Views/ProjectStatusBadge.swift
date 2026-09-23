import Shared
import SwiftUI
import UIKit

struct ProjectStatusBadge: View {
    let project: ModrinthProject

    var body: some View {
        if !project.statusLabel.isEmpty {
            Text(project.statusLabel)
                .font(.inter(.medium, size: 12, relativeTo: .caption))
                .foregroundStyle(project.statusColor)
                .padding(.horizontal, 10)
                .padding(.vertical, 3)
                .background(project.statusColor.opacity(0.16), in: Capsule())
        }
    }
}

extension ModrinthProject {
    var statusColor: Color {
        switch statusTone {
        case .positive: return .green
        case .warning: return Self.warningYellow
        case .negative: return .red
        default: return .gray
        }
    }

    private static let warningYellow = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? .systemYellow
            : UIColor(red: 0.62, green: 0.46, blue: 0.0, alpha: 1)
    })
}
