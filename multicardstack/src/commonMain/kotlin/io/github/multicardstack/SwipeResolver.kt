package io.github.multicardstack

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/**
 * Decides whether a released drag should complete a swipe.
 *
 * A swipe completes when the card was dragged past [threshold] (a fraction of the card size)
 * or flung faster than [velocityThreshold] (px/s) in the same direction it was dragged,
 * and was not flung back toward the center. The dominant axis is tried first.
 */
internal fun resolveSwipeDirection(
    offset: Offset,
    velocity: Velocity,
    cardSize: Size,
    directions: Set<SwipeDirection>,
    threshold: Float,
    velocityThreshold: Float,
): SwipeDirection? {
    if (cardSize.width <= 0f || cardSize.height <= 0f) return null
    val horizontal = axisDirection(
        offset = offset.x,
        velocity = velocity.x,
        extent = cardSize.width,
        positive = SwipeDirection.Right,
        negative = SwipeDirection.Left,
        directions = directions,
        threshold = threshold,
        velocityThreshold = velocityThreshold,
    )
    val vertical = axisDirection(
        offset = offset.y,
        velocity = velocity.y,
        extent = cardSize.height,
        positive = SwipeDirection.Down,
        negative = SwipeDirection.Up,
        directions = directions,
        threshold = threshold,
        velocityThreshold = velocityThreshold,
    )
    return if (isHorizontallyDominant(offset, cardSize)) horizontal ?: vertical else vertical ?: horizontal
}

/** The allowed direction the card currently leans toward, or null when it is at rest. */
internal fun dominantDirection(
    offset: Offset,
    cardSize: Size,
    directions: Set<SwipeDirection>,
): SwipeDirection? {
    if (offset == Offset.Zero || cardSize.width <= 0f || cardSize.height <= 0f) return null
    val direction = if (isHorizontallyDominant(offset, cardSize)) {
        if (offset.x > 0f) SwipeDirection.Right else SwipeDirection.Left
    } else {
        if (offset.y > 0f) SwipeDirection.Down else SwipeDirection.Up
    }
    return direction.takeIf { it in directions }
}

/** How far the card is toward completing a swipe on its dominant axis, in `0f..1f`. */
internal fun swipeProgress(offset: Offset, cardSize: Size, threshold: Float): Float {
    if (cardSize.width <= 0f || cardSize.height <= 0f || threshold <= 0f) return 0f
    val x = abs(offset.x) / (cardSize.width * threshold)
    val y = abs(offset.y) / (cardSize.height * threshold)
    return max(x, y).coerceIn(0f, 1f)
}

private fun isHorizontallyDominant(offset: Offset, cardSize: Size): Boolean =
    abs(offset.x) / cardSize.width >= abs(offset.y) / cardSize.height

private fun axisDirection(
    offset: Float,
    velocity: Float,
    extent: Float,
    positive: SwipeDirection,
    negative: SwipeDirection,
    directions: Set<SwipeDirection>,
    threshold: Float,
    velocityThreshold: Float,
): SwipeDirection? {
    if (offset == 0f) return null
    val direction = if (offset > 0f) positive else negative
    if (direction !in directions) return null
    val fast = abs(velocity) >= velocityThreshold
    val sameWay = sign(velocity) == sign(offset)
    if (fast && !sameWay) return null
    val passed = abs(offset) >= extent * threshold
    return direction.takeIf { passed || (fast && sameWay) }
}
