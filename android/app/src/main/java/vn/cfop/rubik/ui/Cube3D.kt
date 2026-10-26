package vn.cfop.rubik.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight 3D model + projection for the Rubik's cube.
 *
 * Uses the SAME coordinate system as the solver (Cube.java):
 * x: +1 = R face, -1 = L face
 * y: +1 = U face, -1 = D face
 * z: +1 = F face, -1 = B face
 *
 * A sticker is derived directly from the solver's 54-facelet state:
 * every facelet has a fixed cubie position and outward normal
 * (mirroring Cube.faceletPn), so the 3D colours always match the
 * solver state exactly — no separate cubie tracking needed.
 *
 * Animation: a move rotates one layer (or the whole cube) about an
 * axis. While animating, the sticker corners of that layer are
 * rotated by (progress * totalAngle) before the view transform.
 */

/** 3D float vector. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)
}

/** Integer 3D vector (cubie positions and normals). */
data class Vec3i(val x: Int, val y: Int, val z: Int)

/** (cubie position, outward normal) of a facelet, mirroring Cube.faceletPn. */
fun faceletPn(idx: Int): Pair<Vec3i, Vec3i> {
    return when {
        idx < 9 -> { // U
            Vec3i(idx % 3 - 1, 1, idx / 3 - 1) to Vec3i(0, 1, 0)
        }
        idx < 18 -> { // R
            val j = idx - 9
            Vec3i(1, 1 - j / 3, 1 - j % 3) to Vec3i(1, 0, 0)
        }
        idx < 27 -> { // F
            val j = idx - 18
            Vec3i(j % 3 - 1, 1 - j / 3, 1) to Vec3i(0, 0, 1)
        }
        idx < 36 -> { // D
            val j = idx - 27
            Vec3i(j % 3 - 1, -1, 1 - j / 3) to Vec3i(0, -1, 0)
        }
        idx < 45 -> { // L
            val j = idx - 36
            Vec3i(-1, 1 - j / 3, j % 3 - 1) to Vec3i(-1, 0, 0)
        }
        else -> { // B
            val j = idx - 45
            Vec3i(1 - j % 3, 1 - j / 3, -1) to Vec3i(0, 0, -1)
        }
    }
}

/** Rotate a point about the X axis by [angleDeg] (right-hand rule). */
fun rotX(p: Vec3, angleDeg: Float): Vec3 {
    val a = angleDeg * PI.toFloat() / 180f
    val c = cos(a); val s = sin(a)
    return Vec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
}

/** Rotate a point about the Y axis by [angleDeg] (right-hand rule). */
fun rotY(p: Vec3, angleDeg: Float): Vec3 {
    val a = angleDeg * PI.toFloat() / 180f
    val c = cos(a); val s = sin(a)
    return Vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
}

/** Rotate a point about the Z axis by [angleDeg] (right-hand rule). */
fun rotZ(p: Vec3, angleDeg: Float): Vec3 {
    val a = angleDeg * PI.toFloat() / 180f
    val c = cos(a); val s = sin(a)
    return Vec3(p.x * c - p.y * s, p.x * s + p.y * c, p.z)
}

fun rotateAboutAxis(p: Vec3, axis: Int, angleDeg: Float): Vec3 = when (axis) {
    0 -> rotX(p, angleDeg)
    1 -> rotY(p, angleDeg)
    else -> rotZ(p, angleDeg)
}

/**
 * Animation spec for one move: rotate the layer whose coordinate on
 * [axis] equals [layer] by [angleDeg]. [layer] == null means the
 * whole cube (x / y / z rotations).
 *
 * Angles follow the solver (Cube.java): a plain quarter turn is
 * clockwise as seen from outside that face.
 * U=-90 y, D=+90 y, R=-90 x, L=+90 x, F=-90 z, B=+90 z,
 * M=+90 x (layer x=0), E=+90 y (layer y=0), S=-90 z (layer z=0),
 * x=-90 x, y=-90 y, z=-90 z (whole cube).
 */
data class MoveAnim(val axis: Int, val layer: Int?, val angleDeg: Float)

fun moveAnimSpec(move: String): MoveAnim {
    val base = move[0]
    val (axis, layer, baseAngle) = when (base) {
        'U' -> Triple(1, 1, -90f)
        'D' -> Triple(1, -1, 90f)
        'R' -> Triple(0, 1, -90f)
        'L' -> Triple(0, -1, 90f)
        'F' -> Triple(2, 1, -90f)
        'B' -> Triple(2, -1, 90f)
        'M' -> Triple(0, 0, 90f)
        'E' -> Triple(1, 0, 90f)
        'S' -> Triple(2, 0, -90f)
        'x' -> Triple(0, null, -90f)
        'y' -> Triple(1, null, -90f)
        'z' -> Triple(2, null, -90f)
        else -> throw IllegalArgumentException("Unknown move $move")
    }
    val turns = when {
        move.endsWith("'") -> -1f
        move.endsWith("2") -> 2f
        else -> 1f
    }
    return MoveAnim(axis, layer, baseAngle * turns)
}

/** One sticker to draw: its 4 corners in cube space, colour, and normal. */
data class Sticker(
    val index: Int,          // facelet index 0..53
    val corners: List<Vec3>, // 4 corners, cube space
    val colour: Int,         // solver colour code 0..5, -1 = uncoloured
    val normal: Vec3,        // outward normal, cube space
    val centre: Vec3,        // sticker centre, cube space
)

/** Two tangent directions for a face normal (unit vectors). */
private fun tangents(n: Vec3i): Pair<Vec3, Vec3> = when (n) {
    Vec3i(0, 1, 0), Vec3i(0, -1, 0) -> Vec3(1f, 0f, 0f) to Vec3(0f, 0f, 1f)
    Vec3i(1, 0, 0), Vec3i(-1, 0, 0) -> Vec3(0f, 1f, 0f) to Vec3(0f, 0f, 1f)
    else -> Vec3(1f, 0f, 0f) to Vec3(0f, 1f, 0f)
}

/** Sticker side length (cubie side is 1.0, so the gap shows the black body). */
const val STICKER_SIZE = 0.86f

/**
 * Build the 54 stickers for a facelet state, optionally with one move
 * partially applied (for animation).
 *
 * @param state solver facelet colours (54 entries).
 * @param anim the move being animated, or null.
 * @param progress 0..1 — how far the animated move has turned.
 */
fun buildStickers(state: IntArray, anim: MoveAnim?, progress: Float): List<Sticker> {
    val out = ArrayList<Sticker>(54)
    val angle = if (anim != null) anim.angleDeg * progress else 0f
    for (i in 0 until 54) {
        val (pos, nrm) = faceletPn(i)
        val n = Vec3(nrm.x.toFloat(), nrm.y.toFloat(), nrm.z.toFloat())
        val centre = Vec3(
            pos.x + n.x * 0.5f,
            pos.y + n.y * 0.5f,
            pos.z + n.z * 0.5f,
        )
        val (t1, t2) = tangents(nrm)
        val h = STICKER_SIZE / 2f
        val rawCorners = listOf(
            centre + t1 * h + t2 * h,
            centre + t1 * h - t2 * h,
            centre - t1 * h - t2 * h,
            centre - t1 * h + t2 * h,
        )
        // If this cubie is in the animated layer, rotate the whole sticker.
        val inLayer = anim != null && (anim.layer == null ||
            when (anim.axis) {
                0 -> pos.x == anim.layer
                1 -> pos.y == anim.layer
                else -> pos.z == anim.layer
            })
        val corners = if (inLayer && angle != 0f) {
            rawCorners.map { rotateAboutAxis(it, anim.axis, angle) }
        } else {
            rawCorners
        }
        val nRotated = if (inLayer && angle != 0f) rotateAboutAxis(n, anim.axis, angle) else n
        val cRotated = if (inLayer && angle != 0f) rotateAboutAxis(centre, anim.axis, angle) else centre
        out.add(Sticker(i, corners, state[i], nRotated, cRotated))
    }
    return out
}

/**
 * View transform + perspective projection.
 *
 * The cube is rotated by [azimuthDeg] about Y, then [elevationDeg]
 * about X (the user's drag). The viewer sits on +Z looking towards
 * the origin, so a larger view-space Z is closer to the viewer.
 *
 * @param canvasMin the smaller canvas dimension in px; the cube is
 *   sized to fill ~78% of it.
 * @return (screenX, screenY, depth) with the origin at canvas centre;
 *   depth is larger for points closer to the viewer.
 */
data class Projected(val x: Float, val y: Float, val depth: Float)

fun projectPoint(
    p: Vec3,
    azimuthDeg: Float,
    elevationDeg: Float,
    canvasMin: Float,
): Projected {
    // View rotation: azimuth about Y, then elevation about X.
    var v = rotY(p, azimuthDeg)
    v = rotX(v, elevationDeg)
    // Perspective: viewer at distance D on +Z. Scale so the cube
    // (radius ~2.6) fills ~78% of the canvas.
    val d = 9f
    val f = canvasMin * 0.39f * d / 2.6f
    val denom = d - v.z
    val scale = f / denom
    return Projected(v.x * scale, -v.y * scale, v.z)
}
