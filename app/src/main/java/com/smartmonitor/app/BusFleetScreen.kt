package com.smartmonitor.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BusFleetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val watchlist = remember { BusFleetWatchlist(context) }
    var codes by remember { mutableStateOf(watchlist.allCodes()) }
    var newCode by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.home)) }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.fleet_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(
            stringResource(R.string.fleet_count, codes.size),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCode,
                onValueChange = { newCode = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.bus_code_hint)) }
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = {
                if (watchlist.add(newCode)) {
                    codes = watchlist.allCodes()
                    newCode = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_bus))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.fleet_source_note), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(codes, key = { it }) { code ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.bus_code_label, code))
                            Text(stringResource(R.string.bus_waiting_source), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            watchlist.remove(code)
                            codes = watchlist.allCodes()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_bus))
                        }
                    }
                }
            }
        }
    }
}
