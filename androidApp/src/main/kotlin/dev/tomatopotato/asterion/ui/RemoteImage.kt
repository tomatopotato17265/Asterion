package dev.tomatopotato.asterion.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun rememberRemoteImage(url: String?): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = url?.let { u ->
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = URL(u).openStream().use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return state.value
}
