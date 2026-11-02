package com.akrishna87.expiro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.akrishna87.expiro.data.Categories
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.WeightUnit
import com.akrishna87.expiro.data.formatWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(initial: Item?, onCancel: () -> Unit, onSave: (Item) -> Unit, onDelete: (Item) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var quantity by rememberSaveable { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var weight by rememberSaveable { mutableStateOf(initial?.let { formatWeight(it.weight) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(initial?.unit ?: WeightUnit.G.label) }
    var expiryDay by rememberSaveable {
        mutableStateOf(initial?.expiryEpochDay ?: LocalDate.now().plusDays(7).toEpochDay())
    }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val qty = quantity.toIntOrNull()
    val w = weight.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && qty != null && qty >= 1 && w != null && w > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Add item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                enabled = valid,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = {
                    val base = initial ?: Item(name = "", quantity = 1, weight = 1.0, unit = "g", expiryEpochDay = 0)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            quantity = qty!!,
                            weight = w!!,
                            unit = unit,
                            expiryEpochDay = expiryDay,
                            category = category,
                            notes = notes.trim(),
                            // Re-arm reminders if the date changed.
                            notifiedStage = if (initial != null && initial.expiryEpochDay == expiryDay) {
                                initial.notifiedStage
                            } else 0,
                        ),
                    )
                },
            ) { Text(if (initial == null) "Add item" else "Save changes") }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Item name") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            SectionLabel("Amount")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it.filter(Char::isDigit).take(4) },
                    label = { Text("Count") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
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

            SectionLabel("Expiry date")
            OutlinedCard(Modifier.fillMaxWidth().clickable { showPicker = true }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        LocalDate.ofEpochDay(expiryDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickDate("+1 week") { it.plusWeeks(1) }.let { (label, f) ->
                    AssistChip(onClick = { expiryDay = f(LocalDate.now()).toEpochDay() }, label = { Text(label) })
                }
                QuickDate("+1 month") { it.plusMonths(1) }.let { (label, f) ->
                    AssistChip(onClick = { expiryDay = f(LocalDate.now()).toEpochDay() }, label = { Text(label) })
                }
                QuickDate("+3 months") { it.plusMonths(3) }.let { (label, f) ->
                    AssistChip(onClick = { expiryDay = f(LocalDate.now()).toEpochDay() }, label = { Text(label) })
                }
                QuickDate("+1 year") { it.plusYears(1) }.let { (label, f) ->
                    AssistChip(onClick = { expiryDay = f(LocalDate.now()).toEpochDay() }, label = { Text(label) })
                }
            }

            SectionLabel("Location / category")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.all.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) "" else c },
                        label = { Text(c) },
                    )
                }
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it.take(500) }, label = { Text("Notes (optional)") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            if (initial != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.End)) {
                    Text("Delete item", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${initial.name}?") },
            text = { Text("This removes it permanently.") },
            confirmButton = { TextButton(onClick = { onDelete(initial) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis =
                LocalDate.ofEpochDay(expiryDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
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

private fun QuickDate(label: String, f: (LocalDate) -> LocalDate): Pair<String, (LocalDate) -> LocalDate> = label to f

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}
