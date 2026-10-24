/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.qs.composefragment

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ColorOS draws its tile icons as Lottie animations that play when a tile turns on or off. Builds
 * that ship them put them in [DIR]; without them tiles keep their own icons.
 */
internal object CosTileLottie {
    private const val DIR = "/system_ext/etc/cos/qs"

    private val compositions = ConcurrentHashMap<String, Result<LottieComposition>>()

    /** The animation ColorOS gives a tile, by the name its files start with. */
    private fun prefix(spec: String): String? =
        when (spec) {
            "wifi", "internet" -> "qs_wifi_lottie"
            "bt" -> "qs_bluetooth_lottie"
            "cell" -> "qs_cellular_lottie"
            "airplane" -> "qs_airplane_lottie"
            "dnd" -> "qs_dnd_lottie"
            "location" -> "qs_location_lottie"
            "rotation" -> "qs_rotation_lottie"
            "flashlight" -> "qs_flashlight_lottie"
            "hotspot" -> "qs_hotspot_lottie"
            "nfc" -> "qs_nfc_lottie"
            "night" -> "qs_eyeprotect_lottie"
            else -> null
        }

    /** Whether the tile's own icon says more than ColorOS's, as signal bars do. */
    fun keepsOwnIcon(spec: String) = spec == "wifi" || spec == "internet" || spec == "cell"

    /**
     * The animation a tile plays on turning on ([on]) or off. Turning off has none for some tiles;
     * the start of their turning-on animation is how they look while off.
     */
    suspend fun composition(spec: String, on: Boolean): LottieComposition? {
        val prefix = prefix(spec) ?: return null
        val name = "${prefix}_${if (on) "active" else "inactive"}_sep_dual_light.json"
        return compositions
            .getOrPut(name) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val json = File(DIR, name).readText()
                        LottieCompositionFactory.fromJsonStringSync(json, name).value!!
                    }
                }
            }
            .getOrNull()
    }
}

/**
 * A tile's icon drawn with ColorOS's animation: at rest the end of it for the tile's state, and
 * played through when the state changes, crossfading from the old colour to the new. Returns false,
 * drawing nothing, while there is no animation for the tile, so it can draw its own icon instead.
 */
@Composable
internal fun cosTileLottie(
    spec: String?,
    active: Boolean,
    offColor: Color,
    onColor: Color,
    size: Dp,
): Boolean {
    if (spec == null) return false
    var shown by remember(spec) { mutableStateOf<LottieComposition?>(null) }
    var from by remember(spec) { mutableStateOf(active) }
    var playing by remember(spec) { mutableStateOf(false) }
    val progress = remember(spec) { Animatable(1f) }
    LaunchedEffect(spec, active) {
        val forward = CosTileLottie.composition(spec, true) ?: run {
            shown = null
            return@LaunchedEffect
        }
        val changed = from != active
        from = active
        val turn = if (active) forward else CosTileLottie.composition(spec, false)
        if (changed && turn != null) {
            shown = turn
            playing = true
            progress.snapTo(0f)
            progress.animateTo(1f, tween(turn.duration.toInt(), easing = LinearEasing))
            playing = false
        }
        // At rest: the end of turning on, or the start of it while off.
        shown = forward
        progress.snapTo(if (active) 1f else 0f)
    }
    val composition = shown ?: return false
    if (!playing && CosTileLottie.keepsOwnIcon(spec)) return false
    // ColorOS colours its icons in two layers, one shown at the start and one at the end.
    val start = if (playing) (if (active) offColor else onColor) else offColor
    val end = if (playing) (if (active) onColor else offColor) else onColor
    val properties =
        rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(LottieProperty.COLOR, start.toArgb(), "**", "startFill", "**"),
            rememberLottieDynamicProperty(LottieProperty.COLOR, end.toArgb(), "**", "endFill", "**"),
            rememberLottieDynamicProperty(
                LottieProperty.STROKE_COLOR,
                start.toArgb(),
                "**",
                "startStroke",
                "**",
            ),
            rememberLottieDynamicProperty(
                LottieProperty.STROKE_COLOR,
                end.toArgb(),
                "**",
                "endStroke",
                "**",
            ),
        )
    LottieAnimation(
        composition = composition,
        progress = { progress.value },
        dynamicProperties = properties,
        modifier = Modifier.size(size),
    )
    return true
}
