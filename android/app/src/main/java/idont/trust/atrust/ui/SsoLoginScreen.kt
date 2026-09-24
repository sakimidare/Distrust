package idont.trust.atrust.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import idont.trust.atrust.model.ConnectionProfile

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SsoLoginScreen(
    loginUrl: String,
    profile: ConnectionProfile,
    onCallback: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember(loginUrl) { mutableStateOf(loginUrl) }
    var completed by remember(loginUrl) { mutableStateOf(false) }

    fun complete(url: String) {
        if (!completed) {
            completed = true
            webView?.stopLoading()
            onCallback(url)
        }
    }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onCancel()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "网页后退")
            }
            IconButton(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "网页前进")
            }
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, "刷新")
            }
            Text(
                currentUrl,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val uri = request.url
                            val knownCallbackPath = uri.path == "/passport/v1/auth/cas" ||
                                uri.path == "/passport/v1/auth/httpsOauth2"
                            if (isServerUrl(uri, profile) && (request.isRedirect || knownCallbackPath)) {
                                complete(uri.toString())
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            currentUrl = url
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            url?.let { currentUrl = it }
                        }
                    }
                    loadUrl(loginUrl)
                }
            },
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.run {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
}

private fun isServerUrl(uri: Uri, profile: ConnectionProfile): Boolean {
    val expectedScheme = "https"
    val actualPort = if (uri.port == -1) {
        if (uri.scheme.equals("https", ignoreCase = true)) 443 else 80
    } else {
        uri.port
    }
    return uri.scheme.equals(expectedScheme, ignoreCase = true) &&
        uri.host.equals(profile.server, ignoreCase = true) &&
        actualPort == profile.port
}
