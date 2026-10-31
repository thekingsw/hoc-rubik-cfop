package vn.cfop.rubik.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.cfop.rubik.BuildConfig
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Tự động kiểm tra bản mới trên GitHub khi mở app.
 *
 * Cách hoạt động:
 * 1. Gọi GitHub API lấy workflow run thành công mới nhất của repo thekingsw/hoc-rubik-cfop
 * 2. So sánh head_sha với BuildConfig.GIT_SHA của app đang cài
 * 3. Nếu khác nhau → có bản mới → tải artifact (zip chứa APK) về, giải nén, mở màn hình cài đặt
 *
 * Cần token GitHub (fine-grained PAT, quyền Actions: Read + Contents: Read cho repo này),
 * anh nhập 1 lần trong màn hình Cài đặt, app lưu lại.
 */

private const val REPO_OWNER = "thekingsw"
private const val REPO_NAME = "hoc-rubik-cfop"
private const val PREFS = "update_prefs"
private const val KEY_TOKEN = "github_token"
private const val KEY_AUTO_CHECK = "auto_check"
private const val KEY_LAST_SHA = "last_known_sha"

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestSha: String,
    val runId: Long,
    val createdAt: String,
)

fun getSavedToken(context: Context): String? =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

fun saveToken(context: Context, token: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token.trim()).apply()
}

fun isAutoCheckEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_CHECK, true)

fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
}

private fun apiGet(url: String, token: String): String {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connectTimeout = 15000
        readTimeout = 15000
    }
    try {
        val code = conn.responseCode
        if (code != 200) throw Exception("GitHub API trả về $code")
        return conn.inputStream.bufferedReader().readText()
    } finally {
        conn.disconnect()
    }
}

/** Lấy run thành công mới nhất của workflow build APK. Null nếu lỗi hoặc chưa có token. */
suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
    val token = getSavedToken(context) ?: return@withContext null
    try {
        // Lấy danh sách workflow runs thành công, mới nhất trước
        val runsJson = apiGet(
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/runs?status=success&per_page=5",
            token
        )
        val runs = JSONObject(runsJson).getJSONArray("workflow_runs")
        // Tìm run của workflow build Android (có artifact rubik-cfop-apk)
        for (i in 0 until runs.length()) {
            val run = runs.getJSONObject(i)
            val runId = run.getLong("id")
            val sha = run.getString("head_sha").take(7)
            val createdAt = run.getString("created_at")
            // Kiểm tra run này có artifact APK không
            val artsJson = apiGet(
                "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/runs/$runId/artifacts",
                token
            )
            val arts = JSONObject(artsJson).getJSONArray("artifacts")
            var hasApk = false
            for (j in 0 until arts.length()) {
                if (arts.getJSONObject(j).getString("name") == "rubik-cfop-apk") {
                    hasApk = true
                    break
                }
            }
            if (hasApk) {
                val currentSha = BuildConfig.GIT_SHA
                return@withContext UpdateInfo(
                    hasUpdate = sha != currentSha,
                    latestSha = sha,
                    runId = runId,
                    createdAt = createdAt,
                )
            }
        }
        return@withContext null
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

/** Tải artifact APK của run về, giải nén, trả về file APK. Ném Exception khi lỗi. */
suspend fun downloadUpdateApk(context: Context, runId: Long, onProgress: (Int) -> Unit = {}): File =
    withContext(Dispatchers.IO) {
        val token = getSavedToken(context) ?: throw Exception("Chưa nhập token GitHub")
        // Lấy artifact
        val artsJson = apiGet(
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/runs/$runId/artifacts",
            token
        )
        val arts = JSONObject(artsJson).getJSONArray("artifacts")
        var downloadUrl: String? = null
        for (j in 0 until arts.length()) {
            val a = arts.getJSONObject(j)
            if (a.getString("name") == "rubik-cfop-apk") {
                downloadUrl = a.getString("archive_download_url")
                break
            }
        }
        val url = downloadUrl ?: throw Exception("Không tìm thấy file APK trong bản build")
        // Tải zip
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) throw Exception("Tải về thất bại (${conn.responseCode})")
            val total = conn.contentLength
            val zipFile = File(context.cacheDir, "updates/update.zip").apply { parentFile?.mkdirs() }
            conn.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buf = ByteArray(8192)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        if (total > 0) onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            // Giải nén lấy file .apk
            var apkFile: File? = null
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        apkFile = File(context.cacheDir, "updates/update.apk")
                        FileOutputStream(apkFile!!).use { out -> zip.copyTo(out) }
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            zipFile.delete()
            return@withContext apkFile ?: throw Exception("File tải về không có APK")
        } finally {
            conn.disconnect()
        }
    }

/** Mở màn hình cài đặt APK (hệ thống sẽ hỏi anh xác nhận cài đặt). */
fun installApk(context: Context, apkFile: File) {
    val uri: Uri = FileProvider.getUriForFile(context, "vn.cfop.rubik.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

/** Lưu SHA đã biết để lần sau không báo lại bản đã thấy. */
fun saveLastKnownSha(context: Context, sha: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_SHA, sha).apply()
}

fun getLastKnownSha(context: Context): String? =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_SHA, null)
