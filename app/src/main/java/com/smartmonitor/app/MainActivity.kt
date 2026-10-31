package com.smartmonitor.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF12336B)
private val Blue = Color(0xFF1769D2)
private val Canvas = Color(0xFFF3F6FF)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartMonitorApp() }
    }
}

private data class ToolItem(val title: Int, val icon: ImageVector, val color: Color)

@Composable
private fun SmartMonitorApp() {
    val configuration = LocalConfiguration.current
    var selected by remember { mutableIntStateOf(0) }
    var address by remember { mutableStateOf("https://eta.ahvaz.ir") }
    var showBrowser by remember { mutableStateOf(false) }
    var showFleet by remember { mutableStateOf(false) }
    var showHealth by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }
    var showLearning by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val tools = listOf(
        ToolItem(R.string.health_title, Icons.Default.Favorite, Color(0xFFE84B62)),
        ToolItem(R.string.music_title, Icons.Default.MusicNote, Color(0xFF18A879)),
        ToolItem(R.string.learning_title, Icons.Default.Translate, Color(0xFF28B96A)),
        ToolItem(R.string.monitor_apps, Icons.Default.PhoneAndroid, Color(0xFFE84B62)),
        ToolItem(R.string.browser, Icons.Default.Public, Color(0xFF168BD5)),
        ToolItem(R.string.analysis, Icons.Default.Insights, Color(0xFF8155D9)),
        ToolItem(R.string.parameters, Icons.Default.Tune, Color(0xFFF29A28)),
        ToolItem(R.string.reports, Icons.Default.Assessment, Color(0xFF079B83)),
        ToolItem(R.string.alerts, Icons.Default.NotificationsActive, Color(0xFFE34F9A)),
        ToolItem(R.string.history, Icons.Default.History, Color(0xFF239DBA)),
        ToolItem(R.string.favorites, Icons.Default.Star, Color(0xFFE8A51C)),
        ToolItem(R.string.settings, Icons.Default.Settings, Color(0xFF64748B))
    )

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Color(0xFF00A98F), background = Canvas)) {
        if (showBrowser) {
            BrowserScreen(
                initialUrl = address,
                onClose = { showBrowser = false }
            )
        } else if (showFleet) {
            BusFleetScreen(onBack = { showFleet = false })
        } else if (showHealth) {
            HealthScreen(onBack = { showHealth = false })
        } else if (showMusic) {
            MusicScreen(onBack = { showMusic = false })
        } else if (showLearning) {
            EnglishLearningScreen(onBack = { showLearning = false })
        } else if (showSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else {
            Scaffold(
                containerColor = Canvas,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(selected = true, onClick = { selected = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.home)) })
                        NavigationBarItem(selected = false, onClick = { selected = R.string.reports }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text(stringResource(R.string.reports)) })
                        NavigationBarItem(selected = false, onClick = { selected = R.string.history }, icon = { Icon(Icons.Default.History, null) }, label = { Text(stringResource(R.string.history)) })
                        NavigationBarItem(selected = showSettings, onClick = { showSettings = true }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.brand_title), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                            Text(stringResource(R.string.brand_subtitle), color = Color.Gray, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            val current = configuration.locales[0].language
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (current == "fa") "en" else "fa"))
                        }) {
                            Icon(Icons.Default.Translate, contentDescription = stringResource(R.string.language))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.language_toggle), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Navy, Blue)), RoundedCornerShape(22.dp)).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.welcome_title), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(stringResource(R.string.welcome_body), color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Surface(color = Color.White.copy(alpha = .18f), shape = RoundedCornerShape(50)) {
                                Text("●  ${stringResource(R.string.ready)}", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(58.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.services), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tools) { item ->
                            ToolCard(item) {
                                if (item.title == R.string.health_title) showHealth = true
                                else if (item.title == R.string.music_title) showMusic = true
                                else if (item.title == R.string.learning_title) showLearning = true
                                else if (item.title == R.string.browser) showBrowser = true
                                else if (item.title == R.string.monitor_apps) showFleet = true
                                else if (item.title == R.string.settings) showSettings = true
                                else selected = item.title
                            }
                        }
                    }
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.quick_watch), color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(9.dp))
                            OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(stringResource(R.string.url_hint)) })
                            Spacer(Modifier.height(9.dp))
                            Button(
                                onClick = { showBrowser = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Icon(Icons.Default.Public, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.start))
                            }
                        }
                    }
                    Text(stringResource(R.string.prototype_note), color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 7.dp))
                }
            }
            if (selected != 0) {
                AlertDialog(
                    onDismissRequest = { selected = 0 },
                    confirmButton = { TextButton(onClick = { selected = 0 }) { Text("OK") } },
                    title = { Text(stringResource(selected)) },
                    text = { Text(stringResource(R.string.prototype_note)) }
                )
            }
        }
    }
}

@Composable
private fun ToolCard(item: ToolItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(116.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(54.dp).background(Brush.linearGradient(listOf(item.color.copy(alpha = .75f), item.color)), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(stringResource(item.title), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 2)
        }
    }
}
