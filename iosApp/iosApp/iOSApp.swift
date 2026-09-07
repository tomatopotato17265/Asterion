import SwiftUI

@main
struct iOSApp: App {
    @State private var auth = AuthController()
    @State private var account = AccountStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(auth)
                .environment(account)
        }
    }
}
