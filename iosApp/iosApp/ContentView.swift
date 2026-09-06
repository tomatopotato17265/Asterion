import SwiftUI

/// Modrinth brand green — `fill="#1bd96a"` in apps/labrinth/assets/logo.svg.
private let modrinthGreen = Color(red: 0x1B / 255, green: 0xD9 / 255, blue: 0x6A / 255)

struct ContentView: View {
    var body: some View {
        VStack(spacing: 12) {
            Spacer()

            Button {
            } label: {
                Text("Sign in to Modrinth")
                    .font(.inter(.medium, size: 17, relativeTo: .headline))
                    .frame(maxWidth: .infinity)
            }
            .modrinthPrimaryButton()

            Text(disclaimer)
                .font(.inter(.regular, size: 13, relativeTo: .footnote))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var disclaimer: AttributedString {
        let markdown = "By signing in, you accept Modrinth's "
            + "[Terms of Use](https://modrinth.com/legal/terms) and "
            + "[Privacy Policy](https://modrinth.com/legal/privacy)."
        var string = (try? AttributedString(markdown: markdown)) ?? AttributedString(markdown)
        for run in string.runs where run.link != nil {
            string[run.range].underlineStyle = .single
            string[run.range].foregroundColor = .primary
        }
        return string
    }
}

private extension View {
    @ViewBuilder
    func modrinthPrimaryButton() -> some View {
        if #available(iOS 26.0, *) {
            buttonStyle(ModrinthButtonStyle(glass: true))
        } else {
            buttonStyle(ModrinthButtonStyle(glass: false))
        }
    }
}

private struct ModrinthButtonStyle: ButtonStyle {
    var glass: Bool

    func makeBody(configuration: Configuration) -> some View {
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return configuration.label
            .foregroundStyle(Color(.systemBackground))
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(modrinthGreen, in: shape)
            .modifier(GlassSheen(enabled: glass, shape: shape))
            .contentShape(shape)
            .opacity(configuration.isPressed ? 0.85 : 1)
    }
}

private struct GlassSheen: ViewModifier {
    var enabled: Bool
    var shape: RoundedRectangle

    func body(content: Content) -> some View {
        if enabled, #available(iOS 26.0, *) {
            content.glassEffect(.regular.interactive(), in: shape)
        } else {
            content
        }
    }
}

#Preview {
    ContentView()
}
