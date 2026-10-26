package vn.cfop.rubik.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Solver colour codes (0..5) -> display colours. */
fun cubeColour(code: Int): Color = when (code) {
    0 -> Color(0xFFF5F5F5) // white
    1 -> Color(0xFFD32F2F) // red
    2 -> Color(0xFF43A047) // green
    3 -> Color(0xFFFFEB3B) // yellow
    4 -> Color(0xFFFF6D00) // orange
    else -> Color(0xFF1E88E5) // blue
}

/**
 * Interactive 3D Rubik's cube.
 *
 * Drag to rotate the view. [animMove] + [animProgress] (0..1) animate
 * one move: the layer turns while the rest of the cube stays put.
 *
 * @param state solver facelet colours (54 entries) to display.
 * @param animMove the move being animated (e.g. "R", "U'", "F2"), or null.
 * @param animProgress 0..1, how far [animMove] has turned.
 */
@Composable
fun Cube3DView(
    state: IntArray,
    animMove: String?,
    animProgress: Float,
    modifier: Modifier = Modifier,
) {
    var azimuth by remember { mutableFloatStateOf(-35f) }
    var elevation by remember { mutableFloatStateOf(28f) }

    val anim = remember(animMove) { animMove?.let { moveAnimSpec(it) } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    azimuth -= dragAmount.x * 0.45f
                    elevation = (elevation + dragAmount.y * 0.45f).coerceIn(-80f, 80f)
                }
            },
    ) {
        val minDim = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stickers = buildStickers(state, anim, animProgress)

        // Project + keep only stickers facing the viewer.
        data class Draw(val path: Path, val colour: Color, val depth: Float)
        val draws = ArrayList<Draw>(54)
        for (st in stickers) {
            // View-space normal: facing the viewer means z > 0.
            var vn = rotY(st.normal, azimuth)
            vn = rotX(vn, elevation)
            if (vn.z <= 0.02f) continue
            val pts = st.corners.map { c ->
                val p = projectPoint(c, azimuth, elevation, minDim)
                Offset(cx + p.x, cy + p.y)
            }
            val centre = projectPoint(st.centre, azimuth, elevation, minDim)
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                lineTo(pts[1].x, pts[1].y)
                lineTo(pts[2].x, pts[2].y)
                lineTo(pts[3].x, pts[3].y)
                close()
            }
            draws.add(Draw(path, cubeColour(st.colour), centre.depth))
        }
        // Painter's algorithm: farthest first.
        draws.sortBy { it.depth }
        for (d in draws) {
            drawPath(d.path, d.colour)
            drawPath(d.path, Color(0xFF212121), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}
