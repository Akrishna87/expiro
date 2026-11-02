package com.akrishna87.expiro.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** JSON export/import of items. Photos are not included. */
object Backup {
    fun toJson(items: List<Item>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name)
                    .put("quantity", it.quantity)
                    .put("weight", it.weight)
                    .put("unit", it.unit)
                    .put("expiry", it.expiryDate.toString())
                    .put("available", it.available)
                    .put("category", it.category)
                    .put("notes", it.notes)
                    .put("createdAt", it.createdAt),
            )
        }
        return JSONObject()
            .put("app", "expiro")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("items", arr)
            .toString(2)
    }

    /** @throws Exception if the text is not a valid Expiro backup. */
    fun parse(text: String): List<Item> {
        val root = JSONObject(text)
        require(root.optString("app") == "expiro") { "Not an Expiro backup" }
        val arr = root.getJSONArray("items")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val weight = o.optDouble("weight", 1.0)
            Item(
                name = o.getString("name").trim(),
                quantity = o.optInt("quantity", 1).coerceAtLeast(1),
                weight = if (weight > 0) weight else 1.0,
                unit = WeightUnit.fromLabel(o.optString("unit", "g")).label,
                expiryEpochDay = LocalDate.parse(o.getString("expiry")).toEpochDay(),
                available = o.optBoolean("available", true),
                category = o.optString("category", "").takeIf { it in Categories.all } ?: "",
                notes = o.optString("notes", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }
}
