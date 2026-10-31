package vn.cfop.rubik.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.cfop.rubik.BuildConfig
import java.io.File

/**
 * Màn hình Cài đặt: nhập token GitHub 1 lần, bật/tắt tự kiểm tra khi mở app,
 * nút kiểm tra và cài bản mới thủ công.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDownloadAndInstall: (runId: Long, latestSha: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(getSavedToken(context) ?: "") }
    var tokenSaved by remember { mutableStateOf(getSavedToken(context) != null) }
    var autoCheck by remember { mutableStateOf(isAutoCheckEnabled(context)) }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    fun doCheck() {
        if (getSavedToken(context).isNullOrBlank()) {
            message = "Anh nhập token GitHub trước đã nhé."
            return
        }
        checking = true
        message = null
        updateInfo = null
        scope.launch {
            val info = checkForUpdate(context)
            checking = false
            when {
                info == null -> message = "Không kiểm tra được. Anh xem lại token hoặc mạng nhé."
                info.hasUpdate -> {
                    updateInfo = info
                    message = "Có bản mới (bản hiện tại: ${BuildConfig.GIT_SHA}, bản mới: ${info.latestSha})."
                }
                else -> message = "App đang là bản mới nhất rồi (${BuildConfig.GIT_SHA})."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("⚙️ Cài đặt", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Bản app hiện tại: ${BuildConfig.GIT_SHA}",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(20.dp))

        Text("Tự động kiểm tra bản mới", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Mỗi lần mở app sẽ tự quét GitHub xem có bản mới không.",
                fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = autoCheck,
                onCheckedChange = {
                    autoCheck = it
                    setAutoCheckEnabled(context, it)
                },
            )
        }
        Spacer(Modifier.height(20.dp))

        Text("Token GitHub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Để app đọc được bản build mới trên repo riêng của anh, cần 1 token chỉ có quyền đọc. " +
                "Tạo 1 lần duy nhất:",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "1. Trên máy tính, vào github.com → avatar → Settings\n" +
                "2. Kéo xuống Developer settings → Personal access tokens → Fine-grained tokens\n" +
                "3. Generate new token → Only select repositories → chọn hoc-rubik-cfop\n" +
                "4. Permissions: Actions → Read, Contents → Read\n" +
                "5. Generate token rồi copy dán vào ô dưới đây",
            fontSize = 13.sp,
            color = Color(0xFF424242),
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; tokenSaved = false },
            label = { Text("Dán token vào đây") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (token.isBlank()) {
                    message = "Anh dán token vào ô trên đã nhé."
                } else {
                    saveToken(context, token)
                    tokenSaved = true
                    message = "Đã lưu token. Anh bấm \"Kiểm tra ngay\" để thử."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (tokenSaved) "✓ Đã lưu token" else "Lưu token")
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { doCheck() },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (checking) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(end = 8.dp))
                Text("Đang kiểm tra...")
            } else {
                Text("🔄 Kiểm tra bản mới ngay", fontSize = 16.sp)
            }
        }
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(message!!, fontSize = 14.sp, color = Color(0xFF1565C0))
        }
        if (updateInfo != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onDownloadAndInstall(updateInfo!!.runId, updateInfo!!.latestSha) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("⬇️ Tải và cài bản mới", fontSize = 16.sp)
            }
            Text(
                "App sẽ tải file về rồi mở màn hình cài đặt của hệ thống — anh bấm Cài đặt là xong.",
                fontSize = 12.sp, color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("← Quay lại") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Lưu ý: lần đầu cài bản tải về, Android sẽ hỏi \"Cho phép cài app không xác định\" — " +
                "anh bật cho app Học Rubik CFOP là được, chỉ hỏi 1 lần.",
            fontSize = 12.sp, color = Color.Gray,
        )
    }
}

/** Màn hình tải về với tiến trình — dùng chung cho tự động và thủ công. */
@Composable
fun DownloadScreen(
    runId: Long,
    latestSha: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Đang tải bản mới...") }
    var failed by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(runId) {
        scope.launch {
            try {
                val apk = downloadUpdateApk(context, runId) { p -> progress = p }
                saveLastKnownSha(context, latestSha)
                status = "Tải xong, đang mở màn hình cài đặt..."
                installApk(context, apk)
                onDone()
            } catch (e: Exception) {
                failed = "Tải thất bại: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text("⬇️ Đang cập nhật", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        if (failed != null) {
            Text(failed!!, fontSize = 15.sp, color = Color.Red)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Quay lại")
            }
        } else {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("$progress%", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(status, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
