package vn.cfop.rubik.ui

import vn.cfop.rubik.solver.Cube

/**
 * Training mode data: fixed CFOP patterns with a fixed algorithm each,
 * plus a setup sequence that turns a solved cube into the pattern so
 * the user can practise on a real cube. Every case is verified: the
 * setup keeps the earlier steps solved, and the algorithm solves the case.
 * F2L algorithms are the inverse of their setup (verified by GenF2LAll).
 * OLL/PLL algorithms are the standard ones (verified by TestOLLPLLAlgs).
 */

data class TrainingCase(
    val id: String,
    val stage: String, // f2l | oll | pll
    val level: String, // basic | advanced
    val name: String,
    val recognition: String,
    val algorithm: List<String>,
    val setup: List<String>,
    val tip: String,
)

/** Cube state of a case, by applying its setup to a solved cube. */
fun TrainingCase.patternState(): IntArray {
    val cube = Cube()
    cube.applySeq(setup)
    return cube.s
}

// ------------------------------------------------------------ F2L
// Basic: the first 3 (3-move triggers). Advanced: all 41, easiest first.
val F2L_CASES: List<TrainingCase> = listOf(
    TrainingCase("f2l-01", "f2l", "basic", "F2L 1", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F"), listOf("F'", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-02", "f2l", "basic", "F2L 2", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F"), listOf("F'", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-03", "f2l", "basic", "F2L 3", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'"), listOf("R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-04", "f2l", "advanced", "F2L 4", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U", "R'"), listOf("R", "U'", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-05", "f2l", "advanced", "F2L 5", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U2", "R2", "F", "R", "F'"), listOf("F", "R'", "F'", "R2", "U2", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-06", "f2l", "advanced", "F2L 6", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "F'", "U'", "F"), listOf("F'", "U", "F", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-07", "f2l", "advanced", "F2L 7", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F", "R", "U", "R'"), listOf("R", "U'", "R'", "F'", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-08", "f2l", "advanced", "F2L 8", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F", "R", "U", "R'"), listOf("R", "U'", "R'", "F'", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-09", "f2l", "advanced", "F2L 9", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U2", "F", "R", "U", "R'"), listOf("R", "U'", "R'", "F'", "U2", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-10", "f2l", "advanced", "F2L 10", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U2", "F", "R", "U2", "R'"), listOf("R", "U2", "R'", "F'", "U2", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-11", "f2l", "advanced", "F2L 11", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F2", "R'", "F'", "R"), listOf("R'", "F", "R", "F2", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-12", "f2l", "advanced", "F2L 12", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F2", "R'", "F'", "R"), listOf("R'", "F", "R", "F2", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-13", "f2l", "advanced", "F2L 13", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U2", "F2", "R'", "F'", "R"), listOf("R'", "F", "R", "F2", "U2", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-14", "f2l", "advanced", "F2L 14", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F", "U2", "F2", "U'", "F2", "U'", "F'"), listOf("F", "U", "F2", "U", "F2", "U2", "F'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-15", "f2l", "advanced", "F2L 15", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F", "U'", "F'", "U'", "F"), listOf("F'", "U", "F", "U", "F'", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-16", "f2l", "advanced", "F2L 16", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F", "U'", "F'", "U'", "F"), listOf("F'", "U", "F", "U", "F'", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-17", "f2l", "advanced", "F2L 17", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U2", "F", "U", "F'", "U'", "F"), listOf("F'", "U", "F", "U'", "F'", "U2", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-18", "f2l", "advanced", "F2L 18", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "U2", "F'", "U'", "F"), listOf("F'", "U", "F", "U2", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-19", "f2l", "advanced", "F2L 19", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F", "U2", "F'", "U", "F"), listOf("F'", "U'", "F", "U2", "F'", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-20", "f2l", "advanced", "F2L 20", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F2", "U2", "F", "U", "F'", "U", "F2"), listOf("F2", "U'", "F", "U'", "F'", "U2", "F2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-21", "f2l", "advanced", "F2L 21", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U'", "F", "U", "R", "U'", "R'"), listOf("R", "U", "R'", "U'", "F'", "U", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-22", "f2l", "advanced", "F2L 22", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F", "U", "R", "U'", "R'"), listOf("R", "U", "R'", "U'", "F'", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-23", "f2l", "advanced", "F2L 23", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "U2", "R", "U'", "R'"), listOf("R", "U", "R'", "U2", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-24", "f2l", "advanced", "F2L 24", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U", "R'", "U2", "R", "U'", "R'"), listOf("R", "U", "R'", "U2", "R", "U'", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-25", "f2l", "advanced", "F2L 25", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U2", "R'", "U2", "R", "U'", "R'"), listOf("R", "U", "R'", "U2", "R", "U2", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-26", "f2l", "advanced", "F2L 26", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U2", "F", "U'", "R", "U", "R'"), listOf("R", "U'", "R'", "U", "F'", "U2", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-27", "f2l", "advanced", "F2L 27", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "U'", "R", "U", "R'"), listOf("R", "U'", "R'", "U", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-28", "f2l", "advanced", "F2L 28", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U2", "R'", "U'", "R", "U", "R'"), listOf("R", "U'", "R'", "U", "R", "U2", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-29", "f2l", "advanced", "F2L 29", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "U", "R", "U", "R'"), listOf("R", "U'", "R'", "U'", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-30", "f2l", "advanced", "F2L 30", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U2", "R'", "U", "R", "U", "R'"), listOf("R", "U'", "R'", "U'", "R", "U2", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-31", "f2l", "advanced", "F2L 31", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("F'", "U", "F", "U2", "R", "U", "R'"), listOf("R", "U'", "R'", "U2", "F'", "U'", "F"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-32", "f2l", "advanced", "F2L 32", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U'", "R'", "F", "R'", "F'", "R"), listOf("R'", "F", "R", "F'", "R", "U", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-33", "f2l", "advanced", "F2L 33", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U", "R'", "F", "R'", "F'", "R"), listOf("R'", "F", "R", "F'", "R", "U'", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-34", "f2l", "advanced", "F2L 34", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "R'", "U'", "R", "U'", "R2"), listOf("R2", "U", "R'", "U", "R", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-35", "f2l", "advanced", "F2L 35", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "R2", "U'", "R2", "U'", "R2"), listOf("R2", "U", "R2", "U", "R2", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-36", "f2l", "advanced", "F2L 36", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "F", "R2", "F'", "U2", "R2"), listOf("R2", "U2", "F", "R2", "F'", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-37", "f2l", "advanced", "F2L 37", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "U", "F", "R", "U", "R'", "U'", "F'", "R'"), listOf("R", "F", "U", "R", "U'", "R'", "F'", "U'", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-38", "f2l", "advanced", "F2L 38", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R", "F", "U", "R", "U'", "R'", "F'", "U'", "R'"), listOf("R", "U", "F", "R", "U", "R'", "U'", "F'", "R'"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-39", "f2l", "advanced", "F2L 39", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "F", "R2", "F'", "U2", "R'", "U", "R'"), listOf("R", "U'", "R", "U2", "F", "R2", "F'", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-40", "f2l", "advanced", "F2L 40", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "R'", "U'", "R", "U'", "R'", "U2", "R'"), listOf("R", "U2", "R", "U", "R'", "U", "R", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
    TrainingCase("f2l-41", "f2l", "advanced", "F2L 41", "Nhìn 2 viên xanh lá - cam trên khối 3D: vị trí của góc và cạnh quyết định công thức.", listOf("R2", "U2", "F", "R'", "F'", "U2", "R'", "U2", "R'"), listOf("R", "U2", "R", "U2", "F", "R", "F'", "U2", "R2"), "Tìm 2 viên xanh lá - cam, xem chúng đang ở đâu rồi làm công thức."),
)

// ------------------------------------------------------------ OLL (2-look)
val OLL_CASES: List<TrainingCase> = listOf(
    TrainingCase("eo-line", "oll", "basic", "Đường thẳng", "2 cạnh đối nhau chưa lật (tạo thành đường thẳng trên mặt vàng).", listOf("F", "R", "U", "R'", "U'", "F'"), listOf("F", "U", "R", "U'", "R'", "F'"), "Nhìn 2 cạnh vàng chưa lên trên: chúng nằm đối nhau thành 1 đường thẳng."),
    TrainingCase("eo-l", "oll", "basic", "Chữ L", "2 cạnh kề nhau chưa lật (tạo thành hình chữ L trên mặt vàng).", listOf("F", "U", "R", "U'", "R'", "F'"), listOf("F", "R", "U", "R'", "U'", "F'"), "2 cạnh chưa lật nằm kề nhau, tạo thành góc chữ L."),
    TrainingCase("eo-dot", "oll", "basic", "Chấm (4 cạnh)", "Cả 4 cạnh đều chưa lật (không có cạnh vàng nào trên mặt vàng).", listOf("F", "R", "U", "R'", "U'", "F'", "U2", "F", "U", "R", "U'", "R'", "F'"), listOf("F", "R", "U", "R'", "U'", "F'", "U2", "F", "U", "R", "U'", "R'", "F'"), "Làm công thức đường thẳng, xoay U2, rồi làm công thức chữ L."),
    TrainingCase("co-sune", "oll", "basic", "Sune", "1 góc đã đúng, 3 góc còn lại xoắn theo chiều kim đồng hồ.", listOf("R", "U", "R'", "U", "R", "U'", "R'", "U", "R", "U2", "R'"), listOf("R", "U2", "R'", "U'", "R", "U", "R'", "U'", "R", "U'", "R'"), "Trường hợp hay gặp nhất. Nhớ: góc đúng ở phía trước-bên trái."),
    TrainingCase("co-antisune", "oll", "basic", "Anti-Sune", "1 góc đã đúng, 3 góc còn lại xoắn ngược chiều kim đồng hồ.", listOf("R", "U2", "R'", "U'", "R", "U'", "R'"), listOf("R", "U", "R'", "U", "R", "U2", "R'"), "Ngược của Sune."),
    TrainingCase("co-h", "oll", "basic", "Chữ H", "2 góc đối nhau đã đúng, 2 góc còn lại xoắn.", listOf("R", "U", "R'", "U", "R", "U'", "R'", "U", "R", "U'", "R'", "U", "R", "U2", "R'"), listOf("R", "U2", "R'", "U'", "R", "U", "R'", "U'", "R", "U", "R'", "U'", "R", "U'", "R'"), "2 góc đúng nằm đối nhau."),
    TrainingCase("co-pi", "oll", "basic", "Chữ Pi", "2 góc kề nhau đã đúng, 2 góc còn lại xoắn.", listOf("R", "U2", "R2", "U'", "R2", "U'", "R2", "U2", "R"), listOf("R'", "U2", "R2", "U", "R2", "U", "R2", "U2", "R'"), "2 góc đúng nằm kề nhau, tạo thành hình Pi."),
    TrainingCase("co-u", "oll", "basic", "Chữ U", "3 góc đã đúng, 1 góc xoắn (hình chữ U).", listOf("R2", "D", "R'", "U2", "R", "D'", "R'", "U2", "R'"), listOf("R", "U2", "R", "D", "R'", "U2", "R", "D'", "R2"), "Chỉ 1 góc sai, nằm ở góc chữ U."),
)

// ------------------------------------------------------------ PLL (2-look)
val PLL_CASES: List<TrainingCase> = listOf(
    TrainingCase("pll-aa", "pll", "basic", "Đổi góc kề (Aa)", "3 góc xoay vòng cùng chiều kim đồng hồ, 1 góc đã đúng.", listOf("x", "R'", "U", "R'", "D2", "R", "U'", "R'", "D2", "R2", "x'"), listOf("x", "R2", "D2", "R", "U", "R'", "D2", "R", "U'", "R", "x'"), "Tìm góc đã đúng, đặt nó ở phía trước-bên trái rồi làm công thức."),
    TrainingCase("pll-ab", "pll", "basic", "Đổi góc kề (Ab)", "3 góc xoay vòng ngược chiều kim đồng hồ, 1 góc đã đúng.", listOf("x", "R2", "D2", "R", "U", "R'", "D2", "R", "U'", "R", "x'"), listOf("x", "R'", "U", "R'", "D2", "R", "U'", "R'", "D2", "R2", "x'"), "Ngược của Aa."),
    TrainingCase("pll-y", "pll", "basic", "Đổi góc chéo (Y)", "2 cặp góc đối nhau đổi chỗ cho nhau (đổi chéo).", listOf("F", "R", "U'", "R'", "U'", "R", "U", "R'", "F'", "R", "U", "R'", "U'", "R'", "F", "R", "F'"), listOf("F", "R'", "F'", "R", "U", "R", "U'", "R'", "F", "R", "U'", "R'", "U", "R", "U", "R'", "F'"), "Không có góc nào đúng chỗ, 2 góc đổi chéo."),
    TrainingCase("pll-ua", "pll", "basic", "Đổi cạnh xuôi (Ua)", "3 cạnh xoay vòng cùng chiều kim đồng hồ, 1 cạnh đã đúng.", listOf("R", "U'", "R", "U", "R", "U", "R", "U'", "R'", "U'", "R2"), listOf("R2", "U", "R", "U", "R'", "U'", "R'", "U'", "R'", "U", "R'"), "Tìm cạnh đã đúng, đặt nó ở phía sau rồi làm công thức."),
    TrainingCase("pll-ub", "pll", "basic", "Đổi cạnh ngược (Ub)", "3 cạnh xoay vòng ngược chiều kim đồng hồ, 1 cạnh đã đúng.", listOf("R2", "U", "R", "U", "R'", "U'", "R'", "U'", "R'", "U", "R'"), listOf("R", "U'", "R", "U", "R", "U", "R", "U'", "R'", "U'", "R2"), "Ngược của Ua."),
    TrainingCase("pll-h", "pll", "basic", "Đổi cạnh đối (H)", "2 cặp cạnh đối nhau đổi chỗ cho nhau.", listOf("M2", "U", "M2", "U2", "M2", "U", "M2"), listOf("M2", "U'", "M2", "U2", "M2", "U'", "M2"), "2 cạnh đúng nằm đối nhau."),
    TrainingCase("pll-z", "pll", "basic", "Đổi cạnh kề (Z)", "2 cặp cạnh kề nhau đổi chỗ cho nhau.", listOf("M2", "U", "M2", "U", "M'", "U2", "M2", "U2", "M'", "U2"), listOf("U2", "M", "U2", "M2", "U2", "M", "U'", "M2", "U'", "M2"), "2 cạnh đúng nằm kề nhau."),
)

val ALL_TRAINING_CASES: List<TrainingCase> = F2L_CASES + OLL_CASES + PLL_CASES

// ------------------------------------------------------------ Theory
val THEORY_F2L = """
F2L là gì? Sau khi làm xong chữ thập trắng ở mặt dưới, bạn cần đưa 4 cặp
(1 góc + 1 cạnh) vào 4 khe ở 2 tầng dưới. Mỗi cặp gồm 1 viên góc có màu trắng
và 1 viên cạnh — ví dụ cặp xanh lá - cam.

Ý tưởng chỉ có 1: tìm 2 viên của 1 cặp, ghép chúng lại với nhau ở tầng trên,
rồi đưa cả cặp vào đúng khe. Không cần học thuộc lòng — chỉ cần hiểu 3 tình huống:

1. Cặp đã ghép sẵn ở tầng trên: chỉ việc đưa vào khe (3 nước).
2. Cặp tách rời ở tầng trên: ghép chúng lại rồi đưa vào.
3. Cặp đang ở sai khe: lấy nó ra tầng trên trước (dùng công thức đưa vào),
rồi xử lý như tình huống 1 hoặc 2.

Mẹo: luôn làm cặp nào dễ thấy nhất trước. Xoay mặt trên (U) để đưa viên góc
hoặc viên cạnh tới gần nhau, rồi ghép.
"""

val THEORY_OLL = """
OLL là gì? Sau khi xong 2 tầng dưới, mặt vàng trên cùng chưa vàng hết.
OLL xoay các viên tầng trên để mặt vàng thành 1 màu, không quan tâm vị trí.

Học 2-look (2 lượt) cho nhẹ:

Lượt 1 — Lật cạnh (EO): chỉ quan tâm 4 cạnh. Có 3 hình: đường thẳng (2 cạnh
đối nhau chưa lật), chữ L (2 cạnh kề nhau chưa lật), chấm (cả 4 cạnh chưa lật).
Chấm thì làm công thức đường thẳng, xoay U2, rồi làm công thức chữ L.

Lượt 2 — Xoay góc (CO): chỉ quan tâm 4 góc. Có 5 công thức: Sune, Anti-Sune, H,
Pi, U. Nhìn xem góc nào đã vàng (đúng) và góc nào chưa, rồi đối chiếu hình.

Còn 2 hình nữa (T và L) không cần học riêng: gặp chúng (hoặc không có góc nào
đúng) thì làm Sune 1 lần, rồi nhìn lại sẽ ra 1 trong 5 hình trên.

Mẹo: chỉ nhìn mặt vàng. Cạnh nào chưa vàng là chưa lật, góc nào chưa vàng là chưa xoay.
"""

val THEORY_PLL = """
PLL là gì? Sau OLL, mặt vàng đã vàng hết nhưng các viên chưa về đúng chỗ.
PLL đổi chỗ các viên tầng trên mà không làm mất màu vàng.

Học 2-look (2 lượt) cho nhẹ:

Lượt 1 — Đổi góc: tìm góc đã đúng chỗ (nếu có). 3 góc còn lại xoay vòng:
Aa (xuôi), Ab (ngược), hoặc đổi chéo 2 cặp (Y).

Lượt 2 — Đổi cạnh: tương tự với cạnh. Ua (xuôi), Ub (ngược), H (đổi đối),
Z (đổi kề).

Mẹo: tìm viên đã đúng chỗ trước, đặt nó ở vị trí chuẩn (góc đúng ở trước-trái,
cạnh đúng ở sau), rồi làm công thức.
"""
