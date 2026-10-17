package dev.tomatopotato.asterion.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.tomatopotato.asterion.auth.ModrinthSessionPolicy

class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "auth",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(session: String, userId: String) {
        prefs.edit()
            .putString(KEY_TOKEN, session)
            .putLong(KEY_EXPIRES_AT, ModrinthSessionPolicy.expiresAt(nowSeconds()))
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun saveRefreshed(session: String) {
        prefs.edit()
            .putString(KEY_TOKEN, session)
            .putLong(KEY_EXPIRES_AT, ModrinthSessionPolicy.expiresAt(nowSeconds()))
            .apply()
    }
    
    fun accessToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun needsRefresh(): Boolean {
        if (accessToken() == null) return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return expiresAt == 0L || ModrinthSessionPolicy.needsRefresh(expiresAt, nowSeconds())
    }

    fun saveUserID(id: String) {
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }

    fun userID(): String? = prefs.getString(KEY_USER_ID, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val KEY_TOKEN = "modrinth.accessToken"
        const val KEY_EXPIRES_AT = "modrinth.expiresAt"
        const val KEY_USER_ID = "modrinth.userId"
    }
}
