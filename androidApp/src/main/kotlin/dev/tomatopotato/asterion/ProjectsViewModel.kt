package dev.tomatopotato.asterion

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.projects.ModrinthProject
import dev.tomatopotato.asterion.projects.ProjectsFacade
import dev.tomatopotato.asterion.projects.projectsMessage
import kotlinx.coroutines.launch

sealed interface ProjectsState {
    data object Loading : ProjectsState
    data class Loaded(val projects: List<ModrinthProject>) : ProjectsState
    data class Failed(val message: String) : ProjectsState
}

class ProjectsViewModel(app: Application) : AndroidViewModel(app) {

    var state: ProjectsState by mutableStateOf(ProjectsState.Loading)
        private set

    private val facade = ProjectsFacade(
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
        if (state !is ProjectsState.Loaded) state = ProjectsState.Loading
        viewModelScope.launch {
            runCatching { facade.listProjects() }
                .onSuccess { state = ProjectsState.Loaded(it) }
                .onFailure { error ->
                    state = ProjectsState.Failed(
                        (error as? ApiError)?.projectsMessage() ?: error.message ?: "Something went wrong.",
                    )
                }
        }
    }

    fun reset() {
        state = ProjectsState.Loading
        didLoad = false
    }
}
