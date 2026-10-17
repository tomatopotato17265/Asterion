package dev.tomatopotato.asterion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.tomatopotato.asterion.ui.AsterionTheme
import dev.tomatopotato.asterion.ui.LoginScreen
import dev.tomatopotato.asterion.ui.MainTabScreen
import dev.tomatopotato.asterion.ui.ModrinthSignInScreen
import dev.tomatopotato.asterion.viewmodel.AccountViewModel
import dev.tomatopotato.asterion.viewmodel.AddAccountState
import dev.tomatopotato.asterion.viewmodel.AddAccountViewModel
import dev.tomatopotato.asterion.viewmodel.AuthPhase
import dev.tomatopotato.asterion.viewmodel.AuthViewModel
import dev.tomatopotato.asterion.viewmodel.NotificationsViewModel
import dev.tomatopotato.asterion.viewmodel.ProjectsViewModel
import dev.tomatopotato.asterion.viewmodel.ServersViewModel
import dev.tomatopotato.asterion.viewmodel.TabsViewModel

class MainActivity : ComponentActivity() {
    private val auth: AuthViewModel by viewModels()
    private val account: AccountViewModel by viewModels()
    private val addAccount: AddAccountViewModel by viewModels()
    private val servers: ServersViewModel by viewModels()
    private val tabs: TabsViewModel by viewModels()
    private val inbox: NotificationsViewModel by viewModels()
    private val projects: ProjectsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) auth.refreshSessionIfNeeded()
            },
        )

        setContent {
            AsterionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val phase = auth.phase) {
                            AuthPhase.SignedIn -> MainTabScreen(
                                account = account,
                                addAccount = addAccount,
                                servers = servers,
                                tabs = tabs,
                                inbox = inbox,
                                projects = projects,
                                onSignOut = {
                                    account.reset()
                                    inbox.reset()
                                    projects.reset()
                                    auth.signOut()
                                },
                                onAddAccount = addAccount::beginSignIn,
                            )

                            AuthPhase.Verifying -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            AuthPhase.SignedOut, AuthPhase.SigningIn -> LoginScreen(
                                onSignInClick = auth::beginSignIn,
                            )

                            is AuthPhase.Failed -> LoginScreen(
                                onSignInClick = auth::beginSignIn,
                                errorMessage = phase.message,
                            )
                        }

                        if (auth.phase == AuthPhase.SigningIn) {
                            ModrinthSignInScreen(onToken = auth::onToken, onCancel = auth::onCancelled)
                        } else if (addAccount.state == AddAccountState.SigningIn) {
                            ModrinthSignInScreen(onToken = addAccount::onToken, onCancel = addAccount::onCancelled)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    AsterionTheme { LoginScreen() }
}
