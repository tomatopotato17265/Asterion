import Shared
import SwiftUI

struct SettingsView: View {
    @Environment(SelectedTabsStore.self) private var selectedTabs
    @State private var order: [AppTab] = []

    var body: some View {
        List {
            Section {
                ForEach(Array(order.enumerated()), id: \.element) { index, tab in
                    Label {
                        Text(tab.title)
                            .font(.inter(.regular, size: 17, relativeTo: .body))
                    } icon: {
                        Image(systemName: tab.systemImage)
                            .foregroundStyle(index < TabLayout.shared.freeSlotCount ? Color.accentColor : Color.secondary)
                    }
                }
                .onMove { indices, newOffset in
                    order.move(fromOffsets: indices, toOffset: newOffset)
                    selectedTabs.setFreeOrder(order)
                }
            } footer: {
                Text("Drag to reorder. The top 3 tabs appear in your tab bar.")
            }
        }
        .environment(\.editMode, .constant(.active))
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            let current = selectedTabs.freeOrder
            let rest = selectedTabs.freeCandidates.filter { !current.contains($0) }
            order = current + rest
        }
    }
}

#Preview {
    NavigationStack {
        SettingsView()
    }
    .environment(SelectedTabsStore())
}
