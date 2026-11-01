package com.smartmonitor.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private data class MonitorTargetApp(val label: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalMonitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("https://eta.ahvaz.ir") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var latestText by remember { mutableStateOf("") }
    var previousText by remember { mutableStateOf("") }
    var monitoring by remember { mutableStateOf(false) }
    var appList by remember { mutableStateOf(emptyList<MonitorTargetApp>()) }
    var showApps by remember { mutableStateOf(false) }
    var targetApp by remember { mutableStateOf<MonitorTargetApp?>(null) }

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        appList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val ai = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (ai.packageName == context.packageName) null
                else MonitorTargetApp(context.packageManager.getApplicationLabel(ai).toString(), ai.packageName)
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    LaunchedEffect(monitoring, selectedText) {
        while (monitoring && selectedText.isNotBlank()) {
            webView?.evaluateJavascript(
                "(function(){try{return (document.querySelector('[data-sm-monitor=\"selected\"]')?.innerText||'').trim()}catch(e){return ''}})()"
            ) { raw ->
                val value = raw?.trim('"')?.replace("\\n", "\n")?.takeIf { it.isNotBlank() } ?: ""
                if (value.isNotBlank()) {
                    if (latestText.isNotBlank() && value != latestText) previousText = latestText
                    latestText = value
                }
            }
            delay(5000)
        }
    }

    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پایشگر هوشمند صفحه", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت") } },
                actions = { IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, contentDescription = "بارگذاری مجدد") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("۱. سایت را باز کن", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("آدرس سایت") })
                    Button(onClick = {
                        val candidate = if (url.startsWith("https://") || url.startsWith("http://")) url else "https://$url"
                        if (Uri.parse(candidate).host != null) {
                            url = candidate
                            webView?.loadUrl(candidate)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("باز کردن سایت") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showApps = true }, modifier = Modifier.weight(1f)) { Text("انتخاب برنامه گوشی") }
                Button(onClick = {
                    selecting = true
                    webView?.evaluateJavascript(
                        """(function(){
                          document.getElementById('sm-pick-overlay')?.remove();
                          const o=document.createElement('div');o.id='sm-pick-overlay';
                          o.style='position:fixed;inset:0;z-index:2147483647;background:rgba(0,180,255,.08);border:3px dashed #00d9ff;display:flex;align-items:center;justify-content:center;color:white;font:bold 18px sans-serif;text-shadow:0 1px 5px #000;touch-action:pan-y';
                          o.innerHTML='<div style="background:#07152ddd;padding:14px;border-radius:12px">روی بخش موردنظر بزن</div>';
                          o.onclick=function(e){e.preventDefault();const el=document.elementFromPoint(e.clientX,e.clientY);if(!el||el===o)return;
                          document.querySelectorAll('[data-sm-monitor]').forEach(x=>x.removeAttribute('data-sm-monitor'));
                          el.setAttribute('data-sm-monitor','selected');el.style.outline='3px solid #00e5ff';o.remove();};
                          document.body.appendChild(o);
                        })()"""
                    ) { selecting = false }
                }, modifier = Modifier.weight(1f), enabled = pageReady) { Icon(Icons.Default.SelectAll, null); Spacer(Modifier.width(5.dp)); Text("انتخاب محدوده") }
            }
            Spacer(Modifier.height(8.dp))
            Card(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, loadedUrl: String) {
                                    pageReady = true
                                    url = loadedUrl
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    update = { webView = it }
                )
            }
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("۲. وضعیت پایش", fontWeight = FontWeight.Bold)
                    Text(if (selectedText.isBlank()) "ابتدا «انتخاب محدوده» را بزن و روی متن یا عدد موردنظر در صفحه لمس کن." else "محدوده انتخاب شده: $selectedText")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { monitoring = !monitoring }, enabled = selectedText.isNotBlank()) {
                            Icon(Icons.Default.PlayArrow, null); Text(if (monitoring) "توقف پایش" else "شروع پایش")
                        }
                        OutlinedButton(onClick = {
                            webView?.evaluateJavascript("(document.querySelector('[data-sm-monitor=\\\"selected\\\"]')?.innerText||'').trim()") {
                                val value = it?.trim('"')?.replace("\\n", "\n").orEmpty()
                                if (value.isNotBlank()) { selectedText = value; latestText = value }
                            }
                        }, enabled = pageReady) { Text("خواندن مقدار") }
                    }
                    if (latestText.isNotBlank()) Text("آخرین مقدار: $latestText")
                    if (previousText.isNotBlank() && previousText != latestText) Text("تغییر تشخیص داده شد؛ مقدار قبلی: $previousText", color = MaterialTheme.colorScheme.error)
                    Text("بررسی خودکار هر ۵ ثانیه هنگام باز بودن این صفحه انجام می‌شود.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (showApps) {
        AlertDialog(
            onDismissRequest = { showApps = false },
            title = { Text("برنامه موردنظر را انتخاب کن") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(appList) { app ->
                        TextButton(onClick = {
                            targetApp = app
                            showApps = false
                            val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launch != null) context.startActivity(launch)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(app.label, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showApps = false }) { Text("بستن") } }
        )
    }
    targetApp?.let { app ->
        AlertDialog(
            onDismissRequest = { targetApp = null },
            title = { Text("برنامه باز شد: ${app.label}") },
            text = { Text("برای پایش محدوده داخل برنامه‌های دیگر، باید قابلیت تصویربرداری صفحه (MediaProjection) و اجازه صریح اندروید اضافه شود. در این نسخه انتخاب برنامه و بازکردن آن فعال است؛ پایش خودکار محدوده فعلاً در صفحات وب داخل Smart Monitor کار می‌کند.") },
            confirmButton = { TextButton(onClick = { targetApp = null }) { Text("متوجه شدم") } }
        )
    }
}
