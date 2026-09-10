package dev.tomatopotato.asterion

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface AddAccountState {
    data object Idle : AddAccountState
    data object Authorizing : AddAccountState
    data object Working : AddAccountState
    data class Failed(val message: String) : AddAccountState
}

class AddAccountViewModel(app: Application) : AndroidViewModel(app) {

    var accounts by mutableStateOf<List<StoredAccount>>(emptyList())
        private set

    var state by mutableStateOf<AddAccountState>(AddAccountState.Idle)
        private set

    val isAwaitingCallback: Boolean get() = state == AddAccountState.Authorizing

    private val authClient = ModrinthAuthClient()
    private val userClient = ModrinthUserClient()
    private var expectedState = ""

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            "added_accounts",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    init {
        accounts = readAccounts()
        accounts = listOf(
            StoredAccount("tmp1", "seconduser", "https://avatars.githubusercontent.com/u/9919?v=4", null),
            StoredAccount("tmp2", "thirdaccount", null, null),
        )
    }

    fun contains(id: String): Boolean = accounts.any { it.id == id }
    fun token(id: String): String? = prefs.getString(tokenKey(id), null)

    fun beginAuthorize(): Uri {
        expectedState = AuthConfig.generateState()
        state = AddAccountState.Authorizing
        return Uri.parse(AuthConfig.buildAuthorizeUrl(expectedState))
    }

    fun onCancelled() {
        if (state == AddAccountState.Authorizing) state = AddAccountState.Idle
    }

    fun onVerificationFailed() {
        state = AddAccountState.Failed("Link verification failed — check /.well-known/assetlinks.json")
    }

    fun onCallback(uri: Uri?) {
        if (state != AddAccountState.Authorizing) return
        if (uri == null) {
            state = AddAccountState.Failed("Sign-in returned no data")
            return
        }
        val error = uri.getQueryParameter("error")
        val returnedState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        when {
            error != null -> state = AddAccountState.Failed("Authorization denied: $error")
            returnedState == null || returnedState != expectedState ->
                state = AddAccountState.Failed("Sign-in could not be verified")
            code == null -> state = AddAccountState.Failed("Sign-in returned no code")
            else -> exchange(code)
        }
    }

    fun acknowledge() {
        if (state is AddAccountState.Failed) state = AddAccountState.Idle
    }

    private fun exchange(code: String) {
        state = AddAccountState.Working
        viewModelScope.launch {
            runCatching {
                val token = authClient.exchangeCode(code)
                val user = userClient.fetchCurrentUser(token.accessToken)
                token.accessToken to user
            }.onSuccess { (accessToken, user) ->
                if (knownAccountIDs().contains(user.id)) {
                    state = AddAccountState.Failed("${user.username} is already added.")
                    return@onSuccess
                }
                persist(
                    StoredAccount(user.id, user.username, user.avatarUrl, user.bio),
                    accessToken,
                )
                state = AddAccountState.Idle
            }.onFailure {
                state = AddAccountState.Failed(it.message ?: "Couldn't add account")
            }
        }
    }

    private suspend fun knownAccountIDs(): Set<String> {
        val store = TokenStore(getApplication())
        val ids = accounts.map { it.id }.toMutableSet()
        val activeId = store.userID()
        if (activeId != null) {
            ids.add(activeId)
        } else {
            store.accessToken()?.let { token ->
                val primary = userClient.fetchCurrentUser(token)
                store.saveUserID(primary.id)
                ids.add(primary.id)
            }
        }
        return ids
    }

    private fun persist(account: StoredAccount, token: String) {
        if (contains(account.id)) return
        prefs.edit().putString(tokenKey(account.id), token).apply()
        accounts = accounts + account
        prefs.edit().putString(KEY_LIST, encode(accounts)).apply()
    }

    private fun readAccounts(): List<StoredAccount> {
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                StoredAccount(
                    id = o.getString("id"),
                    username = o.getString("username"),
                    avatarUrl = if (o.isNull("avatarUrl")) null else o.getString("avatarUrl"),
                    bio = if (o.isNull("bio")) null else o.getString("bio"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(list: List<StoredAccount>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("username", a.username)
                    put("avatarUrl", a.avatarUrl ?: JSONObject.NULL)
                    put("bio", a.bio ?: JSONObject.NULL)
                },
            )
        }
        return arr.toString()
    }

    private companion object {
        const val KEY_LIST = "modrinth.addedAccounts"
        fun tokenKey(id: String) = "modrinth.token.$id"
    }
}
