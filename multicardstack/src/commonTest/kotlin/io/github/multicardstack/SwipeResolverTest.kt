package io.github.multicardstack

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Velocity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SwipeResolverTest {
    private val card = Size(400f, 600f)

    private fun resolve(
        offset: Offset,
        velocity: Velocity = Velocity.Zero,
        directions: Set<SwipeDirection> = SwipeDirection.All,
    ) = resolveSwipeDirection(offset, velocity, card, directions, threshold = 0.3f, velocityThreshold = 1000f)

    @Test
    fun dragPastThresholdSwipes() {
        assertEquals(SwipeDirection.Right, resolve(Offset(121f, 0f)))
        assertEquals(SwipeDirection.Left, resolve(Offset(-121f, 10f)))
        assertEquals(SwipeDirection.Up, resolve(Offset(0f, -181f)))
        assertEquals(SwipeDirection.Down, resolve(Offset(5f, 181f)))
    }

    @Test
    fun shortDragReturns() {
        assertNull(resolve(Offset(100f, 0f)))
        assertNull(resolve(Offset(0f, -170f)))
    }

    @Test
    fun fastFlingSwipesBeforeThreshold() {
        assertEquals(SwipeDirection.Right, resolve(Offset(40f, 0f), Velocity(1500f, 0f)))
    }

    @Test
    fun flingBackTowardCenterCancels() {
        assertNull(resolve(Offset(200f, 0f), Velocity(-1500f, 0f)))
    }

    @Test
    fun disabledDirectionIsIgnored() {
        assertNull(resolve(Offset(0f, -300f), directions = SwipeDirection.Horizontal))
    }

    @Test
    fun fallsBackToOtherAxisWhenDominantIsDisabled() {
        // Mostly upward, but also far enough right.
        assertEquals(
            SwipeDirection.Right,
            resolve(Offset(150f, -500f), directions = SwipeDirection.Horizontal),
        )
    }

    @Test
    fun dominantAxisIsRelativeToCardSize() {
        // 120/400 = 0.30 horizontally vs 190/600 ≈ 0.32 vertically.
        assertEquals(SwipeDirection.Down, resolve(Offset(120f, 190f)))
    }

    @Test
    fun progressAndDirection() {
        assertEquals(0f, swipeProgress(Offset.Zero, card, 0.3f))
        assertEquals(0.5f, swipeProgress(Offset(60f, 0f), card, 0.3f), absoluteTolerance = 1e-4f)
        assertEquals(1f, swipeProgress(Offset(0f, -900f), card, 0.3f))
        assertEquals(SwipeDirection.Left, dominantDirection(Offset(-10f, 5f), card, SwipeDirection.All))
        assertNull(dominantDirection(Offset(0f, 50f), card, SwipeDirection.Horizontal))
        assertNull(dominantDirection(Offset.Zero, card, SwipeDirection.All))
    }
}
