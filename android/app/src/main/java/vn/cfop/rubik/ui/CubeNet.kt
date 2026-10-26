package vn.cfop.rubik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Display colours for the six cube colours (0..5). */
val CUBE_COLORS = listOf(
    Color(0xFFF5F5F5), // 0 white (D)
    Color(0xFFD32F2F), // 1 red (L)
    Color(0xFF388E3C), // 2 green (F)
    Color(0xFFFDD835), // 3 yellow (U)
    Color(0xFFF57C00), // 4 orange (R)
    Color(0xFF1976D2), // 5 blue (B)
)

/** Face base indices in net order: U, L, F, R, B, D. */
private val NET_FACES = listOf(0, 36, 18, 9, 45, 27)

/**
 * Draws the unfolded cube net (cross layout):
 * ```
 *       [U]
 *   [L] [F] [R] [B]
 *       [D]
 * ```
 * @param state 54 facelet colours (see Cube).
 * @param onStickerClick invoked with the facelet index when a sticker is tapped;
 *   null makes the net read-only.
 * @param editableCenters whether centre stickers can be tapped (normally false).
 */
@Composable
fun CubeNet(
    state: IntArray,
    onStickerClick: ((Int) -> Unit)? = null,
    stickerSize: Dp = 30.dp,
    modifier: Modifier = Modifier,
) {
    val gap = 2.dp
    val faceGap = 4.dp
    Column(modifier = modifier) {
        // Top row: U
        Row {
            Box(Modifier.size(stickerSize * 3 + gap * 2 + faceGap))
            FaceGrid(
                state = state,
                faceBase = NET_FACES[0],
                stickerSize = stickerSize,
                gap = gap,
                onStickerClick = onStickerClick,
            )
        }
        // Middle row: L F R B
        Row {
            for (face in listOf(NET_FACES[1], NET_FACES[2], NET_FACES[3], NET_FACES[4])) {
                FaceGrid(
                    state = state,
                    faceBase = face,
                    stickerSize = stickerSize,
                    gap = gap,
                    onStickerClick = onStickerClick,
                    modifier = Modifier.padding(end = faceGap),
                )
            }
        }
        // Bottom row: D
        Row {
            Box(Modifier.size(stickerSize * 3 + gap * 2 + faceGap))
            FaceGrid(
                state = state,
                faceBase = NET_FACES[5],
                stickerSize = stickerSize,
                gap = gap,
                onStickerClick = onStickerClick,
            )
        }
    }
}

@Composable
private fun FaceGrid(
    state: IntArray,
    faceBase: Int,
    stickerSize: Dp,
    gap: Dp,
    onStickerClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for (row in 0 until 3) {
            Row {
                for (col in 0 until 3) {
                    val idx = faceBase + row * 3 + col
                    val isCentre = (row == 1 && col == 1)
                    val clickable = onStickerClick != null && !isCentre
                    Box(
                        modifier = Modifier
                            .padding(end = if (col < 2) gap else 0.dp,
                                bottom = if (row < 2) gap else 0.dp)
                            .size(stickerSize)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CUBE_COLORS[state[idx]])
                            .border(
                                width = 1.dp,
                                color = Color(0xFF9E9E9E),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .then(
                                if (clickable) {
                                    Modifier.clickable { onStickerClick(idx) }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}
