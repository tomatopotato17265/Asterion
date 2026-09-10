package dev.tomatopotato.asterion

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class AccountViewModel(app: Application) : AndroidViewModel(app) {

    var username by mutableStateOf<String?>(null)
        private set

    var bio by mutableStateOf<String?>(null)
        private set

    var avatar by mutableStateOf<ImageBitmap?>(null)
        private set

    private val client = ModrinthUserClient()
    private var loaded = false

    fun load() {
        if (loaded) return
        val token = TokenStore(getApplication()).accessToken() ?: return
        loaded = true
        viewModelScope.launch {
            runCatching {
                val user = client.fetchCurrentUser(token)
                TokenStore(getApplication()).saveUserID(user.id)
                username = user.username
                bio = user.bio
                user.avatarUrl?.let { url ->
                    withContext(Dispatchers.IO) {
                        val bytes = URL(url).openStream().use { it.readBytes() }
                        decodeSampled(bytes, targetPx = 96)?.asImageBitmap()
                    }
                }
            }.onSuccess { bitmap -> if (bitmap != null) avatar = bitmap }
        }
    }

    fun reset() {
        username = null
        bio = null
        avatar = null
        loaded = false
    }

    private fun decodeSampled(bytes: ByteArray, targetPx: Int): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / (sample * 2) >= targetPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
