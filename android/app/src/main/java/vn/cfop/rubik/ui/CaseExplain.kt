package vn.cfop.rubik.ui

import vn.cfop.rubik.solver.CfopSolver

/**
 * Explain a solve stage in plain language: which training case it is
 * (F2L number, OLL pattern, PLL perm) and what is happening, so the
 * user can connect the solver's moves to what they practised.
 */

// ---- F2L: key -> training case (41 standard cases) ----
private val f2lKeyToCase: Map<Int, TrainingCase> by lazy {
    val map = HashMap<Int, TrainingCase>()
    for (c in ALL_TRAINING_CASES) {
        if (c.stage != "f2l") continue
        val key = CfopSolver.f2lCaseKey(c.patternState(), 0)
        if (key >= 0) map[key] = c
    }
    map
}

/** The training case for an F2L stage, from the state at the stage start. */
fun f2lCaseFor(state: IntArray, pairIndex: Int): TrainingCase? {
    val key = CfopSolver.f2lCaseKey(state, pairIndex)
    if (key < 0) return null
    return f2lKeyToCase[key]
}

/**
 * Where the F2L pair's pieces are at the stage start, in plain words.
 * Corner/edge in the top layer if any of their facelets is on U.
 */
fun describeF2LPosition(state: IntArray, pairIndex: Int): String {
    val facelets = try {
        CfopSolver.f2lPairFacelets(state, pairIndex)
    } catch (e: Exception) {
        return ""
    }
    val cornerTop = facelets[0] < 9 || facelets[1] < 9 || facelets[2] < 9
    val edgeTop = facelets[3] < 9 || facelets[4] < 9
    return when {
        cornerTop && edgeTop -> "Cả góc và cạnh đều đang ở tầng trên."
        cornerTop -> "Góc ở tầng trên, cạnh còn ở tầng dưới."
        edgeTop -> "Cạnh ở tầng trên, góc còn ở tầng dưới."
        else -> "Cả 2 viên đều đang ở tầng dưới (có thể đang nằm ở khe khác) — app sẽ lấy chúng ra rồi ghép lại."
    }
}

// ---- OLL EO: pattern from the state at the stage start ----
/** "Đường thẳng" | "Chữ L" | "Chấm" | null (already solved / unknown). */
fun ollEoPatternName(state: IntArray): String? {
    // U edge facelets: 1=UB, 3=UL, 5=UR, 7=UF. Misoriented = not yellow (3).
    val bad = listOf(1, 3, 5, 7).filter { state[it] != 3 }
    return when (bad.size) {
        2 -> if ((bad.contains(1) && bad.contains(7)) || (bad.contains(3) && bad.contains(5))) {
            "Đường thẳng"
        } else {
            "Chữ L"
        }
        4 -> "Chấm"
        else -> null
    }
}

// ---- Algorithm matching (strip leading/trailing U turns) ----
private fun stripOuterU(moves: List<String>): List<String> {
    var start = 0
    var end = moves.size
    while (start < end && (moves[start] == "U" || moves[start] == "U'" || moves[start] == "U2")) start++
    while (end > start && (moves[end - 1] == "U" || moves[end - 1] == "U'" || moves[end - 1] == "U2")) end--
    return moves.subList(start, end)
}

private fun isOnlyU(moves: List<String>): Boolean =
    moves.isNotEmpty() && moves.all { it == "U" || it == "U'" || it == "U2" }

/** Match a stage's moves to a training case by its core algorithm. */
fun matchAlgorithmCase(moves: List<String>, cases: List<TrainingCase>): TrainingCase? {
    if (isOnlyU(moves)) return null
    val core = stripOuterU(moves)
    if (core.isEmpty()) return null
    return cases.firstOrNull { it.algorithm == core }
}

// ---- OLL CO: name from the stage moves ----
/** Returns the CO training case, or null when the stage is a Sune fallback. */
fun ollCoCase(moves: List<String>): TrainingCase? {
    val coCases = ALL_TRAINING_CASES.filter { it.stage == "oll" && it.id.startsWith("co-") }
    return matchAlgorithmCase(moves, coCases)
}

/** True when the CO stage starts with a Sune (the T/L fallback: Sune once, then look again). */
fun isCoSuneFallback(moves: List<String>): Boolean {
    val sune = listOf("R", "U", "R'", "U", "R", "U2", "R'")
    if (moves.size < sune.size) return false
    return moves.subList(0, sune.size) == sune && moves.size > sune.size
}

// ---- PLL: name from the stage moves ----
fun pllCase(moves: List<String>, corners: Boolean): TrainingCase? {
    val ids = if (corners) listOf("pll-aa", "pll-ab", "pll-y")
    else listOf("pll-ua", "pll-ub", "pll-h", "pll-z")
    val cases = ALL_TRAINING_CASES.filter { it.id in ids }
    return matchAlgorithmCase(moves, cases)
}

/** True when the stage is only U turns (just rotate the top layer into place). */
fun isJustUTurns(moves: List<String>): Boolean = isOnlyU(moves)
