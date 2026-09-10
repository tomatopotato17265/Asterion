package dev.tomatopotato.asterion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.tomatopotato.asterion.ui.AsterionTheme
import dev.tomatopotato.asterion.ui.LoginScreen
import dev.tomatopotato.asterion.ui.MainTabScreen

class MainActivity : ComponentActivity() {
    private val auth: AuthViewModel by viewModels()
    private val account: AccountViewModel by viewModels()
    private val addAccount: AddAccountViewModel by viewModels()
    private val authTabLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> auth.onCallback(result.resultUri)
                AuthTabIntent.RESULT_CANCELED -> auth.onCancelled()
                AuthTabIntent.RESULT_VERIFICATION_FAILED,
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> auth.onVerificationFailed()
            }
        }
    private val addAccountTabLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_OK -> addAccount.onCallback(result.resultUri)
                AuthTabIntent.RESULT_CANCELED -> addAccount.onCancelled()
                AuthTabIntent.RESULT_VERIFICATION_FAILED,
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> addAccount.onVerificationFailed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleRedirect(intent)

        setContent {
            AsterionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val phase = auth.phase) {
                        AuthPhase.SignedIn -> MainTabScreen(
                            account = account,
                            addAccount = addAccount,
                            onSignOut = {
                                account.reset()
                                auth.signOut()
                            },
                            onAddAccount = ::startAddAccount,
                        )

                        AuthPhase.Authorizing, AuthPhase.Exchanging -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }

                        else -> MainTabScreen(
                            account = account,
                            addAccount = addAccount,
                            onSignOut = { account.reset(); auth.signOut() },
                            onAddAccount = ::startAddAccount,
                        )
                    }
                }
            }
        }
    }

    private fun startLogin() {
        val authorizeUrl = auth.beginAuthorize()
        val authTab = AuthTabIntent.Builder().build()
        if (AuthConfig.USE_VERIFIED_HTTPS_CALLBACK) {
            authTab.launch(authTabLauncher, authorizeUrl, AuthConfig.CALLBACK_HOST, AuthConfig.CALLBACK_PATH)
        } else {
            authTab.launch(authTabLauncher, authorizeUrl, AuthConfig.CALLBACK_SCHEME)
        }
    }

    private fun startAddAccount() {
        val authorizeUrl = addAccount.beginAuthorize()
        val authTab = AuthTabIntent.Builder().build()
        if (AuthConfig.USE_VERIFIED_HTTPS_CALLBACK) {
            authTab.launch(addAccountTabLauncher, authorizeUrl, AuthConfig.CALLBACK_HOST, AuthConfig.CALLBACK_PATH)
        } else {
            authTab.launch(addAccountTabLauncher, authorizeUrl, AuthConfig.CALLBACK_SCHEME)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        val data: Uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        if (addAccount.isAwaitingCallback) addAccount.onCallback(data) else auth.onCallback(data)
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    AsterionTheme { LoginScreen() }
}
