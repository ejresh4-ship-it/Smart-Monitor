package com.smartmonitor.app

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.util.UUID

private const val HEALTH_PREFS = "health_safety"
private const val DISCLAIMER_ACCEPTED = "nutrition_disclaimer_accepted"
private enum class Goal { MAINTAIN, LOSE, GAIN }
private enum class Sex { FEMALE, MALE }
private enum class Activity(val factor: Double) { LOW(1.2), LIGHT(1.375), MODERATE(1.55), HIGH(1.725) }

private data class HealthProfile(val id: String, val name: String, val age: String, val height: String, val weight: String, val sex: Sex, val activity: Activity, val goal: Goal, val vegetarian: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HEALTH_PREFS, Context.MODE_PRIVATE) }
    var accepted by remember { mutableStateOf(prefs.getBoolean(DISCLAIMER_ACCEPTED, false)) }
    var profileIds by remember { mutableStateOf(prefs.getStringSet("profile_ids", emptySet())!!.toList()) }
    var activeId by remember { mutableStateOf(prefs.getString("active_profile", "") ?: "") }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.FEMALE) }
    var activity by remember { mutableStateOf(Activity.LOW) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }
    var vegetarian by remember { mutableStateOf(false) }
    var hasMedicalCondition by remember { mutableStateOf(false) }
    var planReady by remember { mutableStateOf(false) }
    var showFoodScanner by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showFeatures by remember { mutableStateOf(false) }

    fun loadProfile(id: String) {
        activeId = id
        val p = "profile_$id"
        name = prefs.getString("${p}_name", "") ?: ""
        age = prefs.getString("${p}_age", "") ?: ""
        height = prefs.getString("${p}_height", "") ?: ""
        weight = prefs.getString("${p}_weight", "") ?: ""
        sex = runCatching { Sex.valueOf(prefs.getString("${p}_sex", Sex.FEMALE.name)!!) }.getOrDefault(Sex.FEMALE)
        activity = runCatching { Activity.valueOf(prefs.getString("${p}_activity", Activity.LOW.name)!!) }.getOrDefault(Activity.LOW)
        goal = runCatching { Goal.valueOf(prefs.getString("${p}_goal", Goal.MAINTAIN.name)!!) }.getOrDefault(Goal.MAINTAIN)
        vegetarian = prefs.getBoolean("${p}_vegetarian", false)
        hasMedicalCondition = prefs.getBoolean("${p}_medical_condition", false)
        planReady = false
    }
    fun saveProfile() {
        val p = "profile_$activeId"
        prefs.edit().putString("${p}_name", name.trim()).putString("${p}_age", age)
            .putString("${p}_height", height).putString("${p}_weight", weight)
            .putString("${p}_sex", sex.name).putString("${p}_activity", activity.name)
            .putString("${p}_goal", goal.name).putBoolean("${p}_vegetarian", vegetarian)
            .putBoolean("${p}_medical_condition", hasMedicalCondition)
            .putString("active_profile", activeId).putStringSet("profile_ids", profileIds.toSet()).apply()
    }
    LaunchedEffect(Unit) {
        if (profileIds.isEmpty()) {
            val id = UUID.randomUUID().toString()
            profileIds = listOf(id)
            activeId = id
            name = "پروفایل من"
            prefs.edit().putStringSet("profile_ids", setOf(id)).putString("active_profile", id).apply()
        } else {
            val selected = activeId.takeIf { it in profileIds } ?: profileIds.first()
            loadProfile(selected)
        }
    }
    val progress by animateFloatAsState(if (age.isNotBlank() && height.isNotBlank() && weight.isNotBlank()) .9f else .35f, label = "profileProgress")

    if (!accepted) {
        AlertDialog(onDismissRequest = { }, icon = { Icon(Icons.Default.HealthAndSafety, null) },
            title = { Text(stringResource(R.string.health_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.health_warning_body)) },
            confirmButton = { Button(onClick = { prefs.edit().putBoolean(DISCLAIMER_ACCEPTED, true).apply(); accepted = true }) { Text(stringResource(R.string.health_warning_accept)) } },
            dismissButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.health_warning_exit)) } },
            shape = RoundedCornerShape(24.dp))
    }

    if (showProfileDialog) {
        AlertDialog(onDismissRequest = { showProfileDialog = false },
            title = { Text(if (editingProfile) "ویرایش نام پروفایل" else "افزودن کاربر جدید") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام ذخیره‌سازی") }, singleLine = true) },
            confirmButton = { Button(onClick = {
                if (name.isNotBlank()) {
                    if (!editingProfile) {
                        val id = UUID.randomUUID().toString()
                        profileIds = profileIds + id
                        activeId = id
                        age = ""; height = ""; weight = ""; sex = Sex.FEMALE; activity = Activity.LOW; goal = Goal.MAINTAIN; vegetarian = false; hasMedicalCondition = false
                    }
                    saveProfile(); showProfileDialog = false; planReady = false
                }
            }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("انصراف") } })
    }

    if (accepted) {
        Scaffold(containerColor = Color(0xFFF4F7FC), topBar = {
            TopAppBar(title = { Text("مرکز سلامت", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { accepted = false }) { Icon(Icons.Default.Info, "هشدار سلامت") } })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AnimatedVisibility(true, enter = fadeIn() + slideInVertically()) {
                    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102C55), Color(0xFF2879C7)))).padding(20.dp)) {
                            Text("سلامتی، قدم‌به‌قدم", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text("پروفایل، تغذیه و فعالیت روزانه در یک جا", color = Color.White.copy(alpha = .85f))
                            Spacer(Modifier.height(14.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF55E0C1), trackColor = Color.White.copy(alpha = .2f))
                            Text(if (progress > .8f) "مشخصات پایه تکمیل شده" else "تکمیل مشخصات پایه", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("پروفایل‌های سلامت", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("اطلاعات هر فرد جداگانه روی همین گوشی ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { editingProfile = false; name = ""; showProfileDialog = true }) { Icon(Icons.Default.PersonAdd, "افزودن کاربر") }
                        }
                        profileIds.forEach { id ->
                            val profileName = prefs.getString("profile_${id}_name", if (id == activeId) name else "کاربر") ?: "کاربر"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = activeId == id, onClick = { saveProfile(); loadProfile(id) }, label = { Text(profileName) }, leadingIcon = { Icon(Icons.Default.Person, null) })
                                if (activeId == id) {
                                    IconButton(onClick = { editingProfile = true; showProfileDialog = true }) { Icon(Icons.Default.Edit, "ویرایش نام") }
                                }
                            }
                        }
                    }
                }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("مشخصات فرد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام پروفایل") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        HealthInput(stringResource(R.string.health_age), age, { age = it; planReady = false }, stringResource(R.string.health_age_unit))
                        HealthInput(stringResource(R.string.health_height), height, { height = it; planReady = false }, stringResource(R.string.health_height_unit))
                        HealthInput(stringResource(R.string.health_weight), weight, { weight = it; planReady = false }, stringResource(R.string.health_weight_unit))
                        Text(stringResource(R.string.health_sex), fontWeight = FontWeight.SemiBold)
                        Row { ChoiceButton(stringResource(R.string.health_female), sex == Sex.FEMALE) { sex = Sex.FEMALE; planReady = false }; ChoiceButton(stringResource(R.string.health_male), sex == Sex.MALE) { sex = Sex.MALE; planReady = false } }
                        Text(stringResource(R.string.health_activity), fontWeight = FontWeight.SemiBold)
                        Activity.values().forEach { item ->
                            ChoiceButton(stringResource(when(item) { Activity.LOW -> R.string.activity_low; Activity.LIGHT -> R.string.activity_light; Activity.MODERATE -> R.string.activity_moderate; Activity.HIGH -> R.string.activity_high }), activity == item) { activity = item; planReady = false }
                        }
                        Text(stringResource(R.string.health_goal), fontWeight = FontWeight.SemiBold)
                        Row { ChoiceButton(stringResource(R.string.goal_maintain), goal == Goal.MAINTAIN) { goal = Goal.MAINTAIN; planReady = false }; ChoiceButton(stringResource(R.string.goal_lose), goal == Goal.LOSE) { goal = Goal.LOSE; planReady = false }; ChoiceButton(stringResource(R.string.goal_gain), goal == Goal.GAIN) { goal = Goal.GAIN; planReady = false } }
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(vegetarian, { vegetarian = it; planReady = false }); Text(stringResource(R.string.health_vegetarian)) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(hasMedicalCondition, { hasMedicalCondition = it; planReady = false }); Text(stringResource(R.string.health_medical_flag)) }
                        if (hasMedicalCondition) Text(stringResource(R.string.health_medical_block), color = MaterialTheme.colorScheme.error)
                        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            val a = age.toAsciiDigits().toIntOrNull(); val h = height.toAsciiDigits().toDoubleOrNull(); val w = weight.toAsciiDigits().toDoubleOrNull()
                            error = when {
                                name.isBlank() -> "برای پروفایل یک نام وارد کن."
                                a == null || h == null || w == null -> context.getString(R.string.health_invalid)
                                a !in 18..75 || h !in 120.0..220.0 || w !in 35.0..250.0 -> context.getString(R.string.health_out_of_range)
                                hasMedicalCondition -> context.getString(R.string.health_medical_block)
                                else -> ""
                            }
                            if (error.isBlank()) { saveProfile(); planReady = true }
                        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("ذخیره مشخصات و محاسبه") }
                    }
                }
                AnimatedVisibility(planReady, enter = fadeIn() + slideInVertically()) {
                    val a = age.toAsciiDigits().toIntOrNull() ?: 0
                    val h = height.toAsciiDigits().toDoubleOrNull() ?: 1.0
                    val w = weight.toAsciiDigits().toDoubleOrNull() ?: 0.0
                    val bmi = w / ((h / 100.0) * (h / 100.0))
                    val bmr = 10 * w + 6.25 * h - 5 * a + if (sex == Sex.MALE) 5 else -161
                    val maintenance = (bmr * activity.factor).roundToInt()
                    val target = when(goal) { Goal.MAINTAIN -> maintenance; Goal.LOSE -> (maintenance * .90).roundToInt(); Goal.GAIN -> (maintenance * 1.05).roundToInt() }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4F2))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("خلاصه سلامت • $name", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(stringResource(R.string.health_bmi, String.format("%.1f", bmi)))
                                Text(stringResource(R.string.health_energy, maintenance))
                                Text(stringResource(R.string.health_target, target))
                                Text(stringResource(R.string.health_estimate_caveat), style = MaterialTheme.typography.bodySmall)
                                if (bmi < 18.5 || bmi >= 30.0) Text(stringResource(R.string.health_bmi_caution), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text("امکانات سلامت", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        OutlinedButton(onClick = { showFeatures = !showFeatures }, modifier = Modifier.fillMaxWidth()) { Text(if (showFeatures) "بستن امکانات" else "نمایش امکانات سلامت") }
                        AnimatedVisibility(showFeatures, enter = fadeIn() + slideInVertically()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HealthAiAssistant()
                                OutlinedButton(onClick = { showFoodScanner = !showFoodScanner }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CameraAlt, null); Text(if (showFoodScanner) "بستن اسکنر غذا" else "محاسبه کالری با عکس غذا") }
                                if (showFoodScanner) HealthFoodScanner()
                                ExercisePlanSection()
                                Text("نمونه برنامه غذایی روزانه", fontWeight = FontWeight.Bold)
                                MealCard(stringResource(R.string.meal_breakfast), if (vegetarian) stringResource(R.string.meal_veg_breakfast) else stringResource(R.string.meal_breakfast_text))
                                MealCard(stringResource(R.string.meal_lunch), if (vegetarian) stringResource(R.string.meal_veg_lunch) else stringResource(R.string.meal_lunch_text))
                                MealCard(stringResource(R.string.meal_snack), stringResource(R.string.meal_snack_text))
                                MealCard(stringResource(R.string.meal_dinner), if (vegetarian) stringResource(R.string.meal_veg_dinner) else stringResource(R.string.meal_dinner_text))
                                Text(stringResource(when(goal) { Goal.MAINTAIN -> R.string.health_plan_note; Goal.LOSE -> R.string.health_lose_note; Goal.GAIN -> R.string.health_gain_note }))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthInput(label: String, value: String, onValue: (String) -> Unit, suffix: String) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        suffix = { Text(suffix) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        if (selected) Text("✓ $label", fontWeight = FontWeight.Bold) else Text(label)
    }
}

@Composable
private fun MealCard(title: String, description: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description)
        }
    }
}

private fun String.toAsciiDigits(): String = map { ch ->
    when (ch) {
        in '۰'..'۹' -> (ch.code - '۰'.code + '0'.code).toChar()
        in '٠'..'٩' -> (ch.code - '٠'.code + '0'.code).toChar()
        else -> ch
    }
}.joinToString("")
