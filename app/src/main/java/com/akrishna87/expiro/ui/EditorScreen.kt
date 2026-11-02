package com.akrishna87.expiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.WeightUnit
import com.akrishna87.expiro.data.formatWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(initial: Item?, onCancel: () -> Unit, onSave: (Item) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var quantity by rememberSaveable { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var weight by rememberSaveable { mutableStateOf(initial?.let { formatWeight(it.weight) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(initial?.unit ?: WeightUnit.G.label) }
    var expiryDay by rememberSaveable { mutableStateOf(initial?.expiryEpochDay ?: LocalDate.now().plusDays(7).toEpochDay()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val qty = quantity.toIntOrNull()
    val w = weight.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && qty != null && qty >= 1 && w != null && w > 0

    Scaffold(topBar = { TopAppBar(title = { Text(if (initial == null) "Add item" else "Edit item") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Item name") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it.filter(Char::isDigit).take(4) },
                    label = { Text("Count") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                    label = { Text("Weight each") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightUnit.entries.forEach { u ->
                    FilterChip(selected = unit == u.label, onClick = { unit = u.label }, label = { Text(u.label) })
                }
            }
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Expiry date: ${LocalDate.ofEpochDay(expiryDay)}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val base = initial ?: Item(name = "", quantity = 1, weight = 1.0, unit = "g", expiryEpochDay = 0)
                        onSave(
                            base.copy(
                                name = name.trim(),
                                quantity = qty!!,
                                weight = w!!,
                                unit = unit,
                                expiryEpochDay = expiryDay,
                                // Re-arm reminders if the date changed.
                                notifiedStage = if (initial != null && initial.expiryEpochDay == expiryDay) initial.notifiedStage else 0,
                            ),
                        )
                    },
                ) { Text("Save") }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(expiryDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        expiryDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
