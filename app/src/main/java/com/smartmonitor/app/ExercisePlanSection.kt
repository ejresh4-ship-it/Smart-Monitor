package com.smartmonitor.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class WorkoutDay(val dayFa: String, val dayEn: String, val titleFa: String, val titleEn: String, val detailFa: String, val detailEn: String)

@Composable
fun ExercisePlanSection() {
    val isFa = LocalConfiguration.current.locales[0].language == "fa"
    var level by remember { mutableStateOf(0) }
    var expanded by remember { mutableIntStateOf(-1) }
    fun t(fa: String, en: String) = if (isFa) fa else en

    val days = listOf(
        WorkoutDay("شنبه","Saturday","پیاده‌روی تند","Brisk walk","۲۰ تا ۳۰ دقیقه با شدتی که بتوانی صحبت کنی، اما آواز خواندن سخت باشد.","20–30 minutes at a pace where you can talk but singing is difficult."),
        WorkoutDay("یکشنبه","Sunday","قدرتی تمام‌بدن A","Full-body strength A","۲ دور: نشستن و برخاستن از صندلی ۸–۱۲ بار؛ شنا روی دیوار ۸–۱۲ بار؛ پل باسن ۱۰–۱۲ بار؛ پرنده-سگ ۸ بار هر سمت. بین حرکات ۶۰–۹۰ ثانیه استراحت.","2 rounds: chair squats 8–12; wall push-ups 8–12; glute bridges 10–12; bird-dog 8 each side. Rest 60–90 seconds between moves."),
        WorkoutDay("دوشنبه","Monday","هوازی کم‌فشار","Low-impact cardio","۱۵ تا ۲۵ دقیقه راه‌رفتن درجا، قدم‌زدن یا دوچرخه ثابت؛ بدون پرش.","15–25 minutes of marching, walking or stationary cycling; no jumping."),
        WorkoutDay("سه‌شنبه","Tuesday","ریکاوری و کشش","Recovery and mobility","۱۰ تا ۱۵ دقیقه حرکت آرام مفاصل و کشش ملایم؛ بدون فشار یا درد.","10–15 minutes of gentle joint mobility and easy stretching; no forcing or pain."),
        WorkoutDay("چهارشنبه","Wednesday","قدرتی تمام‌بدن B","Full-body strength B","۲ دور: بالا رفتن از پله کوتاه ۸ بار هر پا؛ هیپ‌هینج بدون وزنه ۱۰ بار؛ برد-داگ ۸ بار هر سمت؛ پلانک روی دیوار ۱۵–۲۰ ثانیه.","2 rounds: low step-ups 8 each leg; unloaded hip hinges 10; bird-dog 8 each side; wall plank 15–20 seconds."),
        WorkoutDay("پنجشنبه","Thursday","پیاده‌روی هوازی","Aerobic walk","۲۰ تا ۳۰ دقیقه پیاده‌روی با سرعت متوسط؛ در صورت خستگی به بازه‌های کوتاه تقسیم کن.","20–30 minutes of moderate walking; split into shorter intervals if needed."),
        WorkoutDay("جمعه","Friday","استراحت فعال","Active rest","استراحت یا ۱۰ تا ۲۰ دقیقه قدم‌زدن سبک و دلخواه.","Rest or 10–20 minutes of easy, comfortable walking.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t("برنامه تمرین برای تناسب اندام و کاهش چربی","Fitness and fat-loss exercise plan"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(t("چربی‌سوزی موضعی با تمرین یک ناحیه تضمین نمی‌شود؛ ترکیب فعالیت هوازی، تمرین قدرتی، تغذیه متعادل و استمرار به کاهش چربی کلی و حفظ عضله کمک می‌کند.","Spot reduction is not guaranteed. Combining aerobic activity, strength training, balanced nutrition and consistency supports overall fat loss and muscle maintenance."))
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("سطح تمرین","Training level"), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = level == 0, onClick = { level = 0 }, label = { Text(t("مبتدی","Beginner")) })
                    FilterChip(selected = level == 1, onClick = { level = 1 }, label = { Text(t("متوسط","Intermediate")) })
                }
                Text(if (level == 0) t("هفته اول: از حداقل زمان شروع کن و در صورت راحتی هر هفته ۵ دقیقه اضافه کن.","Week 1: start at the lower duration and add 5 minutes weekly if comfortable.") else t("فقط اگر چند هفته بدون درد و با ریکاوری مناسب تمرین کرده‌ای، زمان هوازی را ۵ تا ۱۰ دقیقه بیشتر کن.","Only if you have trained comfortably for several weeks, add 5–10 minutes to cardio sessions."))
            }
        }
        days.forEachIndexed { index, day ->
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isFa) day.dayFa else day.dayEn, fontWeight = FontWeight.Bold)
                    Text(if (isFa) day.titleFa else day.titleEn, fontSize = 16.sp)
                    TextButton(onClick = { expanded = if (expanded == index) -1 else index }) {
                        Text(if (expanded == index) t("بستن جزئیات","Hide details") else t("جزئیات تمرین","Exercise details"))
                    }
                    if (expanded == index) Text(if (isFa) day.detailFa else day.detailEn)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t("نکات ایمنی","Safety notes"), fontWeight = FontWeight.Bold)
                Text(t("• قبل از تمرین ۵ دقیقه گرم‌کردن و در پایان ۵ دقیقه سردکردن آرام انجام بده.\n• حرکت‌ها باید کنترل‌شده و بدون درد باشند؛ در صورت درد، سرگیجه، تنگی نفس غیرعادی یا درد قفسه سینه تمرین را متوقف کن.\n• اگر تازه‌کار هستی، از یک دور و زمان کوتاه شروع کن؛ روزهای استراحت را حذف نکن.\n• در بیماری قلبی، مفصلی، بارداری، محدودیت حرکتی یا رژیم/دستور درمانی، پیش از شروع با پزشک یا فیزیوتراپیست مشورت کن.","• Warm up for 5 minutes and cool down gently for 5 minutes.\n• Move with control and without pain; stop for pain, dizziness, unusual breathlessness or chest pain.\n• Beginners should start with one round and shorter sessions; keep rest days.\n• If you have heart or joint disease, are pregnant, have mobility limits or clinical restrictions, consult a clinician or physiotherapist before starting."))
            }
        }
        Text(t("هدف عمومی سلامت برای بزرگسالان، در صورت متناسب بودن با شرایط فرد، به‌تدریج نزدیک شدن به ۱۵۰ دقیقه فعالیت هوازی متوسط در هفته و تمرین قدرتی دست‌کم ۲ روز در هفته است. این برنامه نمونه آموزشی است، نه نسخه شخصی یا تضمین کاهش وزن.","A common adult health target, when appropriate, is gradually working toward 150 minutes of moderate aerobic activity weekly plus strength work at least 2 days per week. This is an educational example, not a personalized prescription or weight-loss guarantee."), style = MaterialTheme.typography.bodySmall)
    }
}
