import Shared
import SwiftUI

@MainActor
@Observable
final class ServerContentModel {

    enum State {
        case loading
        case loaded(AddonsResponse)
        case failed(String)
    }

    private(set) var state: State = .loading
    var kind: String?
    var query = ""

    private let serverId: String
    private let facade: ServersFacade
    private var didLoad = false

    init(serverId: String, facade: ServersFacade) {
        self.serverId = serverId
        self.facade = facade
    }

    var response: AddonsResponse? {
        if case let .loaded(response) = state { return response }
        return nil
    }

    var kinds: [String] {
        response.map { ServerContentListing.shared.kinds(addons: $0.addons) } ?? []
    }

    var visibleAddons: [Addon] {
        guard let response else { return [] }
        return ServerContentListing.shared.filter(addons: response.addons, kind: kind, query: query)
    }

    func load() async {
        guard !didLoad else { return }
        didLoad = true
        await refresh()
    }

    func retry() async {
        state = .loading
        await refresh()
    }

    func refresh() async {
        do {
            state = .loaded(try await facade.getInstalledContent(serverId: serverId))
        } catch {
            if response == nil { state = .failed(ServersStore.message(for: error)) }
        }
    }
}
