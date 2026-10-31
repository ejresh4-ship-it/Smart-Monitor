package com.smartmonitor.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EnglishLesson(val category: String, val title: String, val question: String, val options: List<String>, val answer: Int, val translation: String, val explanation: String)

@Composable
fun EnglishLearningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("english_learning", Context.MODE_PRIVATE) }
    val isFa = LocalConfiguration.current.locales[0].language == "fa"
    fun t(fa: String, en: String) = if (isFa) fa else en
    var dailyGoal by remember { mutableIntStateOf(prefs.getInt("goal", 10)) }
    var completed by remember { mutableIntStateOf(prefs.getInt("completed", 0)) }
    var lessonIndex by remember { mutableIntStateOf(-1) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(0) }
    val lessons = listOf(
        EnglishLesson("Basics","Greetings","How are you?",listOf("حالت چطوره؟","اسمت چیه؟","کجا می‌ری؟"),0,"حالت چطوره؟","How = چطور، are = هستی، you = تو"),
        EnglishLesson("Travel","Hotel","Did you reserve a room?",listOf("اتاق را تمیز کردی؟","اتاقی رزرو کردی؟","اتاق را ترک کردی؟"),1,"اتاقی رزرو کردی؟","reserve = رزرو کردن؛ room = اتاق"),
        EnglishLesson("Daily life","Food","I would like a glass of water.",listOf("یک لیوان آب میل دارم.","من آب را دوست دارم.","آب کجاست؟"),0,"یک لیوان آب میل دارم.","would like = میل داشتن؛ a glass of = یک لیوان"),
        EnglishLesson("Grammar","Present simple","She works every day.",listOf("او دیروز کار کرد.","او هر روز کار می‌کند.","او فردا کار خواهد کرد."),1,"او هر روز کار می‌کند.","برای he / she / it در زمان حال ساده معمولاً به فعل s اضافه می‌شود."),
        EnglishLesson("Travel","Airport","Where is the train station?",listOf("ایستگاه قطار کجاست؟","قطار چه ساعتی می‌رسد؟","بلیط قطار چند است؟"),0,"ایستگاه قطار کجاست؟","Where = کجا؛ train station = ایستگاه قطار")
    )
    val filtered = when(category) { 1 -> lessons.filter { it.category == "Travel" }; 2 -> lessons.filter { it.category == "Daily life" }; 3 -> lessons.filter { it.category == "Grammar" }; else -> lessons }
    val lesson = if (lessonIndex >= 0 && lessonIndex < filtered.size) filtered[lessonIndex] else null

    Column(Modifier.fillMaxSize().background(Color(0xFFFFF9F0)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { if (lessonIndex >= 0) { lessonIndex = -1; selected = -1; answered = false } else onBack() }) { Icon(Icons.Default.ArrowBack, t("بازگشت","Back")) }
            Text(t("آموزش زبان انگلیسی","English learning"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF253C78))
            Box {
                TextButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MenuBook, null); Text(t("منو","Menu")) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    listOf(t("همه درس‌ها","All lessons"),t("سفر و گردش","Travel"),t("زندگی روزمره","Daily life"),t("گرامر","Grammar")).forEachIndexed { i, label ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { category = i; lessonIndex = -1; menuOpen = false })
                    }
                }
            }
        }
        if (lesson == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF63C96B)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("هر روز کمی انگلیسی، هر روز یک قدم جلوتر!","A little English every day!"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(t("هدف روزانه‌ات را انتخاب کن","Choose your daily learning goal"), color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5,10,15,20).forEach { m -> FilterChip(selected = dailyGoal == m, onClick = { dailyGoal = m; prefs.edit().putInt("goal",m).apply() }, label = { Text("$m "+t("دقیقه","min")) }) }
                    }
                    LinearProgressIndicator(progress = { (completed.toFloat()/dailyGoal).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha=.35f))
                    Text(t("$completed از $dailyGoal دقیقه امروز","$completed of $dailyGoal minutes today"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text(t("مسیر یادگیری","Learning path"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF253C78))
            listOf(t("شروع و احوال‌پرسی","Getting started"),t("سفر و هتل","Travel & hotel"),t("زندگی روزمره","Daily life"),t("گرامر کاربردی","Useful grammar")).forEachIndexed { i, label ->
                val colors = listOf(Color(0xFF28B96A),Color(0xFF38A9DD),Color(0xFFFFB547),Color(0xFFAB83E8))
                Card(onClick = { category = i; lessonIndex = 0; selected = -1; answered = false }, colors = CardDefaults.cardColors(containerColor = colors[i]), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha=.25f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.MenuBook, null, tint = Color.White, modifier = Modifier.padding(12.dp).size(28.dp)) }
                        Spacer(Modifier.width(12.dp)); Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f)); Text("➜", color = Color.White, fontSize = 22.sp)
                    }
                }
            }
            Text(t("درس‌ها کوتاه هستند و ترجمه فارسی هر سؤال نمایش داده می‌شود.","Short lessons with Persian translations for every question."), color = Color.Gray)
        } else {
            Text(lesson.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F4FF)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("جمله انگلیسی را بخوان و معنی درست را انتخاب کن","Read the English sentence and choose its meaning"), fontWeight = FontWeight.SemiBold)
                    Text(lesson.question, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFF214F9B))
                    Text(t("ترجمه: ","Translation: ")+lesson.translation, color = Color(0xFF52647D))
                }
            }
            lesson.options.forEachIndexed { i, option ->
                val bg = if (answered && i == lesson.answer) Color(0xFFD9F5DF) else if (answered && selected == i) Color(0xFFFFE0E0) else Color.White
                Card(onClick = { if (!answered) { selected = i; answered = true; if (i == lesson.answer) { completed += 2; prefs.edit().putInt("completed",completed).apply() } } }, colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(16.dp)) {
                    Text(option, Modifier.fillMaxWidth().padding(17.dp), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (answered) {
                Text(if (selected == lesson.answer) t("آفرین! پاسخ درست بود 🌟","Great! Correct answer 🌟") else t("اشکالی ندارد؛ پاسخ درست مشخص شده است.","That's okay—the correct answer is highlighted."), fontWeight = FontWeight.Bold)
                Text(lesson.explanation)
                Button(onClick = { lessonIndex = (lessonIndex + 1) % filtered.size; selected = -1; answered = false }, modifier = Modifier.fillMaxWidth()) { Text(t("درس بعدی","Next lesson")) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFB52E))
            Text(t("تمرین پیوسته از حفظ کردن عجولانه مهم‌تر است.","Consistency matters more than rushing."), style = MaterialTheme.typography.bodySmall)
        }
    }
}
