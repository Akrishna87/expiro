package com.akrishna87.expiro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class WeightUnit(val label: String) {
    G("g"), KG("kg"), ML("ml"), L("l");

    companion object {
        fun fromLabel(label: String): WeightUnit = entries.firstOrNull { it.label == label } ?: G
    }
}

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Number of packs/pieces. */
    val quantity: Int,
    /** Weight (or volume) of ONE pack/piece. */
    val weight: Double,
    val unit: String,
    val expiryEpochDay: Long,
    /** true = still have it; false = used up / discarded (shown in History). */
    val available: Boolean = true,
    /** 0 = nothing sent, 1 = "expiring soon" sent, 2 = "expires today/expired" sent. */
    val notifiedStage: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** One of [Categories.all], or "" for none. */
    val category: String = "",
    val notes: String = "",
    /** Reserved for the photo feature. */
    val photoUri: String? = null,
) {
    val expiryDate: LocalDate get() = LocalDate.ofEpochDay(expiryEpochDay)

    fun daysLeft(today: LocalDate = LocalDate.now()): Long = expiryEpochDay - today.toEpochDay()

    fun amountText(): String = "$quantity × ${formatWeight(weight)} $unit"
}

fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) w.toLong().toString() else w.toString()

enum class ExpiryStatus { EXPIRED, SOON, FRESH }

fun Item.status(reminderDays: Int, today: LocalDate = LocalDate.now()): ExpiryStatus {
    val d = daysLeft(today)
    return when {
        d < 0 -> ExpiryStatus.EXPIRED
        d <= reminderDays -> ExpiryStatus.SOON
        else -> ExpiryStatus.FRESH
    }
}

object Categories {
    val all = listOf("Fridge", "Freezer", "Pantry", "Medicine", "Cosmetics", "Other")
}
