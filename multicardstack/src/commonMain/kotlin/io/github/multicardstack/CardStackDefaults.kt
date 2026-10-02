package io.github.multicardstack

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default values used by [CardStack] and [CardStackState]. */
public object CardStackDefaults {
    /** Number of cards drawn at once, including the top card. */
    public const val VisibleCount: Int = 3

    /** Fraction of the card size a drag has to cover to complete a swipe. */
    public const val SwipeThreshold: Float = 0.3f

    /** Fling speed (per second) that completes a swipe regardless of distance. */
    public val VelocityThreshold: Dp = 1000.dp

    /** Rotation in degrees of the top card when dragged a full card width sideways. */
    public const val MaxRotation: Float = 15f

    /** How much smaller each card behind the top card is drawn. */
    public const val StackScaleStep: Float = 0.04f

    /** How far each card behind the top card peeks out below it. */
    public val StackOffset: Dp = 10.dp

    /** Animation used by [CardStackState.swipe]. */
    public val SwipeAnimationSpec: AnimationSpec<Offset> =
        tween(durationMillis = 400, easing = FastOutSlowInEasing)

    /** Animation used by [CardStackState.undo]. */
    public val UndoAnimationSpec: AnimationSpec<Offset> =
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset.VisibilityThreshold)

    internal val FlingAnimationSpec: AnimationSpec<Offset> =
        tween(durationMillis = 250, easing = LinearOutSlowInEasing)

    internal val SettleAnimationSpec: AnimationSpec<Offset> =
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium, visibilityThreshold = Offset.VisibilityThreshold)
}
