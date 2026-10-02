package io.github.multicardstack

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

/**
 * Creates and remembers a [CardStackState]. The current position and swipe history survive
 * configuration changes and process death.
 */
@Composable
public fun rememberCardStackState(initialIndex: Int = 0): CardStackState =
    rememberSaveable(saver = CardStackState.Saver) { CardStackState(initialIndex) }

/**
 * State of a [CardStack]: which card is on top, where it is being dragged, and what has been
 * swiped so far. Use it to swipe or undo programmatically, e.g. from buttons.
 */
@Stable
public class CardStackState internal constructor(
    initialIndex: Int,
    initialHistory: List<SwipeDirection>,
) {
    public constructor(initialIndex: Int = 0) : this(initialIndex, emptyList())

    init {
        require(initialIndex >= 0) { "initialIndex must be >= 0, was $initialIndex" }
    }

    /** Index of the item shown on top of the stack. Equals the item count once every card is swiped. */
    public var currentIndex: Int by mutableIntStateOf(initialIndex)
        private set

    /** Current translation of the top card, in px. */
    public var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    /** Whether the user is currently dragging the top card. */
    public var isDragging: Boolean by mutableStateOf(false)
        private set

    /** Whether a swipe or undo animation is running. Drags are ignored meanwhile. */
    public var isAnimating: Boolean by mutableStateOf(false)
        private set

    private val history = mutableStateListOf<SwipeDirection>().apply { addAll(initialHistory) }

    /** Directions of the swipes so far, oldest first. One entry per swiped card. */
    public val swipeHistory: List<SwipeDirection> get() = history.toList()

    /** Whether [undo] would bring a card back. */
    public val canUndo: Boolean get() = history.isNotEmpty() && currentIndex > 0

    /** The allowed direction the top card currently leans toward, or null when it is at rest. */
    public val swipeDirection: SwipeDirection? by derivedStateOf {
        dominantDirection(offset, cardSize, directions)
    }

    /** How close the top card is to completing a swipe, from `0f` (at rest) to `1f` (past the threshold). */
    public val swipeProgress: Float by derivedStateOf {
        swipeProgress(offset, cardSize, swipeThreshold)
    }

    // Kept in sync by CardStack.
    internal var cardSize by mutableStateOf(Size.Zero)
    internal var directions by mutableStateOf(SwipeDirection.Horizontal)
    internal var swipeThreshold by mutableFloatStateOf(CardStackDefaults.SwipeThreshold)
    internal var travelSize = Size.Zero
    internal var velocityThreshold = 0f
    internal var itemCount = 0
    internal var onSwiped: ((index: Int, direction: SwipeDirection) -> Unit)? = null
    internal var onUndone: ((index: Int, direction: SwipeDirection) -> Unit)? = null

    private val mutex = Mutex()
    private var settleJob: Job? = null

    /**
     * Swipes the top card toward [direction] and suspends until it has left the stack.
     * Works for any direction, including ones not enabled for gestures. Calls made while
     * another swipe or undo is running wait for it to finish. Does nothing when no card is left.
     */
    public suspend fun swipe(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset> = CardStackDefaults.SwipeAnimationSpec,
    ) {
        mutex.withLock {
            if (currentIndex >= itemCount) return
            stopUserInteraction()
            animating {
                animateOffsetTo(exitOffset(direction, offset), animationSpec)
                commit(direction)
            }
        }
    }

    /**
     * Brings back the most recently swiped card, animating it in from the side it left.
     *
     * @return false when there was nothing to undo.
     */
    public suspend fun undo(
        animationSpec: AnimationSpec<Offset> = CardStackDefaults.UndoAnimationSpec,
    ): Boolean {
        mutex.withLock {
            if (!canUndo) return false
            stopUserInteraction()
            val direction = history.last()
            val index = currentIndex - 1
            Snapshot.withMutableSnapshot {
                history.removeAt(history.lastIndex)
                currentIndex = index
                offset = exitOffset(direction, Offset.Zero)
            }
            onUndone?.invoke(index, direction)
            animating { animateOffsetTo(Offset.Zero, animationSpec) }
            return true
        }
    }

    /** Jumps to [index] without animation and clears the swipe history. */
    public suspend fun reset(index: Int = 0) {
        require(index >= 0) { "index must be >= 0, was $index" }
        mutex.withLock {
            stopUserInteraction()
            Snapshot.withMutableSnapshot {
                history.clear()
                currentIndex = index
                offset = Offset.Zero
            }
        }
    }

    internal fun startDrag() {
        if (isAnimating || currentIndex >= itemCount) return
        settleJob?.cancel()
        isDragging = true
    }

    internal fun dragBy(delta: Offset) {
        if (isDragging) offset += delta
    }

    internal fun endDrag(velocity: Velocity, scope: CoroutineScope) {
        if (!isDragging) return
        isDragging = false
        val direction = resolveSwipeDirection(
            offset = offset,
            velocity = velocity,
            cardSize = cardSize,
            directions = directions,
            threshold = swipeThreshold,
            velocityThreshold = velocityThreshold,
        )
        if (direction != null) {
            // Undispatched so the lock is taken before the next drag can start.
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                mutex.withLock {
                    animating {
                        animateOffsetTo(exitOffset(direction, offset), CardStackDefaults.FlingAnimationSpec)
                        commit(direction)
                    }
                }
            }
        } else {
            settleJob = scope.launch {
                animateOffsetTo(
                    target = Offset.Zero,
                    animationSpec = CardStackDefaults.SettleAnimationSpec,
                    initialVelocity = Offset(velocity.x, velocity.y),
                )
            }
        }
    }

    private fun stopUserInteraction() {
        isDragging = false
        settleJob?.cancel()
        settleJob = null
    }

    private inline fun animating(block: () -> Unit) {
        isAnimating = true
        try {
            block()
        } finally {
            isAnimating = false
        }
    }

    private suspend fun animateOffsetTo(
        target: Offset,
        animationSpec: AnimationSpec<Offset>,
        initialVelocity: Offset = Offset.Zero,
    ) {
        animate(
            typeConverter = Offset.VectorConverter,
            initialValue = offset,
            targetValue = target,
            initialVelocity = initialVelocity,
            animationSpec = animationSpec,
        ) { value, _ -> offset = value }
    }

    private fun commit(direction: SwipeDirection) {
        val index = currentIndex
        // One snapshot so the next card never renders at the old card's offset.
        Snapshot.withMutableSnapshot {
            offset = Offset.Zero
            currentIndex = index + 1
            history.add(direction)
        }
        onSwiped?.invoke(index, direction)
    }

    /** Far enough that the card, rotated, is fully off screen. Keeps the cross-axis offset. */
    private fun exitOffset(direction: SwipeDirection, from: Offset): Offset {
        val reach = (cardSize.width + cardSize.height) / 2f
        val dx = max(travelSize.width, cardSize.width) + reach
        val dy = max(travelSize.height, cardSize.height) + reach
        return when (direction) {
            SwipeDirection.Left -> Offset(-dx, from.y)
            SwipeDirection.Right -> Offset(dx, from.y)
            SwipeDirection.Up -> Offset(from.x, -dy)
            SwipeDirection.Down -> Offset(from.x, dy)
        }
    }

    public companion object {
        /** [Saver] keeping [currentIndex] and [swipeHistory]. */
        public val Saver: Saver<CardStackState, *> = listSaver(
            save = { state -> listOf(state.currentIndex) + state.history.map { it.ordinal } },
            restore = { saved ->
                CardStackState(
                    initialIndex = saved.first(),
                    initialHistory = saved.drop(1).map { SwipeDirection.entries[it] },
                )
            },
        )
    }
}
