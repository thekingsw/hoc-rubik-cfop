package vn.cfop.rubik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private data class Question(
    val case: TrainingCase,
    val options: List<TrainingCase>,
)

private fun makeQuestion(pool: List<TrainingCase>, rnd: Random): Question? {
    if (pool.size < 4) return null
    val correct = pool.random(rnd)
    val others = pool.filter { it.id != correct.id }.shuffled(rnd).take(3)
    return Question(correct, (others + correct).shuffled(rnd))
}

@Composable
fun TestScreen(onBack: () -> Unit) {
    var stage by remember { mutableStateOf("all") }
    var level by remember { mutableStateOf("basic") }
    val pool = remember(stage, level) {
        ALL_TRAINING_CASES.filter {
            (stage == "all" || it.stage == stage) && it.level == level
        }
    }
    // Null-safe: no question when the pool is too small (e.g. advanced
    // OLL/PLL, which do not exist yet) instead of crashing.
    val question = remember(pool) { makeQuestion(pool, Random.Default) }
    var pickedId by remember(question) { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    // Bump to force a fresh question.
    var round by remember { mutableIntStateOf(0) }
    val currentQuestion = remember(pool, round) { makeQuestion(pool, Random.Default) }

    val answered = pickedId != null
    val isCorrect = answered && currentQuestion != null && pickedId == currentQuestion.case.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text("← Luyện tập") }
            Spacer(Modifier.weight(1f))
            Text(
                "Điểm: $correctCount/$totalCount",
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Kiểm tra",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Nhìn hình trên khối, chọn đúng trường hợp (và công thức của nó).",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        // Filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf("basic" to "Cơ bản", "advanced" to "Nâng cao").forEach { (lv, label) ->
                OutlinedButton(
                    onClick = { level = lv },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontWeight = if (level == lv) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf("all" to "Tất cả", "f2l" to "F2L", "oll" to "OLL", "pll" to "PLL").forEach { (st, label) ->
                OutlinedButton(
                    onClick = { stage = st },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, fontWeight = if (stage == st) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (currentQuestion == null) {
            Text(
                if (pool.isEmpty())
                    "Mục này chưa có bài. Nâng cao hiện chỉ có F2L (đủ 41 trường hợp); OLL/PLL nâng cao chưa làm. Anh chọn Cơ bản hoặc mục Tất cả / F2L nhé."
                else
                    "Chưa đủ trường hợp để kiểm tra ở mục này (cần ít nhất 4).",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }

        // Pattern cube (smaller so the answers fit on screen).
        Cube3DView(
            state = remember(currentQuestion) { currentQuestion.case.patternState() },
            animMove = null,
            animProgress = 0f,
            highlight = remember(currentQuestion) {
                currentQuestion.case.highlightFacelets()
            },
            modifier = Modifier.fillMaxWidth(0.72f),
        )
        Text(
            "Hình này là trường hợp nào?",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Các viên viền vàng là những viên chưa đúng chỗ — nhìn chúng để nhận ra trường hợp.",
            fontSize = 12.sp,
            color = Color(0xFFE65100),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        // Options
        currentQuestion.options.forEach { opt ->
            val isPicked = pickedId == opt.id
            val isAnswer = opt.id == currentQuestion.case.id
            Button(
                onClick = {
                    if (!answered) {
                        pickedId = opt.id
                        totalCount++
                        if (opt.id == currentQuestion.case.id) correctCount++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        answered && isAnswer -> Color(0xFFC8E6C9)
                        answered && isPicked -> Color(0xFFFFCDD2)
                        else -> Color(0xFFE3F2FD)
                    },
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    opt.name,
                    fontWeight = if (isPicked || (answered && isAnswer)) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        if (answered) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (isCorrect) "✅ Đúng rồi!" else "❌ Sai rồi — đáp án đúng là “${currentQuestion.case.name}”.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF388E3C) else Color(0xFFD32F2F),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Công thức: ${currentQuestion.case.algorithm.joinToString(" ")}",
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                currentQuestion.case.tip,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    round++
                    pickedId = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Câu tiếp theo →")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
