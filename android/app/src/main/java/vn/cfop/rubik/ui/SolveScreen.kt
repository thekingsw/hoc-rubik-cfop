package vn.cfop.rubik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube

private val STAGE_TIPS = mapOf(
    "cross" to "Đưa 4 cạnh trắng xuống mặt dưới, khớp màu với tâm các mặt bên.",
    "f2l" to "Ghép từng cặp góc + cạnh rồi đưa vào đúng vị trí, không phá chữ thập.",
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

@Composable
fun SolveScreen(
    initialState: IntArray,
    stages: List<CfopSolver.Stage>,
    onBack: () -> Unit,
    onNewCube: () -> Unit,
) {
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

    // Facelet state after the first k moves.
    fun stateAt(k: Int): IntArray {
        val cube = Cube(initialState)
        for (i in 0 until k) {
            cube.apply(flat[i].move)
        }
        return cube.s
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
        )
        Text(
            "Kéo để xoay khối xem các mặt",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )
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

        // Stages
        stages.forEachIndexed { si, stage ->
            val isCurrent = si == currentStageIndex && !done
            val isPast = si < currentStageIndex || done
            val start = stageStarts[si]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
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
                    STAGE_TIPS[stage.key] ?: "",
                    fontSize = 13.sp,
                    color = Color(0xFF616161),
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(8.dp))
                // Move chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                .clickable {
                                    autoPlay = false
                                    current = globalIdx
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                mv,
                                fontSize = 15.sp,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                color = if (isNext) Color.White else Color.Black,
                            )
                        }
                    }
                }
            }
        }

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
            "Ký hiệu: R phải, L trái, U trên, D dưới, F trước, B sau. " +
                "M là lớp giữa (xoay như L). x là xoay cả khối theo chiều R. " +
                "Không dấu = xoay theo chiều kim đồng hồ, ' = ngược chiều, 2 = xoay 180°.",
            fontSize = 12.sp,
            color = Color.Gray,
        )
    }
}
