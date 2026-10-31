package com.smartmonitor.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class FoodEstimate(
    val nameRes: Int,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val keywords: List<String>
)

private val FOOD_ESTIMATES = listOf(
    FoodEstimate(R.string.food_rice, 130.0, 2.7, 28.2, 0.3, listOf("rice")),
    FoodEstimate(R.string.food_bread, 260.0, 9.0, 52.0, 1.5, listOf("bread")),
    FoodEstimate(R.string.food_chicken, 165.0, 31.0, 0.0, 3.6, listOf("chicken", "poultry")),
    FoodEstimate(R.string.food_fish, 206.0, 22.0, 0.0, 12.0, listOf("fish", "seafood")),
    FoodEstimate(R.string.food_salad, 35.0, 1.3, 7.0, 0.2, listOf("salad", "vegetable", "cucumber", "tomato")),
    FoodEstimate(R.string.food_soup, 90.0, 4.0, 13.0, 2.0, listOf("soup")),
    FoodEstimate(R.string.food_pasta, 165.0, 6.0, 25.0, 5.0, listOf("pasta", "spaghetti", "noodle")),
    FoodEstimate(R.string.food_pizza, 266.0, 11.0, 33.0, 10.0, listOf("pizza")),
    FoodEstimate(R.string.food_egg, 155.0, 13.0, 1.1, 11.0, listOf("egg", "omelet")),
    FoodEstimate(R.string.food_potato, 87.0, 1.9, 20.0, 0.1, listOf("potato")),
    FoodEstimate(R.string.food_ghormeh, 160.0, 8.0, 8.0, 10.0, emptyList()),
    FoodEstimate(R.string.food_gheimeh, 170.0, 8.0, 12.0, 9.0, emptyList()),
    FoodEstimate(R.string.food_kebab, 250.0, 17.0, 2.0, 19.0, listOf("kebab", "meat")),
    FoodEstimate(R.string.food_adas_polo, 160.0, 6.0, 28.0, 3.0, emptyList()),
    FoodEstimate(R.string.food_lentil, 116.0, 9.0, 20.0, 0.4, listOf("lentil", "bean"))
)

@Composable
fun HealthFoodScanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("health_safety", Context.MODE_PRIVATE) }
    val labeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.40f).build()
        )
    }
    DisposableEffect(labeler) {
        onDispose { labeler.close() }
    }

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var labels by remember { mutableStateOf<List<ImageLabel>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf(FOOD_ESTIMATES.first()) }
    var foodMenuExpanded by remember { mutableStateOf(false) }
    var grams by remember { mutableStateOf("250") }
    var loggedCalories by remember { mutableIntStateOf(readTodayCalories(prefs)) }

    val scope = rememberCoroutineScope()

    val analyze: (Bitmap) -> Unit = { bitmap ->
        isAnalyzing = true
        scanError = ""
        labels = emptyList()
        labeler.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                labels = result.sortedByDescending { it.confidence }.take(5)
                val descriptions = labels.map { it.text.lowercase(Locale.ROOT) }
                val detected = FOOD_ESTIMATES.firstOrNull { food ->
                    food.keywords.any { keyword -> descriptions.any { keyword in it } }
                }
                if (detected != null) selectedFood = detected
                if (result.isEmpty()) scanError = context.getString(R.string.food_no_labels)
                isAnalyzing = false
            }
            .addOnFailureListener {
                scanError = context.getString(R.string.food_scan_failed)
                isAnalyzing = false
            }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            photo = bitmap
            analyze(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                scanError = ""
                val bitmap = withContext(Dispatchers.IO) { decodeFoodBitmap(context, uri) }
                if (bitmap == null) {
                    isAnalyzing = false
                    scanError = context.getString(R.string.food_image_load_failed)
                } else {
                    photo = bitmap
                    analyze(bitmap)
                }
            }
        }
    }

    val gramsValue = grams.foodAsciiDigits().toIntOrNull()
    val validGrams = gramsValue != null && gramsValue in 10..2000
    val amount = if (validGrams) gramsValue!! / 100.0 else 0.0
    val calories = (selectedFood.kcalPer100g * amount).roundToInt()
    val protein = selectedFood.proteinPer100g * amount
    val carbs = selectedFood.carbsPer100g * amount
    val fat = selectedFood.fatPer100g * amount

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.food_scan_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.food_scan_intro), style = MaterialTheme.typography.bodyMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.food_take_photo))
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.food_choose_photo))
                }
            }

            photo?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(R.string.food_photo_description),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (isAnalyzing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.food_analyzing))
            }
            if (scanError.isNotBlank()) {
                Text(scanError, color = MaterialTheme.colorScheme.error)
            }
            if (labels.isNotEmpty()) {
                Text(stringResource(R.string.food_detected_labels), fontWeight = FontWeight.SemiBold)
                Text(labels.joinToString(" • ") { "${it.text} (${(it.confidence * 100).roundToInt()}%)" })
            }

            Text(stringResource(R.string.food_choose_type), fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { foodMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(selectedFood.nameRes))
                }
                DropdownMenu(expanded = foodMenuExpanded, onDismissRequest = { foodMenuExpanded = false }) {
                    FOOD_ESTIMATES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.nameRes)) },
                            onClick = { selectedFood = option; foodMenuExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it },
                label = { Text(stringResource(R.string.food_amount_grams)) },
                suffix = { Text(stringResource(R.string.food_gram_unit)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(100, 150, 200, 250).forEach { value ->
                    OutlinedButton(
                        onClick = { grams = value.toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) { Text("$value") }
                }
            }

            if (validGrams) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.food_estimated_result), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.food_calorie_result, calories), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.food_macro_result, String.format(Locale.getDefault(), "%.1f", protein), String.format(Locale.getDefault(), "%.1f", carbs), String.format(Locale.getDefault(), "%.1f", fat)))
                        Text(stringResource(R.string.food_per_100g, selectedFood.kcalPer100g.roundToInt()))
                    }
                }
                Button(
                    onClick = {
                        val key = todayFoodLogKey()
                        val entries = runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())
                        entries.put(
                            JSONObject()
                                .put("food", context.getString(selectedFood.nameRes))
                                .put("grams", gramsValue)
                                .put("kcal", calories)
                                .put("time", System.currentTimeMillis())
                        )
                        prefs.edit().putString(key, entries.toString()).apply()
                        loggedCalories += calories
                        Toast.makeText(context, context.getString(R.string.food_saved), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.food_add_to_log)) }
            } else {
                Text(stringResource(R.string.food_amount_validation), color = MaterialTheme.colorScheme.error)
            }

            Text(stringResource(R.string.food_today_total, loggedCalories), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.food_accuracy_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun decodeFoodBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

private fun todayFoodLogKey(): String =
    "food_log_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun readTodayCalories(prefs: android.content.SharedPreferences): Int {
    val entries = runCatching { JSONArray(prefs.getString(todayFoodLogKey(), "[]") ?: "[]") }.getOrDefault(JSONArray())
    return (0 until entries.length()).sumOf { index -> entries.optJSONObject(index)?.optInt("kcal") ?: 0 }
}

private fun String.foodAsciiDigits(): String = map { ch ->
    when (ch) {
        in '۰'..'۹' -> (ch.code - '۰'.code + '0'.code).toChar()
        in '٠'..'٩' -> (ch.code - '٠'.code + '0'.code).toChar()
        else -> ch
    }
}.joinToString("")
