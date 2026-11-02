package com.akrishna87.expiro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Stores item photos as downscaled JPEGs in the app's private storage. */
object Photos {
    private const val MAX_SIDE = 1280
    private val cache = LruCache<String, Bitmap>(40)

    private fun dir(ctx: Context) = File(ctx.filesDir, "photos").apply { mkdirs() }

    /** A temp file for the camera app to write into. */
    fun newCameraFile(ctx: Context): File =
        File(File(ctx.cacheDir, "camera").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")

    fun uriFor(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

    /** Copies, rotates and downscales the image at [uri]; returns the saved file path or null on failure. */
    suspend fun import(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val cr = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2

            val decoded = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            val orientation = cr.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            }
            val scale = min(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
            m.postScale(scale, scale)
            val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)

            val file = File(dir(ctx), "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun load(path: String, maxPx: Int): Bitmap? {
        val key = "$path@$maxPx"
        cache.get(key)?.let { return it }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.also { cache.put(key, it) }
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path == null) return
        try {
            val f = File(path)
            if (f.parentFile?.name == "photos") f.delete()
        } catch (_: Exception) {
        }
    }
}
