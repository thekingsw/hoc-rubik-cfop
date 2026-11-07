package vn.cfop.rubik.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube
import vn.cfop.rubik.solver.CubeOrientation

private val STAGE_TIPS = mapOf(
    "cross" to "Đưa 4 cạnh trắng xuống mặt dưới, khớp màu với tâm các mặt bên.",
    "f2l" to "4 bước: đưa khe về trước-phải (xoay cả khối trên tay rồi bấm nút xoay) → hất viên kẹt lên tầng trên → xoay U về trường hợp chuẩn → làm R U R' hoặc F' U F theo ký hiệu mới.",
    "oll_eo" to "Lật các cạnh tầng trên để mặt trên thành một màu.",
    "oll_co" to "Xoay các góc tầng trên để mặt trên thành một màu.",
    "pll_c" to "Đổi chỗ các góc tầng trên về đúng vị trí.",
    "pll_e" to "Đổi chỗ các cạnh tầng trên về đúng vị trí. Xong!",
)

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
    dispMove: (String) -> String,
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
            val disp = dispMove(mv)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    disp,
                    fontSize = 15.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNext) Color.White else Color.Black,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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

    // Các bước (stage) dàn hàng ngang: pager tự chạy theo bước hiện tại,
    // lúc nào bước hiện tại cũng nằm ngay dưới khối rubik.
    var showAllSteps by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { stages.size })
    // Về trang đầu khi đổi sang khối mới.
    LaunchedEffect(stages) {
        pagerState.scrollToPage(0)
    }

    // Xoay cả khối trên tay: app theo dõi hướng cầm (viewRot) và tự dịch
    // mọi nước còn lại sang ký hiệu mới — B thật sự thành F/R/L theo hướng
    // anh đang cầm. (Góp ý của anh 2026-10-31: "xoay khối thì công thức
    // phải đổi theo".)
    var viewRot by remember { mutableStateOf(CubeOrientation.identity()) }
    var rotating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun dispMove(m: String): String = CubeOrientation.translateForView(m, viewRot)

    // Facelet state after the first k moves, shown in the current view
    // orientation (rotated if the user turned the cube).
    fun stateAt(k: Int): IntArray {
        val cube = Cube(initialState)
        for (i in 0 until k) {
            cube.apply(flat[i].move)
        }
        return CubeOrientation.applyView(cube.s, viewRot)
    }

    // --- Case explanations: which training case each stage is, in plain
    // language, so the user can connect the solver's moves to practice.
    // Computed once from the state at each stage's start.
    val stageExplanations = remember(stages, initialState) {
        stages.mapIndexed { si, st ->
            val startState = stateAt(stageStarts[si])
            when (st.key) {
                "cross" -> STAGE_TIPS["cross"] ?: ""
                "f2l" -> {
                    describeF2LSteps(startState, st.pairIndex, st.moves)
                }
                "oll_eo" -> {
                    if (isJustUTurns(st.moves)) {
                        "Chỉ cần xoay mặt trên cho khớp, không cần công thức."
                    } else {
                        val pattern = ollEoPatternName(startState)
                        if (pattern != null) {
                            "Trường hợp “$pattern” trong phần Luyện tập → OLL (lật cạnh). Công thức dưới đây sẽ đưa tất cả cạnh về màu vàng."
                        } else {
                            STAGE_TIPS["oll_eo"] ?: ""
                        }
                    }
                }
                "oll_co" -> {
                    if (isJustUTurns(st.moves)) {
                        "Chỉ cần xoay mặt trên cho khớp, không cần công thức."
                    } else {
                        val co = ollCoCase(st.moves)
                        if (co != null) {
                            "Trường hợp “${co.name}” trong phần Luyện tập → OLL (xoay góc). ${co.recognition}"
                        } else if (isCoSuneFallback(st.moves)) {
                            "Trường hợp T/L (không nằm trong 5 công thức góc) — làm Sune 1 lần rồi nhìn lại, sẽ ra 1 trong 5 hình kia."
                        } else {
                            STAGE_TIPS["oll_co"] ?: ""
                        }
                    }
                }
                "pll_c" -> {
                    if (isJustUTurns(st.moves)) {
                        "Chỉ cần xoay mặt trên cho khớp, không cần công thức."
                    } else {
                        val pc = pllCase(st.moves, corners = true)
                        if (pc != null) {
                            "Trường hợp “${pc.name}” trong phần Luyện tập → PLL (đổi góc). ${pc.recognition}"
                        } else {
                            STAGE_TIPS["pll_c"] ?: ""
                        }
                    }
                }
                "pll_e" -> {
                    if (isJustUTurns(st.moves)) {
                        "Chỉ cần xoay mặt trên cho khớp, không cần công thức."
                    } else {
                        val pe = pllCase(st.moves, corners = false)
                        if (pe != null) {
                            "Trường hợp “${pe.name}” trong phần Luyện tập → PLL (đổi cạnh). ${pe.recognition}"
                        } else {
                            STAGE_TIPS["pll_e"] ?: ""
                        }
                    }
                }
                else -> STAGE_TIPS[st.key] ?: ""
            }
        }
    }

    // --- 3D animation state ---
    // baseState = facelets shown as the animation base (state before
    // the animated move). animMove = the move being turned, progress 0..1.
    var baseState by remember { mutableStateOf(initialState.copyOf()) }
    var animMove by remember { mutableStateOf<String?>(null) }
    val animProgress = remember { Animatable(0f) }
    var lastCurrent by remember { mutableIntStateOf(0) }

    /** Xoay cả khối trên tay rồi bấm nút — khối 3D xoay theo, các nước còn
     *  lại tự đổi ký hiệu theo hướng cầm mới (B thành F/R/L thật). */
    fun onRotate(rot: String) {
        if (rotating) return
        scope.launch {
            rotating = true
            autoPlay = false
            baseState = stateAt(current)
            animMove = rot
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, tween(450))
            animMove = null
            viewRot = CubeOrientation.composeView(viewRot, rot)
            baseState = stateAt(current)
            rotating = false
        }
    }

    LaunchedEffect(current) {
        val delta = current - lastCurrent
        lastCurrent = current
        when (delta) {
            1 -> {
                // One step forward: animate the move just applied (in display notation).
                val mv = dispMove(flat[current - 1].move)
                baseState = stateAt(current - 1)
                animMove = mv
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, tween(450))
                baseState = stateAt(current)
                animMove = null
            }
            -1 -> {
                // One step back: animate the inverse of the move undone.
                val mv = dispMove(flat[current].move)
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

    // Pager tự đi ngang theo bước hiện tại (từ trái sang phải).
    LaunchedEffect(currentStageIndex) {
        pagerState.animateScrollToPage(currentStageIndex)
    }

    // Highlight what still needs fixing in the current stage, so the user
    // learns to recognise each pattern. The pieces move as the solve
    // progresses, so this follows the displayed state.
    // F2L: the pair's corner + edge. OLL: pieces not oriented yet.
    // PLL: pieces not in their solved position yet.
    val highlightFacelets: Set<Int> = remember(baseState, currentStageIndex, done) {
        if (done) {
            emptySet()
        } else {
            val st = stages[currentStageIndex]
            try {
                when (st.key) {
                    "f2l" -> if (st.pairIndex >= 0) {
                        CfopSolver.f2lPairFacelets(baseState, st.pairIndex).toSet()
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

    // Stage-specific hint for the highlight.
    val highlightHint: String? = if (done || highlightFacelets.isEmpty()) {
        null
    } else {
        when (stages[currentStageIndex].key) {
            "f2l" -> "👀 2 viên cần ghép đang nhấp nháy vàng — tìm chúng trên khối rồi làm theo các nước dưới đây."
            "oll_eo" -> "👀 Các cạnh chưa lật đúng đang nhấp nháy — nhìn hình chúng tạo thành để nhận biết trường hợp (thẳng / chữ L / chấm)."
            "oll_co" -> "👀 Các góc chưa xoay đúng đang nhấp nháy — nhìn vị trí chúng để nhận biết trường hợp."
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (done) "Xong! Khối đã được giải 🎉" else "Hướng dẫn từng bước — CFOP",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else current.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Bước $current / $total",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))

        Cube3DView(
            state = baseState,
            animMove = animMove,
            animProgress = animProgress.value,
            highlight = highlightFacelets,
        )
        if (highlightHint != null) {
            Text(
                highlightHint,
                fontSize = 13.sp,
                color = Color(0xFFE65100),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            "Kéo để xoay góc nhìn (chỉ xem, không đổi ký hiệu)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        // === Bước hiện tại luôn nằm ngay dưới khối rubik ===
        // Các bước dàn hàng ngang: pager tự đi từ trái sang phải theo tiến trình.
        // Vuốt ngang để ngó bước khác; bấm "Bước tiếp" là pager tự về bước hiện tại.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { it },
        ) { page ->
            StageCard(
                stage = stages[page],
                isCurrent = page == currentStageIndex && !done,
                isPast = page < currentStageIndex || done,
                explanation = stageExplanations[page],
            ) {
                MoveChips(
                    stage = stages[page],
                    start = stageStarts[page],
                    current = current,
                    dispMove = ::dispMove,
                    onJumpToMove = { idx ->
                        autoPlay = false
                        current = idx
                    },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Bước ${currentStageIndex + 1}/${stages.size}",
                fontSize = 13.sp,
                color = Color.Gray,
            )
            TextButton(onClick = { showAllSteps = true }) {
                Text("📋 Xem tất cả bước", fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Xoay cả khối trên tay rồi bấm nút tương ứng — ký hiệu các nước
        // còn lại tự đổi theo hướng cầm mới.
        Text(
            "Xoay khối trên tay rồi bấm:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0),
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onRotate("y'") },
                enabled = !rotating && !done,
            ) { Text("◀ Trái ra trước", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { onRotate("y2") },
                enabled = !rotating && !done,
            ) { Text("Sau ra trước", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { onRotate("y") },
                enabled = !rotating && !done,
            ) { Text("Phải ra trước ▶", fontSize = 13.sp) }
        }
        Spacer(Modifier.height(16.dp))

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    autoPlay = false
                    if (current > 0) current--
                },
                enabled = current > 0,
            ) { Text("◀ Lùi") }
            Button(
                onClick = {
                    if (current < total) {
                        autoPlay = false
                        current++
                    }
                },
                enabled = current < total,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (current == 0) "Bắt đầu ▶" else "Bước tiếp ▶", fontSize = 15.sp)
            }
            OutlinedButton(
                onClick = { autoPlay = !autoPlay },
                enabled = current < total,
            ) { Text(if (autoPlay) "⏸ Dừng" else "▶ Tự chạy") }
        }
        Spacer(Modifier.height(16.dp))

        if (done) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Chúc mừng anh! Hãy xáo lại khối và giải tiếp để nhớ công thức nhé.",
                fontSize = 14.sp,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Sửa khối") }
            OutlinedButton(onClick = {
                autoPlay = false
                current = 0
            }) { Text("↺ Chơi lại từ đầu") }
            Button(onClick = onNewCube) { Text("Khối mới") }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Ký hiệu luôn theo hướng anh đang cầm khối: R là mặt bên phải, L trái, U trên, " +
                "D dưới, F trước mặt, B sau lưng — không theo màu cố định. " +
                "M là lớp giữa (xoay như L). " +
                "Nếu anh xoay cả khối trên tay, bấm nút xoay tương ứng ở trên: " +
                "khối 3D xoay theo và mọi nước còn lại tự đổi ký hiệu cho khớp. " +
                "Không dấu = theo chiều kim đồng hồ, ' = ngược chiều, 2 = xoay 180°.",
            fontSize = 12.sp,
            color = Color.Gray,
        )
    }

    // Dialog xem tất cả bước dạng dọc (như trước đây).
    if (showAllSteps) {
        AlertDialog(
            onDismissRequest = { showAllSteps = false },
            title = { Text("Tất cả các bước", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    stages.forEachIndexed { si, stage ->
                        StageCard(
                            stage = stage,
                            isCurrent = si == currentStageIndex && !done,
                            isPast = si < currentStageIndex || done,
                            explanation = stageExplanations[si],
                        ) {
                            MoveChips(
                                stage = stage,
                                start = stageStarts[si],
                                current = current,
                                dispMove = ::dispMove,
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
