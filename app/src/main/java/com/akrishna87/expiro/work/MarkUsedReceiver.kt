package com.akrishna87.expiro.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.akrishna87.expiro.ExpiroApp
import com.akrishna87.expiro.widget.ExpiroWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Mark as used" action on an expiry notification. */
class MarkUsedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (id < 0) return
        Notifications.cancel(context, id)
        val pending = goAsync()
        val app = context.applicationContext as ExpiroApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.db.itemDao().setAvailable(id, false)
                ExpiroWidget.refresh(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
