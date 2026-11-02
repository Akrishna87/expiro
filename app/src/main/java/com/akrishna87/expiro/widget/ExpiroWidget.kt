package com.akrishna87.expiro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.akrishna87.expiro.ExpiroApp
import com.akrishna87.expiro.MainActivity
import com.akrishna87.expiro.R
import com.akrishna87.expiro.data.ExpiryStatus
import com.akrishna87.expiro.data.status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Home-screen widget listing the four items that expire first. */
class ExpiroWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val rows = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4)
        private val names = intArrayOf(R.id.name1, R.id.name2, R.id.name3, R.id.name4)
        private val statuses = intArrayOf(R.id.status1, R.id.status2, R.id.status3, R.id.status4)

        /** Redraws all placed widgets; call after items change. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExpiroWidget::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { render(context, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as ExpiroApp
            val reminderDays = app.settings.reminderDays.first()
            val items = app.db.itemDao().getAvailable()
                .sortedWith(compareBy({ it.expiryEpochDay }, { it.name.lowercase() }))
                .take(rows.size)

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_expiro)
            views.setOnClickPendingIntent(R.id.widget_root, open)

            rows.forEachIndexed { i, row ->
                val item = items.getOrNull(i)
                if (item == null) {
                    if (i == 0) {
                        views.setViewVisibility(row, View.VISIBLE)
                        views.setTextViewText(names[0], context.getString(R.string.widget_empty))
                        views.setTextViewText(statuses[0], "")
                    } else {
                        views.setViewVisibility(row, View.GONE)
                    }
                } else {
                    val d = item.daysLeft()
                    views.setViewVisibility(row, View.VISIBLE)
                    views.setTextViewText(names[i], item.name)
                    views.setTextViewText(
                        statuses[i],
                        when {
                            d < 0 -> "overdue ${-d}d"
                            d == 0L -> "today"
                            d == 1L -> "tomorrow"
                            else -> "in ${d}d"
                        },
                    )
                    views.setTextColor(
                        statuses[i],
                        when (item.status(reminderDays)) {
                            ExpiryStatus.EXPIRED -> Color.parseColor("#D94A3D")
                            ExpiryStatus.SOON -> Color.parseColor("#E08A00")
                            ExpiryStatus.FRESH -> Color.parseColor("#2E9E55")
                        },
                    )
                }
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
