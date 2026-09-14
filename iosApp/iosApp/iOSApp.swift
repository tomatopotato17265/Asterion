import SwiftUI

@main
struct iOSApp: App {
    @State private var auth = AuthController()
    @State private var account = AccountStore()
    @State private var addedAccounts = AddedAccountsStore()
    @State private var servers = ServersStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(auth)
                .environment(account)
                .environment(addedAccounts)
                .environment(servers)
        }
    }
}
