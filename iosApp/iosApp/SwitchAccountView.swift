import SwiftUI

struct SwitchAccountView: View {
    var body: some View {
        List {
            Section {
                Button {
                } label: {
                    Text("+ Add Account")
                        .font(.inter(.regular, size: 17, relativeTo: .body))
                }
            }
        }
        .navigationTitle("Switch Account")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        SwitchAccountView()
    }
}
