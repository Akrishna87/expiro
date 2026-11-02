package com.akrishna87.expiro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ProductInfo(val name: String, val weight: Double?, val unit: String?)

/** Looks up a scanned barcode on Open Food Facts (public database; needs internet). */
object ProductLookup {
    suspend fun find(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        try {
            val code = barcode.filter(Char::isDigit)
            if (code.isEmpty()) return@withContext null
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,quantity")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "Expiro-Android/1.0")
            }
            try {
                if (conn.responseCode != 200) return@withContext null
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                if (json.optInt("status") != 1) return@withContext null
                val product = json.getJSONObject("product")
                val name = product.optString("product_name").trim()
                if (name.isEmpty()) return@withContext null
                val (w, u) = parseQuantity(product.optString("quantity"))
                ProductInfo(name, w, u)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** "500 g", "1,5 L", "33 cl" -> weight + unit label (cl is converted to ml). */
    internal fun parseQuantity(text: String): Pair<Double?, String?> {
        val m = Regex("""(\d+(?:[.,]\d+)?)\s*(kg|g|ml|cl|l)\b""", RegexOption.IGNORE_CASE).find(text)
            ?: return null to null
        val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null to null
        return when (m.groupValues[2].lowercase()) {
            "kg" -> n to "kg"
            "g" -> n to "g"
            "ml" -> n to "ml"
            "cl" -> n * 10 to "ml"
            "l" -> n to "l"
            else -> null to null
        }
    }
}
