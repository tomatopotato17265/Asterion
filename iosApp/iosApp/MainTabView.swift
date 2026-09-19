import Shared
import SwiftUI

struct MainTabView: View {
    @Environment(AccountStore.self) private var account
    @Environment(SelectedTabsStore.self) private var selectedTabs
    @State private var selection: AppTab = .home

    var body: some View {
        TabView(selection: $selection) {
            tabContent(for: selectedTabs.visibleTabs[0])
                .tabItem { tabLabel(for: selectedTabs.visibleTabs[0]) }
                .tag(selectedTabs.visibleTabs[0])

            tabContent(for: selectedTabs.visibleTabs[1])
                .tabItem { tabLabel(for: selectedTabs.visibleTabs[1]) }
                .tag(selectedTabs.visibleTabs[1])

            tabContent(for: selectedTabs.visibleTabs[2])
                .tabItem { tabLabel(for: selectedTabs.visibleTabs[2]) }
                .tag(selectedTabs.visibleTabs[2])

            InboxView()
                .tabItem { Label("Inbox", systemImage: "tray") }
                .tag(AppTab.inbox)

            AccountView()
                .tabItem { accountTabLabel }
                .tag(AppTab.account)
        }
        .modifier(GlassTabBar())
        .task { await account.load() }
        .onAppear {
            if !selectedTabs.visibleTabs.contains(selection) {
                selection = selectedTabs.visibleTabs.first ?? .home
            }
        }
    }

    @ViewBuilder
    private func tabContent(for tab: AppTab) -> some View {
        switch tab {
        case .home: HomeView()
        case .projects: ProjectsView()
        case .servers: ServersView()
        case .analytics: AnalyticsView()
        case .payouts: PayoutsView()
        default: EmptyView()
        }
    }

    @ViewBuilder
    private func tabLabel(for tab: AppTab) -> some View {
        Label(tab.title, systemImage: tab.systemImage)
    }

    @ViewBuilder
    private var accountTabLabel: some View {
        if let image = account.avatarTabImage {
            Label {
                Text("Account")
            } icon: {
                Image(uiImage: image)
            }
        } else {
            Label("Account", systemImage: "person.crop.circle")
        }
    }
}

private struct GlassTabBar: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
    }
}

#Preview {
    MainTabView()
        .environment(AccountStore())
        .environment(ServersStore())
        .environment(SelectedTabsStore())
        .environment(InboxStore())
        .environment(ProjectsStore())
}
