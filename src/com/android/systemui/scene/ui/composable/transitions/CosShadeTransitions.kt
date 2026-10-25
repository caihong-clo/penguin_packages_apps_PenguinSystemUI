/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.scene.ui.composable.transitions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.TransitionBuilder
import com.android.systemui.notifications.ui.composable.NotificationsShade
import com.android.systemui.qs.ui.composable.QuickSettingsShade
import com.android.systemui.shade.ui.composable.OverlayShade
import kotlin.math.PI

fun cosSpring(response: Double, durationScale: Double): SpringSpec<Float> =
    spring(
        dampingRatio = 1f,
        stiffness = ((2 * PI / (response * durationScale)).let { it * it }).toFloat(),
        visibilityThreshold = Spring.DefaultDisplacementThreshold,
    )

fun TransitionBuilder.cosToQuickSettingsShadeTransition(
    durationScale: Double = 1.0,
    isLargeScreen: Boolean = false,
) {
    spec = cosSpring(COS_PANEL_RESPONSE, durationScale)

    fractionRange(end = .86f) { translate(QuickSettingsShade.Elements.Panel, y = -CosPanelDrop) }
    fractionRange(end = .33f) {
        fade(OverlayShade.Elements.Scrim)
        if (isLargeScreen) fade(QuickSettingsShade.Elements.Panel)
    }
    if (isLargeScreen) {
        scaleDraw(QuickSettingsShade.Elements.Panel, scaleX = .94f, scaleY = .94f)
    }
    fractionRange(start = .5f) {
        fade(QuickSettingsShade.Elements.StatusBar)
        fade(QuickSettingsShade.Elements.Header)
    }
}

fun TransitionBuilder.cosFromQuickSettingsShadeTransition(
    durationScale: Double = 1.0,
    isLargeScreen: Boolean = false,
) {
    spec = cosSpring(COS_PANEL_RESPONSE, durationScale)

    translate(QuickSettingsShade.Elements.Panel, y = -CosPanelDrop)
    if (isLargeScreen) {
        scaleDraw(QuickSettingsShade.Elements.Panel, scaleX = .94f, scaleY = .94f)
    }
    fractionRange(end = .5f) {
        if (isLargeScreen) fade(QuickSettingsShade.Elements.Panel)
        fade(QuickSettingsShade.Elements.StatusBar)
        fade(QuickSettingsShade.Elements.Header)
    }
    fractionRange(start = .67f) { fade(OverlayShade.Elements.Scrim) }
}

/**
 * COS separate mode: the notification and Quick Settings shades sit side by side, and a horizontal
 * swipe slides the open one off one edge and the other in from the opposite edge. Only the panels
 * move; the blurred backdrop behind them stays as it is.
 */
fun TransitionBuilder.cosNotificationsToQuickSettingsSwitch() {
    spec =
        spring(
            dampingRatio = 1f - SWITCH_BOUNCE,
            stiffness = (2 * PI / SWITCH_RESPONSE).let { it * it }.toFloat(),
            visibilityThreshold = Spring.DefaultDisplacementThreshold,
        )
    translate(NotificationsShade.Elements.Panel, Edge.Start)
    translate(NotificationsShade.Elements.StatusBar, Edge.Start)
    translate(QuickSettingsShade.Elements.Panel, Edge.End)
    translate(QuickSettingsShade.Elements.StatusBar, Edge.End)
}

private const val COS_PANEL_RESPONSE = 0.314

// COS settles the switch on a spring with a half-second response and a little bounce.
private const val SWITCH_RESPONSE = 0.5
private const val SWITCH_BOUNCE = 0.1f
private val CosPanelDrop = 38.dp
