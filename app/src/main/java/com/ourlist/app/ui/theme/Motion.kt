package com.ourlist.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize

/**
 * Springs matching PayTrace's iOS-derived spring simulation.
 */
object OurListMotion {

    /** iOS `.smooth`, response 0.4s. No overshoot: content that swaps in place. */
    fun <T> smooth(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = SMOOTH_STIFFNESS)

    /** [smooth] for layout size, which needs a one-pixel threshold to settle. */
    fun smoothSize(): FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 1f,
        stiffness = SMOOTH_STIFFNESS,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )

    /** iOS `.snappy`, response 0.35s. Settles a hair past and back: controls. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = SNAPPY_STIFFNESS)

    private const val SMOOTH_STIFFNESS = 247f
    private const val SNAPPY_STIFFNESS = 322f
}
