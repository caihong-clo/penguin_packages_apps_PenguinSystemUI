/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.keyguard.ui.composable.media

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.android.systemui.common.shared.model.Icon as CommonIcon
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TopBand = 186.dp
private val BottomBand = 346.dp
private const val BLUR_RADIUS = 120f
private const val BLUR_CURVE = 1.4f
private const val MASK_ALPHA = 0.4f
private const val BAND_ALPHA = 0.6f

@Composable
internal fun CosImmersiveArt(artwork: CommonIcon, modifier: Modifier = Modifier) {
    val maskColor by
        produceState(Color.Transparent, artwork) {
            value = withContext(Dispatchers.Default) { cosMaskColor(artwork) }
        }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        val base: @Composable (Modifier) -> Unit = { layer ->
            Box(layer.fillMaxSize()) {
                ArtStack(artwork, width, height * 0.2f)
                Box(Modifier.fillMaxSize().background(maskColor.copy(alpha = MASK_ALPHA)))
            }
        }
        base(Modifier)
        base(
            Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    renderEffect = BlurEffect(BLUR_RADIUS, BLUR_RADIUS, TileMode.Clamp)
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        blurMask(size.height, TopBand.toPx(), BottomBand.toPx()),
                        blendMode = BlendMode.DstIn,
                    )
                }
        )
        Box(
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(TopBand)
                .background(
                    Brush.verticalGradient(
                        listOf(maskColor.copy(alpha = BAND_ALPHA), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BottomBand)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, maskColor.copy(alpha = BAND_ALPHA))
                    )
                )
        )
    }
}

@Composable
private fun ArtStack(artwork: CommonIcon, side: Dp, top: Dp) {
    Box(Modifier.fillMaxSize()) {
        Artwork(artwork, Modifier.offset(y = top - side).size(side).graphicsLayer { scaleY = -1f })
        Artwork(artwork, Modifier.offset(y = top).size(side))
        Artwork(artwork, Modifier.offset(y = top + side).size(side).graphicsLayer { scaleY = -1f })
    }
}

private fun blurMask(height: Float, top: Float, bottom: Float): Brush {
    val steps = 8
    val stops = ArrayList<Pair<Float, Color>>()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        stops += (t * top / height) to Color.Black.copy(alpha = (1f - t).pow(BLUR_CURVE))
    }
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        stops += ((height - bottom + t * bottom) / height) to Color.Black.copy(alpha = t.pow(BLUR_CURVE))
    }
    return Brush.verticalGradient(*stops.toTypedArray())
}

private fun cosMaskColor(artwork: CommonIcon): Color {
    val drawable = (artwork as? CommonIcon.Loaded)?.drawable ?: return Color.Transparent
    val bitmap = drawable.toBitmap(36, 36, Bitmap.Config.ARGB_8888)
    val rgb = Palette.from(bitmap).generate().dominantSwatch?.rgb ?: return Color.Transparent
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(rgb, hsv)
    hsv[1] = maxOf(hsv[1], 0.6f)
    hsv[2] = 0.8f
    return Color(ColorUtils.setAlphaComponent(android.graphics.Color.HSVToColor(hsv), 255))
}
