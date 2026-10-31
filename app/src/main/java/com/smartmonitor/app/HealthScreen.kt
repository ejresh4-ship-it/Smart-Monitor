package com.smartmonitor.app

import android.content.Context
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val HEALTH_PREFS = "health_safety"
private const val DISCLAIMER_ACCEPTED = "nutrition_disclaimer_accepted"

@Composable
fun HealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HEALTH_PREFS, Context.MODE_PRIVATE) }
    var accepted by remember { mutableStateOf(prefs.getBoolean(DISCLAIMER_ACCEPTED, false)) }

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
            dismissButton = {
                TextButton(onClick = onBack) { Text(stringResource(R.string.health_warning_exit)) }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.health_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (accepted) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.health_ai_title), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(stringResource(R.string.health_ai_coming))
                    Text(stringResource(R.string.health_source_note), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
