package garden.ephemeral.composevr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize

/**
 * Holds state for a single overlay.
 *
 * @property size the size of the overlay window in pixels.
 */
interface OverlayState {
    var size: IntSize
}

/**
 * Creates state for a single overlay.
 *
 * @param size the size of the overlay window in pixels.
 * @return the overlay state.
 */
fun OverlayState(
    size: IntSize = IntSize(640, 480)
): OverlayState = OverlayStateImpl(size)

/**
 * Convenience function to remember overlay state as saveable state.
 *
 * @param size the size of the overlay window in pixels.
 * @return the overlay state.
 */
@Composable
fun rememberOverlayState(
    size: IntSize = IntSize(640, 480)
): OverlayState = rememberSaveable(saver = OverlayStateImpl.Saver()) {
    OverlayStateImpl(
        size
    )
}

private class OverlayStateImpl(
    size: IntSize
) : OverlayState {
    override var size by mutableStateOf(size)

    companion object {
        fun Saver() = listSaver<OverlayState, Any>(
            save = {
                listOf(
                    it.size.width,
                    it.size.height,
                )
            },
            restore = { state ->
                OverlayStateImpl(
                    size = IntSize(state[0] as Int, state[1] as Int),
                )
            }
        )
    }
}
