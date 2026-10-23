/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.systemui.qs.shared.style.glassBlurRegion
import com.android.systemui.qs.shared.style.liquidGlassRim

val CosAutoBrightnessTint = Color(0xFFFFC000)
val CosVolumeTint = Color(0xFF2F6BFF)
private val CosSliderFill = Color(0xF7FFFFFF)
private val CosIconOnGlass = Color(0xE6FFFFFF)
private val CosIconOnFill = Color(0xB3000000)

@Composable
fun Modifier.cosGlass(shape: Shape, active: Boolean = false): Modifier =
    clip(shape)
        .glassBlurRegion(
            shape,
            material = (if (active) 2 else 1) + if (isSystemInDarkTheme()) 0 else 2,
        )
        .cosRim(shape, active)

@Composable
private fun Modifier.cosRim(shape: Shape, active: Boolean): Modifier {
    val rim = remember { CosHooks.createRim() } ?: return liquidGlassRim(shape)
    val dark = isSystemInDarkTheme()
    return drawWithCache {
        val radius = when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
            is Outline.Rectangle -> 0f
            else -> size.minDimension / 2f
        }
        CosHooks.updateRim(rim, size.width, size.height, radius, active, dark)
        val brush = ShaderBrush(rim)
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.Plus)
        }
    }
}

@Composable
fun CosGlassSlider(
    width: Dp,
    height: Dp,
    fraction: Float,
    icon: Painter,
    iconTint: Color?,
    slider: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val iconSize = 26.dp
    val iconBottom = 18.dp
    val covered = fraction * height.value > iconBottom.value + iconSize.value / 2
    Box(Modifier.size(width, height).cosGlass(shape)) {
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fraction.coerceIn(0f, 1f))
                .background(CosSliderFill)
        )
        slider()
        Image(
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                iconTint ?: if (covered) CosIconOnFill else CosIconOnGlass
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = iconBottom)
                .size(iconSize),
        )
    }
}
