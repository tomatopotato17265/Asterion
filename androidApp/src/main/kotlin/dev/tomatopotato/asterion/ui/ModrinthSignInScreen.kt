package dev.tomatopotato.asterion.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.tomatopotato.asterion.auth.LauncherAuth
import dev.tomatopotato.asterion.auth.LauncherSealKey

private const val LOAD_FAILED = "Couldn't load the Modrinth sign-in page. Check your connection and try again."
private const val HANDOFF_FAILED = "Modrinth signed you in, but Asterion couldn't finish. Please try again."

private const val HIDE_PASSKEY_SCRIPT = """
(function () {
  if (!/(^|\.)modrinth\.com$/.test(location.hostname)) { return; }
  if (window.__asterionHidePasskey) { return; }
  window.__asterionHidePasskey = true;
  function hide() {
    document.querySelectorAll('button, a').forEach(function (el) {
      if (/continue with passkey/i.test(el.textContent || '')) { el.style.display = 'none'; }
    });
  }
  new MutationObserver(hide).observe(document.documentElement, { childList: true, subtree: true });
  hide();
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ModrinthSignInScreen(
    onToken: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnToken by rememberUpdatedState(onToken)
    var host by remember { mutableStateOf("modrinth.com") }
    var progress by remember { mutableFloatStateOf(0f) }
    var failure by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val delivered = remember { booleanArrayOf(false) }
    // A fresh key pair and nonce per attempt; Modrinth seals the app session to them.
    var sealKey by remember { mutableStateOf(LauncherSealKey()) }

    fun load(view: WebView) {
        sealKey = LauncherSealKey()
        view.loadUrl(sealKey.signInUrl)
    }

    fun deliver(token: String) {
        if (delivered[0]) return
        delivered[0] = true
        latestOnToken(token)
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onCancel()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Text(
                    text = host,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                // balances the Cancel button so the host stays centered.
                Spacer(Modifier.width(72.dp))
            }

            if (progress in 0.01f..0.99f) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        CookieManager.getInstance().removeAllCookies(null)
                        WebStorage.getInstance().deleteAllData()

                        WebView(context).apply {
                            // pages that size themselves at height: 100% collapse to nothing if the webview believes its height is wrap content
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportMultipleWindows(false)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress / 100f
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                /** Handles the `modrinth://auth` hand-off. Returns whether [url] was one. */
                                fun catchHandoff(view: WebView, url: String?): Boolean {
                                    val handoff = url?.let(LauncherAuth::parseHandoff) ?: return false
                                    view.stopLoading()
                                    val token = sealKey.open(handoff)
                                    if (token != null) deliver(token) else failure = HANDOFF_FAILED
                                    return true
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    if (catchHandoff(view, request.url.toString())) return true
                                    // keep the page inside this view. never hand custom schemes to other apps
                                    return request.url.scheme != "http" && request.url.scheme != "https"
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    failure = null
                                    url?.let { Uri.parse(it).host }?.let { host = it }
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    if (url?.let { Uri.parse(it).host } != "modrinth.com") return
                                    view.evaluateJavascript(HIDE_PASSKEY_SCRIPT, null)
                                    view.evaluateJavascript(LauncherAuth.FOLLOW_HANDOFF_SCRIPT, null)
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame) failure = LOAD_FAILED
                                }
                            }
                            load(this)
                            webView = this
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                    },
                )

                failure?.let { message ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            failure = null
                            webView?.let(::load)
                        }) { Text("Try again") }
                    }
                }
            }
        }
    }
}
