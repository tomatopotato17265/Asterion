import SwiftUI

struct AccountView: View {
    @Environment(AuthController.self) private var auth
    @Environment(AccountStore.self) private var account
    @State private var confirmingSignOut = false

    var body: some View {
        List {
            Section {
                Button(role: .destructive) {
                    confirmingSignOut = true
                } label: {
                    Text("Sign Out")
                        .font(.inter(.regular, size: 17, relativeTo: .body))
                }
            }
        }
        .alert("Sign Out", isPresented: $confirmingSignOut) {
            Button("Cancel", role: .cancel) {}
            Button("Sign Out", role: .destructive) {
                account.reset()
                auth.signOut()
            }
        } message: {
            Text("Are you sure you want to sign out? You'll need to sign in again.")
        }
    }
}

#Preview {
    AccountView()
        .environment(AuthController())
        .environment(AccountStore())
}
