import SwiftUI

@main
struct iOSApp: App {
    @State private var auth = AuthController()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(auth)
        }
    }
}
