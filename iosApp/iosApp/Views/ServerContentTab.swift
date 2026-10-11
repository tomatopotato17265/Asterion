import Shared
import SwiftUI

/// read-only view of the mods, plugins and data packs installed on the server's active world
struct ServerContentTab: View {
    @Bindable var model: ServerContentModel

    var body: some View {
        Group {
            switch model.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .failed(let message):
                VStack(spacing: 12) {
                    Text(message)
                        .font(.inter(.regular, size: 14, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    Button("Try again") { Task { await model.retry() } }
                        .buttonStyle(.bordered)
                }
                .padding(.horizontal, 24)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded(let response):
                loaded(response)
            }
        }
        .task { await model.load() }
    }

    private func loaded(_ response: AddonsResponse) -> some View {
        let addons = model.visibleAddons
        return VStack(alignment: .leading, spacing: 10) {
            if let installing = response.installing, !installing.isEmpty {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("Installing \(installing)…")
                        .font(.inter(.regular, size: 13, relativeTo: .caption))
                        .foregroundStyle(.secondary)
                }
            }

            if !response.addons.isEmpty {
                Text(summary(response))
                    .font(.inter(.medium, size: 13, relativeTo: .caption))
                    .foregroundStyle(.secondary)

                HStack(spacing: 8) {
                    searchField
                    if model.kinds.count > 1 { kindMenu }
                }
            }

            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(addons, id: \.key) { addon in
                        AddonRow(addon: addon)
                    }
                }
                .padding(.bottom, 8)
            }
            .overlay {
                if addons.isEmpty {
                    Text(emptyMessage(response))
                        .font(.inter(.regular, size: 14, relativeTo: .subheadline))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
            }
            .refreshable { await model.refresh() }
        }
    }

    private func summary(_ response: AddonsResponse) -> String {
        let all = response.addons
        let kinds = model.kinds
        var text = kinds.count == 1
            ? Self.count(all.count, kind: kinds[0])
            : "\(all.count) \(all.count == 1 ? "item" : "items")"
        let updates = all.filter { $0.hasUpdate != nil }.count
        if updates > 0 { text += " · \(updates) \(updates == 1 ? "update" : "updates") available" }
        return text
    }

    private func emptyMessage(_ response: AddonsResponse) -> String {
        if !response.addons.isEmpty { return "No matching content" }
        let loader = response.modloader?.lowercased() ?? "vanilla"
        return loader == "vanilla" ? "Vanilla servers don't run mods or plugins." : "No mods or plugins installed"
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Search content", text: $model.query)
                .font(.inter(.regular, size: 14, relativeTo: .body))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
            if !model.query.isEmpty {
                Button {
                    model.query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var kindMenu: some View {
        Menu {
            Picker("Kind", selection: $model.kind) {
                Text("All").tag(String?.none)
                ForEach(model.kinds, id: \.self) { kind in
                    Text(Self.plural(kind)).tag(Optional(kind))
                }
            }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(model.kind == nil ? Color.primary : Color.accentColor)
                if let kind = model.kind {
                    Text(Self.plural(kind))
                        .font(.inter(.medium, size: 15, relativeTo: .body))
                        .foregroundStyle(Color.accentColor)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .glassCapsule()
        }
        .menuOrder(.fixed)
        .buttonStyle(.plain)
        .accessibilityLabel("Filter by kind")
    }

    private static func plural(_ kind: String) -> String {
        kind == "datapack" ? "Data packs" : kind.prefix(1).uppercased() + kind.dropFirst() + "s"
    }

    private static func count(_ count: Int, kind: String) -> String {
        let name = plural(kind).lowercased()
        return "\(count) \(count == 1 ? String(name.dropLast()) : name)"
    }
}

private struct AddonRow: View {
    let addon: Addon

    var body: some View {
        HStack(spacing: 12) {
            icon

            VStack(alignment: .leading, spacing: 3) {
                Text(addon.displayName)
                    .font(.inter(.semibold, size: 15, relativeTo: .body))
                    .lineLimit(1)
                if addon.displayName != addon.filename {
                    Text(addon.filename)
                        .font(.inter(.regular, size: 12, relativeTo: .caption))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }

            Spacer(minLength: 8)

            HStack(spacing: 4) {
                if addon.hasUpdate != nil { badge("Update", .accentColor) }
                if addon.fromModpack { badge("Modpack", .secondary) }
                if addon.disabled { badge("Disabled", .secondary) }
            }
        }
        .padding(10)
        .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 14))
        .opacity(addon.disabled ? 0.6 : 1)
    }

    private var icon: some View {
        AsyncImage(url: addon.iconUrl.flatMap(URL.init(string:))) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else {
                Image(systemName: "cube")
                    .font(.system(size: 16))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 40, height: 40)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func badge(_ title: String, _ color: Color) -> some View {
        Text(title)
            .font(.inter(.semibold, size: 11, relativeTo: .caption2))
            .foregroundStyle(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(color.opacity(0.16), in: Capsule())
    }
}
