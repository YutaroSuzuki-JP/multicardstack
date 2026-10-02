package io.github.multicardstack

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize

/**
 * A Tinder-style stack of swipeable cards.
 *
 * The top card follows the finger, tilts while dragged sideways, and leaves the stack when
 * dragged past [swipeThreshold] or flung toward one of [directions]. The cards behind it
 * grow into place as it moves away. Cards fill this composable's bounds, minus the space
 * reserved at the bottom for the cards peeking out behind.
 *
 * Once every card is swiped `state.currentIndex == items.size` and nothing is drawn; show your
 * own empty state in that case. Items appended to [items] simply continue the stack.
 *
 * @param items the cards, top first.
 * @param state controls and observes the stack, see [rememberCardStackState].
 * @param directions directions the user can swipe with gestures. [CardStackState.swipe]
 *   accepts any direction regardless.
 * @param enabled whether gestures are accepted.
 * @param visibleCount number of cards drawn at once, including the top card.
 * @param swipeThreshold fraction of the card size a drag has to cover to complete a swipe.
 * @param velocityThreshold fling speed, per second, that completes a swipe regardless of distance.
 * @param maxRotation tilt in degrees of the top card when dragged a full card width sideways.
 * @param stackScaleStep how much smaller each card behind the top card is drawn.
 * @param stackOffset how far each card behind the top card peeks out below it.
 * @param key stable key for an item. Defaults to its index.
 * @param onSwiped called after a card has left the stack, by gesture or [CardStackState.swipe].
 * @param onUndo called when [CardStackState.undo] brings a card back.
 * @param overlay drawn over the top card while it leans toward an allowed direction, e.g. a
 *   "LIKE" stamp. `progress` goes from `0f` at rest to `1f` once releasing would swipe.
 * @param content a single card.
 */
@Composable
public fun <T> CardStack(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: CardStackState = rememberCardStackState(),
    directions: Set<SwipeDirection> = SwipeDirection.Horizontal,
    enabled: Boolean = true,
    visibleCount: Int = CardStackDefaults.VisibleCount,
    swipeThreshold: Float = CardStackDefaults.SwipeThreshold,
    velocityThreshold: Dp = CardStackDefaults.VelocityThreshold,
    maxRotation: Float = CardStackDefaults.MaxRotation,
    stackScaleStep: Float = CardStackDefaults.StackScaleStep,
    stackOffset: Dp = CardStackDefaults.StackOffset,
    key: ((item: T) -> Any)? = null,
    onSwiped: (item: T, direction: SwipeDirection) -> Unit = { _, _ -> },
    onUndo: (item: T, direction: SwipeDirection) -> Unit = { _, _ -> },
    overlay: @Composable BoxScope.(item: T, direction: SwipeDirection, progress: Float) -> Unit = { _, _, _ -> },
    content: @Composable BoxScope.(item: T) -> Unit,
) {
    require(visibleCount >= 1) { "visibleCount must be >= 1, was $visibleCount" }
    require(swipeThreshold > 0f) { "swipeThreshold must be > 0, was $swipeThreshold" }

    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val scope = rememberCoroutineScope()
    val velocityThresholdPx = with(density) { velocityThreshold.toPx() }
    val stackOffsetPx = with(density) { stackOffset.toPx() }
    val peek = stackOffset * (visibleCount - 1)
    val peekPx = stackOffsetPx * (visibleCount - 1)

    SideEffect {
        state.itemCount = items.size
        state.directions = directions
        state.swipeThreshold = swipeThreshold
        state.velocityThreshold = velocityThresholdPx
        state.travelSize = windowSize.toSize()
    }

    val currentItems by rememberUpdatedState(items)
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentOnUndo by rememberUpdatedState(onUndo)
    DisposableEffect(state) {
        state.onSwiped = { index, direction ->
            currentItems.getOrNull(index)?.let { currentOnSwiped(it, direction) }
        }
        state.onUndone = { index, direction ->
            currentItems.getOrNull(index)?.let { currentOnUndo(it, direction) }
        }
        onDispose {
            state.onSwiped = null
            state.onUndone = null
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { state.cardSize = Size(it.width.toFloat(), (it.height - peekPx).coerceAtLeast(0f)) }
            .pointerInput(state, enabled) {
                if (!enabled) return@pointerInput
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        state.startDrag()
                    },
                    onDragEnd = { state.endDrag(velocityTracker.calculateVelocity(), scope) },
                    onDragCancel = { state.endDrag(Velocity.Zero, scope) },
                    onDrag = { change, dragAmount ->
                        if (state.isDragging) {
                            change.consume()
                            velocityTracker.addPointerInputChange(change)
                            state.dragBy(dragAmount)
                        }
                    },
                )
            },
    ) {
        val topIndex = state.currentIndex
        // One extra card fades in at the back while the top card moves away.
        val deepest = minOf(visibleCount, items.lastIndex - topIndex)
        for (depth in deepest downTo 0) {
            val index = topIndex + depth
            val item = items[index]
            key(key?.invoke(item) ?: index) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = peek)
                        .graphicsLayer {
                            if (depth == 0) {
                                val offset = state.offset
                                translationX = offset.x
                                translationY = offset.y
                                rotationZ = (offset.x / size.width * maxRotation).coerceIn(-maxRotation, maxRotation)
                                transformOrigin = TransformOrigin.Center
                            } else {
                                val progress = state.swipeProgress
                                val position = depth - progress
                                val scale = 1f - stackScaleStep * position
                                scaleX = scale
                                scaleY = scale
                                translationY = stackOffsetPx * position
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                if (depth == visibleCount) alpha = progress
                            }
                        },
                ) {
                    content(item)
                    if (depth == 0) TopCardOverlay(state, item, overlay)
                }
            }
        }
    }
}

// Separate restartable scope so dragging only recomposes the overlay, not the stack.
@Composable
private fun <T> BoxScope.TopCardOverlay(
    state: CardStackState,
    item: T,
    overlay: @Composable BoxScope.(item: T, direction: SwipeDirection, progress: Float) -> Unit,
) {
    val direction = state.swipeDirection ?: return
    overlay(item, direction, state.swipeProgress)
}
