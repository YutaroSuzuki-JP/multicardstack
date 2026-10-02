package io.github.multicardstack

/**
 * The direction a card was (or is being) swiped toward.
 */
public enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down;

    public companion object {
        /** [Left] and [Right]. */
        public val Horizontal: Set<SwipeDirection> = setOf(Left, Right)

        /** [Up] and [Down]. */
        public val Vertical: Set<SwipeDirection> = setOf(Up, Down)

        /** Every direction. */
        public val All: Set<SwipeDirection> = entries.toSet()
    }
}
