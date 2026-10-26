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
    var state by remember { mutableStateOf(initialState.copyOf()) }
    var selectedColor by remember { mutableIntStateOf(2) } // green
    var showScrambleDialog by remember { mutableStateOf(false) }
    var scrambleText by remember { mutableStateOf("") }
    var solving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val counts = IntArray(6) { c -> state.count { it == c } }
    val valid = counts.all { it == 9 }

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
            "Chạm vào một ô rồi chọn màu. Đặt mặt vàng lên trên, mặt xanh lá ra trước.",
            fontSize = 14.sp,
            color = Color.Gray,
        )
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
        Spacer(Modifier.height(12.dp))

        CubeNet(
            state = state,
            onStickerClick = { idx ->
                val next = state.copyOf()
                next[idx] = selectedColor
                state = next
                error = null
            },
            stickerSize = 30.dp,
        )
        Spacer(Modifier.height(12.dp))

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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val cube = Cube()
                cube.applySeq(randomScramble())
                state = cube.s.copyOf()
                error = null
            }) { Text("Xáo trộn") }
            OutlinedButton(onClick = {
                scrambleText = ""
                showScrambleDialog = true
            }) { Text("Nhập ký hiệu") }
            OutlinedButton(onClick = {
                state = Cube().s.copyOf()
                error = null
            }) { Text("Đặt lại") }
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (!valid) {
                    val bad = (0 until 6).filter { counts[it] != 9 }
                        .joinToString(", ") { "${COLOR_NAMES[it]} (${counts[it]}/9)" }
                    error = "Chưa đủ màu: $bad. Mỗi màu cần đúng 9 ô."
                    return@Button
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
                        error = "Không giải được khối này: ${e.message}"
                    } finally {
                        solving = false
                    }
                }
            },
            enabled = !solving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (solving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text("  Đang tính cách giải...", modifier = Modifier.padding(start = 8.dp))
            } else {
                Text("Giải khối này →", fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
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
