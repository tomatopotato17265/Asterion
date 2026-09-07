package dev.tomatopotato.asterion

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface AuthPhase {
    data object SignedOut : AuthPhase
    data object Authorizing : AuthPhase
    data object Exchanging : AuthPhase
    data object SignedIn : AuthPhase
    data class Failed(val message: String) : AuthPhase
}

class AuthViewModel(
    app: Application,
    private val handle: SavedStateHandle,
) : AndroidViewModel(app) {

    var phase by mutableStateOf<AuthPhase>(
        if (TokenStore(app).accessToken() != null) AuthPhase.SignedIn else AuthPhase.SignedOut
    )
        private set

    private val auth = ModrinthAuthClient()

    private var expectedState: String
        get() = handle["oauth_state"] ?: ""
        set(value) {
            handle["oauth_state"] = value
        }

    fun beginAuthorize(): Uri {
        val state = AuthConfig.generateState()
        expectedState = state
        phase = AuthPhase.Authorizing
        return Uri.parse(AuthConfig.buildAuthorizeUrl(state))
    }

    fun onCancelled() {
        if (phase == AuthPhase.Authorizing) phase = AuthPhase.SignedOut
    }

    fun onVerificationFailed() {
        phase = AuthPhase.Failed("Link verification failed — check /.well-known/assetlinks.json")
    }

    fun onCallback(uri: Uri?) {
        if (phase is AuthPhase.SignedIn) return
        if (uri == null) {
            phase = AuthPhase.Failed("Sign-in returned no data")
            return
        }
        val error = uri.getQueryParameter("error")
        val returnedState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        when {
            error != null -> phase = AuthPhase.Failed("Authorization denied: $error")
            returnedState == null || returnedState != expectedState ->
                phase = AuthPhase.Failed("Sign-in could not be verified")
            code == null -> phase = AuthPhase.Failed("Sign-in returned no code")
            else -> exchange(code)
        }
    }

    private fun exchange(code: String) {
        phase = AuthPhase.Exchanging
        viewModelScope.launch {
            runCatching { auth.exchangeCode(code) }
                .onSuccess { token ->
                    TokenStore(getApplication()).save(token.accessToken, token.expiresIn)
                    phase = AuthPhase.SignedIn
                }
                .onFailure { phase = AuthPhase.Failed(it.message ?: "Token exchange failed") }
        }
    }

    fun signOut() {
        TokenStore(getApplication()).clear()
        phase = AuthPhase.SignedOut
    }
}
