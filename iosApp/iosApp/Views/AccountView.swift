import Shared
import SwiftUI

struct AccountView: View {
    @Environment(AuthController.self) private var auth
    @Environment(AccountStore.self) private var account
    @Environment(ServersStore.self) private var servers
    @Environment(InboxStore.self) private var inbox
    @Environment(ProjectsStore.self) private var projects
    @Environment(\.openURL) private var openURL
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
                        SettingsView()
                    } label: {
                        Text("Settings")
                            .font(.inter(.regular, size: 17, relativeTo: .body))
                    }
                }

                Section {
                    if let url = URL(string: AppLinks.shared.PRIVACY_POLICY_URL) {
                        Button {
                            openURL(url)
                        } label: {
                            HStack(spacing: 12) {
                                Image("PrivacyPolicyMark")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 20, height: 20)
                                    .foregroundStyle(.primary)
                                Text(AppLinks.shared.PRIVACY_POLICY_TITLE)
                                    .font(.inter(.regular, size: 17, relativeTo: .body))
                                    .foregroundStyle(.primary)
                                Spacer()
                                Image(systemName: "arrow.up.right")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.tertiary)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }

                    if let url = URL(string: AppLinks.shared.GITHUB_URL) {
                        Button {
                            openURL(url)
                        } label: {
                            HStack(spacing: 12) {
                                Image("GitHubMark")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 20, height: 20)
                                    .foregroundStyle(.primary)
                                Text(AppLinks.shared.GITHUB_TITLE)
                                    .font(.inter(.regular, size: 17, relativeTo: .body))
                                    .foregroundStyle(.primary)
                                Spacer()
                                Image(systemName: "arrow.up.right")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.tertiary)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }

                    if let url = URL(string: AppLinks.shared.KOFI_URL) {
                        Button {
                            openURL(url)
                        } label: {
                            HStack(spacing: 12) {
                                Image("KoFiMark")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 20, height: 20)
                                    .foregroundStyle(.primary)
                                Text(AppLinks.shared.KOFI_TITLE)
                                    .font(.inter(.regular, size: 17, relativeTo: .body))
                                    .foregroundStyle(.primary)
                                Spacer()
                                Image(systemName: "arrow.up.right")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(.tertiary)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
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
                    servers.reset()
                    inbox.reset()
                    projects.reset()
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
        .environment(AddedAccountsStore())
        .environment(ServersStore())
        .environment(InboxStore())
        .environment(ProjectsStore())
}
