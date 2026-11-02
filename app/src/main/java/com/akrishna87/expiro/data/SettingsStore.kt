package com.akrishna87.expiro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val reminderDaysKey = intPreferencesKey("reminder_days")

    val reminderDays: Flow<Int> = context.dataStore.data.map { it[reminderDaysKey] ?: DEFAULT_REMINDER_DAYS }

    suspend fun setReminderDays(days: Int) {
        context.dataStore.edit { it[reminderDaysKey] = days.coerceIn(0, MAX_REMINDER_DAYS) }
    }

    companion object {
        const val DEFAULT_REMINDER_DAYS = 3
        const val MAX_REMINDER_DAYS = 30
    }
}
