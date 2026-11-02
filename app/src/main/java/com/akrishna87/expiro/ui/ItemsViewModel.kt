package com.akrishna87.expiro.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akrishna87.expiro.ExpiroApp
import com.akrishna87.expiro.data.Backup
import com.akrishna87.expiro.data.ExpiryStatus
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.Photos
import com.akrishna87.expiro.data.SettingsStore
import com.akrishna87.expiro.data.status
import com.akrishna87.expiro.work.Notifications
import com.akrishna87.expiro.work.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

enum class StatusFilter(val label: String) { ALL("All"), EXPIRED("Expired"), SOON("Soon"), FRESH("Fresh") }

enum class SortOrder(val label: String) {
    EXPIRY("Expiry date"), NAME("Name"), RECENT("Recently added")
}

data class FilterState(
    val query: String = "",
    val status: StatusFilter = StatusFilter.ALL,
    val category: String? = null,
    val sort: SortOrder = SortOrder.EXPIRY,
)

class ItemsViewModel(app: Application) : AndroidViewModel(app) {
    private val expiro = app as ExpiroApp
    private val dao = expiro.db.itemDao()
    private val settings = expiro.settings

    /** Every item still available, unfiltered (used for summary counts). */
    val active: StateFlow<List<Item>> =
        dao.observeAvailable().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Item>> =
        dao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminderDays: StateFlow<Int> =
        settings.reminderDays.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_REMINDER_DAYS,
        )

    val reminderTime: StateFlow<LocalTime> =
        settings.reminderTime.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalTime.of(SettingsStore.DEFAULT_HOUR, 0),
        )

    val filter = MutableStateFlow(FilterState())

    /** Items after search / filter / sort are applied. */
    val visible: StateFlow<List<Item>> =
        combine(active, filter, reminderDays) { items, f, days ->
            val q = f.query.trim()
            val filtered = items.filter { item ->
                val matchesQuery = q.isEmpty() ||
                    item.name.contains(q, ignoreCase = true) ||
                    item.notes.contains(q, ignoreCase = true) ||
                    item.category.contains(q, ignoreCase = true)
                val matchesStatus = when (f.status) {
                    StatusFilter.ALL -> true
                    StatusFilter.EXPIRED -> item.status(days) == ExpiryStatus.EXPIRED
                    StatusFilter.SOON -> item.status(days) == ExpiryStatus.SOON
                    StatusFilter.FRESH -> item.status(days) == ExpiryStatus.FRESH
                }
                val matchesCategory = f.category == null || item.category == f.category
                matchesQuery && matchesStatus && matchesCategory
            }
            when (f.sort) {
                SortOrder.EXPIRY -> filtered.sortedWith(compareBy({ it.expiryEpochDay }, { it.name.lowercase() }))
                SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
                SortOrder.RECENT -> filtered.sortedByDescending { it.createdAt }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateFilter(change: (FilterState) -> FilterState) = filter.update(change)

    fun save(item: Item) = viewModelScope.launch {
        if (item.id == 0L) dao.insert(item) else dao.update(item)
        ReminderScheduler.checkNow(expiro)
    }

    fun setAvailable(item: Item, available: Boolean) = viewModelScope.launch {
        dao.setAvailable(item.id, available)
        if (!available) Notifications.cancel(expiro, item.id)
    }

    fun delete(item: Item) = viewModelScope.launch {
        dao.delete(item)
        Photos.delete(item.photoUri)
        Notifications.cancel(expiro, item.id)
    }

    fun setReminderDays(days: Int) = viewModelScope.launch {
        settings.setReminderDays(days)
        ReminderScheduler.checkNow(expiro)
    }

    fun setReminderTime(time: LocalTime) = viewModelScope.launch {
        settings.setReminderTime(time)
        ReminderScheduler.schedule(expiro, time)
    }

    /** Writes every item to [uri] as JSON. [onDone] gets the number of items or the error. */
    fun exportBackup(uri: Uri, onDone: (Result<Int>) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val items = dao.getAll()
                val out = expiro.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot write file")
                out.use { it.write(Backup.toJson(items).toByteArray()) }
                items.size
            }
        }
        onDone(result)
    }

    /** Adds items from a backup, skipping ones that already exist. [onDone] gets the number added. */
    fun importBackup(uri: Uri, onDone: (Result<Int>) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val text = expiro.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: error("Cannot read file")
                val incoming = Backup.parse(text)
                fun key(i: Item) = listOf(i.name.lowercase(), i.expiryEpochDay, i.quantity, i.weight, i.unit, i.available)
                val existing = dao.getAll().map(::key).toMutableSet()
                var added = 0
                for (item in incoming) {
                    if (existing.add(key(item))) {
                        dao.insert(item)
                        added++
                    }
                }
                added
            }
        }
        if (result.getOrDefault(0) > 0) ReminderScheduler.checkNow(expiro)
        onDone(result)
    }
}
