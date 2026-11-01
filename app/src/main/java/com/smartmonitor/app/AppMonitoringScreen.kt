package com.smartmonitor.app

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

private data class MonitoredApp(val packageName: String, val label: String, val minutes: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMonitoringScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(hasUsageAccess(context)) }
    var apps by remember { mutableStateOf(emptyList<MonitoredApp>()) }
    var selected by remember { mutableStateOf<MonitoredApp?>(null) }
    val prefs = remember { context.getSharedPreferences("app_monitor_limits", Context.MODE_PRIVATE) }
    fun reload() {
        hasAccess = hasUsageAccess(context)
        if (!hasAccess) return
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val usage = manager.queryAndAggregateUsageStats(start, System.currentTimeMillis())
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = context.packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        apps = launchableApps.mapNotNull { resolveInfo ->
            val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
            val pkg = appInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val minutes = (usage[pkg]?.totalTimeInForeground ?: 0L) / 60000L
            MonitoredApp(pkg, context.packageManager.getApplicationLabel(appInfo).toString(), minutes)
        }.distinctBy { it.packageName }.sortedByDescending { it.minutes }
    }
    LaunchedEffect(Unit) { reload() }
    Scaffold(topBar = { TopAppBar(title = { Text("مانیتورینگ برنامه‌ها", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }, actions = { IconButton(onClick = { reload() }) { Icon(Icons.Default.Refresh, contentDescription = "به‌روزرسانی") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1FF))) { Column(Modifier.padding(16.dp)) { Text("پایش مصرف واقعی برنامه‌ها", color = Color(0xFF12336B), fontWeight = FontWeight.Bold); Text("زمان حضور هر برنامه در پیش‌زمینه از آمار استفاده اندروید خوانده می‌شود.", color = Color.DarkGray) } }
            Spacer(Modifier.height(12.dp))
            if (!hasAccess) {
                Card { Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("دسترسی Usage Access فعال نیست", fontWeight = FontWeight.Bold)
                    Text("برای مشاهده زمان واقعی استفاده، دسترسی Usage access را برای Smart Monitor فعال کن.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("رفتن به تنظیمات دسترسی") }
                    OutlinedButton(onClick = { reload() }, modifier = Modifier.fillMaxWidth()) { Text("بررسی دوباره دسترسی") }
                } }
            } else {
                val monitoredApps = apps.filter { prefs.getBoolean("enabled_" + it.packageName, true) }
                Text("امروز · " + monitoredApps.size + " برنامه تحت نظارت · " + formatDuration(monitoredApps.sumOf { it.minutes }), fontWeight = FontWeight.Bold, color = Color(0xFF12336B))
                Text("برنامه‌های قابل اجرا هم نمایش داده می‌شوند؛ اگر امروز استفاده نشده باشند زمان آن‌ها صفر است.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                if (apps.isEmpty()) Text("برنامه قابل اجرایی پیدا نشد.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        val limit = prefs.getInt("limit_" + app.packageName, 0)
                        val enabled = prefs.getBoolean("enabled_" + app.packageName, true)
                        Card { Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = Color(0xFF1769D2), modifier = Modifier.size(34.dp)); Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.Bold); Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
                                Text(formatDuration(app.minutes), fontWeight = FontWeight.Bold, color = Color(0xFF1769D2))
                            }
                            Spacer(Modifier.height(8.dp))
                            if (limit > 0) { LinearProgressIndicator(progress = (app.minutes.toFloat() / limit).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth()); Text("سقف روزانه: " + limit + " دقیقه" + if (app.minutes >= limit) " · سقف رد شده" else "", style = MaterialTheme.typography.labelSmall) } else Text("سقف روزانه تعیین نشده", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (enabled) "در محاسبه مجموع لحاظ شود" else "از محاسبه مجموع کنار گذاشته شود", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Switch(checked = enabled, onCheckedChange = { prefs.edit().putBoolean("enabled_" + app.packageName, it).apply() }); TextButton(onClick = { selected = app }) { Text("تنظیم سقف") } }
                        } }
                    }
                }
            }
        }
    }
    selected?.let { app ->
        var value by remember(app.packageName) { mutableStateOf(prefs.getInt("limit_" + app.packageName, 60).toString()) }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("سقف استفاده · " + app.label) }, text = { OutlinedTextField(value = value, onValueChange = { value = it.filter(Char::isDigit).take(4) }, label = { Text("دقیقه در روز") }, singleLine = true) }, confirmButton = { TextButton(onClick = { prefs.edit().putInt("limit_" + app.packageName, value.toIntOrNull() ?: 0).apply(); selected = null }) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("انصراف") } })
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
}
private fun formatDuration(minutes: Long): String = if (minutes < 60) minutes.toString() + " دقیقه" else (minutes / 60).toString() + " ساعت و " + (minutes % 60) + " دقیقه"
