package io.github.multicardstack

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CardStackUiTest {
    private val items = listOf("A", "B", "C", "D")

    private class Recorder {
        val swiped = mutableListOf<Pair<String, SwipeDirection>>()
        val undone = mutableListOf<Pair<String, SwipeDirection>>()
    }

    private fun stackTest(
        directions: Set<SwipeDirection> = SwipeDirection.Horizontal,
        block: suspend androidx.compose.ui.test.ComposeUiTest.(CardStackState, Recorder, CoroutineScope) -> Unit,
    ) = runComposeUiTest {
        val state = CardStackState()
        val recorder = Recorder()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            CardStack(
                items = items,
                state = state,
                directions = directions,
                modifier = Modifier.size(300.dp, 450.dp).testTag("stack"),
                onSwiped = { item, direction -> recorder.swiped += item to direction },
                onUndo = { item, direction -> recorder.undone += item to direction },
            ) { item -> BasicText(item) }
        }
        block(state, recorder, scope)
    }

    @Test
    fun swipeGestureRemovesTopCard() = stackTest { state, recorder, _ ->
        onNodeWithTag("stack").performTouchInput { swipeRight() }
        waitForIdle()
        assertEquals(listOf("A" to SwipeDirection.Right), recorder.swiped)
        assertEquals(1, state.currentIndex)

        onNodeWithTag("stack").performTouchInput { swipeLeft() }
        waitForIdle()
        assertEquals("B" to SwipeDirection.Left, recorder.swiped.last())
        assertEquals(listOf(SwipeDirection.Right, SwipeDirection.Left), state.swipeHistory)
    }

    @Test
    fun shortSlowDragReturnsToCenter() = stackTest { state, recorder, _ ->
        onNodeWithTag("stack").performTouchInput {
            swipe(start = center, end = center + Offset(40f, 0f), durationMillis = 1000)
        }
        waitForIdle()
        assertTrue(recorder.swiped.isEmpty())
        assertEquals(0, state.currentIndex)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun disabledDirectionDoesNotSwipe() = stackTest { state, recorder, _ ->
        onNodeWithTag("stack").performTouchInput { swipeUp() }
        waitForIdle()
        assertTrue(recorder.swiped.isEmpty())
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun verticalSwipeWhenEnabled() = stackTest(directions = SwipeDirection.All) { state, recorder, _ ->
        onNodeWithTag("stack").performTouchInput { swipeUp() }
        waitForIdle()
        assertEquals(listOf("A" to SwipeDirection.Up), recorder.swiped)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun programmaticSwipeAndUndo() = stackTest { state, recorder, scope ->
        scope.launch {
            state.swipe(SwipeDirection.Up) // Allowed even though gestures are horizontal only.
            state.swipe(SwipeDirection.Right)
        }
        waitForIdle()
        assertEquals(2, state.currentIndex)
        onNodeWithText("C").assertIsDisplayed()

        var undone = false
        scope.launch { undone = state.undo() }
        waitForIdle()
        assertTrue(undone)
        assertEquals(listOf("B" to SwipeDirection.Right), recorder.undone)
        assertEquals(1, state.currentIndex)
        assertEquals(Offset.Zero, state.offset)
        assertEquals(listOf(SwipeDirection.Up), state.swipeHistory)
    }

    @Test
    fun swipeStopsAtEndAndResetRestarts() = stackTest { state, recorder, scope ->
        // Launched together: the swipes queue up and run one after another.
        repeat(items.size + 2) { scope.launch { state.swipe(SwipeDirection.Left) } }
        waitForIdle()
        assertEquals(items.size, state.currentIndex)
        assertEquals(items.size, recorder.swiped.size)

        scope.launch { state.reset() }
        waitForIdle()
        assertEquals(0, state.currentIndex)
        assertFalse(state.canUndo)
        onNodeWithText("A").assertIsDisplayed()
    }
}
