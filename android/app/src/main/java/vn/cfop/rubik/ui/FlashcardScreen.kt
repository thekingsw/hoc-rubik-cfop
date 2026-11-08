package vn.cfop.rubik.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlin.random.Random
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.F2lCases
import vn.cfop.rubik.solver.F2lPractice

/**
 * Thẻ nhớ F2L: nhìn hình → nhớ công thức → làm trên khối thật → tự chấm.
 * Lịch ôn (Srs.kt) nhắc lại đúng lúc sắp quên. Mỗi lần hiện, case được đặt ở một khe
 * và độ xoay U ngẫu nhiên (F2lPractice) để không học vẹt theo một hướng.
 */
private val CASE_ORDER: List<String> = F2L_CASES.map { it.id }
private const val NEW_BATCH = 3

private fun caseNumber(id: String): Int = id.removePrefix("f2l-").toInt()

private class CardSession(val caseId: String, val drill: F2lPractice.Drill)

private fun makeDrill(caseId: String, rnd: Random): F2lPractice.Drill {
    val n = caseNumber(caseId)
    repeat(20) {
        val d = F2lPractice.make(n, rnd.nextInt(4), rnd.nextInt(4))
        if (d != null) return d
    }
    return F2lPractice.make(n, 0, 0)!!
}

/** Mặt để xoay góc nhìn sao cho khe cần giải nằm trong tầm mắt: F, F, R, L cho khe 0..3. */
private fun viewFaceFor(slot: Int): Int = when (slot) {
    0, 1 -> 2
    2 -> 1
    else -> 4
}

@Composable
fun FlashcardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { StudyStore.from(context) }
    var data by remember {
        val loaded = store.load()
        mutableStateOf(
            if (loaded.studySet.isEmpty()) {
                loaded.copy(studySet = CASE_ORDER.take(NEW_BATCH).toSet())
            } else {
                loaded
            },
        )
    }
    var playing by remember { mutableStateOf(false) }
    var startEarly by remember { mutableStateOf(false) }

    fun update(d: StudyData) {
        data = d
        store.save(d)
    }

    if (playing) {
        FlashcardSession(
            data = data,
            startEarly = startEarly,
            onData = { update(it) },
            onExit = { playing = false },
        )
    } else {
        FlashcardMap(
            data = data,
            onData = { update(it) },
            onStart = { early ->
                startEarly = early
                playing = true
            },
            onBack = onBack,
        )
    }
}

// ------------------------------------------------------------------ bản đồ 41 case

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardMap(
    data: StudyData,
    onData: (StudyData) -> Unit,
    onStart: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val due = Srs.dueCount(CASE_ORDER, data.progress, data.studySet, now)
    val fresh = Srs.newCount(CASE_ORDER, data.progress, data.studySet)
    val nextAt = Srs.nextDueAt(CASE_ORDER, data.progress, data.studySet)
    val canStart = due + fresh > 0
    val toAdd = CASE_ORDER.filter { it !in data.studySet }.take(NEW_BATCH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("← Luyện tập") }
        }
        Spacer(Modifier.height(12.dp))
        Text("🃏 Thẻ nhớ F2L", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Nhìn hình → nhớ công thức → làm trên khối thật → tự chấm. " +
                "App nhắc ôn lại đúng lúc sắp quên.",
            fontSize = 13.sp,
            color = Color.Gray,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Đang học ${data.studySet.size} case · cần ôn: $due · mới: $fresh",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStart(!canStart) },
            enabled = data.studySet.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(if (canStart) "▶ Bắt đầu luyện" else "⏩ Ôn sớm", fontSize = 16.sp)
        }
        if (!canStart && nextAt != null) {
            Text(
                "Chưa có thẻ nào đến hạn. Thẻ gần nhất sau ${Srs.describeWait(nextAt - now)}.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onData(data.copy(studySet = data.studySet + toAdd)) },
            enabled = toAdd.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("＋ Thêm 3 case tiếp theo")
        }
        Spacer(Modifier.height(16.dp))
        Text("41 trường hợp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CASE_ORDER.forEach { id ->
                val inSet = id in data.studySet
                val level = Srs.level(data.progress[id])
                val isDue = inSet && Srs.isDue(data.progress[id] ?: CardProgress(), now)
                val bg = when {
                    !inSet -> Color(0xFFEEEEEE)
                    level < 0 -> Color.White
                    level <= 1 -> Color(0xFFFFCCBC)
                    level <= 3 -> Color(0xFFFFF59D)
                    else -> Color(0xFFC8E6C9)
                }
                val borderColor = when {
                    isDue -> Color(0xFFD32F2F)
                    inSet -> Color(0xFF1976D2)
                    else -> Color(0xFFBDBDBD)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(if (inSet) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            val newSet = if (inSet) data.studySet - id else data.studySet + id
                            onData(data.copy(studySet = newSet))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${caseNumber(id)}",
                        fontSize = 14.sp,
                        color = if (inSet) Color.Black else Color.Gray,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Chạm vào số để thêm / bỏ khỏi bộ đang học. Xám: chưa học · trắng: mới · " +
                "cam: còn yếu · vàng: khá · xanh: thuộc · viền đỏ: đến hạn ôn.",
            fontSize = 12.sp,
            color = Color.Gray,
        )
    }
}

// ------------------------------------------------------------------ phiên luyện

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashChips(moves: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        moves.forEach { mv ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(mv, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun FlashcardSession(
    data: StudyData,
    startEarly: Boolean,
    onData: (StudyData) -> Unit,
    onExit: () -> Unit,
) {
    val rnd = remember { Random(System.nanoTime()) }

    fun draw(exclude: String?, early: Boolean): CardSession? {
        val id = Srs.pickNext(
            CASE_ORDER, data.progress, data.studySet, System.currentTimeMillis(), exclude, early,
        ) ?: return null
        return CardSession(id, makeDrill(id, rnd))
    }

    var card by remember { mutableStateOf<CardSession?>(draw(null, startEarly)) }
    var phase by remember { mutableIntStateOf(0) } // 0: đang hỏi · 1: đã xem công thức · 2: đã chấm
    var seconds by remember { mutableIntStateOf(0) }
    var showSetup by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var done by remember { mutableIntStateOf(0) }

    // Đồng hồ "thời gian nhớ": chạy từ lúc hiện thẻ đến lúc chấm.
    LaunchedEffect(done, phase) {
        if (phase < 2) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    fun grade(g: Grade) {
        val c = card ?: return
        val now = System.currentTimeMillis()
        val updated = Srs.apply(data.progress[c.caseId] ?: CardProgress(), g, now, seconds * 1000L)
        onData(data.copy(progress = data.progress + (c.caseId to updated)))
        resultText = when (g) {
            Grade.GOOD -> "✅ Nhớ ngay (${seconds}s) — ôn lại sau ${Srs.describeWait(updated.dueAt - now)}."
            Grade.SLOW -> "🐢 Chậm — ôn lại sau ${Srs.describeWait(updated.dueAt - now)}."
            Grade.FORGOT -> "😵 Quên — hỏi lại sau ${Srs.describeWait(updated.dueAt - now)}. Xem kỹ công thức bên trên nhé."
        }
        phase = 2
    }

    fun next(early: Boolean) {
        card = draw(card?.caseId, early)
        phase = 0
        seconds = 0
        showSetup = false
        done++
    }

    val current = card
    if (current == null) {
        // Hết thẻ trong lượt này.
        val now = System.currentTimeMillis()
        val nextAt = Srs.nextDueAt(CASE_ORDER, data.progress, data.studySet)
        val toAdd = CASE_ORDER.filter { it !in data.studySet }.take(NEW_BATCH)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Xong lượt này 🎉", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (nextAt != null) {
                    "Thẻ kế tiếp đến hạn sau ${Srs.describeWait(nextAt - now)}."
                } else {
                    "Bộ đang học trống — thêm case ở bản đồ."
                },
                fontSize = 14.sp,
                color = Color.Gray,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { next(true) },
                enabled = data.studySet.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("⏩ Ôn thêm ngay") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onData(data.copy(studySet = data.studySet + toAdd))
                    onExit()
                },
                enabled = toAdd.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("＋ Thêm 3 case mới") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onExit) { Text("← Về bản đồ") }
        }
        return
    }

    val drill = current.drill
    val fc = F2lCases.byNumber(drill.caseNumber)
    val pairFacelets: Set<Int> = remember(current) {
        CfopSolver.f2lPairFacelets(drill.state, drill.slot).toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onExit) { Text("← Bản đồ", fontSize = 14.sp) }
            Text("Thẻ ${done + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("⏱ ${seconds}s", fontSize = 14.sp, color = Color.Gray)
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            Cube3DView(
                state = drill.state,
                animMove = null,
                animProgress = 0f,
                modifier = Modifier.size(side),
                highlight = emptySet(),
                dimExcept = pairFacelets,
                viewFace = viewFaceFor(drill.slot),
            )
        }

        // --- Khung câu hỏi / đáp án (gọn, cuộn bên trong nếu dài) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (phase == 2) Color(0xFFF1F8E9) else Color(0xFFE3F2FD))
                .border(
                    1.dp,
                    if (phase == 2) Color(0xFF9CCC65) else Color(0xFF1976D2),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .heightIn(max = 170.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Khe cần giải: ${drill.slotName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (phase == 0) {
                Text(
                    "Đây là case nào, công thức ra sao? Nhớ rồi làm thử trên khối thật. " +
                        "Kéo khối 3D để xoay xem.",
                    fontSize = 13.sp,
                    color = Color(0xFF424242),
                )
                if (showSetup) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Từ khối đã giải (xanh lá ở trước, vàng ở trên), làm các nước này:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                    FlashChips(drill.setup)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { showSetup = !showSetup }) {
                        Text(if (showSetup) "🙈 Ẩn nước xáo" else "🔧 Tạo trên khối thật", fontSize = 14.sp)
                    }
                    TextButton(onClick = { phase = 1 }) {
                        Text("👁 Xem công thức", fontSize = 14.sp)
                    }
                }
            } else {
                Text(
                    "Case ${fc.number}/41 — ${F2lCases.GROUP_NAMES[fc.group]}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(fc.recognition, fontSize = 12.sp, color = Color(0xFF616161))
                Spacer(Modifier.height(4.dp))
                if (drill.align.isNotEmpty()) {
                    Text("Xoay U cho đúng thế:", fontSize = 12.sp, color = Color.Gray)
                    FlashChips(drill.align)
                    Spacer(Modifier.height(4.dp))
                }
                Text("Công thức:", fontSize = 12.sp, color = Color.Gray)
                FlashChips(drill.formula)
                if (drill.slot != 0) {
                    Text(
                        "y / y' / y2 là xoay CẢ khối để đưa khe ra trước-phải rồi xoay lại.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (phase == 2) {
                    Text(
                        resultText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        // --- Nút chấm điểm: luôn sát đáy ---
        Spacer(Modifier.height(6.dp))
        if (phase < 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { grade(Grade.FORGOT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text("😵 Quên", fontSize = 14.sp) }
                OutlinedButton(
                    onClick = { grade(Grade.SLOW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text("🐢 Chậm", fontSize = 14.sp) }
                Button(
                    onClick = { grade(Grade.GOOD) },
                    enabled = phase == 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) { Text("✅ Nhớ ngay", fontSize = 14.sp) }
            }
            if (phase == 1) {
                Text(
                    "Đã xem công thức nên không chấm được \"Nhớ ngay\" — thật thà thì mới nhớ lâu 😉",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
        } else {
            Button(
                onClick = { next(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Thẻ tiếp ▶", fontSize = 16.sp) }
        }
    }
}
