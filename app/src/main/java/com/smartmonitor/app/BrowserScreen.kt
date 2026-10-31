package com.smartmonitor.app

import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@Composable
internal fun BrowserScreen(
    initialUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf(initialUrl) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isTrustedMonitorSite by remember { mutableStateOf(false) }

    fun openAddress(rawAddress: String) {
        val uri = normalizeHttpsUrl(rawAddress)
        if (uri == null) {
            Toast.makeText(context, R.string.browser_https_only, Toast.LENGTH_LONG).show()
            return
        }
        address = uri.toString()
        webView?.loadUrl(uri.toString())
    }

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_browser))
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.url_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { openAddress(address) })
            )
            Spacer(Modifier.width(6.dp))
            FilledIconButton(onClick = { openAddress(address) }) {
                Icon(Icons.Default.TravelExplore, contentDescription = stringResource(R.string.go))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = webView?.canGoBack() == true,
                    onClick = { webView?.goBack() }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.browser_back))
                }
                IconButton(
                    enabled = webView?.canGoForward() == true,
                    onClick = { webView?.goForward() }
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.browser_forward))
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.browser_refresh))
                }
            }
            Surface(
                color = if (isTrustedMonitorSite) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(
                            if (isTrustedMonitorSite) R.string.trusted_monitor_site
                            else R.string.standard_web_page
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    webView = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = false
                        allowContentAccess = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            return !isHttpsWebUri(request.url)
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                            isTrustedMonitorSite = isTrustedMonitorUrl(url)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            address = url
                            isLoading = false
                            isTrustedMonitorSite = isTrustedMonitorUrl(url)
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            isLoading = newProgress < 100
                        }
                    }
                    val startUri = normalizeHttpsUrl(initialUrl)
                        ?: Uri.parse("https://eta.ahvaz.ir")
                    loadUrl(startUri.toString())
                }
            }
        )
    }
}

private fun normalizeHttpsUrl(rawAddress: String): Uri? {
    val trimmed = rawAddress.trim()
    if (trimmed.isEmpty()) return null
    val candidate = if ("://" in trimmed) trimmed else "https://$trimmed"
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    return uri.takeIf { isHttpsWebUri(it) }
}

private fun isHttpsWebUri(uri: Uri): Boolean {
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

private fun isTrustedMonitorUrl(rawUrl: String): Boolean {
    val host = runCatching { Uri.parse(rawUrl).host?.lowercase(Locale.ROOT) }.getOrNull()
    return host == "eta.ahvaz.ir"
}
