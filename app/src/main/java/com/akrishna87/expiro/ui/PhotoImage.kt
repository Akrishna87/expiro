package com.akrishna87.expiro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akrishna87.expiro.data.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberPhoto(path: String?, maxPx: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, path, maxPx) {
        value = if (path == null) null else withContext(Dispatchers.IO) { Photos.load(path, maxPx)?.asImageBitmap() }
    }
    return image
}

@Composable
fun PhotoThumb(path: String, size: Dp, modifier: Modifier = Modifier) {
    val img = rememberPhoto(path, 200)
    if (img != null) {
        Image(
            bitmap = img,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Box(modifier.size(size))
    }
}
