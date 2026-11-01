package com.smartmonitor.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private enum class HealthAiProvider(val label: String, val endpoint: String, val model: String, val keyHint: String) {
    GROQ("Groq • GPT-OSS 120B", "https://api.groq.com/openai/v1/chat/completions", "openai/gpt-oss-120b", "کلید Groq"),
    OPENROUTER("OpenRouter • مدل رایگان خودکار", "https://openrouter.ai/api/v1/chat/completions", "openrouter/free", "کلید OpenRouter"),
    MISTRAL("Mistral • Ministral", "https://api.mistral.ai/v1/chat/completions", "ministral-3b-latest", "کلید Mistral")
}

@Composable
fun HealthAiAssistant() {
    var provider by remember { mutableStateOf(HealthAiProvider.GROQ) }
    var apiKey by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("دستیار هوش مصنوعی سلامت", style = MaterialTheme.typography.titleLarge)
            Text("یکی از سرویس‌ها را انتخاب کن. استفاده رایگان به سهمیه و شرایط حساب همان سرویس بستگی دارد.", style = MaterialTheme.typography.bodySmall)
            Box {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text(provider.label) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    HealthAiProvider.values().forEach { item ->
                        DropdownMenuItem(text = { Text(item.label) }, onClick = { provider = item; menu = false; answer = ""; error = "" })
                    }
                }
            }
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it; error = "" },
                label = { Text("کلید API شخصی • ${provider.keyHint}") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = question, onValueChange = { question = it },
                label = { Text("سؤال عمومی سلامت یا تغذیه") },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )
            Button(
                enabled = !loading && apiKey.isNotBlank() && question.isNotBlank(),
                onClick = {
                    loading = true; error = ""; answer = ""
                    scope.launch {
                        try {
                            answer = withContext(Dispatchers.IO) { requestHealthAi(provider, apiKey.trim(), question.trim()) }
                        } catch (e: Exception) {
                            error = e.message ?: "ارتباط با سرویس برقرار نشد."
                        } finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "در حال دریافت پاسخ…" else "پرسیدن از هوش مصنوعی") }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if (answer.isNotBlank()) {
                HorizontalDivider()
                Text("پاسخ", style = MaterialTheme.typography.titleMedium)
                Text(answer)
            }
            Text(
                "حریم خصوصی: کلید فقط تا زمان بازبودن این صفحه در حافظه برنامه می‌ماند و ذخیره نمی‌شود. سؤال به سرویس انتخابی فرستاده می‌شود؛ مشخصات پروفایل به‌طور خودکار ارسال نمی‌شود. سؤال را بدون نام، اطلاعات شناسایی یا جزئیات پزشکی حساس بنویس. پاسخ برای آموزش عمومی است، نه تشخیص یا درمان.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun requestHealthAi(provider: HealthAiProvider, key: String, question: String): String {
    val connection = (URL(provider.endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 45000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $key")
        setRequestProperty("Content-Type", "application/json")
        if (provider == HealthAiProvider.OPENROUTER) {
            setRequestProperty("HTTP-Referer", "https://ejresh4-ship-it.github.io/Smart-Monitor/")
            setRequestProperty("X-Title", "Smart Monitor")
        }
    }
    try {
        val systemPrompt = "You are a cautious health education assistant. Answer in Persian. Provide general evidence-informed information only. Never diagnose, prescribe medication, or replace a clinician. Clearly state uncertainty. For urgent symptoms, advise local emergency care. Do not recommend extreme diets or unsafe weight loss."
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", question))
        val body = JSONObject().put("model", provider.model).put("messages", messages).put("temperature", 0.3).put("max_tokens", 900)
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val detail = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
            throw IllegalStateException(detail?.takeIf { it.isNotBlank() } ?: "خطای سرویس (HTTP $code). کلید و سهمیه را بررسی کن.")
        }
        val parsed = JSONObject(raw)
        val text = parsed.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        text.takeIf { it.isNotBlank() } ?: throw IllegalStateException("پاسخ خالی از سرویس دریافت شد.")
    } finally { connection.disconnect() }
}
