package vn.cfop.rubik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.ui.InputScreen
import vn.cfop.rubik.ui.SolveScreen
import vn.cfop.rubik.ui.TestScreen
import vn.cfop.rubik.ui.TrainingScreen
import vn.cfop.rubik.ui.blankState

private enum class Screen { HOME, INPUT, SOLVE, TRAINING, TEST }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                var cubeState by remember { mutableStateOf(blankState()) }
                var stages by remember { mutableStateOf<List<CfopSolver.Stage>>(emptyList()) }

                when (screen) {
                    Screen.HOME -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        ) {
                            Spacer(Modifier.height(32.dp))
                            Text("Học Rubik CFOP", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Giải khối từng bước, hoặc luyện từng trường hợp cho thuộc.",
                                fontSize = 14.sp, color = Color.Gray,
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { screen = Screen.INPUT },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                Text("🧩  Giải khối của anh", fontSize = 17.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Nhập khối rubik đang cầm trên tay, app hướng dẫn xoay từng bước.",
                                fontSize = 13.sp, color = Color.Gray,
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { screen = Screen.TRAINING },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                Text("📚  Luyện tập CFOP", fontSize = 17.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Học lý thuyết F2L → OLL → PLL, tập từng trường hợp từ dễ đến khó, có công thức tạo hình trên khối thật và chế độ kiểm tra.",
                                fontSize = 13.sp, color = Color.Gray,
                            )
                        }
                    }
                    Screen.INPUT -> {
                        InputScreen(
                            initialState = cubeState,
                            onSolved = { state, solvedStages ->
                                cubeState = state
                                stages = solvedStages
                                screen = Screen.SOLVE
                            },
                            onBack = { screen = Screen.HOME },
                        )
                    }
                    Screen.SOLVE -> {
                        SolveScreen(
                            initialState = cubeState,
                            stages = stages,
                            onBack = { screen = Screen.INPUT },
                            onNewCube = {
                                cubeState = blankState()
                                stages = emptyList()
                                screen = Screen.HOME
                            },
                        )
                    }
                    Screen.TRAINING -> {
                        TrainingScreen(
                            onBack = { screen = Screen.HOME },
                            onTest = { screen = Screen.TEST },
                        )
                    }
                    Screen.TEST -> {
                        TestScreen(onBack = { screen = Screen.TRAINING })
                    }
                }
            }
        }
    }
}
