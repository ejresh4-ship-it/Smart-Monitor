package com.smartmonitor.app

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SetBg = Color(0xFF080C28)
private val SetMuted = Color(0xFFB2B6D8)
private fun prefs(c: Context) = c.getSharedPreferences("smart_monitor_preferences", Context.MODE_PRIVATE)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = LocalContext.current
    var theme by remember { mutableStateOf(prefs(c).getString("appearance", "dark") ?: "dark") }
    var font by remember { mutableFloatStateOf(prefs(c).getFloat("font_scale", 1f)) }
    var animations by remember { mutableStateOf(prefs(c).getBoolean("animations", true)) }
    var battery by remember { mutableStateOf(prefs(c).getBoolean("battery_saver", false)) }
    var haptics by remember { mutableStateOf(prefs(c).getBoolean("vibration", true)) }
    var bgMusic by remember { mutableStateOf(prefs(c).getBoolean("background_music", true)) }
    var autoplay by remember { mutableStateOf(prefs(c).getBoolean("music_autoplay", false)) }
    var alerts by remember { mutableStateOf(prefs(c).getBoolean("notifications", true)) }
    var language by remember { mutableStateOf(prefs(c).getString("app_language", "fa") ?: "fa") }
    var reset by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = SetBg,
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "بازگشت", tint = Color.White) }
                Text("تنظیمات", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Settings, null, tint = Color(0xFFB78AFF), modifier = Modifier.size(30.dp))
            }
        }
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            AnimatedVisibility(true, enter = fadeIn() + slideInVertically { it / 4 }) {
                Row(Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF7026D9), Color(0xFF164DB8), Color(0xFF111A52))), RoundedCornerShape(24.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(66.dp).background(Color.White.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tune, null, tint = Color(0xFF70DCFF), modifier = Modifier.size(38.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("شخصی‌سازی Smart Monitor", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("ظاهر و عملکرد برنامه را مطابق سلیقه‌ات تنظیم کن", color = Color(0xFFD8DCFF), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("ظاهر و تم", Icons.Default.Palette)
            Panel {
                Text("حالت نمایش", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeTile("تیره نئونی", Icons.Default.DarkMode, theme == "dark", Modifier.weight(1f)) { theme = "dark"; prefs(c).edit().putString("appearance", theme).apply() }
                    ThemeTile("روشن", Icons.Default.LightMode, theme == "light", Modifier.weight(1f)) { theme = "light"; prefs(c).edit().putString("appearance", theme).apply() }
                    ThemeTile("سیستم", Icons.Default.Brightness6, theme == "system", Modifier.weight(1f)) { theme = "system"; prefs(c).edit().putString("appearance", theme).apply() }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اندازه نوشته‌ها", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text((font * 100).toInt().toString() + "٪", color = Color(0xFF78DFFF), fontWeight = FontWeight.Bold)
                }
                Slider(value = font, onValueChange = { font = it }, onValueChangeFinished = { prefs(c).edit().putFloat("font_scale", font).apply() }, valueRange = .85f..1.25f, colors = SliderDefaults.colors(thumbColor = Color(0xFFB78AFF), activeTrackColor = Color(0xFF8A39FF)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("کوچک", color = SetMuted, fontSize = 10.sp); Text("معمولی", color = SetMuted, fontSize = 10.sp); Text("بزرگ", color = SetMuted, fontSize = 10.sp) }
                Text("پیش‌نمایش نوشته", color = SetMuted, fontSize = 11.sp)
                Text("سلام! این اندازه نوشته در برنامه است.", color = Color.White, fontSize = (14 * font).sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("رفتار و عملکرد", Icons.Default.Tune)
            Panel {
                ToggleRow("انیمیشن‌های نرم", "حرکت‌های ظریف بین صفحات", Icons.Default.AutoAwesome, animations) { animations = it; prefs(c).edit().putBoolean("animations", it).apply() }
                ToggleRow("صرفه‌جویی باتری", "کاهش جلوه‌های سنگین", Icons.Default.BatterySaver, battery) { battery = it; prefs(c).edit().putBoolean("battery_saver", it).apply() }
                ToggleRow("لرزش هنگام لمس", "بازخورد لمسی دکمه‌ها", Icons.Default.Vibration, haptics) { haptics = it; prefs(c).edit().putBoolean("vibration", it).apply() }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("موسیقی و اعلان‌ها", Icons.Default.Notifications)
            Panel {
                ToggleRow("پخش در پس‌زمینه", "ادامه پخش هنگام خروج از صفحه موسیقی", Icons.Default.MusicNote, bgMusic) { bgMusic = it; prefs(c).edit().putBoolean("background_music", it).apply() }
                ToggleRow("پخش خودکار آهنگ بعدی", "پس از پایان آهنگ، مورد بعدی پخش شود", Icons.Default.QueueMusic, autoplay) { autoplay = it; prefs(c).edit().putBoolean("music_autoplay", it).apply() }
                ToggleRow("اعلان‌ها", "یادآوری‌ها و پیام‌های برنامه", Icons.Default.NotificationsActive, alerts) { alerts = it; prefs(c).edit().putBoolean("notifications", it).apply() }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("زبان و حریم خصوصی", Icons.Default.Language)
            Panel {
                Text("زبان برنامه", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LanguageTile("فارسی", language == "fa", Modifier.weight(1f)) { language = "fa"; prefs(c).edit().putString("app_language", language).apply() }
                    LanguageTile("English", language == "en", Modifier.weight(1f)) { language = "en"; prefs(c).edit().putString("app_language", language).apply() }
                }
                Spacer(Modifier.height(14.dp))
                ToggleInfo("حریم خصوصی", "مدیریت داده‌ها و مجوزهای برنامه", Icons.Default.Security)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9AAA))) {
                Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("بازگردانی تنظیمات پیش‌فرض")
            }
            Spacer(Modifier.height(12.dp))
            Text("Smart Monitor • نسخه 1.0.0", color = SetMuted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("بازگردانی تنظیمات؟") }, text = { Text("گزینه‌های شخصی‌سازی به حالت پیش‌فرض برمی‌گردند.") }, confirmButton = {
        TextButton(onClick = {
            prefs(c).edit().clear().apply()
            theme = "dark"; font = 1f; animations = true; battery = false; haptics = true; bgMusic = true; autoplay = false; alerts = true; language = "fa"; reset = false
        }) { Text("بازگردانی") }
    }, dismissButton = { TextButton(onClick = { reset = false }) { Text("انصراف") } })
}

@Composable private fun SectionLabel(t: String, i: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(i, null, tint = Color(0xFFB98BFF), modifier = Modifier.size(22.dp)); Spacer(Modifier.width(9.dp)); Text(t, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(9.dp, RoundedCornerShape(21.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1C2358), Color(0xFF11183F))), RoundedCornerShape(21.dp)).padding(15.dp), content = content)
}
@Composable private fun ThemeTile(t: String, i: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    Column(modifier.background(if (selected) Brush.linearGradient(listOf(Color(0xFF7C2DFF), Color(0xFF145FEA))) else Brush.linearGradient(listOf(Color(0xFF2A326A), Color(0xFF1B214E))), RoundedCornerShape(14.dp)).clickable(onClick = click).padding(vertical = 12.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(i, null, tint = Color.White, modifier = Modifier.size(23.dp)); Spacer(Modifier.height(6.dp)); Text(t, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        if (selected) Text("انتخاب‌شده", color = Color(0xFFB9F4FF), fontSize = 8.sp)
    }
}
@Composable private fun ToggleRow(t: String, s: String, i: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(Color(0xFF392A80), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(i, null, tint = Color(0xFF83DFFF), modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(t, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(s, color = SetMuted, fontSize = 9.sp) }
        Switch(checked = checked, onCheckedChange = change)
    }
}
@Composable private fun LanguageTile(t: String, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = click), color = if (selected) Color(0xFF4B269E) else Color(0xFF252B5C), shape = RoundedCornerShape(13.dp), border = if (selected) BorderStroke(1.dp, Color(0xFFB28AFF)) else null) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { if (selected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF75E0FF), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text(t, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}
@Composable private fun ToggleInfo(t: String, s: String, i: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(i, null, tint = Color(0xFF67D6FF)); Spacer(Modifier.width(10.dp)); Column { Text(t, color = Color.White, fontWeight = FontWeight.SemiBold); Text(s, color = SetMuted, fontSize = 10.sp) }; Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronLeft, null, tint = SetMuted) }
}
