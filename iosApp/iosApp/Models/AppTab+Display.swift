import Shared

extension AppTab {
    var title: String {
        switch self {
        case .home: "Home"
        case .projects: "Projects"
        case .servers: "Servers"
        case .analytics: "Analytics"
        case .payouts: "Payouts"
        case .inbox: "Inbox"
        case .account: "Account"
        default: ""
        }
    }

    var systemImage: String {
        switch self {
        case .home: "house"
        case .projects: "square.grid.2x2"
        case .servers: "server.rack"
        case .analytics: "chart.bar.xaxis"
        case .payouts: "dollarsign"
        case .inbox: "tray"
        case .account: "person.crop.circle"
        default: "questionmark"
        }
    }
}
