import Shared
import SwiftUI

@MainActor
@Observable
final class SelectedTabsStore {

    private static let defaultsKey = "selectedFreeTabOrder"

    private(set) var freeOrder: [AppTab]

    var visibleTabs: [AppTab] {
        TabLayout.shared.resolveVisibleTabs(persistedFreeOrderIds: freeOrder.map { $0.id })
    }

    var freeCandidates: [AppTab] {
        TabLayout.shared.freeCandidates
    }

    init() {
        let stored = UserDefaults.standard.stringArray(forKey: Self.defaultsKey) ?? []
        let resolvedFreeOrder = Array(
            TabLayout.shared.resolveVisibleTabs(persistedFreeOrderIds: stored)
                .prefix(Int(TabLayout.shared.freeSlotCount))
        )
        freeOrder = resolvedFreeOrder

        if stored != resolvedFreeOrder.map({ $0.id }) {
            persist()
        }
    }

    func setFreeOrder(_ newOrder: [AppTab]) {
        freeOrder = Array(
            TabLayout.shared.resolveVisibleTabs(persistedFreeOrderIds: newOrder.map { $0.id })
                .prefix(Int(TabLayout.shared.freeSlotCount))
        )
        persist()
    }

    private func persist() {
        UserDefaults.standard.set(freeOrder.map { $0.id }, forKey: Self.defaultsKey)
    }
}
