package com.smartmonitor.app

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import org.json.JSONArray
import org.json.JSONObject

private data class LanguageLearner(val id:String,val name:String,val purpose:String,val minutes:Int,val unlocked:Int)
private data class LanguageQuestion(val sentence:String,val translation:String,val choices:List<String>,val choiceTranslations:List<String>,val correct:Int,val tip:String)

private val languageCourses = listOf(
    "شروع آشنایی" to listOf(
        LanguageQuestion("How are you?","حالت چطور است؟",listOf("How are you?","Where are you?","Who are you?","How old are you?"),listOf("حالت چطور است؟","کجا هستی؟","تو چه کسی هستی؟","چند سال داری؟"),0,"How = چگونه؛ are = هستی؛ you = تو"),
        LanguageQuestion("My name is Sara.","اسم من سارا است.",listOf("I am hungry.","My name is Sara.","This is my bag.","She is Sara."),listOf("من گرسنه‌ام.","اسم من سارا است.","این کیف من است.","او سارا است."),1,"My name is برای معرفی نام استفاده می‌شود."),
        LanguageQuestion("Nice to meet you.","از آشنایی با تو خوشحالم.",listOf("See you tomorrow.","Good night.","Nice to meet you.","Thank you very much."),listOf("فردا می‌بینمت.","شب بخیر.","از آشنایی با تو خوشحالم.","خیلی متشکرم."),2,"این عبارت هنگام آشنایی استفاده می‌شود.")
    ),
    "زندگی روزمره" to listOf(
        LanguageQuestion("I drink coffee every morning.","من هر روز صبح قهوه می‌نوشم.",listOf("I make coffee at night.","I drink coffee every morning.","I don't like coffee.","Coffee is on the table."),listOf("شب‌ها قهوه درست می‌کنم.","من هر روز صبح قهوه می‌نوشم.","قهوه دوست ندارم.","قهوه روی میز است."),1,"برای عادت‌های روزانه از حال ساده استفاده می‌کنیم."),
        LanguageQuestion("Where is the nearest bank?","نزدیک‌ترین بانک کجاست؟",listOf("Where is the nearest bank?","When does the bank close?","I work at a bank.","The bank is new."),listOf("نزدیک‌ترین بانک کجاست؟","بانک چه زمانی بسته می‌شود؟","من در بانک کار می‌کنم.","بانک جدید است."),0,"Where is یعنی کجاست؛ nearest یعنی نزدیک‌ترین."),
        LanguageQuestion("Could you help me, please?","ممکن است لطفاً به من کمک کنید؟",listOf("Can I pay here?","Could you help me, please?","I can do it alone.","Please wait outside."),listOf("می‌توانم اینجا پرداخت کنم؟","ممکن است لطفاً به من کمک کنید؟","می‌توانم تنهایی انجامش دهم.","لطفاً بیرون منتظر بمانید."),1,"Could you یک درخواست مؤدبانه است.")
    ),
    "سفر و مکالمه" to listOf(
        LanguageQuestion("I'd like to book a room.","می‌خواهم یک اتاق رزرو کنم.",listOf("I'd like to book a room.","I cleaned my room.","Where is my room?","The room is too small."),listOf("می‌خواهم یک اتاق رزرو کنم.","اتاقم را تمیز کردم.","اتاق من کجاست؟","اتاق خیلی کوچک است."),0,"I'd like to یعنی مایلم؛ book یعنی رزرو کردن."),
        LanguageQuestion("What time does the train leave?","قطار چه ساعتی حرکت می‌کند؟",listOf("Where is the train?","What time does the train leave?","How much is the ticket?","The train is late."),listOf("قطار کجاست؟","قطار چه ساعتی حرکت می‌کند؟","قیمت بلیت چقدر است؟","قطار تأخیر دارد."),1,"What time یعنی چه ساعتی؛ leave یعنی حرکت کردن."),
        LanguageQuestion("I need to change my reservation.","باید رزرو خودم را تغییر بدهم.",listOf("I lost my passport.","I need to change my reservation.","I want to cancel the trip.","I booked two tickets."),listOf("گذرنامه‌ام را گم کرده‌ام.","باید رزرو خودم را تغییر بدهم.","می‌خواهم سفر را لغو کنم.","دو بلیت رزرو کردم."),1,"change یعنی تغییر دادن؛ reservation یعنی رزرو.")
    ),
    "جمله‌سازی" to listOf(
        LanguageQuestion("She has lived here for five years.","او پنج سال است که اینجا زندگی می‌کند.",listOf("She will live here.","She has lived here for five years.","She moved here yesterday.","She lives here for a week."),listOf("او اینجا زندگی خواهد کرد.","او پنج سال است که اینجا زندگی می‌کند.","او دیروز به اینجا نقل مکان کرد.","او یک هفته اینجا زندگی می‌کند."),1,"حال کامل برای کاری که از گذشته شروع شده و ادامه دارد."),
        LanguageQuestion("If I had more time, I would travel.","اگر وقت بیشتری داشتم، سفر می‌کردم.",listOf("I travel every week.","If I have time, I travel.","If I had more time, I would travel.","I travelled last year."),listOf("هر هفته سفر می‌کنم.","اگر وقت داشته باشم سفر می‌کنم.","اگر وقت بیشتری داشتم، سفر می‌کردم.","سال گذشته سفر کردم."),2,"ساختار شرطی نوع دوم برای حالت فرضی است."),
        LanguageQuestion("The meeting has been postponed.","جلسه به تعویق افتاده است.",listOf("The meeting has been postponed.","The meeting starts now.","The meeting was cancelled forever.","The meeting is important."),listOf("جلسه به تعویق افتاده است.","جلسه اکنون شروع می‌شود.","جلسه برای همیشه لغو شد.","جلسه مهم است."),0,"has been postponed ساختار مجهول حال کامل است.")
    ),
    "پیشرفته‌تر" to listOf(
        LanguageQuestion("Despite the rain, we continued walking.","با وجود باران، به پیاده‌روی ادامه دادیم.",listOf("Because it rained, we stayed home.","Despite the rain, we continued walking.","The rain stopped suddenly.","We walked before the rain."),listOf("چون باران آمد، در خانه ماندیم.","با وجود باران، به پیاده‌روی ادامه دادیم.","باران ناگهان متوقف شد.","قبل از باران پیاده‌روی کردیم."),1,"Despite به معنی با وجودِ است."),
        LanguageQuestion("I wish I had studied harder.","ای کاش بیشتر درس خوانده بودم.",listOf("I will study tomorrow.","I wish I had studied harder.","I enjoy studying now.","I studied for two hours."),listOf("فردا درس خواهم خواند.","ای کاش بیشتر درس خوانده بودم.","اکنون از درس خواندن لذت می‌برم.","دو ساعت درس خواندم."),1,"I wish + past perfect برای حسرت درباره گذشته است."),
        LanguageQuestion("The results are likely to improve.","احتمالاً نتایج بهتر خواهند شد.",listOf("The results got worse.","The results are likely to improve.","The results are already perfect.","Nobody checked the results."),listOf("نتایج بدتر شدند.","احتمالاً نتایج بهتر خواهند شد.","نتایج از قبل بی‌نقص هستند.","هیچ‌کس نتایج را بررسی نکرد."),1,"be likely to یعنی احتمال دارد.")
    )
)

@Composable
fun EnglishLearningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("english_learning_profiles", Context.MODE_PRIVATE) }
    var learners by remember { mutableStateOf(readLanguageLearners(prefs)) }
    var activeId by remember { mutableStateOf(prefs.getString("active_id","") ?: "") }
    var page by remember { mutableStateOf("home") }
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(15) }
    var wizardStep by remember { mutableIntStateOf(0) }
    var stage by remember { mutableIntStateOf(0) }
    var question by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var passed by remember { mutableStateOf(false) }
    var questionTranslation by remember { mutableStateOf(false) }
    var optionTranslations by remember { mutableStateOf(setOf<Int>()) }
    var showProfiles by remember { mutableStateOf(false) }
    val learner = learners.firstOrNull { it.id == activeId }
    val ink = Color(0xFF20264A)
    val violet = Color(0xFF6957D8)
    val pale = Color(0xFFF5F3FF)

    fun saveProfiles(list:List<LanguageLearner>, active:String = activeId) {
        learners = list
        activeId = active
        prefs.edit().putString("profiles", JSONArray().apply {
            list.forEach { item -> put(JSONObject().put("id",item.id).put("name",item.name).put("purpose",item.purpose).put("minutes",item.minutes).put("unlocked",item.unlocked)) }
        }.toString()).putString("active_id",active).apply()
    }
    fun openStage(index:Int) {
        stage=index; question=0; selected=-1; answered=false; score=0; passed=false
        questionTranslation=false; optionTranslations=emptySet(); page="lesson"
    }
    fun advance() {
        if (question < 2) {
            question++; selected=-1; answered=false; questionTranslation=false; optionTranslations=emptySet()
        } else {
            passed = score >= 2
            val current = learner
            if (passed && current != null) saveProfiles(learners.map { if(it.id==current.id) it.copy(unlocked=maxOf(it.unlocked,(stage+1).coerceAtMost(languageCourses.lastIndex))) else it })
            page="result"
        }
    }

    Column(Modifier.fillMaxSize().background(pale).verticalScroll(rememberScrollState()).padding(17.dp), verticalArrangement=Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
            IconButton(onClick={
                when(page) { "lesson","result" -> page="stages"; "wizard" -> if(wizardStep>0) wizardStep-- else page="home"; "stages" -> page="home"; else -> onBack() }
            }) { Icon(Icons.Default.ArrowBack,"بازگشت",tint=ink) }
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("LANGUAGE STUDIO",color=violet,fontSize=10.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=2.sp)
                Text("آموزش زبان انگلیسی",color=ink,fontSize=20.sp,fontWeight=FontWeight.ExtraBold)
            }
            Icon(Icons.Default.AutoAwesome,null,tint=Color(0xFFFFB84D),modifier=Modifier.size(27.dp))
        }
        when(page) {
            "home" -> {
                Box(Modifier.fillMaxWidth().shadow(7.dp,RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF252B66),Color(0xFF806AE8))),RoundedCornerShape(26.dp)).padding(21.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("هر روز یک قدم به انگلیسی روان‌تر نزدیک شو ✨",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text("مسیر یادگیری مخصوص خودت را بساز یا ادامه بده.",color=Color.White.copy(alpha=.82f),fontSize=13.sp)
                        Text("✦ تمرین مرحله‌ای  •  ترجمه هوشمند  •  ذخیره پیشرفت",color=Color.White,fontSize=12.sp)
                    }
                }
                Text("شروع یادگیری",color=ink,fontSize=18.sp,fontWeight=FontWeight.Bold)
                Card(onClick={name="";purpose="";minutes=15;wizardStep=0;page="wizard"},shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(3.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(Color(0xFFEDEAFF),CircleShape),contentAlignment=Alignment.Center) { Icon(Icons.Default.Add,null,tint=violet,modifier=Modifier.size(29.dp)) }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) { Text("ساخت مسیر جدید",color=ink,fontWeight=FontWeight.Bold,fontSize=17.sp); Text("نام، هدف و زمان روزانه‌ات را انتخاب کن",color=Color.Gray,fontSize=12.sp) }
                        Icon(Icons.Default.ChevronRight,null,tint=violet)
                    }
                }
                Text("مسیرهای من",color=ink,fontSize=18.sp,fontWeight=FontWeight.Bold)
                if(learners.isEmpty()) Text("هنوز مسیری نساخته‌ای. از گزینه بالا شروع کن.",color=Color.Gray,fontSize=13.sp)
                learners.forEach { item ->
                    Card(onClick={saveProfiles(learners,item.id);page="stages"},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(45.dp).background(Color(0xFFE4F7EF),CircleShape),contentAlignment=Alignment.Center) { Icon(Icons.Default.Person,null,tint=Color(0xFF1B9B72)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(item.name,color=ink,fontWeight=FontWeight.Bold); Text("${item.minutes} دقیقه در روز • ${item.unlocked+1} مرحله باز",color=Color.Gray,fontSize=12.sp) }
                            Icon(Icons.Default.ArrowForwardIos,null,tint=violet,modifier=Modifier.size(15.dp))
                        }
                    }
                }
            }
            "wizard" -> {
                Text("ساخت مسیر یادگیری",color=ink,fontSize=22.sp,fontWeight=FontWeight.Bold)
                LinearProgressIndicator(progress={ (wizardStep+1)/3f },modifier=Modifier.fillMaxWidth(),color=violet)
                AnimatedVisibility(true,enter=fadeIn()+slideInVertically { it/6 }) {
                    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(19.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
                            when(wizardStep) {
                                0 -> {
                                    Text("دوست داری چی صدات کنیم؟",color=ink,fontSize=20.sp,fontWeight=FontWeight.Bold)
                                    Text("برای مسیر یادگیری‌ات یک نام انتخاب کن.",color=Color.Gray,fontSize=13.sp)
                                    OutlinedTextField(value=name,onValueChange={name=it},label={Text("نام یادگیرنده")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp))
                                }
                                1 -> {
                                    Text("برای چه هدفی انگلیسی می‌خوانی؟",color=ink,fontSize=19.sp,fontWeight=FontWeight.Bold)
                                    listOf("مکالمه و زندگی روزمره","کار و پیشرفت شغلی","سفر و گردشگری","تحصیل و آزمون").forEach { ChoiceRow(it,purpose==it,violet) { purpose=it } }
                                }
                                else -> {
                                    Text("روزانه چقدر زمان داری؟",color=ink,fontSize=20.sp,fontWeight=FontWeight.Bold)
                                    Text("یک برنامه واقع‌بینانه انتخاب کن.",color=Color.Gray,fontSize=13.sp)
                                    listOf(5,10,15,20,30,45).forEach { m -> ChoiceRow("$m دقیقه در روز",minutes==m,violet) { minutes=m } }
                                }
                            }
                            Button(onClick={
                                when(wizardStep) {
                                    0 -> if(name.isNotBlank()) wizardStep=1
                                    1 -> if(purpose.isNotBlank()) wizardStep=2
                                    else -> {
                                        val new=LanguageLearner(System.currentTimeMillis().toString(),name.trim(),purpose,minutes,0)
                                        saveProfiles(learners+new,new.id);page="stages"
                                    }
                                }
                            },enabled=when(wizardStep){0->name.isNotBlank();1->purpose.isNotBlank();else->true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)) {
                                Text(if(wizardStep==2) "ساخت مسیر من" else "ادامه");Icon(Icons.Default.ArrowForward,null)
                            }
                        }
                    }
                }
            }
            "stages" -> {
                if(learner==null) {
                    Text("ابتدا یک مسیر یادگیری بساز.",color=ink)
                    Button(onClick={page="home"}) { Text("بازگشت") }
                } else {
                    Text("مسیر ${learner.name}",color=ink,fontSize=22.sp,fontWeight=FontWeight.Bold)
                    Text("هدف: ${learner.purpose}  •  روزانه ${learner.minutes} دقیقه",color=Color.Gray,fontSize=12.sp)
                    Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF292F69),violet)),RoundedCornerShape(22.dp)).padding(18.dp)) {
                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("قدم به قدم تا تسلط",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)
                            Text("برای باز شدن مرحله بعد، حداقل ۲ پاسخ از ۳ سؤال را درست بده.",color=Color.White.copy(alpha=.85f),fontSize=12.sp)
                        }
                    }
                    languageCourses.forEachIndexed { index,item ->
                        val open=index<=learner.unlocked
                        Card(onClick={if(open) openStage(index)},shape=RoundedCornerShape(19.dp),colors=CardDefaults.cardColors(containerColor=if(open) Color.White else Color(0xFFE7E6EE)),modifier=Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).background(if(open) Color(0xFFEDEAFF) else Color(0xFFDAD9E2),CircleShape),contentAlignment=Alignment.Center) {
                                    Icon(if(open) Icons.Default.LockOpen else Icons.Default.Lock,null,tint=if(open) violet else Color.Gray)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("مرحله ${index+1} • ${item.first}",color=ink,fontWeight=FontWeight.Bold)
                                    Text(if(open) "۳ تمرین • آماده شروع" else "با گذراندن مرحله قبل باز می‌شود",color=Color.Gray,fontSize=12.sp)
                                }
                                if(index<learner.unlocked) Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF1B9B72)) else if(!open) Icon(Icons.Default.Lock,null,tint=Color.Gray) else Icon(Icons.Default.PlayCircle,null,tint=violet)
                            }
                        }
                    }
                    TextButton(onClick={showProfiles=true}) { Icon(Icons.Default.SwapHoriz,null);Text("تغییر یادگیرنده / مسیر") }
                    DropdownMenu(expanded=showProfiles,onDismissRequest={showProfiles=false}) {
                        learners.forEach { item -> DropdownMenuItem(text={Text(item.name)},onClick={saveProfiles(learners,item.id);showProfiles=false}) }
                        DropdownMenuItem(text={Text("ساخت مسیر جدید")},onClick={name="";wizardStep=0;page="wizard";showProfiles=false})
                    }
                }
            }
            "lesson" -> {
                val q=languageCourses[stage].second[question]
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("مرحله ${stage+1}: ${languageCourses[stage].first}",color=ink,fontWeight=FontWeight.Bold)
                    Text("${question+1}/3",color=violet,fontWeight=FontWeight.Bold)
                }
                LinearProgressIndicator(progress={ (question+1)/3f },modifier=Modifier.fillMaxWidth(),color=violet)
                Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(3.dp)) {
                    Column(Modifier.fillMaxWidth().padding(19.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Text("جمله انگلیسی",color=Color.Gray,fontSize=12.sp)
                        Text(q.sentence,color=ink,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        TextButton(onClick={questionTranslation=!questionTranslation}) {
                            Icon(if(questionTranslation) Icons.Default.VisibilityOff else Icons.Default.Translate,null,tint=violet)
                            Text(if(questionTranslation) "مخفی کردن ترجمه" else "نمایش ترجمه")
                        }
                        AnimatedVisibility(questionTranslation,enter=fadeIn()) {
                            Text(q.translation,color=violet,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth().background(Color(0xFFF0EDFF),RoundedCornerShape(12.dp)).padding(12.dp))
                        }
                    }
                }
                Text("کدام گزینه معنی درست جمله است؟",color=ink,fontSize=17.sp,fontWeight=FontWeight.Bold)
                q.choices.forEachIndexed { index,choice ->
                    val correct=answered && index==q.correct
                    val wrong=answered && index==selected && index!=q.correct
                    Card(onClick={if(!answered){selected=index;answered=true;if(index==q.correct)score++}},shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=if(correct) Color(0xFFE5F7EE) else if(wrong) Color(0xFFFFE9EC) else Color.White),modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text(listOf("A","B","C","D")[index],color=violet,fontWeight=FontWeight.ExtraBold,modifier=Modifier.background(Color(0xFFEDEAFF),CircleShape).padding(horizontal=11.dp,vertical=7.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(choice,color=ink,fontSize=15.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                                if(correct) Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF1B9B72))
                                if(wrong) Icon(Icons.Default.Cancel,null,tint=Color(0xFFD94F66))
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                                TextButton(onClick={optionTranslations=if(index in optionTranslations) optionTranslations-index else optionTranslations+index}) {
                                    Icon(if(index in optionTranslations) Icons.Default.VisibilityOff else Icons.Default.Translate,null,tint=violet,modifier=Modifier.size(17.dp))
                                    Text(if(index in optionTranslations) "مخفی" else "ترجمه",fontSize=12.sp)
                                }
                            }
                            AnimatedVisibility(index in optionTranslations,enter=fadeIn()) {
                                Text(q.choiceTranslations[index],color=violet,modifier=Modifier.fillMaxWidth().background(Color(0xFFF2F0FF),RoundedCornerShape(10.dp)).padding(10.dp))
                            }
                        }
                    }
                }
                if(answered) {
                    Text(if(selected==q.correct) "آفرین! پاسخ درست بود 🌟" else "پاسخ درست: ${q.choices[q.correct]}",color=if(selected==q.correct) Color(0xFF16865E) else Color(0xFFD34E62),fontWeight=FontWeight.Bold)
                    Text(q.tip,color=Color.Gray,fontSize=13.sp)
                    Button(onClick={advance()},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)) { Text(if(question==2) "پایان مرحله" else "سؤال بعدی");Icon(Icons.Default.ArrowForward,null) }
                }
            }
            "result" -> {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF292F69),violet)),RoundedCornerShape(25.dp)).padding(24.dp),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Icon(if(passed) Icons.Default.EmojiEvents else Icons.Default.Replay,null,tint=Color(0xFFFFD36B),modifier=Modifier.size(52.dp))
                        Text(if(passed) "مرحله را با موفقیت گذراندی!" else "یک بار دیگر تمرین کن",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                        Text("$score از ۳ پاسخ درست",color=Color.White,fontSize=17.sp)
                        Text(if(passed) "مرحله بعدی برایت باز شد 🎉" else "برای عبور، حداقل ۲ پاسخ درست لازم است.",color=Color.White.copy(alpha=.85f),fontSize=13.sp)
                    }
                }
                Button(onClick={page="stages"},modifier=Modifier.fillMaxWidth()) { Text("بازگشت به مراحل") }
                if(passed && stage<languageCourses.lastIndex) OutlinedButton(onClick={openStage(stage+1)},modifier=Modifier.fillMaxWidth()) { Text("شروع مرحله بعد") }
                if(!passed) OutlinedButton(onClick={openStage(stage)},modifier=Modifier.fillMaxWidth()) { Text("تلاش دوباره") }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ChoiceRow(label:String,selected:Boolean,accent:Color,onClick:()->Unit) {
    Card(onClick=onClick,shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=if(selected) Color(0xFFEDEAFF) else Color(0xFFFAFAFD)),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(if(selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,null,tint=accent)
            Spacer(Modifier.width(10.dp))
            Text(label,color=Color(0xFF25294B),fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

private fun readLanguageLearners(prefs:android.content.SharedPreferences):List<LanguageLearner> = runCatching {
    val array=JSONArray(prefs.getString("profiles","[]"))
    (0 until array.length()).map { i ->
        val item=array.getJSONObject(i)
        LanguageLearner(item.getString("id"),item.getString("name"),item.optString("purpose"),item.optInt("minutes",15),item.optInt("unlocked",0).coerceIn(0,languageCourses.lastIndex))
    }
}.getOrDefault(emptyList())
