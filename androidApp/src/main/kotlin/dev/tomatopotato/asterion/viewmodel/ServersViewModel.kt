package dev.tomatopotato.asterion.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.isPermissionError
import dev.tomatopotato.asterion.net.userMessage
import dev.tomatopotato.asterion.servers.ArchonServer
import dev.tomatopotato.asterion.servers.ServersFacade
import dev.tomatopotato.asterion.service.TokenStore
import kotlinx.coroutines.launch

sealed interface ServersState {
    data object Loading : ServersState
    data class Loaded(val servers: List<ArchonServer>) : ServersState
    data class Failed(val message: String) : ServersState
    data class NotPermitted(val detail: String) : ServersState
}

class ServersViewModel(app: Application) : AndroidViewModel(app) {

    var state: ServersState by mutableStateOf(ServersState.Loading)
        private set

    private val facade = ServersFacade(tokenProvider = { TokenStore(getApplication()).accessToken() })
    private var didLoad = false

    fun load() {
        if (didLoad) return
        didLoad = true
        refresh()
    }

    fun refresh() {
        state = ServersState.Loading
        viewModelScope.launch {
            runCatching { facade.listServers() }
                .onSuccess { state = ServersState.Loaded(it) }
                .onFailure { error ->
                    val apiError = error as? ApiError
                    state = when {
                        apiError != null && apiError.isPermissionError ->
                            ServersState.NotPermitted(apiError.userMessage())
                        apiError != null -> ServersState.Failed(apiError.userMessage())
                        else -> ServersState.Failed(error.message ?: "Something went wrong.")
                    }
                }
        }
    }

    fun server(id: String): ArchonServer? {
        val loaded = state as? ServersState.Loaded ?: return null
        return loaded.servers.firstOrNull { it.serverId == id }
    }
}
