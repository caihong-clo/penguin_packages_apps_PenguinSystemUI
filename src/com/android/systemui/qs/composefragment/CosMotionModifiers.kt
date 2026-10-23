/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

private const val STAGGER_MS = 16L

@Composable
fun Modifier.cosModuleReveal(index: Int): Modifier {
    if (!rememberCosExperience()) return this
    var scale by remember { mutableFloatStateOf(0.8f) }
    var alpha by remember { mutableFloatStateOf(0f) }
    val scaleSpring = remember { CosHooks.newSpring(0.8f) { scale = it } }
    val alphaSpring = remember { CosHooks.newSpring(0f) { alpha = it } }
    if (scaleSpring == null || alphaSpring == null) return this

    LaunchedEffect(Unit) {
        delay(index * STAGGER_MS)
        scaleSpring.animateTo(1f, response = 0.2f)
        alphaSpring.animateTo(1f, response = 0.2f)
    }

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
