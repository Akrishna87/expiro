package com.akrishna87.expiro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.SettingsStore

private enum class Tab(val label: String, val emoji: String) {
    ITEMS("Items", "📦"), HISTORY("History", "🕘"), SETTINGS("Settings", "⚙️")
}

/** null = list; EditorTarget(null) = new item; EditorTarget(item) = edit. */
private data class EditorTarget(val item: Item?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiroRoot(vm: ItemsViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.ITEMS) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    val active by vm.active.collectAsState()
    val history by vm.history.collectAsState()
    val reminderDays by vm.reminderDays.collectAsState()

    editor?.let { target ->
        BackHandler { editor = null }
        EditorScreen(
            initial = target.item,
            onCancel = { editor = null },
            onSave = { vm.save(it); editor = null },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expiro") }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.emoji) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.ITEMS) {
                FloatingActionButton(onClick = { editor = EditorTarget(null) }) {
                    Text("+ Add", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.ITEMS -> ItemList(
                    list = active,
                    emptyText = "Nothing tracked yet. Tap “+ Add” to add your first item.",
                    reminderDays = reminderDays,
                    primaryLabel = "Used up",
                    onPrimary = { vm.setAvailable(it, false) },
                    onEdit = { editor = EditorTarget(it) },
                    onDelete = { vm.delete(it) },
                )
                Tab.HISTORY -> ItemList(
                    list = history,
                    emptyText = "Items you mark as used up will show here.",
                    reminderDays = reminderDays,
                    primaryLabel = "Restore",
                    onPrimary = { vm.setAvailable(it, true) },
                    onEdit = null,
                    onDelete = { vm.delete(it) },
                )
                Tab.SETTINGS -> SettingsScreen(reminderDays, vm::setReminderDays)
            }
        }
    }
}

@Composable
private fun ItemList(
    list: List<Item>,
    emptyText: String,
    reminderDays: Int,
    primaryLabel: String,
    onPrimary: (Item) -> Unit,
    onEdit: ((Item) -> Unit)?,
    onDelete: (Item) -> Unit,
) {
    if (list.isEmpty()) {
        Text(emptyText, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
        return
    }
    var toDelete by remember { mutableStateOf<Item?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(list, key = { it.id }) { item ->
            ItemCard(item, reminderDays, primaryLabel, { onPrimary(item) }, onEdit?.let { { it(item) } }, { toDelete = item })
        }
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${item.name}?") },
            text = { Text("This removes it permanently.") },
            confirmButton = { TextButton(onClick = { onDelete(item); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ItemCard(
    item: Item,
    reminderDays: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val days = item.daysLeft()
    val (status, color) = when {
        !item.available -> "Expiry ${item.expiryDate}" to MaterialTheme.colorScheme.onSurfaceVariant
        days < 0 -> "Expired ${-days} day${if (-days == 1L) "" else "s"} ago" to Color(0xFFC62828)
        days == 0L -> "Expires today" to Color(0xFFC62828)
        days == 1L -> "Expires tomorrow" to Color(0xFFEF6C00)
        days <= reminderDays -> "Expires in $days days" to Color(0xFFEF6C00)
        else -> "Expires in $days days" to Color(0xFF2E7D32)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.amountText(), style = MaterialTheme.typography.bodyMedium)
            Text(status, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (item.available) Text("Date: ${item.expiryDate}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
                OutlinedButton(onClick = onPrimary) { Text(primaryLabel) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(reminderDays: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Remind me before expiry", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { onChange(reminderDays - 1) }, enabled = reminderDays > 0) { Text("−") }
            Text(
                if (reminderDays == 1) "1 day" else "$reminderDays days",
                style = MaterialTheme.typography.titleLarge,
            )
            Button(onClick = { onChange(reminderDays + 1) }, enabled = reminderDays < SettingsStore.MAX_REMINDER_DAYS) { Text("+") }
        }
        Text(
            "You'll get one reminder when an item is within this many days of expiring, " +
                "and another on the expiry day. Checks run daily around 9:00.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
