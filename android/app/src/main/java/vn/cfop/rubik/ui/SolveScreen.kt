package vn.cfop.rubik.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube

// Lời giải thích của mọi bước (trừ chữ thập) do bộ giải tạo sẵn trong Stage.note:
// tên case, cách nhận ra, công thức. Chữ thập dùng lời gợi ý chung dưới đây.
private const val CROSS_TIP = "Đưa 4 cạnh trắng xuống mặt dưới, khớp màu với tâm các mặt bên."

/** Flat move list with stage boundaries. */
private data class FlatMove(val stageIndex: Int, val move: String)

/** Inverse of one move: "R" <-> "R'", "R2" stays "R2". */
private fun invertMove(m: String): String = when {
    m.endsWith("'") -> m.dropLast(1)
    m.endsWith("2") -> m
    else -> m + "'"
}

/** Thẻ hiển thị một bước (stage): tiêu đề, giải thích, các chip nước đi. */
@Composable
private fun StageCard(
    stage: CfopSolver.Stage,
    isCurrent: Boolean,
    isPast: Boolean,
    explanation: String,
    moveChips: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isCurrent -> Color(0xFFE3F2FD)
                    isPast -> Color(0xFFF1F8E9)
                    else -> Color(0xFFF5F5F5)
                },
            )
            .border(
                1.dp,
                if (isCurrent) Color(0xFF1976D2) else Color(0xFFE0E0E0),
                RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isPast) "✓ " else "",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C),
            )
            Text(
                stage.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${stage.moves.size} nước",
                fontSize = 13.sp,
                color = Color.Gray,
            )
        }
        Text(
            explanation,
            fontSize = 13.sp,
            color = Color(0xFF616161),
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(8.dp))
        moveChips()
    }
}

/** Các chip nước đi của một bước, tự xuống hàng khi dài. Bấm chip để nhảy tới nước đó. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveChips(
    stage: CfopSolver.Stage,
    start: Int,
    current: Int,
    onJumpToMove: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stage.moves.forEachIndexed { mi, mv ->
            val globalIdx = start + mi
            val isNext = globalIdx == current
            val isDoneMove = globalIdx < current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isNext -> Color(0xFF1976D2)
                            isDoneMove -> Color(0xFFC8E6C9)
                            else -> Color.White
                        },
                    )
                    .border(
                        1.dp,
                        if (isNext) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onJumpToMove(globalIdx) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mv,
                    fontSize = 16.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNext) Color.White else Color.Black,
                )
            }
        }
    }
}

/**
 * Màn Giải từng bước. Bố cục một màn hình, KHÔNG cuộn cả trang:
 *   đầu (tiêu đề + tiến độ) → khối 3D (chiếm phần còn lại) → bước hiện tại
 *   (nước đi dạng chip, gọn) → hàng nút điều khiển luôn nằm sát đáy.
 * Lời giải thích dài mở bằng nút "Giải thích" (hộp thoại) nên không che nút.
 * Các nước xoay cả khối (y, y', y2) nằm ngay trong công thức, không còn nút
 * xoay riêng; ký hiệu luôn theo hướng cầm chuẩn (xanh lá ở trước, vàng ở trên).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SolveScreen(
    initialState: IntArray,
    stages: List<CfopSolver.Stage>,
    onBack: () -> Unit,
    onNewCube: () -> Unit,
    f2lOnly: Boolean = false,
) {
    // Chế độ Học F2L: chỉ hiện các bước F2L (kèm chữ thập để bắt đầu), ẩn OLL/PLL.
    val shownStages = remember(stages, f2lOnly) {
        if (f2lOnly) stages.filter { it.key == "cross" || it.key == "f2l" } else stages
    }
    val stages = shownStages
    val flat = remember(stages) {
        val list = ArrayList<FlatMove>()
        stages.forEachIndexed { si, st ->
            st.moves.forEach { m -> list.add(FlatMove(si, m)) }
        }
        list
    }
    // start offsets of each stage in the flat list
    val stageStarts = remember(stages) {
        val starts = ArrayList<Int>()
        var acc = 0
        stages.forEach { st ->
            starts.add(acc)
            acc += st.moves.size
        }
        starts
    }
    var current by remember { mutableIntStateOf(0) } // moves applied
    var autoPlay by remember { mutableStateOf(false) }
    val total = flat.size

    var showAllSteps by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    // Facelet state after the first k moves.
    fun stateAt(k: Int): IntArray {
        val cube = Cube(initialState)
        for (i in 0 until k) {
            cube.apply(flat[i].move)
        }
        return cube.s
    }

    // Lời giải thích từng bước: do bộ giải tạo sẵn (Stage.note).
    val stageExplanations = remember(stages) {
        stages.map { st ->
            if (st.note.isNotEmpty()) st.note else if (st.key == "cross") CROSS_TIP else ""
        }
    }

    // --- 3D animation state ---
    // baseState = facelets shown as the animation base (state before
    // the animated move). animMove = the move being turned, progress 0..1.
    var baseState by remember { mutableStateOf(initialState.copyOf()) }
    var animMove by remember { mutableStateOf<String?>(null) }
    val animProgress = remember { Animatable(0f) }
    var lastCurrent by remember { mutableIntStateOf(0) }

    LaunchedEffect(current) {
        val delta = current - lastCurrent
        lastCurrent = current
        when (delta) {
            1 -> {
                // One step forward: animate the move just applied.
                val mv = flat[current - 1].move
                baseState = stateAt(current - 1)
                animMove = mv
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, tween(450))
                baseState = stateAt(current)
                animMove = null
            }
            -1 -> {
                // One step back: animate the inverse of the move undone.
                val mv = flat[current].move
                baseState = stateAt(current + 1)
                animMove = invertMove(mv)
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, tween(450))
                baseState = stateAt(current)
                animMove = null
            }
            else -> {
                // Jump (chip tap / reset): snap, no animation.
                baseState = stateAt(current)
                animMove = null
                animProgress.snapTo(0f)
            }
        }
    }

    val done = current >= total
    val currentStageIndex = if (done) stages.size - 1 else flat[current].stageIndex
    val stage = stages.getOrNull(currentStageIndex)

    // Highlight what still needs fixing in the current stage, so the user
    // learns to recognise each pattern. The pieces move as the solve
    // progresses, so this follows the displayed state.
    // F2L: the pair's corner + edge. OLL: pieces not oriented yet.
    // PLL: pieces not in their solved position yet.
    val highlightFacelets: Set<Int> = remember(baseState, currentStageIndex, done) {
        if (done || stage == null) {
            emptySet()
        } else {
            try {
                when (stage.key) {
                    "f2l" -> if (stage.pairIndex >= 0) {
                        CfopSolver.f2lPairFacelets(baseState, stage.pairIndex).toSet()
                    } else {
                        emptySet()
                    }
                    "oll_eo" -> CfopSolver.ollEoFacelets(baseState).toSet()
                    "oll_co" -> CfopSolver.ollCoFacelets(baseState).toSet()
                    "pll_c" -> CfopSolver.pllCornersFacelets(baseState).toSet()
                    "pll_e" -> CfopSolver.pllEdgesFacelets(baseState).toSet()
                    else -> emptySet()
                }
            } catch (e: Exception) {
                emptySet()
            }
        }
    }

    // Stage-specific hint for the highlight (hiện trong hộp thoại "Giải thích").
    val highlightHint: String? = if (done || stage == null || highlightFacelets.isEmpty()) {
        null
    } else {
        when (stage.key) {
            "f2l" -> "👀 2 viên cần ghép đang nhấp nháy vàng trên khối 3D."
            "oll_eo" -> "👀 Các cạnh chưa lật đúng đang nhấp nháy — nhìn hình chúng tạo thành (chấm / chữ L / đường thẳng)."
            "oll_co" -> "👀 Các góc chưa xoay đúng đang nhấp nháy — nhìn vị trí chúng để nhận ra trường hợp."
            "pll_c" -> "👀 Các góc chưa về đúng chỗ đang nhấp nháy — chúng sẽ đổi chỗ cho nhau."
            "pll_e" -> "👀 Các cạnh chưa về đúng chỗ đang nhấp nháy — chúng sẽ đổi chỗ cho nhau."
            else -> null
        }
    }

    LaunchedEffect(autoPlay, current) {
        if (autoPlay && current < total) {
            delay(900)
            current++
        } else if (current >= total) {
            autoPlay = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Đầu: tiêu đề + tiến độ (gọn, 1 dòng + thanh) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (done) "Xong! Khối đã giải 🎉" else "Giải từng bước — CFOP",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("$current / $total nước", fontSize = 13.sp, color = Color.Gray)
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else current.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )

        // --- Khối 3D chiếm toàn bộ chỗ còn lại, luôn là hình vuông vừa khít ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            Cube3DView(
                state = baseState,
                animMove = animMove,
                animProgress = animProgress.value,
                modifier = Modifier.size(side),
                highlight = highlightFacelets,
            )
        }

        // --- Bước hiện tại: tiêu đề + chip nước đi + 2 nút nhỏ ---
        if (stage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (done) Color(0xFFF1F8E9) else Color(0xFFE3F2FD))
                    .border(
                        1.dp,
                        if (done) Color(0xFF9CCC65) else Color(0xFF1976D2),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stage.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${currentStageIndex + 1}/${stages.size}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 104.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    MoveChips(
                        stage = stage,
                        start = stageStarts[currentStageIndex],
                        current = current,
                        onJumpToMove = { idx ->
                            autoPlay = false
                            current = idx
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showNote = true }) {
                        Text("💡 Giải thích", fontSize = 14.sp)
                    }
                    TextButton(onClick = { showAllSteps = true }) {
                        Text("📋 Tất cả bước", fontSize = 14.sp)
                    }
                }
            }
        }

        // --- Nút điều khiển: luôn nằm sát đáy, không bao giờ bị che ---
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    autoPlay = false
                    if (current > 0) current--
                },
                enabled = current > 0,
                modifier = Modifier.height(52.dp),
            ) { Text("◀ Lùi") }
            Button(
                onClick = {
                    if (current < total) {
                        autoPlay = false
                        current++
                    }
                },
                enabled = current < total,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Text(if (current == 0) "Bắt đầu ▶" else "Bước tiếp ▶", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { autoPlay = !autoPlay },
                enabled = current < total,
                modifier = Modifier.height(52.dp),
            ) { Text(if (autoPlay) "⏸ Dừng" else "▶ Tự chạy") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("← Sửa khối", fontSize = 13.sp) }
            TextButton(onClick = {
                autoPlay = false
                current = 0
            }) { Text("↺ Từ đầu", fontSize = 13.sp) }
            TextButton(onClick = onNewCube) { Text("Khối mới", fontSize = 13.sp) }
        }
    }

    // Hộp thoại giải thích bước hiện tại (dài thì cuộn trong hộp thoại).
    if (showNote && stage != null) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text(stage.title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stageExplanations[currentStageIndex], fontSize = 14.sp)
                    if (highlightHint != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            highlightHint,
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ký hiệu theo hướng cầm chuẩn: xanh lá ở trước, vàng ở trên. " +
                            "R phải, L trái, U trên, D dưới, F trước, B sau; M là lớp giữa. " +
                            "y / y' / y2 là xoay CẢ khối (không phải xoay một mặt). " +
                            "Không dấu = chiều kim đồng hồ, ' = ngược chiều, 2 = 180°. " +
                            "Kéo trên khối 3D để đổi góc nhìn (chỉ để xem).",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNote = false }) { Text("Đóng") }
            },
        )
    }

    // Dialog xem tất cả bước dạng dọc.
    if (showAllSteps) {
        AlertDialog(
            onDismissRequest = { showAllSteps = false },
            title = { Text("Tất cả các bước", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    stages.forEachIndexed { si, st ->
                        StageCard(
                            stage = st,
                            isCurrent = si == currentStageIndex && !done,
                            isPast = si < currentStageIndex || done,
                            explanation = stageExplanations[si],
                        ) {
                            MoveChips(
                                stage = st,
                                start = stageStarts[si],
                                current = current,
                                onJumpToMove = { idx ->
                                    autoPlay = false
                                    current = idx
                                    showAllSteps = false
                                },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllSteps = false }) {
                    Text("Đóng")
                }
            },
        )
    }
}
