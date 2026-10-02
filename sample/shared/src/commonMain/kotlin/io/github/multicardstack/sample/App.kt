package io.github.multicardstack.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.multicardstack.CardStack
import io.github.multicardstack.SwipeDirection
import io.github.multicardstack.rememberCardStackState
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF4F4F6)) {
            CardStackScreen()
        }
    }
}

@Composable
private fun CardStackScreen() {
    val profiles = remember { sampleProfiles() }
    val state = rememberCardStackState()
    val scope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf("Swipe a card or use the buttons") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MultiCardStack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(lastAction, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (state.currentIndex >= profiles.size) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No more cards", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { state.reset() } }) { Text("Start over") }
                }
            }
            CardStack(
                items = profiles,
                state = state,
                directions = SwipeDirection.All,
                modifier = Modifier.fillMaxSize(),
                key = { it.id },
                onSwiped = { profile, direction -> lastAction = "${direction.label}: ${profile.name}" },
                onUndo = { profile, _ -> lastAction = "Undo: ${profile.name}" },
                overlay = { _, direction, progress -> SwipeStamp(direction, progress) },
            ) { profile ->
                ProfileCard(profile)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val canSwipe = state.currentIndex < profiles.size
            ActionButton("↺", Color(0xFFF5B748), size = 48, enabled = state.canUndo) {
                scope.launch { state.undo() }
            }
            ActionButton("✕", Color(0xFFEC5E6F), enabled = canSwipe) {
                scope.launch { state.swipe(SwipeDirection.Left) }
            }
            ActionButton("↓", Color(0xFF8E8E93), size = 48, enabled = canSwipe) {
                scope.launch { state.swipe(SwipeDirection.Down) }
            }
            ActionButton("★", Color(0xFF3CA4FF), size = 48, enabled = canSwipe) {
                scope.launch { state.swipe(SwipeDirection.Up) }
            }
            ActionButton("♥", Color(0xFF2DC98A), enabled = canSwipe) {
                scope.launch { state.swipe(SwipeDirection.Right) }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: Profile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(profile.colors)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000))))
                .padding(20.dp),
        ) {
            Column {
                Text(
                    "${profile.name}, ${profile.age}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(profile.bio, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeStamp(direction: SwipeDirection, progress: Float) {
    val (alignment, angle) = when (direction) {
        SwipeDirection.Right -> Alignment.TopStart to -15f
        SwipeDirection.Left -> Alignment.TopEnd to 15f
        SwipeDirection.Up -> Alignment.BottomCenter to 0f
        SwipeDirection.Down -> Alignment.TopCenter to 0f
    }
    Text(
        text = direction.label,
        color = direction.color,
        fontSize = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .align(alignment)
            .padding(32.dp)
            .graphicsLayer { alpha = progress }
            .rotate(angle)
            .border(4.dp, direction.color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun ActionButton(
    symbol: String,
    color: Color,
    size: Int = 60,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = color),
        modifier = Modifier.size(size.dp).shadow(4.dp, CircleShape),
    ) {
        Text(symbol, fontSize = (size / 2.4).sp, fontWeight = FontWeight.Bold)
    }
}

private val SwipeDirection.label: String
    get() = when (this) {
        SwipeDirection.Right -> "LIKE"
        SwipeDirection.Left -> "NOPE"
        SwipeDirection.Up -> "SUPER LIKE"
        SwipeDirection.Down -> "LATER"
    }

private val SwipeDirection.color: Color
    get() = when (this) {
        SwipeDirection.Right -> Color(0xFF2DC98A)
        SwipeDirection.Left -> Color(0xFFEC5E6F)
        SwipeDirection.Up -> Color(0xFF3CA4FF)
        SwipeDirection.Down -> Color(0xFF8E8E93)
    }
