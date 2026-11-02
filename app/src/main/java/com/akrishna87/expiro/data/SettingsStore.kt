package com.akrishna87.expiro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val reminderDaysKey = intPreferencesKey("reminder_days")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")

    val reminderDays: Flow<Int> = context.dataStore.data.map { it[reminderDaysKey] ?: DEFAULT_REMINDER_DAYS }

    val reminderTime: Flow<LocalTime> = context.dataStore.data.map {
        LocalTime.of(it[reminderHourKey] ?: DEFAULT_HOUR, it[reminderMinuteKey] ?: 0)
    }

    suspend fun setReminderDays(days: Int) {
        context.dataStore.edit { it[reminderDaysKey] = days.coerceIn(0, MAX_REMINDER_DAYS) }
    }

    suspend fun setReminderTime(time: LocalTime) {
        context.dataStore.edit {
            it[reminderHourKey] = time.hour
            it[reminderMinuteKey] = time.minute
        }
    }

    companion object {
        const val DEFAULT_REMINDER_DAYS = 3
        const val MAX_REMINDER_DAYS = 30
        const val DEFAULT_HOUR = 9
    }
}
