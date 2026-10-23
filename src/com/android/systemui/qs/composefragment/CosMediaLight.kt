/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.google.ux.material.libmonet.hct.Hct
import com.google.ux.material.libmonet.scheme.SchemeContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Modifier.cosMediaLight(artwork: Drawable?, corner: Dp, artworkCenter: Offset): Modifier {
    if (!rememberCosExperience()) return this
    val context = LocalContext.current
    val light = remember { CosHooks.createMediaLight(context) } ?: return this
    val night = isSystemInDarkTheme()
    val density = LocalDensity.current
    var frame by remember { mutableIntStateOf(0) }

    DisposableEffect(light) {
        light.callback =
            object : Drawable.Callback {
                override fun invalidateDrawable(who: Drawable) {
                    frame++
                }

                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}

                override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
            }
        onDispose { light.callback = null }
    }

    val color = rememberCosMediaColor(artwork)

    var measured by remember { mutableIntStateOf(0) }
    LaunchedEffect(color, night, measured) {
        if (color == 0) {
            CosHooks.clearMediaLight(light)
            return@LaunchedEffect
        }
        CosHooks.applyMediaLight(light, color, night)
        if (measured != 0) {
            CosHooks.applyMediaLightSpread(
                light,
                artworkCenter.x - light.bounds.width() / 2f,
                artworkCenter.y - light.bounds.height() / 2f,
            )
        }
    }

    return onSizeChanged {
            light.setBounds(0, 0, it.width, it.height)
            with(density) { CosHooks.setMediaLightCorner(light, corner.toPx()) }
            measured = it.width * 31 + it.height
        }
        .drawWithContent {
            frame
            drawIntoCanvas { light.draw(it.nativeCanvas) }
            drawContent()
        }
}

@Composable
fun rememberCosMediaColor(artwork: Drawable?): Int {
    val night = isSystemInDarkTheme()
    val color by
        produceState(0, artwork, night) {
            value = withContext(Dispatchers.Default) { artwork?.let { cosMediaColor(it, night) } ?: 0 }
        }
    return color
}

private fun cosMediaColor(artwork: Drawable, night: Boolean): Int {
    val bitmap: Bitmap = artwork.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val seed = WallpaperColors.fromBitmap(bitmap).primaryColor.toArgb()
    return SchemeContent(Hct.fromInt(seed), night, 0.0).primaryPalette.tone(80)
}
