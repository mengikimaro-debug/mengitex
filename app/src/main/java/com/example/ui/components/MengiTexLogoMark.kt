package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Volatile
private var cachedLogoImageBitmap: ImageBitmap? = null

private fun getDownsampledLogoBitmap(context: Context): ImageBitmap? {
    cachedLogoImageBitmap?.let { return it }
    return runCatching {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = 4 // 1024x1024 -> crisp 256x256 (~16x faster decode & GPU upload)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(context.resources, R.drawable.app_icon_mengitex_ui_1795208513143, opts)
            ?.asImageBitmap()
            ?.also { cachedLogoImageBitmap = it }
    }.getOrNull()
}

/**
 * Solid Android 17 MengiTex brand emblem with zero glow or border.
 */
@Composable
fun MengiTexLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = (size * 0.28f),
    showGlow: Boolean = false
) {
    val shape = RoundedCornerShape(cornerRadius)
    val context = LocalContext.current
    val sampledBitmap = remember(context) { getDownsampledLogoBitmap(context) }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (sampledBitmap != null) {
            Image(
                bitmap = sampledBitmap,
                contentDescription = "MengiTex logo",
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(shape)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.app_icon_mengitex_ui_1795208513143),
                contentDescription = "MengiTex logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(shape)
            )
        }
    }
}
