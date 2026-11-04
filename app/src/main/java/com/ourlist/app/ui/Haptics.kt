package com.ourlist.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class OurListHaptics(private val view: View) {

    fun toggle(on: Boolean) = perform(
        when {
            Build.VERSION.SDK_INT >= 34 && on -> HapticFeedbackConstants.TOGGLE_ON
            Build.VERSION.SDK_INT >= 34 -> HapticFeedbackConstants.TOGGLE_OFF
            Build.VERSION.SDK_INT >= 30 -> HapticFeedbackConstants.CONFIRM
            else -> HapticFeedbackConstants.VIRTUAL_KEY
        },
    )

    fun tick() = perform(
        if (Build.VERSION.SDK_INT >= 34) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        },
    )

    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= 30) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        },
    )

    fun reject() = perform(
        if (Build.VERSION.SDK_INT >= 30) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): OurListHaptics {
    val view = LocalView.current
    return remember(view) { OurListHaptics(view) }
}
