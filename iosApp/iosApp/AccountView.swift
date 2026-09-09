import SwiftUI

struct AccountView: View {
    @Environment(AuthController.self) private var auth
    @Environment(AccountStore.self) private var account
    @State private var confirmingSignOut = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    profileCard
                        .listRowInsets(EdgeInsets(top: 20, leading: 16, bottom: 20, trailing: 16))
                }

                Section {
                    NavigationLink {
                        SwitchAccountView()
                    } label: {
                        Text("Switch Account")
                            .font(.inter(.regular, size: 17, relativeTo: .body))
                    }

                    Button(role: .destructive) {
                        confirmingSignOut = true
                    } label: {
                        Text("Sign Out")
                            .font(.inter(.regular, size: 17, relativeTo: .body))
                    }
                }
            }
            .navigationTitle("Account")
            .navigationBarTitleDisplayMode(.inline)
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

    private var profileCard: some View {
        HStack(spacing: 16) {
            avatarView

            VStack(alignment: .leading, spacing: 4) {
                Text(account.username ?? "")
                    .font(.inter(.bold, size: 20, relativeTo: .title3))

                if let bio = account.bio,
                   !bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(bio)
                        .font(.inter(.regular, size: 15, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                }
            }

            Spacer(minLength: 0)
        }
    }

    @ViewBuilder
    private var avatarView: some View {
        Group {
            if let image = account.avatarImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 60, height: 60)
        .clipShape(Circle())
    }
}

#Preview {
    AccountView()
        .environment(AuthController())
        .environment(AccountStore())
}
