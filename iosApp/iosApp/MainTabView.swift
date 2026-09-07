import SwiftUI

struct MainTabView: View {
    @Environment(AccountStore.self) private var account
    @State private var selection: TabItem = .projects

    enum TabItem: Hashable {
        case projects, analytics, payouts, account
    }

    var body: some View {
        TabView(selection: $selection) {
            ProjectsView()
                .tabItem { Label("Projects", systemImage: "square.grid.2x2") }
                .tag(TabItem.projects)

            AnalyticsView()
                .tabItem { Label("Analytics", systemImage: "chart.bar.xaxis") }
                .tag(TabItem.analytics)

            PayoutsView()
                .tabItem { Label("Payouts", systemImage: "dollarsign") }
                .tag(TabItem.payouts)

            AccountView()
                .tabItem { accountTabLabel }
                .tag(TabItem.account)
        }
        .modifier(GlassTabBar())
        .task { await account.load() }
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
}
