package dev.tomatopotato.asterion.servers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import java.io.ByteArrayOutputStream
import java.util.EnumSet

suspend fun loadServerIcon(host: String, username: String, password: String): Bitmap? =
    runCatching {
        withSftpSession(host, username, password) { sftp ->
            ServerIconPaths.candidates.firstNotNullOfOrNull { path -> readIcon(sftp, path) }
        }
    }.getOrNull()

private fun readIcon(sftp: SFTPClient, path: String): Bitmap? {
    val bytes = runCatching {
        sftp.open(path, EnumSet.of(OpenMode.READ)).use { file ->
            val out = ByteArrayOutputStream()
            file.RemoteFileInputStream().use { it.copyTo(out) }
            out.toByteArray()
        }
    }.getOrNull() ?: return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
