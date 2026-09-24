package idont.trust.atrust.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.SslErrorHandler
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import idont.trust.atrust.model.ConnectionProfile
import java.net.URI
import idont.trust.atrust.logging.Logger

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SsoLoginScreen(
    loginUrl: String,
    profile: ConnectionProfile,
    onCallback: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val resolvedLoginUrl = remember(loginUrl, profile.server, profile.port, profile.loginDomain) {
        resolveLoginUrl(loginUrl, profile)
    }
    LaunchedEffect(resolvedLoginUrl) {
        Logger.i("SSO", "Opening embedded login page; url=$resolvedLoginUrl")
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember(resolvedLoginUrl) { mutableStateOf(resolvedLoginUrl) }
    var completed by remember(resolvedLoginUrl) { mutableStateOf(false) }
    var progress by remember(resolvedLoginUrl) { mutableStateOf(0) }
    var pageError by remember(resolvedLoginUrl) { mutableStateOf<String?>(null) }

    fun complete(url: String) {
        if (!completed) {
            completed = true
            Logger.i("SSO", "Captured SSO callback; url=$url")
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
        if (progress in 0..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        pageError?.let { message ->
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
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
                    settings.setSupportMultipleWindows(false)
                    val loginWebView = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(loginWebView, true)
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val uri = request.url
                            val knownCallbackPath = uri.path == "/passport/v1/auth/cas" ||
                                uri.path == "/passport/v1/auth/httpsOauth2"
                            if (isServerUrl(uri, profile) && (request.isRedirect || knownCallbackPath)) {
                                Logger.d("SSO", "Intercepting redirect before navigation; redirect=${request.isRedirect}, path=${uri.path}")
                                complete(uri.toString())
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            Logger.d("SSO", "Page started; url=$url")
                            currentUrl = url
                            pageError = null
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            url?.let { currentUrl = it }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                Logger.e("SSO", "Main frame load failed; code=${error.errorCode}, url=${request.url}, description=${error.description}")
                                pageError = "页面加载失败：${error.description}（${error.errorCode}）"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse,
                        ) {
                            if (request.isForMainFrame) {
                                Logger.e("SSO", "HTTP error ${errorResponse.statusCode}; url=${request.url}")
                                pageError = "服务器返回 HTTP ${errorResponse.statusCode} ${errorResponse.reasonPhrase.orEmpty()}"
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView,
                            handler: SslErrorHandler,
                            error: SslError,
                        ) {
                            handler.cancel()
                            Logger.e("SSO", "TLS validation failed; code=${error.primaryError}, url=${error.url}")
                            pageError = "TLS 证书校验失败（${error.primaryError}），为保护账号安全已停止加载"
                        }
                    }
                    if (resolvedLoginUrl.isBlank()) {
                        Logger.e("SSO", "Resolved login URL is empty")
                        pageError = "服务器未提供有效的 SSO 登录地址"
                    } else {
                        loadUrl(resolvedLoginUrl)
                    }
                }
            },
            onRelease = { releasedWebView ->
                Logger.d("SSO", "Releasing embedded WebView after it left composition")
                releasedWebView.stopLoading()
                releasedWebView.webChromeClient = null
                releasedWebView.webViewClient = WebViewClient()
                releasedWebView.removeAllViews()
                releasedWebView.destroy()
                if (webView === releasedWebView) webView = null
            },
        )
    }
}

private fun resolveLoginUrl(loginUrl: String, profile: ConnectionProfile): String {
    val host = profile.server.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .substringBefore(':')
    if (host.isBlank()) return ""
    val authority = if (profile.port == 443) host else "$host:${profile.port}"
    val base = "https://$authority/"
    if (loginUrl.isBlank()) {
        return Uri.parse(base).buildUpon()
            .encodedPath("/passport/v1/public/casLogin")
            .appendQueryParameter("sfDomain", profile.loginDomain)
            .build()
            .toString()
    }
    return runCatching { URI(base).resolve(loginUrl.trim()).toString() }
        .getOrElse { loginUrl.trim() }
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
