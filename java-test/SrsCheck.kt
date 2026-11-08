package vn.cfop.rubik.ui
var fails = 0
fun check(ok: Boolean, msg: String) { if (!ok) { fails++; println("FAIL: $msg") } }
fun main() {
    val DAY = 24L * 60 * 60_000
    val order = (1..41).map { "f2l-%02d".format(it) }
    val set = setOf("f2l-01", "f2l-02", "f2l-03")
    var prog = emptyMap<String, CardProgress>()
    val t0 = 1_000_000L
    // thẻ mới theo thứ tự
    check(Srs.pickNext(order, prog, set, t0, null) == "f2l-01", "thẻ mới đầu tiên")
    check(Srs.pickNext(order, prog, set, t0, "f2l-01") == "f2l-02", "bỏ thẻ vừa làm")
    // Nhớ ngay: hộp 1, đến hạn sau 1 ngày
    var p = Srs.apply(CardProgress(), Grade.GOOD, t0, 4000)
    check(p.box == 1 && p.dueAt == t0 + DAY && p.reps == 1 && p.bestMs == 4000L, "GOOD từ thẻ mới: $p")
    p = Srs.apply(p, Grade.GOOD, t0 + DAY, 6000)
    check(p.box == 2 && p.dueAt == t0 + DAY + 3 * DAY && p.bestMs == 4000L, "GOOD lần 2 giữ best: $p")
    // Hộp tối đa 5
    var q = CardProgress(box = 5, reps = 9)
    q = Srs.apply(q, Grade.GOOD, t0, 1000)
    check(q.box == 5 && q.dueAt == t0 + 30 * DAY, "hộp tối đa")
    // Quên: về hộp 0, ôn lại sau 1 phút, tăng lapses
    q = Srs.apply(q, Grade.FORGOT, t0, 0)
    check(q.box == 0 && q.dueAt == t0 + 60_000L && q.lapses == 1, "FORGOT: $q")
    // Chậm: không tụt hộp, ôn lại sau 10 phút; thẻ mới thì lên hộp 1
    val s1 = Srs.apply(CardProgress(box = 3, reps = 4), Grade.SLOW, t0, 9000)
    check(s1.box == 3 && s1.dueAt == t0 + 600_000L, "SLOW giữ hộp")
    check(Srs.apply(CardProgress(), Grade.SLOW, t0, 9000).box == 1, "SLOW thẻ mới lên hộp 1")
    // Chọn thẻ: đến hạn trước thẻ mới, quá hạn lâu nhất trước
    prog = mapOf(
        "f2l-01" to CardProgress(box = 2, dueAt = t0 - 1000, reps = 2),
        "f2l-02" to CardProgress(box = 2, dueAt = t0 - 5000, reps = 2),
    )
    check(Srs.pickNext(order, prog, set, t0, null) == "f2l-02", "quá hạn lâu nhất trước")
    check(Srs.pickNext(order, prog, set, t0, "f2l-02") == "f2l-01", "loại trừ thẻ vừa làm")
    // Hết thẻ đến hạn → thẻ mới
    prog = mapOf("f2l-01" to CardProgress(box = 1, dueAt = t0 + DAY, reps = 1),
        "f2l-02" to CardProgress(box = 1, dueAt = t0 + DAY, reps = 1))
    check(Srs.pickNext(order, prog, set, t0, null) == "f2l-03", "thẻ mới khi chưa có thẻ đến hạn")
    // Hết hết → null, trừ khi ôn sớm
    prog = prog + ("f2l-03" to CardProgress(box = 1, dueAt = t0 + 2 * DAY, reps = 1))
    check(Srs.pickNext(order, prog, set, t0, null) == null, "hết thẻ")
    check(Srs.pickNext(order, prog, set, t0, null, earlyReview = true) == "f2l-01", "ôn sớm lấy thẻ sắp đến hạn nhất")
    check(Srs.nextDueAt(order, prog, set) == t0 + DAY, "nextDueAt")
    check(Srs.dueCount(order, prog, set, t0) == 0 && Srs.dueCount(order, prog, set, t0 + 3 * DAY) == 3, "dueCount")
    // Bộ chỉ 1 thẻ: vẫn ra lại được khi đến hạn
    check(Srs.pickNext(order, mapOf("f2l-05" to CardProgress(box = 1, dueAt = 0, reps = 1)), setOf("f2l-05"), t0, "f2l-05") == "f2l-05", "bộ 1 thẻ")
    // Mô tả thời gian
    check(Srs.describeWait(0) == "ngay bây giờ" && Srs.describeWait(61_000) == "2 phút" && Srs.describeWait(DAY) == "1 ngày", "describeWait ${Srs.describeWait(DAY)}")
    // Lưu / đọc
    val data = StudyData(mapOf("f2l-01" to CardProgress(3, 12345L, 7, 2, 4100L)), setOf("f2l-01", "f2l-02"))
    val back = StudyData.decode(data.encode())
    check(back == data, "encode/decode: $back")
    check(StudyData.decode(null) == StudyData() && StudyData.decode("") == StudyData(), "decode rỗng")
    check(StudyData.decode("rác\nP|x|a|b\nS|f2l-09").studySet == setOf("f2l-09"), "bỏ dòng hỏng")
    println(if (fails == 0) "SRS TESTS PASSED" else "$fails FAILURES")
    if (fails != 0) kotlin.system.exitProcess(1)
}
