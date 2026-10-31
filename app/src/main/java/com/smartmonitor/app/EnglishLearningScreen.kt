package com.smartmonitor.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private data class EnglishLesson(val level: String, val title: String, val question: String, val options: List<String>, val answer: Int, val translation: String, val explanation: String)

@Composable
fun EnglishLearningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("english_learning", Context.MODE_PRIVATE) }
    val isFa = LocalConfiguration.current.locales[0].language == "fa"
    fun t(fa: String, en: String) = if (isFa) fa else en
    var dailyGoal by remember { mutableIntStateOf(prefs.getInt("goal", 10)) }
    var completed by remember { mutableIntStateOf(prefs.getInt("completed", 0)) }
    var level by remember { mutableStateOf(prefs.getString("level", "") ?: "") }
    var lessonIndex by remember { mutableIntStateOf(-1) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(0) }
    var assessmentIndex by remember { mutableIntStateOf(-1) }
    var assessmentScore by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") }
    var speechStatus by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown(); recognizer?.destroy() }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (!granted) speechStatus = t("مجوز میکروفون داده نشد.","Microphone permission was not granted.")
    }
    val lessons = listOf(
        EnglishLesson("A1","Greetings","How are you?",listOf("حالت چطوره؟","اسمت چیه؟","کجا می‌ری؟"),0,"حالت چطوره؟","How = چطور؛ are = هستی؛ you = تو"),
        EnglishLesson("A1","Hotel","Did you reserve a room?",listOf("اتاق را تمیز کردی؟","اتاقی رزرو کردی؟","اتاق را ترک کردی؟"),1,"اتاقی رزرو کردی؟","reserve = رزرو کردن؛ room = اتاق"),
        EnglishLesson("A1","Food","I would like a glass of water.",listOf("یک لیوان آب میل دارم.","من آب را دوست دارم.","آب کجاست؟"),0,"یک لیوان آب میل دارم.","would like = میل داشتن"),
        EnglishLesson("A2","Daily routine","She works every day.",listOf("او دیروز کار کرد.","او هر روز کار می‌کند.","او فردا کار خواهد کرد."),1,"او هر روز کار می‌کند.","در زمان حال ساده برای she به فعل معمولاً s اضافه می‌شود."),
        EnglishLesson("A2","Travel","Where is the train station?",listOf("ایستگاه قطار کجاست؟","قطار چه ساعتی می‌رسد؟","بلیط قطار چند است؟"),0,"ایستگاه قطار کجاست؟","Where = کجا؛ train station = ایستگاه قطار"),
        EnglishLesson("B1","Opinion","I have been learning English for two years.",listOf("دو سال است انگلیسی یاد می‌گیرم.","دو سال انگلیسی درس دادم.","دو سال دیگر انگلیسی می‌خوانم."),0,"دو سال است انگلیسی یاد می‌گیرم.","حال کامل استمراری برای کاری که در گذشته شروع شده و ادامه دارد."),
        EnglishLesson("B1","Conditionals","If I had more time, I would travel.",listOf("اگر وقت بیشتری داشتم، سفر می‌کردم.","وقتی وقت دارم سفر می‌کنم.","اگر سفر کنم وقت ندارم."),0,"اگر وقت بیشتری داشتم، سفر می‌کردم.","ساختار شرطی نوع دوم برای موقعیت فرضی.")
    )
    val assessment = listOf(
        Triple("What is your name?", listOf("اسم تو چیست؟","چند سالته؟","کجا زندگی می‌کنی؟"),0),
        Triple("I went to work yesterday.", listOf("فردا سر کار می‌روم.","دیروز به محل کار رفتم.","هر روز کار می‌کنم."),1),
        Triple("She has lived here since 2020.", listOf("او از سال ۲۰۲۰ اینجا زندگی می‌کند.","او سال ۲۰۲۰ اینجا را ترک کرد.","او اینجا زندگی خواهد کرد."),0),
        Triple("If I had known, I would have called.", listOf("اگر می‌دانستم، تماس می‌گرفتم.","اگر بدانم تماس می‌گیرم.","تماس گرفتم و فهمیدم."),0),
        Triple("The proposal was rejected despite its benefits.", listOf("پیشنهاد به دلیل مزایایش پذیرفته شد.","پیشنهاد با وجود مزایایش رد شد.","پیشنهاد هنوز بررسی نشده است."),1)
    )
    val currentLesson = if (lessonIndex in lessons.indices) lessons[lessonIndex] else null

    LaunchedEffect(currentLesson != null) {
        while (currentLesson != null) {
            kotlinx.coroutines.delay(60000)
            completed++
            prefs.edit().putInt("completed", completed).apply()
        }
    }

    fun speak(text: String) {
        tts?.language = Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_lesson")
    }
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speechStatus = t("تشخیص گفتار در این دستگاه در دسترس نیست.","Speech recognition is unavailable on this device.")
            return
        }
        if (!permissionGranted) { micPermission.launch(Manifest.permission.RECORD_AUDIO); return }
        recognizer?.destroy()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; speechStatus = t("گوش می‌دهم؛ جمله را انگلیسی بگو.","Listening—speak in English.") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false; speechStatus = t("گفتار دریافت نشد؛ دوباره تلاش کن.","Speech not captured; please try again.") }
            override fun onResults(results: Bundle?) {
                listening = false
                speechText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                speechStatus = if (speechText.isBlank()) t("متنی تشخیص داده نشد.","No speech was recognized.") else t("متن تشخیص‌داده‌شده: ","Recognized: ")+speechText
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        sr.startListening(intent)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFF8EF)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { if (assessmentIndex >= 0) assessmentIndex = -1 else if (lessonIndex >= 0) lessonIndex = -1 else onBack() }) { Icon(Icons.Default.ArrowBack, t("بازگشت","Back")) }
            Text(t("آموزش زبان انگلیسی","English learning"), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF253C78))
            Box {
                TextButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MenuBook, null); Text(t("منو","Menu")) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(t("تعیین سطح","Placement test")) }, onClick = { assessmentIndex = 0; assessmentScore = 0; menuOpen = false })
                    DropdownMenuItem(text = { Text(t("همه درس‌ها","All lessons")) }, onClick = { category = 0; lessonIndex = -1; menuOpen = false })
                }
            }
        }
        if (assessmentIndex >= 0) {
            if (assessmentIndex < assessment.size) {
                val q = assessment[assessmentIndex]
                Text(t("آزمون تعیین سطح","Placement test")+"  ${assessmentIndex+1}/${assessment.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LinearProgressIndicator(progress = { (assessmentIndex+1f)/assessment.size }, modifier = Modifier.fillMaxWidth())
                Text(q.first, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF24569A))
                q.second.forEachIndexed { i, answer ->
                    Card(onClick = { if (i == q.third) assessmentScore++; assessmentIndex++ }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Text(answer, Modifier.fillMaxWidth().padding(16.dp), fontSize = 17.sp)
                    }
                }
            } else {
                val resultLevel = when { assessmentScore <= 1 -> "A1"; assessmentScore <= 3 -> "A2"; else -> "B1" }
                LaunchedEffect(Unit) { level = resultLevel; prefs.edit().putString("level",resultLevel).apply() }
                Text(t("نتیجه تعیین سطح","Your level"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(resultLevel, fontSize = 44.sp, color = Color(0xFF269C5A), fontWeight = FontWeight.ExtraBold)
                Text(when(resultLevel) { "A1" -> t("مبتدی","Beginner"); "A2" -> t("پایه رو به متوسط","Elementary"); else -> t("متوسط","Intermediate") })
                Text(t("این آزمون کوتاه تخمینی است و جایگزین آزمون استاندارد نیست.","This short quiz is an estimate, not a certified placement test."))
                Button(onClick = { assessmentIndex = -1; lessonIndex = -1 }, modifier = Modifier.fillMaxWidth()) { Text(t("شروع یادگیری","Start learning")) }
            }
        } else if (currentLesson == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF60C96D)), shape = RoundedCornerShape(23.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(t("هر روز کمی انگلیسی، هر روز یک قدم جلوتر!","A little English every day!"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(t("هدف روزانه","Daily goal"), color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(5,10,15,20).forEach { m -> FilterChip(selected = dailyGoal == m, onClick = { dailyGoal=m; prefs.edit().putInt("goal",m).apply() }, label = { Text("$m "+t("دقیقه","min")) }) } }
                    LinearProgressIndicator(progress = { (completed.toFloat()/dailyGoal).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha=.3f))
                    Text(t("$completed از $dailyGoal دقیقه امروز","$completed of $dailyGoal minutes today"), color = Color.White)
                    Text(t("سطح فعلی: ${if(level.isBlank()) "تعیین نشده" else level}","Current level: ${if(level.isBlank()) "Not set" else level}"), color = Color.White)
                }
            }
            Button(onClick = { assessmentIndex = 0; assessmentScore = 0 }, modifier = Modifier.fillMaxWidth()) { Text(t("🎯 آزمون تعیین سطح","🎯 Take placement test")) }
            Text(t("مسیر یادگیری","Learning path"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            listOf("A1","A2","B1").forEachIndexed { idx, lv ->
                val color = listOf(Color(0xFF28B96A),Color(0xFF36A9DD),Color(0xFFAB83E8))[idx]
                Card(onClick = { lessonIndex = when(lv) { "A1" -> 0; "A2" -> 3; else -> 5 } }, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(19.dp)) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lv, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(14.dp))
                        Text(when(lv) { "A1" -> t("مبتدی؛ واژگان و جمله‌های ساده","Beginner · words and simple sentences"); "A2" -> t("پایه؛ مکالمه‌های روزمره","Elementary · everyday conversations"); else -> t("متوسط؛ جمله‌های پیچیده‌تر","Intermediate · more complex sentences") }, color = Color.White, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(currentLesson.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("CEFR ${currentLesson.level}", color = Color(0xFF278B58), fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F4FF)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(currentLesson.question, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFF214F9B))
                    Text(t("ترجمه: ","Translation: ")+currentLesson.translation)
                    OutlinedButton(onClick = { speak(currentLesson.question) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VolumeUp,null); Text(t("شنیدن تلفظ","Listen to pronunciation")) }
                }
            }
            currentLesson.options.forEachIndexed { i, option ->
                Card(onClick = { if (!answered) { selected=i; answered=true } }, colors = CardDefaults.cardColors(containerColor = if(answered && i==currentLesson.answer) Color(0xFFD9F5DF) else if(answered && selected==i) Color(0xFFFFDFDF) else Color.White), shape = RoundedCornerShape(15.dp)) {
                    Text(option, Modifier.fillMaxWidth().padding(16.dp), fontSize = 17.sp)
                }
            }
            if(answered) {
                Text(if(selected==currentLesson.answer) t("آفرین! 🌟","Correct! 🌟") else t("پاسخ درست مشخص شد.","Correct answer highlighted."), fontWeight = FontWeight.Bold)
                Text(currentLesson.explanation)
                Button(onClick = { lessonIndex=(lessonIndex+1).coerceAtMost(lessons.lastIndex); answered=false; selected=-1 }, modifier = Modifier.fillMaxWidth()) { Text(t("درس بعدی","Next lesson")) }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBCB)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("تمرین تلفظ و مکالمه","Pronunciation & speaking practice"), fontWeight = FontWeight.Bold)
                    Text(t("جمله بالا را با صدای بلند بگو و دکمه میکروفون را بزن.","Say the sentence aloud, then tap the microphone."))
                    Button(onClick = { startListening() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Mic,null); Text(if(listening) t("در حال شنیدن…","Listening…") else t("شروع تشخیص گفتار","Start speech recognition")) }
                    if(speechStatus.isNotBlank()) Text(speechStatus)
                    if(speechText.isNotBlank()) {
                        Text(t("جمله هدف: ","Target: ")+currentLesson.question)
                        Text(t("متن تشخیص داده‌شده: ","Recognized: ")+speechText)
                        Text(t("مقایسه ساده متنی؛ ارزیابی دقیق لهجه یا نمره تلفظ انجام نمی‌شود.","Basic text comparison only; accent and pronunciation are not scored."))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.EmojiEvents,null,tint=Color(0xFFFFB52E)); Text(t("تمرین پیوسته مهم‌تر از عجله است.","Consistency matters more than rushing."), style=MaterialTheme.typography.bodySmall) }
    }
}
