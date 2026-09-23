import SwiftUI

struct SwitchAccountView: View {
    @Environment(AddedAccountsStore.self) private var addedAccounts
    @State private var adder = AddAccountController()

    var body: some View {
        List {
            if !addedAccounts.accounts.isEmpty {
                Section {
                    ForEach(addedAccounts.accounts) { account in
                        HStack(spacing: 12) {
                            avatar(for: account)
                            Text(account.username)
                                .font(.inter(.regular, size: 17, relativeTo: .body))
                        }
                    }
                }
            }

            Section {
                Button {
                    adder.start(into: addedAccounts)
                } label: {
                    Text("+ Add Account")
                        .font(.inter(.regular, size: 17, relativeTo: .body))
                }
                .disabled(adder.isWorking)
            }
        }
        .navigationTitle("Switch Account")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Couldn't add account", isPresented: addFailureAlertBinding) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(addFailureMessage ?? "")
        }
    }

    @ViewBuilder
    private func avatar(for account: StoredAccount) -> some View {
        AsyncImage(url: account.avatarURL.flatMap { URL(string: $0) }) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Image(systemName: "person.crop.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(.secondary)
        }
        .frame(width: 32, height: 32)
        .clipShape(Circle())
    }

    private var addFailureMessage: String? {
        if case let .failed(message) = adder.state { return message }
        return nil
    }

    private var addFailureAlertBinding: Binding<Bool> {
        Binding(
            get: { if case .failed = adder.state { return true } else { return false } },
            set: { presenting in if !presenting { adder.acknowledge() } }
        )
    }
}

#Preview {
    NavigationStack {
        SwitchAccountView()
    }
    .environment(AddedAccountsStore())
}
