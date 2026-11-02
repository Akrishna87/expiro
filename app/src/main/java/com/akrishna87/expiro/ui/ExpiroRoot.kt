package com.akrishna87.expiro.ui

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akrishna87.expiro.data.ExpiryStatus
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.SettingsStore
import com.akrishna87.expiro.data.status
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Tab(val label: String, val icon: ImageVector) {
    ITEMS("Items", Icons.Default.Home),
    HISTORY("History", Icons.Default.DateRange),
    SETTINGS("Settings", Icons.Default.Settings),
}

private data class EditorTarget(val item: Item?)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiroRoot(vm: ItemsViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.ITEMS) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    val all by vm.active.collectAsState()
    val visible by vm.visible.collectAsState()
    val history by vm.history.collectAsState()
    val reminderDays by vm.reminderDays.collectAsState()
    val reminderTime by vm.reminderTime.collectAsState()
    val filter by vm.filter.collectAsState()
    val helpSeen by vm.helpSeen.collectAsState()

    if (showHelp) {
        BackHandler { showHelp = false }
        HelpScreen(onBack = { showHelp = false })
        return
    }

    editor?.let { target ->
        EditorScreen(
            initial = target.item,
            onCancel = { editor = null },
            onSave = { vm.save(it); editor = null },
            onDelete = { item -> vm.delete(item); editor = null },
        )
        return
    }

    if (!helpSeen && tab == Tab.ITEMS) {
        AlertDialog(
            onDismissRequest = { vm.markHelpSeen() },
            title = { Text("Welcome to Expiro") },
            text = {
                Text(
                    "Track what you have and get reminded before it expires. " +
                        "Tap + to add your first item. You can open the help guide any time with the ⓘ button at the top.",
                )
            },
            confirmButton = { TextButton(onClick = { vm.markHelpSeen(); showHelp = true }) { Text("Show me how") } },
            dismissButton = { TextButton(onClick = { vm.markHelpSeen() }) { Text("Skip") } },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (tab == Tab.ITEMS) "Expiro" else tab.label) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.ITEMS) {
                FloatingActionButton(onClick = { editor = EditorTarget(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.ITEMS -> ItemsScreen(
                    all = all,
                    visible = visible,
                    filter = filter,
                    reminderDays = reminderDays,
                    onFilter = vm::updateFilter,
                    onOpen = { editor = EditorTarget(it) },
                    onUsedUp = { vm.setAvailable(it, false) },
                )
                Tab.HISTORY -> HistoryScreen(history, vm::setAvailable, vm::delete)
                Tab.SETTINGS -> SettingsScreen(
                    reminderDays, reminderTime, vm::setReminderDays, vm::setReminderTime,
                    onExport = vm::exportBackup, onImport = vm::importBackup,
                    onOpenHelp = { showHelp = true },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Items tab

@Composable
private fun ItemsScreen(
    all: List<Item>,
    visible: List<Item>,
    filter: FilterState,
    reminderDays: Int,
    onFilter: ((FilterState) -> FilterState) -> Unit,
    onOpen: (Item) -> Unit,
    onUsedUp: (Item) -> Unit,
) {
    val counts = remember(all, reminderDays) {
        ExpiryStatus.entries.associateWith { s -> all.count { it.status(reminderDays) == s } }
    }
    val categories = remember(all) { all.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted() }
    val grouped = filter.sort == SortOrder.EXPIRY && filter.status == StatusFilter.ALL

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(Modifier.weight(1f), "Expired", counts[ExpiryStatus.EXPIRED] ?: 0, ExpiryStatus.EXPIRED) {
                    onFilter { it.copy(status = StatusFilter.EXPIRED) }
                }
                StatCard(Modifier.weight(1f), "Soon", counts[ExpiryStatus.SOON] ?: 0, ExpiryStatus.SOON) {
                    onFilter { it.copy(status = StatusFilter.SOON) }
                }
                StatCard(Modifier.weight(1f), "Fresh", counts[ExpiryStatus.FRESH] ?: 0, ExpiryStatus.FRESH) {
                    onFilter { it.copy(status = StatusFilter.FRESH) }
                }
            }
        }
        item {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { q -> onFilter { it.copy(query = q) } },
                placeholder = { Text("Search items") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { onFilter { it.copy(query = "") } }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusFilter.entries.forEach { s ->
                    FilterChip(
                        selected = filter.status == s,
                        onClick = { onFilter { it.copy(status = s) } },
                        label = { Text(s.label) },
                    )
                }
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    categories.forEach { c ->
                        FilterChip(
                            selected = filter.category == c,
                            onClick = { onFilter { it.copy(category = if (it.category == c) null else c) } },
                            label = { Text(c) },
                        )
                    }
                }
            }
        }
        item {
            var menu by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${visible.size} item${if (visible.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    TextButton(onClick = { menu = true }) { Text("Sort: ${filter.sort.label}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        SortOrder.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label) },
                                onClick = { onFilter { it.copy(sort = s) }; menu = false },
                            )
                        }
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    if (all.isEmpty()) "Nothing tracked yet" else "No matches",
                    if (all.isEmpty()) "Tap + to add your first item." else "Try a different search or filter.",
                )
            }
        } else if (grouped) {
            val groups = ExpiryStatus.entries.map { s -> s to visible.filter { it.status(reminderDays) == s } }
            groups.filter { it.second.isNotEmpty() }.forEach { (status, list) ->
                item(key = "header-$status") {
                    Text(
                        when (status) {
                            ExpiryStatus.EXPIRED -> "Expired"
                            ExpiryStatus.SOON -> "Expiring soon"
                            ExpiryStatus.FRESH -> "Fresh"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColors(status).accent,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(list, key = { it.id }) { item -> ItemCard(item, status, onOpen, onUsedUp) }
            }
        } else {
            items(visible, key = { it.id }) { item -> ItemCard(item, item.status(reminderDays), onOpen, onUsedUp) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, count: Int, status: ExpiryStatus, onClick: () -> Unit) {
    val c = statusColors(status)
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = c.container, contentColor = c.onContainer),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("$count", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ItemCard(item: Item, status: ExpiryStatus, onOpen: (Item) -> Unit, onUsedUp: (Item) -> Unit) {
    val c = statusColors(status)
    Card(
        onClick = { onOpen(item) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(c.accent))
            if (item.photoUri != null) {
                PhotoThumb(item.photoUri, 52.dp, Modifier.padding(start = 10.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(item.amountText())
                        if (item.category.isNotEmpty()) append("  ·  ").append(item.category)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    item.expiryDate.format(dateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DaysBadge(item, c)
            FilledTonalIconButton(onClick = { onUsedUp(item) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Mark as used up")
            }
        }
    }
}

@Composable
private fun DaysBadge(item: Item, c: StatusColors) {
    val d = item.daysLeft()
    val (big, small) = when {
        d < 0 -> "${-d}" to (if (-d == 1L) "day overdue" else "days overdue")
        d == 0L -> "Today" to "expires"
        d == 1L -> "1" to "day left"
        else -> "$d" to "days left"
    }
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(c.container)
            .widthIn(min = 68.dp).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(big, style = MaterialTheme.typography.titleLarge, color = c.onContainer)
        Text(small, style = MaterialTheme.typography.labelSmall, color = c.onContainer, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// -------------------------------------------------------------- History tab

@Composable
private fun HistoryScreen(history: List<Item>, onSetAvailable: (Item, Boolean) -> Unit, onDelete: (Item) -> Unit) {
    var toDelete by remember { mutableStateOf<Item?>(null) }

    if (history.isEmpty()) {
        EmptyState("No history yet", "Items you mark as used up will show here.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(history, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.amountText(), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Expiry ${item.expiryDate.format(dateFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { toDelete = item }) { Text("Delete") }
                            OutlinedButton(onClick = { onSetAvailable(item, true) }) { Text("Restore") }
                        }
                    }
                }
            }
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

// ------------------------------------------------------------- Settings tab

@Composable
private fun SettingsScreen(
    reminderDays: Int,
    reminderTime: LocalTime,
    onDaysChange: (Int) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onExport: (Uri, (Result<Int>) -> Unit) -> Unit,
    onImport: (Uri, (Result<Int>) -> Unit) -> Unit,
    onOpenHelp: () -> Unit,
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) onExport(uri) { r ->
            Toast.makeText(
                context,
                r.fold({ "Exported $it items" }, { "Export failed: ${it.message}" }),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImport(uri) { r ->
            Toast.makeText(
                context,
                r.fold({ "Imported $it new items" }, { "That file isn't a valid Expiro backup" }),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reminders", style = MaterialTheme.typography.titleMedium)

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remind me before expiry", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (reminderDays == 1) "1 day before" else "$reminderDays days before",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { onDaysChange(reminderDays - 1) }, enabled = reminderDays > 0) { Text("−") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onDaysChange(reminderDays + 1) },
                        enabled = reminderDays < SettingsStore.MAX_REMINDER_DAYS,
                    ) { Text("+") }
                }

                Row(
                    Modifier.fillMaxWidth().clickable {
                        TimePickerDialog(
                            context,
                            { _, h, m -> onTimeChange(LocalTime.of(h, m)) },
                            reminderTime.hour, reminderTime.minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder time", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Checked once a day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        reminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    "You get one reminder when an item enters the reminder window and another on the expiry day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Save your items to a file, or restore them on a new phone. " +
                        "Importing adds items and skips ones you already have. Photos are not included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        exportLauncher.launch("expiro-backup-${java.time.LocalDate.now()}.json")
                    }) { Text("Export") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("Import") }
                }
            }
        }

        Card(
            onClick = onOpenHelp,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Help & guide", style = MaterialTheme.typography.titleMedium)
                Text(
                    "How to add items, reminders, backup and more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Expiro $version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Your data stays on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
