package vn.cfop.rubik.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube
import vn.cfop.rubik.solver.CubeOrientation

private fun invertMove(m: String): String = when {
    m.endsWith("'") -> m.dropLast(1)
    m.endsWith("2") -> m
    else -> m + "'"
}

/** Chip showing one move. */
@Composable
private fun MoveChip(move: String, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                if (highlighted) Color(0xFFFFEB3B) else Color(0xFFE0E0E0),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(move, fontSize = 15.sp, fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun TrainingScreen(onBack: () -> Unit, onTest: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("training", android.content.Context.MODE_PRIVATE)
    }
    // Learned case ids, persisted.
    var learned by remember {
        mutableStateOf(prefs.getStringSet("learned", emptySet()) ?: emptySet())
    }
    fun toggleLearned(id: String) {
        val next = if (learned.contains(id)) learned - id else learned + id
        learned = next
        prefs.edit().putStringSet("learned", next).apply()
    }

    var level by remember { mutableStateOf("basic") }
    var stage by remember { mutableStateOf("f2l") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showTheory by remember { mutableStateOf(false) }

    val cases = remember(level, stage) {
        ALL_TRAINING_CASES.filter { it.level == level && it.stage == stage }
    }
    val selected = cases.firstOrNull { it.id == selectedId }

    val stageTitle = when (stage) {
        "f2l" -> "F2L — Ghép 2 tầng dưới"
        "oll" -> "OLL — Lật mặt vàng"
        else -> "PLL — Đổi chỗ tầng trên"
    }
    val theory = when (stage) {
        "f2l" -> THEORY_F2L
        "oll" -> THEORY_OLL
        else -> THEORY_PLL
    }

    if (showTheory) {
        AlertDialog(
            onDismissRequest = { showTheory = false },
            title = { Text(stageTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(theory, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTheory = false }) { Text("Đóng") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("← Trang chính") }
            Spacer(Modifier.weight(1f))
            Button(onClick = onTest) { Text("📝 Kiểm tra") }
        }
        Spacer(Modifier.height(12.dp))
        Text("Luyện tập CFOP", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Mỗi trường hợp chỉ có 1 công thức cố định. Nhận ra hình là biết phải làm gì.",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(12.dp))

        // Level selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("basic" to "Cơ bản", "advanced" to "Nâng cao").forEach { (lv, label) ->
                val chosen = level == lv
                Button(
                    onClick = { level = lv; selectedId = null },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        label,
                        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                        color = if (chosen) Color.White else Color.Black,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Stage selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("f2l" to "F2L", "oll" to "OLL", "pll" to "PLL").forEach { (st, label) ->
                val chosen = stage == st
                OutlinedButton(
                    onClick = { stage = st; selectedId = null },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        label,
                        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stageTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { showTheory = true }) { Text("📖 Lý thuyết") }
        }
        val learnedCount = cases.count { learned.contains(it.id) }
        Text(
            "Đã tập: $learnedCount/${cases.size} trường hợp",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))

        if (selected == null) {
            // Case list
            cases.forEach { c ->
                val done = learned.contains(c.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (done) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                        .clickable { selectedId = c.id }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (done) "✅ " else "○ ", fontSize = 16.sp)
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            c.algorithm.joinToString(" "),
                            fontSize = 13.sp, color = Color.Gray,
                        )
                    }
                    Text("›", fontSize = 20.sp, color = Color.Gray)
                }
            }
            if (cases.isEmpty()) {
                Text(
                    "Chưa có trường hợp nào ở cấp này. Hãy học Cơ bản trước.",
                    fontSize = 14.sp, color = Color.Gray,
                )
            }
        } else {
            CaseDetail(
                case = selected,
                learned = learned.contains(selected.id),
                onToggleLearned = { toggleLearned(selected.id) },
                onBack = { selectedId = null },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CaseDetail(
    case: TrainingCase,
    learned: Boolean,
    onToggleLearned: () -> Unit,
    onBack: () -> Unit,
) {
    val pattern = remember(case) { case.patternState() }
    var step by remember(case) { mutableIntStateOf(0) } // algorithm moves applied
    var autoPlay by remember { mutableStateOf(false) }
    val total = case.algorithm.size

    // Khung hình cố định cho luyện tập: xanh lá trước, cam phải. Không xoay cả khối.
    fun dispMove(m: String): String = m

    fun stateAt(k: Int): IntArray {
        val cube = Cube(pattern)
        for (i in 0 until k) cube.apply(case.algorithm[i])
        return cube.s
    }

    var baseState by remember(case) { mutableStateOf(pattern.copyOf()) }
    var animMove by remember(case) { mutableStateOf<String?>(null) }
    val animProgress = remember(case) { Animatable(0f) }
    var lastStep by remember(case) { mutableIntStateOf(0) }

    LaunchedEffect(step) {
        val delta = step - lastStep
        lastStep = step
        when (delta) {
            1 -> {
                val mv = dispMove(case.algorithm[step - 1])
                baseState = stateAt(step - 1)
                animMove = mv
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, tween(450))
                baseState = stateAt(step)
                animMove = null
            }
            -1 -> {
                val mv = dispMove(case.algorithm[step])
                baseState = stateAt(step + 1)
                animMove = invertMove(mv)
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, tween(450))
                baseState = stateAt(step)
                animMove = null
            }
            else -> {
                baseState = stateAt(step)
                animMove = null
            }
        }
    }
    LaunchedEffect(autoPlay, step) {
        if (autoPlay && step < total) {
            delay(900)
            step++
        } else if (step >= total) {
            autoPlay = false
        }
    }

    // Highlight the pieces that need moving, following them as the
    // algorithm runs. F2L: the pair's corner + edge. OLL: pieces not
    // oriented yet. PLL: pieces not in place yet.
    val highlight: Set<Int> = remember(baseState, case) {
        try {
            when (case.stage) {
                "f2l" -> CfopSolver.f2lPairFacelets(baseState, 0).toSet()
                "oll" -> (CfopSolver.ollEoFacelets(baseState).toList()
                    + CfopSolver.ollCoFacelets(baseState).toList()).toSet()
                "pll" -> (CfopSolver.pllCornersFacelets(baseState).toList()
                    + CfopSolver.pllEdgesFacelets(baseState).toList()).toSet()
                else -> emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }
    val highlightHint = when (case.stage) {
        "f2l" -> "👀 2 viên cần ghép đang nhấp nháy vàng — góc và cạnh xanh lá - cam, tìm chúng trước rồi mới xoay."
        "oll" -> "👀 Các viên chưa đúng đang nhấp nháy — nhìn hình chúng tạo thành để nhận biết trường hợp."
        "pll" -> "👀 Các viên chưa về đúng chỗ đang nhấp nháy — chúng sẽ đổi chỗ cho nhau."
        else -> null
    }

    Column {
        OutlinedButton(onClick = onBack) { Text("← Danh sách") }
        Spacer(Modifier.height(8.dp))
        Text(case.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(case.recognition, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // 3D pattern cube
        Cube3DView(
            state = baseState,
            animMove = animMove,
            animProgress = animProgress.value,
            highlight = highlight,
            modifier = Modifier.fillMaxWidth(),
        )
        if (highlightHint != null && step == 0) {
            Text(
                highlightHint,
                fontSize = 13.sp,
                color = Color(0xFFE65100),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            if (step == 0) "Hình cần tập — khối đang ở trạng thái ví dụ."
            else if (step < total) "Đang làm nước ${step + 1}/$total: ${dispMove(case.algorithm[step])}"
            else "Xong! Khối đã về trạng thái đã giải (đối với bước này).",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))

        // Algorithm + step controls
        Text("Công thức:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            case.algorithm.forEachIndexed { i, m ->
                MoveChip(dispMove(m), highlighted = i == step && step < total)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { autoPlay = false; step = 0 }) { Text("↺ Làm lại") }
            OutlinedButton(
                onClick = { autoPlay = false; if (step > 0) step-- },
                enabled = step > 0,
            ) { Text("← Trước") }
            Button(onClick = {
                if (step >= total) {
                    step = 0
                    autoPlay = true
                } else {
                    autoPlay = !autoPlay
                }
            }) {
                Text(if (autoPlay) "⏸ Dừng" else if (step >= total) "▶ Chạy lại" else "▶ Chạy")
            }
            OutlinedButton(
                onClick = { autoPlay = false; if (step < total) step++ },
                enabled = step < total,
            ) { Text("Sau →") }
        }
        Spacer(Modifier.height(8.dp))
        Text(case.tip, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))

        // Setup for the real cube
        Text("Tạo trên khối thật:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "Xoay khối đã giải theo các nước dưới đây để tạo đúng hình này, rồi tập làm công thức trên.",
            fontSize = 13.sp, color = Color.Gray,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            case.setup.forEach { m -> MoveChip(m) }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onToggleLearned,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (learned) "✅ Đã thuộc — Bỏ đánh dấu" else "Đánh dấu đã thuộc")
        }
        Spacer(Modifier.height(8.dp))
    }
}
