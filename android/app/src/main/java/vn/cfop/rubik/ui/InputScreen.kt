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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube

private val COLOR_NAMES = listOf("Trắng", "Đỏ", "Xanh lá", "Vàng", "Cam", "Xanh dương")

/** Face order for step-by-step input: U R F D L B. */
private val FACE_NAMES = listOf("Mặt trên", "Mặt phải", "Mặt trước", "Mặt dưới", "Mặt trái", "Mặt sau")
private val FACE_HINTS = listOf(
    "Mặt vàng — đặt lên trên",
    "Mặt cam — bên phải",
    "Mặt xanh lá — ra trước",
    "Mặt trắng — ở dưới",
    "Mặt đỏ — bên trái",
    "Mặt xanh dương — ở sau",
)
/** Centre colour of each face (fixed, cannot change). */
private val FACE_CENTER_COLORS = listOf(3, 4, 2, 0, 1, 5)

/** Blank cube: centres fixed, every other sticker uncoloured (-1). */
fun blankState(): IntArray {
    val s = IntArray(54) { -1 }
    for (f in 0 until 6) {
        s[f * 9 + 4] = FACE_CENTER_COLORS[f]
    }
    return s
}

/** Random scramble: 22 moves, no two consecutive moves on the same face. */
fun randomScramble(): List<String> {
    val faces = listOf("U", "D", "R", "L", "F", "B")
    val suffixes = listOf("", "'", "2")
    val out = ArrayList<String>()
    var prev = ""
    while (out.size < 22) {
        val f = faces.random()
        if (f == prev) continue
        prev = f
        out.add(f + suffixes.random())
    }
    return out
}

@Composable
fun InputScreen(
    initialState: IntArray,
    onSolved: (IntArray, List<CfopSolver.Stage>) -> Unit,
) {
    // Start from the given state: blank on first launch, or the cube
    // the user already entered when coming back with "Sửa khối".
    var state by remember { mutableStateOf(initialState.copyOf()) }
    var faceIndex by remember { mutableIntStateOf(0) }
    var selectedColor by remember { mutableIntStateOf(2) } // green
    var showScrambleDialog by remember { mutableStateOf(false) }
    var scrambleText by remember { mutableStateOf("") }
    var solving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val counts = IntArray(6) { c -> state.count { it == c } }
    val uncoloured = state.count { it == -1 }

    fun faceComplete(f: Int): Boolean {
        for (k in 0 until 9) {
            if (k == 4) continue
            if (state[f * 9 + k] == -1) return false
        }
        return true
    }

    val currentFaceComplete = faceComplete(faceIndex)
    val isLastFace = faceIndex == 5

    fun trySolve() {
        if (uncoloured > 0) {
            val badFaces = (0 until 6).filter { !faceComplete(it) }
                .joinToString(", ") { FACE_NAMES[it] }
            error = "Còn $uncoloured ô chưa tô: $badFaces. Anh tô đủ rồi bấm lại nhé."
            return
        }
        val bad = (0 until 6).filter { counts[it] != 9 }
        if (bad.isNotEmpty()) {
            val msg = bad.joinToString(", ") { "${COLOR_NAMES[it]} (${counts[it]}/9)" }
            error = "Sai số lượng màu: $msg. Mỗi màu cần đúng 9 ô — anh kiểm tra lại các mặt nhé."
            return
        }
        solving = true
        error = null
        scope.launch {
            try {
                val stages = withContext(Dispatchers.Default) {
                    CfopSolver.solve(Cube(state))
                }
                onSolved(state.copyOf(), stages)
            } catch (e: Exception) {
                error = "Khối này không giải được — có thể anh nhập sai một vài ô " +
                    "(ví dụ góc bị xoay sai hướng). Anh kiểm tra lại các mặt nhé."
            } finally {
                solving = false
            }
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
            "Nhập khối Rubik của anh",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Đặt mặt vàng lên trên, mặt xanh lá ra trước, rồi nhập từng mặt.",
            fontSize = 14.sp,
            color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))

        // 3D cube: colours appear as the user fills them in; it turns
        // to show the face being edited.
        Cube3DView(
            state = state,
            animMove = null,
            animProgress = 0f,
            viewFace = faceIndex,
        )
        Text(
            "Kéo để xoay khối xem các mặt",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))

        // Current face header
        Text(
            "${FACE_NAMES[faceIndex]} (${faceIndex + 1}/6) — ${FACE_HINTS[faceIndex]}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (currentFaceComplete) "Mặt này đã đủ 8 ô ✓" else "Chạm vào một ô rồi chọn màu. Ô giữa đã cố định.",
            fontSize = 13.sp,
            color = if (currentFaceComplete) Color(0xFF388E3C) else Color.Gray,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(10.dp))

        // 3x3 grid for the current face
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 3) {
                        val k = r * 3 + c
                        val idx = faceIndex * 9 + k
                        val isCentre = k == 4
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cubeColour(state[idx]))
                                .border(
                                    width = if (isCentre) 3.dp else 1.dp,
                                    color = if (isCentre) Color.Black else Color(0xFF9E9E9E),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable(enabled = !isCentre) {
                                    val next = state.copyOf()
                                    next[idx] = selectedColor
                                    state = next
                                    error = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCentre) {
                                Text("●", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Palette
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (c in 0 until 6) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CUBE_COLORS[c])
                        .border(
                            width = if (selectedColor == c) 3.dp else 1.dp,
                            color = if (selectedColor == c) Color.Black else Color(0xFF9E9E9E),
                            shape = CircleShape,
                        )
                        .clickable { selectedColor = c },
                )
            }
        }
        Text(
            "Đang chọn: ${COLOR_NAMES[selectedColor]} (${counts[selectedColor]}/9 ô)",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(8.dp))

        // Colour counters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (c in 0 until 6) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CUBE_COLORS[c])
                            .border(1.dp, Color(0xFF9E9E9E), CircleShape),
                    )
                    Text(
                        "${counts[c]}/9",
                        fontSize = 12.sp,
                        color = if (counts[c] == 9) Color(0xFF388E3C) else Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, color = Color.Red, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        // Navigation: back / next (or finish on the last face)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (faceIndex > 0) faceIndex-- },
                enabled = faceIndex > 0,
            ) { Text("← Mặt trước") }
            Button(
                onClick = {
                    if (isLastFace) {
                        trySolve()
                    } else if (currentFaceComplete) {
                        error = null
                        faceIndex++
                    } else {
                        val missing = (0 until 9).count { k -> k != 4 && state[faceIndex * 9 + k] == -1 }
                        error = "Mặt này còn $missing ô chưa tô. Anh tô đủ rồi bấm tiếp nhé."
                    }
                },
                enabled = !solving,
                modifier = Modifier.weight(1f),
            ) {
                if (solving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("  Đang tính cách giải...", modifier = Modifier.padding(start = 8.dp))
                } else if (isLastFace) {
                    Text("Xong — Giải khối này →", fontSize = 15.sp)
                } else {
                    Text("Mặt tiếp theo →", fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val cube = Cube()
                cube.applySeq(randomScramble())
                state = cube.s.copyOf()
                faceIndex = 5
                error = null
            }) { Text("Xáo trộn") }
            OutlinedButton(onClick = {
                scrambleText = ""
                showScrambleDialog = true
            }) { Text("Nhập ký hiệu") }
            OutlinedButton(onClick = {
                state = blankState()
                faceIndex = 0
                error = null
            }) { Text("Đặt lại") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "App hướng dẫn theo phương pháp CFOP 2-look: chữ thập → 4 cặp F2L → OLL → PLL.",
            fontSize = 13.sp,
            color = Color.Gray,
        )
    }

    if (showScrambleDialog) {
        AlertDialog(
            onDismissRequest = { showScrambleDialog = false },
            title = { Text("Nhập công thức xáo trộn") },
            text = {
                Column {
                    Text("Ví dụ: R U R' U' R' F R2 U' R' U' R U R' F'")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scrambleText,
                        onValueChange = { scrambleText = it },
                        label = { Text("Công thức") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val moves = Cube.parseMoves(scrambleText)
                        val cube = Cube()
                        cube.applySeq(moves)
                        state = cube.s.copyOf()
                        faceIndex = 5
                        error = null
                        showScrambleDialog = false
                    } catch (e: Exception) {
                        error = "Công thức không hợp lệ."
                    }
                }) { Text("Áp dụng") }
            },
            dismissButton = {
                TextButton(onClick = { showScrambleDialog = false }) { Text("Hủy") }
            },
        )
    }
}
