package vn.cfop.rubik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private fun makeQuestion(pool: List<TrainingCase>, rnd: Random): Question {
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
    var question by remember(pool) { mutableStateOf(makeQuestion(pool, Random.Default)) }
    var pickedId by remember(question) { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }

    val answered = pickedId != null
    val isCorrect = answered && pickedId == question.case.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row {
            OutlinedButton(onClick = onBack) { Text("← Luyện tập") }
            Spacer(Modifier.weight(1f))
            Text(
                "Điểm: $correctCount/$totalCount",
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Kiểm tra", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Nhìn hình trên khối, chọn đúng trường hợp (và công thức của nó).",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(12.dp))

        // Filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        if (pool.size < 4) {
            Text("Chưa đủ trường hợp để kiểm tra ở mục này.", fontSize = 14.sp, color = Color.Gray)
            return@Column
        }

        // Pattern cube
        Cube3DView(
            state = remember(question) { question.case.patternState() },
            animMove = null,
            animProgress = 0f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Hình này là trường hợp nào?",
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))

        // Options
        question.options.forEach { opt ->
            val isPicked = pickedId == opt.id
            val isAnswer = opt.id == question.case.id
            val bg = when {
                answered && isAnswer -> Color(0xFFC8E6C9)
                answered && isPicked -> Color(0xFFFFCDD2)
                else -> Color(0xFFF5F5F5)
            }
            Button(
                onClick = {
                    if (!answered) {
                        pickedId = opt.id
                        totalCount++
                        if (opt.id == question.case.id) correctCount++
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    opt.name,
                    color = Color.Black,
                    fontWeight = if (isPicked || (answered && isAnswer)) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        if (answered) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (isCorrect) "✅ Đúng rồi!" else "❌ Sai rồi — đáp án đúng là “${question.case.name}”.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF388E3C) else Color(0xFFD32F2F),
            )
            Text(
                "Công thức: ${question.case.algorithm.joinToString(" ")}",
                fontSize = 14.sp,
            )
            Text(question.case.tip, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    question = makeQuestion(pool, Random.Default)
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
