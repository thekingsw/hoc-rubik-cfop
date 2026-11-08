package vn.cfop.rubik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.ui.DownloadScreen
import vn.cfop.rubik.ui.InputScreen
import vn.cfop.rubik.ui.SettingsScreen
import vn.cfop.rubik.ui.SolveScreen
import vn.cfop.rubik.ui.TestScreen
import vn.cfop.rubik.ui.FlashcardScreen
import vn.cfop.rubik.ui.TrainingScreen
import vn.cfop.rubik.ui.blankState
import vn.cfop.rubik.ui.checkForUpdate
import vn.cfop.rubik.ui.getLastKnownSha
import vn.cfop.rubik.ui.getSavedToken
import vn.cfop.rubik.ui.isAutoCheckEnabled
import vn.cfop.rubik.ui.saveLastKnownSha
import vn.cfop.rubik.ui.UpdateInfo

private enum class Screen { HOME, INPUT, SOLVE, TRAINING, TEST, FLASHCARD, SETTINGS, DOWNLOAD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                var cubeState by remember { mutableStateOf(blankState()) }
                var stages by remember { mutableStateOf<List<CfopSolver.Stage>>(emptyList()) }
                var f2lOnly by remember { mutableStateOf(false) }
                var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
                var downloadRunId by remember { mutableStateOf(0L) }
                var downloadSha by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()

                // Tự động kiểm tra bản mới khi mở app (nếu đã nhập token và bật tự kiểm tra)
                LaunchedEffect(Unit) {
                    val ctx = this@MainActivity
                    if (getSavedToken(ctx) != null && isAutoCheckEnabled(ctx)) {
                        scope.launch {
                            val info = checkForUpdate(ctx)
                            if (info != null && info.hasUpdate && info.latestSha != getLastKnownSha(ctx)) {
                                pendingUpdate = info
                            }
                        }
                    }
                }

                // Hộp thoại báo có bản mới
                if (pendingUpdate != null) {
                    AlertDialog(
                        onDismissRequest = {
                            saveLastKnownSha(this@MainActivity, pendingUpdate!!.latestSha)
                            pendingUpdate = null
                        },
                        title = { Text("Có bản mới!") },
                        text = {
                            Text(
                                "App có bản cập nhật mới trên GitHub (bản ${pendingUpdate!!.latestSha}). " +
                                    "Anh có muốn tải về và cài đặt không?"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                downloadRunId = pendingUpdate!!.runId
                                downloadSha = pendingUpdate!!.latestSha
                                pendingUpdate = null
                                screen = Screen.DOWNLOAD
                            }) { Text("Tải và cài") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                saveLastKnownSha(this@MainActivity, pendingUpdate!!.latestSha)
                                pendingUpdate = null
                            }) { Text("Để sau") }
                        },
                    )
                }

                when (screen) {
                    Screen.HOME -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Học Rubik CFOP", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { screen = Screen.SETTINGS }) {
                                    Text("⚙️", fontSize = 18.sp)
                                }
                            }
                            Text(
                                "Giải khối từng bước, hoặc luyện từng trường hợp cho thuộc.",
                                fontSize = 14.sp, color = Color.Gray,
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { f2lOnly = false; screen = Screen.INPUT },
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
                                onClick = { f2lOnly = true; screen = Screen.INPUT },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                Text("🎯  Học F2L", fontSize = 17.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Chỉ học F2L: nhập khối, app đưa 2 viên lên tầng 3, nhận diện trường hợp trong 41 công thức rồi hướng dẫn áp dụng công thức đó. Không hiện OLL/PLL.",
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
                            f2lOnly = f2lOnly,
                        )
                    }
                    Screen.TRAINING -> {
                        TrainingScreen(
                            onBack = { screen = Screen.HOME },
                            onTest = { screen = Screen.TEST },
                            onFlashcards = { screen = Screen.FLASHCARD },
                        )
                    }
                    Screen.FLASHCARD -> {
                        FlashcardScreen(onBack = { screen = Screen.TRAINING })
                    }
                    Screen.TEST -> {
                        TestScreen(onBack = { screen = Screen.TRAINING })
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            onBack = { screen = Screen.HOME },
                            onDownloadAndInstall = { runId, sha ->
                                downloadRunId = runId
                                downloadSha = sha
                                screen = Screen.DOWNLOAD
                            },
                        )
                    }
                    Screen.DOWNLOAD -> {
                        DownloadScreen(
                            runId = downloadRunId,
                            latestSha = downloadSha,
                            onDone = { screen = Screen.HOME },
                            onBack = { screen = Screen.SETTINGS },
                        )
                    }
                }
            }
        }
    }
}
