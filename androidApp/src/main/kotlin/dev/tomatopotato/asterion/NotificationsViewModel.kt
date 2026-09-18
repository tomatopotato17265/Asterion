package dev.tomatopotato.asterion

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.net.isPermissionError
import dev.tomatopotato.asterion.net.userMessage
import dev.tomatopotato.asterion.notifications.ModrinthNotification
import dev.tomatopotato.asterion.notifications.NotificationsFacade
import kotlinx.coroutines.launch

sealed interface InboxState {
    data object Loading : InboxState
    data class Loaded(val notifications: List<ModrinthNotification>) : InboxState
    data class Failed(val message: String) : InboxState
    data class NotPermitted(val detail: String) : InboxState
}

class NotificationsViewModel(app: Application) : AndroidViewModel(app) {

    var state: InboxState by mutableStateOf(InboxState.Loading)
        private set

    private val facade = NotificationsFacade(
        tokenProvider = { TokenStore(getApplication()).accessToken() },
        userIdProvider = { TokenStore(getApplication()).userID() },
    )
    private var didLoad = false

    fun load() {
        if (didLoad) return
        didLoad = true
        refresh()
    }

    fun refresh() {
        state = InboxState.Loading
        viewModelScope.launch {
            runCatching { facade.listNotifications().filter { !it.read } }
                .onSuccess { state = InboxState.Loaded(it) }
                .onFailure { error ->
                    val apiError = error as? ApiError
                    state = when {
                        apiError != null && apiError.isPermissionError ->
                            InboxState.NotPermitted(apiError.userMessage())
                        apiError != null -> InboxState.Failed(apiError.userMessage())
                        else -> InboxState.Failed(error.message ?: "Something went wrong.")
                    }
                }
        }
    }

    fun markRead(notification: ModrinthNotification) {
        val loaded = state as? InboxState.Loaded ?: return
        state = InboxState.Loaded(loaded.notifications.filter { it.id != notification.id })
        viewModelScope.launch {
            runCatching { facade.markRead(notification.id) }
                .onFailure { state = loaded }
        }
    }

    fun reset() {
        state = InboxState.Loading
        didLoad = false
    }
}
