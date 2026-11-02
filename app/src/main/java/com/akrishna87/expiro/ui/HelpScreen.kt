package com.akrishna87.expiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class HelpSection(val title: String, val body: String)

private val sections = listOf(
    HelpSection(
        "Quick start",
        "1. On the Items tab, tap the + button.\n" +
            "2. Type the item name.\n" +
            "3. Enter how many you have (Count) and the weight of ONE pack (Weight each), then pick the unit: g, kg, ml or l.\n" +
            "4. Set the expiry date. Tap the date to open the calendar, or use the quick buttons (+1 week, +1 month...).\n" +
            "5. Tap Add item.\n\n" +
            "Example: 3 packs of rice, 500 g each → Count 3, Weight each 500, unit g.",
    ),
    HelpSection(
        "Reading the Items screen",
        "• The three tiles at the top count your items: Expired, Soon and Fresh. Tap a tile to show only those items.\n" +
            "• Red = already expired.\n" +
            "• Orange = expiring soon: today, or within your reminder window (3 days by default).\n" +
            "• Green = still fresh.\n" +
            "• The badge on the right shows the number of days left (or days overdue).\n" +
            "• Tap an item to edit it. Tap the ✓ button when you've used it up.",
    ),
    HelpSection(
        "Used up items and History",
        "• Tap ✓ on an item when it's finished or thrown away. It moves to the History tab and its reminders stop.\n" +
            "• In History you can Restore an item (for example if you tapped ✓ by mistake) or Delete it for good.\n" +
            "• You can also tap \"Mark as used\" directly on an expiry notification.\n" +
            "• To delete an item you still have, open it and tap Delete item at the bottom.",
    ),
    HelpSection(
        "Search, filter and sort",
        "• The search box looks in names, notes and categories.\n" +
            "• Use the chips under it to filter by All, Expired, Soon or Fresh. Once you give items a category, category chips appear too; tap one to filter, tap it again to clear.\n" +
            "• \"Sort\" changes the order: by expiry date, by name, or by recently added.",
    ),
    HelpSection(
        "Reminders (notifications)",
        "• Expiro sends one notification when an item gets within your reminder window, and another on the expiry day.\n" +
            "• Go to Settings to choose how many days before expiry you want to be reminded, and what time of day.\n" +
            "• When Android asks, allow notifications. Without that permission you won't get reminders.\n\n" +
            "Not getting notifications?\n" +
            "• Open your phone's Settings → Apps → Expiro → Notifications and make sure they're on.\n" +
            "• On some phones (Samsung, Xiaomi, OnePlus and others), battery saving can stop background checks. Set Expiro's battery usage to Unrestricted.",
    ),
    HelpSection(
        "Categories and notes",
        "When adding or editing an item you can tag where it's kept (Fridge, Freezer, Pantry, Medicine, Cosmetics or Other) and write a note, for example \"opened on 3 Nov\" or \"use first\". Tap a selected category again to clear it.",
    ),
    HelpSection(
        "Photos",
        "In the add/edit screen, use Camera to take a picture or Gallery to pick one. The photo shows as a small thumbnail in your list. Tap Remove to take it off.",
    ),
    HelpSection(
        "Scan a barcode",
        "• In the add screen, tap \"Scan barcode to fill in details\" and point the camera at the product's barcode.\n" +
            "• If the product is found, its name and weight are filled in for you. Normal barcodes don't contain the expiry date, so set it yourself or use \"Read date from pack\" below.\n" +
            "• This needs internet and Google Play services, and not every product is in the database. If it isn't found, just type the details yourself.",
    ),
    HelpSection(
        "Read the expiry date with the camera",
        "• In the add/edit screen, tap \"Read date from pack (camera)\" and take a clear, close photo of the printed date, such as \"EXP 12/11/2026\" or \"BEST BEFORE 12 NOV 26\".\n" +
            "• The date is filled in for you. Always check it matches the pack before saving.\n" +
            "• Dates are read as day/month/year. If several dates are printed, it prefers the one after EXP / USE BY / BEST BEFORE, otherwise the latest.\n" +
            "• Works best in good light with the date flat and in focus. The first time, it may need internet to download the text reader. If it can't find a date, set it by hand.",
    ),
    HelpSection(
        "Home-screen widget",
        "Long-press an empty spot on your home screen, choose Widgets, find Expiro and drag it onto the screen. It lists the items that expire first, and tapping it opens the app.",
    ),
    HelpSection(
        "Backup and moving to a new phone",
        "• Your items are stored only on this phone. If you uninstall the app or lose the phone, they're gone.\n" +
            "• Settings → Export saves all your items to a file. Keep it somewhere safe, such as Google Drive.\n" +
            "• On a new phone, install Expiro, then Settings → Import and pick that file. Items you already have aren't duplicated.\n" +
            "• Photos are not included in the backup.",
    ),
    HelpSection(
        "Updating the app",
        "When a new version is available, download the new file from the same link and install it over the old one. Your items stay. Don't uninstall first, or your data will be deleted.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Tap a topic to read more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sections.forEachIndexed { index, section ->
                HelpCard(section, startExpanded = index == 0)
            }
            Text(
                "Expiro keeps everything on your phone. No account is needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun HelpCard(section: HelpSection, startExpanded: Boolean) {
    var expanded by rememberSaveable(section.title) { mutableStateOf(startExpanded) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            if (expanded) {
                Text(section.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
