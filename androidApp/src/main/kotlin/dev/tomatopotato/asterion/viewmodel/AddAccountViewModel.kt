package dev.tomatopotato.asterion.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.tomatopotato.asterion.ModrinthUserClient
import dev.tomatopotato.asterion.auth.LauncherAuth
import dev.tomatopotato.asterion.model.StoredAccount
import dev.tomatopotato.asterion.service.TokenStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface AddAccountState {
    data object Idle : AddAccountState
    data object SigningIn : AddAccountState
    data object Working : AddAccountState
    data class Failed(val message: String) : AddAccountState
}

class AddAccountViewModel(app: Application) : AndroidViewModel(app) {

    var accounts by mutableStateOf<List<StoredAccount>>(emptyList())
        private set

    var state by mutableStateOf<AddAccountState>(AddAccountState.Idle)
        private set

    private val userClient = ModrinthUserClient()

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

    fun beginSignIn() {
        state = AddAccountState.SigningIn
    }

    fun onCancelled() {
        if (state == AddAccountState.SigningIn) state = AddAccountState.Idle
    }

    fun acknowledge() {
        if (state is AddAccountState.Failed) state = AddAccountState.Idle
    }

    fun onToken(token: String) {
        if (state != AddAccountState.SigningIn) return
        state = AddAccountState.Working
        viewModelScope.launch {
            runCatching {
                val session = LauncherAuth.verify(token)
                session.token to session.user
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
