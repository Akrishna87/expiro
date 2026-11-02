package com.akrishna87.expiro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akrishna87.expiro.ExpiroApp
import com.akrishna87.expiro.data.Item
import com.akrishna87.expiro.data.SettingsStore
import com.akrishna87.expiro.work.Notifications
import com.akrishna87.expiro.work.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(app: Application) : AndroidViewModel(app) {
    private val expiro = app as ExpiroApp
    private val dao = expiro.db.itemDao()
    private val settings = expiro.settings

    val active: StateFlow<List<Item>> =
        dao.observeAvailable().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Item>> =
        dao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminderDays: StateFlow<Int> =
        settings.reminderDays.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_REMINDER_DAYS,
        )

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
        Notifications.cancel(expiro, item.id)
    }

    fun setReminderDays(days: Int) = viewModelScope.launch {
        settings.setReminderDays(days)
        ReminderScheduler.checkNow(expiro)
    }
}
