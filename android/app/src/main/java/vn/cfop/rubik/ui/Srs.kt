package vn.cfop.rubik.ui

/**
 * Lịch ôn thẻ nhớ (kiểu hộp Leitner): thẻ nhớ tốt thì giãn lịch ôn ra, quên thì ôn lại sớm.
 * Toàn bộ là code thuần Kotlin (không phụ thuộc Android) để test được.
 */
enum class Grade { FORGOT, SLOW, GOOD }

data class CardProgress(
    /** 0 = mới hoặc vừa quên; 1..5 = càng cao càng thuộc. */
    val box: Int = 0,
    /** Thời điểm đến hạn ôn lại (epoch ms). */
    val dueAt: Long = 0L,
    /** Số lần đã tự chấm. */
    val reps: Int = 0,
    /** Số lần quên. */
    val lapses: Int = 0,
    /** Thời gian nhớ nhanh nhất khi chấm "Nhớ ngay" (ms); 0 = chưa có. */
    val bestMs: Long = 0L,
)

object Srs {
    const val MAX_BOX = 5
    private const val MINUTE = 60_000L
    private const val DAY = 24L * 60L * MINUTE

    /** Khoảng cách đến lần ôn kế tiếp theo hộp (hộp 0 không dùng). */
    private val BOX_INTERVAL = longArrayOf(0L, 1 * DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)
    const val FORGOT_DELAY = 1 * MINUTE
    const val SLOW_DELAY = 10 * MINUTE

    fun apply(p: CardProgress, grade: Grade, now: Long, elapsedMs: Long): CardProgress {
        return when (grade) {
            Grade.GOOD -> {
                val box = minOf(p.box + 1, MAX_BOX)
                val best = if (elapsedMs > 0L && (p.bestMs == 0L || elapsedMs < p.bestMs)) elapsedMs else p.bestMs
                p.copy(box = box, dueAt = now + BOX_INTERVAL[box], reps = p.reps + 1, bestMs = best)
            }
            Grade.SLOW -> {
                val box = maxOf(p.box, 1)
                p.copy(box = box, dueAt = now + SLOW_DELAY, reps = p.reps + 1)
            }
            Grade.FORGOT -> p.copy(box = 0, dueAt = now + FORGOT_DELAY, reps = p.reps + 1, lapses = p.lapses + 1)
        }
    }

    private fun prog(progress: Map<String, CardProgress>, id: String): CardProgress =
        progress[id] ?: CardProgress()

    /** Thẻ đã học (reps > 0) và đến hạn ôn. */
    fun isDue(p: CardProgress, now: Long): Boolean = p.reps > 0 && p.dueAt <= now

    fun isNew(p: CardProgress): Boolean = p.reps == 0

    /**
     * Thẻ tiếp theo trong bộ đang học: ôn thẻ đến hạn (quá hạn lâu nhất trước), rồi thẻ mới
     * (theo thứ tự). Nếu earlyReview = true và hết thẻ đến hạn thì lấy thẻ sắp đến hạn nhất.
     * Trả null nếu không còn thẻ nào.
     */
    fun pickNext(
        order: List<String>,
        progress: Map<String, CardProgress>,
        studySet: Set<String>,
        now: Long,
        exclude: String?,
        earlyReview: Boolean = false,
    ): String? {
        val pool = order.filter { it in studySet }
        val others = pool.filter { it != exclude }
        val candidates = if (others.isEmpty()) pool else others
        val due = candidates
            .filter { isDue(prog(progress, it), now) }
            .sortedBy { prog(progress, it).dueAt }
        if (due.isNotEmpty()) return due.first()
        val fresh = candidates.firstOrNull { isNew(prog(progress, it)) }
        if (fresh != null) return fresh
        if (earlyReview) {
            return candidates.minByOrNull { prog(progress, it).dueAt }
        }
        return null
    }

    fun dueCount(order: List<String>, progress: Map<String, CardProgress>, studySet: Set<String>, now: Long): Int =
        order.count { it in studySet && isDue(prog(progress, it), now) }

    fun newCount(order: List<String>, progress: Map<String, CardProgress>, studySet: Set<String>): Int =
        order.count { it in studySet && isNew(prog(progress, it)) }

    /** Thời điểm sớm nhất có thẻ đến hạn (ms), null nếu bộ học rỗng hoặc còn thẻ đến hạn/mới. */
    fun nextDueAt(order: List<String>, progress: Map<String, CardProgress>, studySet: Set<String>): Long? =
        order.filter { it in studySet && !isNew(prog(progress, it)) }
            .map { prog(progress, it).dueAt }
            .minOrNull()

    /** Mức thuộc để tô màu: -1 = chưa học, 0..5 = hộp. */
    fun level(p: CardProgress?): Int = if (p == null || p.reps == 0) -1 else p.box

    /** Câu mô tả khoảng thời gian còn lại (tiếng Việt), ví dụ "3 phút", "2 ngày". */
    fun describeWait(ms: Long): String {
        if (ms <= 0L) return "ngay bây giờ"
        val minutes = (ms + MINUTE - 1) / MINUTE
        if (minutes < 60) return "$minutes phút"
        val hours = (minutes + 59) / 60
        if (hours < 24) return "$hours giờ"
        val days = (hours + 23) / 24
        return "$days ngày"
    }
}

/** Dữ liệu học đã lưu: bộ case đang học + tiến độ từng thẻ. */
data class StudyData(
    val progress: Map<String, CardProgress> = emptyMap(),
    val studySet: Set<String> = emptySet(),
) {
    /** Dạng văn bản để lưu vào SharedPreferences. */
    fun encode(): String {
        val sb = StringBuilder()
        sb.append("S|").append(studySet.sorted().joinToString(",")).append('\n')
        for ((id, p) in progress.toSortedMap()) {
            sb.append("P|").append(id).append('|').append(p.box).append('|').append(p.dueAt)
                .append('|').append(p.reps).append('|').append(p.lapses).append('|').append(p.bestMs).append('\n')
        }
        return sb.toString()
    }

    companion object {
        /** Đọc lại, bỏ qua dòng hỏng; null hoặc rỗng cho dữ liệu trống. */
        fun decode(text: String?): StudyData {
            if (text.isNullOrBlank()) return StudyData()
            var set: Set<String> = emptySet()
            val progress = HashMap<String, CardProgress>()
            for (line in text.lines()) {
                val parts = line.split('|')
                if (parts.isEmpty()) continue
                if (parts[0] == "S" && parts.size >= 2) {
                    set = parts[1].split(',').filter { it.isNotBlank() }.toSet()
                } else if (parts[0] == "P" && parts.size >= 7) {
                    val box = parts[2].toIntOrNull()
                    val due = parts[3].toLongOrNull()
                    val reps = parts[4].toIntOrNull()
                    val lapses = parts[5].toIntOrNull()
                    val best = parts[6].toLongOrNull()
                    if (box != null && due != null && reps != null && lapses != null && best != null) {
                        progress[parts[1]] = CardProgress(box.coerceIn(0, Srs.MAX_BOX), due, reps, lapses, best)
                    }
                }
            }
            return StudyData(progress, set)
        }
    }
}
