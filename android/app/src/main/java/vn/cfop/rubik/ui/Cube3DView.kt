package vn.cfop.rubik.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** Solver colour codes (0..5) -> display colours; -1 = not coloured yet. */
fun cubeColour(code: Int): Color = when (code) {
    -1 -> Color(0xFF616161) // uncoloured (dark grey)
    0 -> Color(0xFFF5F5F5) // white
    1 -> Color(0xFFD32F2F) // red
    2 -> Color(0xFF43A047) // green
    3 -> Color(0xFFFFEB3B) // yellow
    4 -> Color(0xFFFF6D00) // orange
    else -> Color(0xFF1E88E5) // blue
}

/** View angles that bring a face front-and-centre. Face: 0=U 1=R 2=F 3=D 4=L 5=B. */
fun faceViewAngles(face: Int): Pair<Float, Float> = when (face) {
    0 -> -35f to 55f // U: look down at the top
    1 -> -90f to 20f // R
    2 -> 0f to 20f // F
    3 -> -35f to -55f // D: look up at the bottom
    4 -> 90f to 20f // L
    else -> 180f to 20f // B
}

/**
 * Interactive 3D Rubik's cube.
 *
 * Drag to rotate the view. [animMove] + [animProgress] (0..1) animate
 * one move: the layer turns while the rest of the cube stays put.
 *
 * @param state solver facelet colours (54 entries) to display (-1 = uncoloured).
 * @param animMove the move being animated (e.g. "R", "U'", "F2"), or null.
 * @param animProgress 0..1, how far [animMove] has turned.
 * @param highlight facelet indices to pulse-highlight (e.g. the F2L pair).
 * @param dimExcept when non-empty, facelets NOT in this set are rendered
 *   gray (for F2L practice: only show the 2 relevant faces, gray out the rest).
 * @param viewFace face to bring front-and-centre (0=U 1=R 2=F 3=D 4=L 5=B),
 *   or null to keep the current view. Applied whenever it changes.
 */
@Composable
fun Cube3DView(
    state: IntArray,
    animMove: String?,
    animProgress: Float,
    modifier: Modifier = Modifier,
    highlight: Set<Int> = emptySet(),
    dimExcept: Set<Int> = emptySet(),
    viewFace: Int? = null,
) {
    var azimuth by remember { mutableFloatStateOf(-35f) }
    var elevation by remember { mutableFloatStateOf(28f) }

    // When the caller asks for a face, turn the cube to show it.
    LaunchedEffect(viewFace) {
        if (viewFace != null) {
            val (az, el) = faceViewAngles(viewFace)
            azimuth = az
            elevation = el
        }
    }

    // Pulsing alpha for highlighted stickers.
    val infiniteTransition = rememberInfiniteTransition(label = "highlight")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

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
        data class Draw(val path: Path, val colour: Color, val depth: Float, val isHighlight: Boolean, val isDimmed: Boolean)
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
            val isDimmed = dimExcept.isNotEmpty() && st.index !in dimExcept
            val col = if (isDimmed) Color(0xFF9E9E9E) else cubeColour(st.colour)
            draws.add(Draw(path, col, centre.depth, st.index in highlight, isDimmed))
        }
        // Painter's algorithm: farthest first.
        draws.sortBy { it.depth }
        for (d in draws) {
            drawPath(d.path, d.colour)
            if (d.isHighlight) {
                // Bright pulsing border so the pieces stand out.
                drawPath(
                    d.path,
                    Color(0xFFFFEB3B).copy(alpha = pulse),
                    style = Stroke(width = 4.dp.toPx()),
                )
                drawPath(
                    d.path,
                    Color.White.copy(alpha = pulse),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            } else {
                drawPath(d.path, Color(0xFF212121), style = Stroke(width = 1.5.dp.toPx()))
            }
        }
    }
}
