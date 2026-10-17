package dev.tomatopotato.asterion.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tomatopotato.asterion.auth.LauncherAuth
import dev.tomatopotato.asterion.auth.ModrinthSessionClient
import dev.tomatopotato.asterion.net.ApiError
import dev.tomatopotato.asterion.service.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface AuthPhase {
    data object SignedOut : AuthPhase
    data object SigningIn : AuthPhase
    data object Verifying : AuthPhase
    data object SignedIn : AuthPhase
    data class Failed(val message: String) : AuthPhase
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TokenStore(app)

    var phase by mutableStateOf<AuthPhase>(AuthPhase.SignedOut)
        private set

    private var isRefreshing = false

    init {
        if (store.accessToken() != null) phase = AuthPhase.SignedIn
    }

    fun beginSignIn() {
        phase = AuthPhase.SigningIn
    }

    fun onCancelled() {
        if (phase == AuthPhase.SigningIn) phase = AuthPhase.SignedOut
    }

    fun onToken(token: String) {
        if (phase != AuthPhase.SigningIn) return
        phase = AuthPhase.Verifying
        viewModelScope.launch {
            try {
                val session = LauncherAuth.verify(token)
                store.save(session.token, session.user.id)
                phase = AuthPhase.SignedIn
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                phase = AuthPhase.Failed(e.message ?: "Couldn't sign in")
            }
        }
    }

    // sessions last two weeks, so renew one that is close to expiring. rejected renewal == signed out == no bueno
    fun refreshSessionIfNeeded() {
        if (phase != AuthPhase.SignedIn || isRefreshing || !store.needsRefresh()) return
        val current = store.accessToken() ?: return
        isRefreshing = true
        viewModelScope.launch {
            try {
                store.saveRefreshed(ModrinthSessionClient().refresh(current))
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError.Unauthorized) {
                signOut()
            } catch (_: Throwable) {
                // a network blip shouldn't sign anyone out. try again next time the app opens.
            } finally {
                isRefreshing = false
            }
        }
    }

    fun signOut() {
        store.clear()
        phase = AuthPhase.SignedOut
    }
}
