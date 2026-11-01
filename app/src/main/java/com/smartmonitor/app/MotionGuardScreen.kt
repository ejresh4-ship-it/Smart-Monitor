package com.smartmonitor.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionGuardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("motion_guard", Context.MODE_PRIVATE) }
    var left by remember { mutableFloatStateOf(prefs.getFloat("left", .15f)) }
    var right by remember { mutableFloatStateOf(prefs.getFloat("right", .85f)) }
    var top by remember { mutableFloatStateOf(prefs.getFloat("top", .20f)) }
    var bottom by remember { mutableFloatStateOf(prefs.getFloat("bottom", .80f)) }
    var sensitivity by remember { mutableFloatStateOf(prefs.getInt("sensitivity", 12).toFloat()) }
    var active by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("برای شروع، محدوده و حساسیت را تنظیم کن.") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            prefs.edit().putFloat("left", left).putFloat("right", right).putFloat("top", top).putFloat("bottom", bottom).putInt("sensitivity", sensitivity.toInt()).apply()
            val svc = Intent(context, MotionGuardService::class.java).putExtra(MotionGuardService.EXTRA_RESULT, result.resultCode).putExtra(MotionGuardService.EXTRA_DATA, result.data)
            ContextCompat.startForegroundService(context, svc); active = true; status = "نگهبان فعال است."
        } else status = "مجوز ضبط صفحه داده نشد."
    }
    BackHandler { onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text("نگهبان حرکت", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("محدوده نگهبانی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("محدوده را با چهار کنترل زیر روی صفحه تنظیم کن. برنامه فریم‌های صفحه را روی خود گوشی مقایسه می‌کند.")
            Spacer(Modifier.height(12.dp))
            Text("لبه چپ: ${(left * 100).toInt()}٪"); Slider(left, { left = it.coerceAtMost(right - .05f) }, valueRange = 0f..0.9f)
            Text("لبه راست: ${(right * 100).toInt()}٪"); Slider(right, { right = it.coerceAtLeast(left + .05f) }, valueRange = .1f..1f)
            Text("لبه بالا: ${(top * 100).toInt()}٪"); Slider(top, { top = it.coerceAtMost(bottom - .05f) }, valueRange = 0f..0.9f)
            Text("لبه پایین: ${(bottom * 100).toInt()}٪"); Slider(bottom, { bottom = it.coerceAtLeast(top + .05f) }, valueRange = .1f..1f)
            Spacer(Modifier.height(8.dp))
            Text("حساسیت: ${sensitivity.toInt()}٪"); Slider(sensitivity, { sensitivity = it }, valueRange = 3f..40f)
            Text("عدد کمتر یعنی هشدار حساس‌تر؛ عدد بیشتر یعنی فقط تغییرات واضح.")
            Spacer(Modifier.height(14.dp))
            Button(onClick = { val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; launcher.launch(mgr.createScreenCaptureIntent()) }, enabled = !active, modifier = Modifier.fillMaxWidth()) { Text("شروع نگهبانی و اجازه ضبط صفحه") }
            OutlinedButton(onClick = { context.startService(Intent(context, MotionGuardService::class.java).setAction(MotionGuardService.ACTION_STOP)); active = false; status = "نگهبانی متوقف شد." }, enabled = active, modifier = Modifier.fillMaxWidth()) { Text("توقف نگهبانی") }
            Spacer(Modifier.height(8.dp)); Text(status, color = MaterialTheme.colorScheme.primary)
            Text("تصویر ذخیره یا ارسال نمی‌شود. بعضی صفحه‌های امن اندروید ممکن است ضبط نشوند.", style = MaterialTheme.typography.bodySmall)
        }
    }
}