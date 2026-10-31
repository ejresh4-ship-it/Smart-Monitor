package com.smartmonitor.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val HEALTH_PREFS = "health_safety"
private const val DISCLAIMER_ACCEPTED = "nutrition_disclaimer_accepted"

private enum class Goal { MAINTAIN, LOSE, GAIN }
private enum class Sex { FEMALE, MALE }
private enum class Activity(val factor: Double) { LOW(1.2), LIGHT(1.375), MODERATE(1.55), HIGH(1.725) }

@Composable
fun HealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HEALTH_PREFS, Context.MODE_PRIVATE) }
    var accepted by remember { mutableStateOf(prefs.getBoolean(DISCLAIMER_ACCEPTED, false)) }
    var age by remember { mutableStateOf(prefs.getString("age", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("height", "") ?: "") }
    var weight by remember { mutableStateOf(prefs.getString("weight", "") ?: "") }
    var sex by remember { mutableStateOf(Sex.FEMALE) }
    var activity by remember { mutableStateOf(Activity.LOW) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }
    var hasMedicalCondition by remember { mutableStateOf(false) }
    var vegetarian by remember { mutableStateOf(false) }
    var planReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    if (!accepted) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
            title = { Text(stringResource(R.string.health_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.health_warning_body)) },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putBoolean(DISCLAIMER_ACCEPTED, true).apply()
                    accepted = true
                }) { Text(stringResource(R.string.health_warning_accept)) }
            },
            dismissButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.health_warning_exit)) } },
            shape = RoundedCornerShape(22.dp)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.health_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (accepted) {
            Text(stringResource(R.string.health_ai_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.health_intro))
            HealthInput(stringResource(R.string.health_age), age, { age = it }, stringResource(R.string.health_age_unit))
            HealthInput(stringResource(R.string.health_height), height, { height = it }, stringResource(R.string.health_height_unit))
            HealthInput(stringResource(R.string.health_weight), weight, { weight = it }, stringResource(R.string.health_weight_unit))

            Text(stringResource(R.string.health_sex), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceButton(stringResource(R.string.health_female), sex == Sex.FEMALE) { sex = Sex.FEMALE; planReady = false }
                ChoiceButton(stringResource(R.string.health_male), sex == Sex.MALE) { sex = Sex.MALE; planReady = false }
            }
            Text(stringResource(R.string.health_activity), fontWeight = FontWeight.SemiBold)
            Activity.values().forEach { item ->
                ChoiceButton(stringResource(when(item) {
                    Activity.LOW -> R.string.activity_low
                    Activity.LIGHT -> R.string.activity_light
                    Activity.MODERATE -> R.string.activity_moderate
                    Activity.HIGH -> R.string.activity_high
                }), activity == item) { activity = item; planReady = false }
            }
            Text(stringResource(R.string.health_goal), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceButton(stringResource(R.string.goal_maintain), goal == Goal.MAINTAIN) { goal = Goal.MAINTAIN; planReady = false }
                ChoiceButton(stringResource(R.string.goal_lose), goal == Goal.LOSE) { goal = Goal.LOSE; planReady = false }
                ChoiceButton(stringResource(R.string.goal_gain), goal == Goal.GAIN) { goal = Goal.GAIN; planReady = false }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vegetarian, onCheckedChange = { vegetarian = it; planReady = false })
                Text(stringResource(R.string.health_vegetarian))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasMedicalCondition, onCheckedChange = { hasMedicalCondition = it; planReady = false })
                Text(stringResource(R.string.health_medical_flag))
            }
            if (hasMedicalCondition) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(stringResource(R.string.health_medical_block), Modifier.padding(14.dp))
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = {
                    val a = age.toAsciiDigits().toIntOrNull()
                    val h = height.toAsciiDigits().toDoubleOrNull()
                    val w = weight.toAsciiDigits().toDoubleOrNull()
                    error = when {
                        a == null || h == null || w == null -> context.getString(R.string.health_invalid)
                        a !in 18..75 || h !in 120.0..220.0 || w !in 35.0..250.0 -> context.getString(R.string.health_out_of_range)
                        hasMedicalCondition -> context.getString(R.string.health_medical_block)
                        else -> ""
                    }
                    if (error.isBlank()) {
                        prefs.edit().putString("age", age).putString("height", height).putString("weight", weight).putString("sex", sex.name).putString("activity", activity.name).putString("goal", goal.name).putBoolean("vegetarian", vegetarian).apply()
                        planReady = true
                    } else planReady = false
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.health_make_plan)) }

            if (planReady) {
                val a = age.toAsciiDigits().toInt()
                val h = height.toAsciiDigits().toDouble()
                val w = weight.toAsciiDigits().toDouble()
                val bmi = w / ((h / 100.0) * (h / 100.0))
                val bmr = 10 * w + 6.25 * h - 5 * a + if (sex == Sex.MALE) 5 else -161
                val maintenance = (bmr * activity.factor).roundToInt()
                val target = when(goal) {
                    Goal.MAINTAIN -> maintenance
                    Goal.LOSE -> (maintenance * 0.90).roundToInt()
                    Goal.GAIN -> (maintenance * 1.05).roundToInt()
                }
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.health_estimate_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(stringResource(R.string.health_bmi, String.format("%.1f", bmi)))
                        Text(stringResource(R.string.health_energy, maintenance))
                        Text(stringResource(R.string.health_target, target))
                        Text(stringResource(R.string.health_estimate_caveat))
                        if (bmi < 18.5 || bmi >= 30.0) Text(stringResource(R.string.health_bmi_caution), color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(stringResource(R.string.health_daily_plan), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                MealCard(stringResource(R.string.meal_breakfast), if (vegetarian) stringResource(R.string.meal_veg_breakfast) else stringResource(R.string.meal_breakfast_text))
                MealCard(stringResource(R.string.meal_lunch), if (vegetarian) stringResource(R.string.meal_veg_lunch) else stringResource(R.string.meal_lunch_text))
                MealCard(stringResource(R.string.meal_snack), stringResource(R.string.meal_snack_text))
                MealCard(stringResource(R.string.meal_dinner), if (vegetarian) stringResource(R.string.meal_veg_dinner) else stringResource(R.string.meal_dinner_text))
                Text(stringResource(when (goal) {
                    Goal.MAINTAIN -> R.string.health_plan_note
                    Goal.LOSE -> R.string.health_lose_note
                    Goal.GAIN -> R.string.health_gain_note
                }))
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
