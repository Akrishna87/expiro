package com.akrishna87.expiro

import android.app.Application
import com.akrishna87.expiro.data.AppDatabase
import com.akrishna87.expiro.data.SettingsStore
import com.akrishna87.expiro.work.Notifications
import com.akrishna87.expiro.work.ReminderScheduler

class ExpiroApp : Application() {
    val db by lazy { AppDatabase.create(this) }
    val settings by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        ReminderScheduler.schedule(this)
    }
}
