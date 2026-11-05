package vn.cfop.rubik.ui

import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube
import vn.cfop.rubik.solver.CubeOrientation

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

/**
 * Facelets to highlight on the 3D pattern cube: the pieces that are
 * out of place and need moving, so the user sees what to look for
 * instead of guessing. F2L: the pair's corner + edge (all training
 * F2L cases are the FR pair, green-orange). OLL: pieces not oriented
 * yet. PLL: pieces not in their solved position yet.
 */
fun TrainingCase.highlightFacelets(): Set<Int> {
    val pattern = patternState()
    return try {
        when (stage) {
            "f2l" -> CfopSolver.f2lPairFacelets(pattern, 0).toSet()
            "oll" -> (CfopSolver.ollEoFacelets(pattern).toList()
                + CfopSolver.ollCoFacelets(pattern).toList()).toSet()
            "pll" -> (CfopSolver.pllCornersFacelets(pattern).toList()
                + CfopSolver.pllEdgesFacelets(pattern).toList()).toSet()
            else -> emptySet()
        }
    } catch (e: Exception) {
        emptySet()
    }
}

// ------------------------------------------------------------ F2L
// Phương pháp trực quan (theo cách anh hiểu):
// 1. Đưa 2 viên (góc + cạnh) lên tầng trên (nếu kẹt ở dưới thì dùng trigger để hất lên)
// 2. Xoay U/U' để đưa về trường hợp cơ bản
// 3. Áp dụng công thức cơ bản R U R'
//
// (Khung hình cố định: xanh lá ở trước, cam ở phải. Phần luyện tập không xoay cả khối.)
// 41 công thức F2L từ cuberesolver.com (khung cố định: xanh lá=F, cam=R)
// Case 35, 37 tạm bỏ qua (dùng 'd', Cube không hỗ trợ) — sẽ bổ sung sau
// 41 công thức F2L GỐC từ cuberesolver.com (giữ nguyên y, y', d)
// Theo yêu cầu của anh (2026-11-05): dùng đúng như nguồn, y giúp đưa về công thức cơ bản
val F2L_CASES: List<TrainingCase> = listOf(
    TrainingCase(
        "f2l-01", "f2l", "basic", "Cơ bản 1: U R U' R'",
        "Trường hợp 1/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-02", "f2l", "basic", "Cơ bản 2: y' U' R' U R",
        "Trường hợp 2/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U'", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-03", "f2l", "basic", "Cơ bản 3: y' R' U' R",
        "Trường hợp 3/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-04", "f2l", "basic", "Cơ bản 4: R U R'",
        "Trường hợp 4/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U", "R'"),
        listOf("R", "U'", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-05", "f2l", "advanced", "Trắng trên 1",
        "Trường hợp 5/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U'", "R'", "U", "y'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "y", "U'", "R", "U", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-06", "f2l", "advanced", "Trắng trên 2",
        "Trường hợp 6/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U", "R'", "U", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U'", "R", "U'", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-07", "f2l", "advanced", "Trắng trên 3",
        "Trường hợp 7/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U2", "R'", "U", "y'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "y", "U'", "R", "U2", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-08", "f2l", "advanced", "Trắng trên 4",
        "Trường hợp 8/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R'", "U2", "R2", "U", "R2", "U", "R"),
        listOf("R'", "U'", "R2", "U'", "R2", "U2", "R"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-09", "f2l", "advanced", "Trắng trên 5",
        "Trường hợp 9/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U", "R'", "U", "R", "U'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "U", "R'", "U'", "R", "U'", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-10", "f2l", "advanced", "Trắng trên 6",
        "Trường hợp 10/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U'", "R'", "U", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U'", "R", "U", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-11", "f2l", "advanced", "Trắng trên 7",
        "Trường hợp 11/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U", "R'", "U2", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U2", "R", "U'", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-12", "f2l", "advanced", "Trắng trên 8",
        "Trường hợp 12/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U", "R'", "U'", "R", "U2", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U2", "R'", "U", "R", "U'", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-13", "f2l", "advanced", "Trắng trên 9",
        "Trường hợp 13/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U2", "R'", "U2", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U2", "R", "U2", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-14", "f2l", "advanced", "Trắng trên 10",
        "Trường hợp 14/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U", "R'", "U2", "R", "U2", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U2", "R'", "U2", "R", "U'", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-15", "f2l", "advanced", "Trắng trên 11",
        "Trường hợp 15/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U2", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U2", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-16", "f2l", "advanced", "Trắng trên 12",
        "Trường hợp 16/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U'", "R'", "U2", "R", "U'", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U", "R'", "U2", "R", "U", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-17", "f2l", "advanced", "Trắng bên 1",
        "Trường hợp 17/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U2", "R", "U", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U'", "R'", "U2"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-18", "f2l", "advanced", "Trắng bên 2",
        "Trường hợp 18/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U2", "R'", "U'", "R", "U'", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U", "R'", "U", "R", "U2", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-19", "f2l", "advanced", "Trắng bên 3",
        "Trường hợp 19/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "R'", "U", "R", "U2", "y", "R", "U", "R'"),
        listOf("R", "U'", "R'", "y'", "U2", "R'", "U'", "R", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-20", "f2l", "advanced", "Trắng bên 4",
        "Trường hợp 20/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U'", "R'", "U2", "y'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "y", "U2", "R", "U", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-21", "f2l", "advanced", "Trắng bên 5",
        "Trường hợp 21/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U2", "R'", "U'", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U", "R", "U2", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-22", "f2l", "advanced", "Trắng bên 6",
        "Trường hợp 22/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "R'", "U2", "R", "U", "R'", "U'", "R"),
        listOf("R'", "U", "R", "U'", "R'", "U2", "R", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-23", "f2l", "advanced", "Trắng bên 7",
        "Trường hợp 23/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U'", "R'", "U'", "R", "U'", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U", "R'", "U", "R", "U", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-24", "f2l", "advanced", "Trắng bên 8",
        "Trường hợp 24/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "U'", "R'", "U", "R", "U", "R'", "U", "R", "U'", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U", "R'", "U'", "R", "U'", "R'", "U'", "R", "U", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-25", "f2l", "advanced", "Góc kẹt 1",
        "Trường hợp 25/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "F'", "R", "U", "R'", "U'", "R'", "F", "R"),
        listOf("R'", "F'", "R", "U", "R", "U'", "R'", "F", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-26", "f2l", "advanced", "Góc kẹt 2",
        "Trường hợp 26/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U'", "R'", "U'", "F'", "U", "F"),
        listOf("F'", "U'", "F", "U", "R", "U", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-27", "f2l", "advanced", "Góc kẹt 3",
        "Trường hợp 27/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U'", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-28", "f2l", "advanced", "Góc kẹt 4",
        "Trường hợp 28/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "R'", "U", "R", "U'", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U", "R'", "U'", "R", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-29", "f2l", "advanced", "Góc kẹt 5",
        "Trường hợp 29/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("y'", "R'", "U'", "R", "U", "R'", "U'", "R"),
        listOf("R'", "U", "R", "U'", "R'", "U", "R", "y"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-30", "f2l", "advanced", "Góc kẹt 6",
        "Trường hợp 30/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U", "R'", "U'", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U", "R", "U'", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-31", "f2l", "advanced", "Góc kẹt 7",
        "Trường hợp 31/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R'", "F", "R", "F'", "R", "U'", "R'"),
        listOf("R", "U", "R'", "F", "R'", "F'", "R", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-32", "f2l", "advanced", "Góc kẹt 8",
        "Trường hợp 32/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U'", "R'", "U", "R", "U'", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U", "R'", "U'", "R", "U", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-33", "f2l", "advanced", "Cạnh kẹt 1",
        "Trường hợp 33/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U'", "R'", "U2", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U2", "R", "U", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-34", "f2l", "advanced", "Cạnh kẹt 2",
        "Trường hợp 34/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "R", "U", "R'", "U2", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U2", "R", "U'", "R'", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-35", "f2l", "advanced", "Cạnh kẹt 3",
        "Trường hợp 35/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U'", "R", "U", "R'", "d", "R'", "U'", "R"),
        listOf("R'", "U", "R", "d'", "R", "U'", "R'", "U"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-36", "f2l", "advanced", "Cạnh kẹt 4",
        "Trường hợp 36/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("U", "F'", "U'", "F", "U'", "R", "U", "R'"),
        listOf("R", "U'", "R'", "U", "F'", "U", "F", "U'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-37", "f2l", "advanced", "Cạnh kẹt 5",
        "Trường hợp 37/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U'", "R'", "d", "R'", "U2", "R", "U2", "R'", "U", "R"),
        listOf("R'", "U'", "R", "U2", "R'", "U2", "R", "d'", "R", "U", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-38", "f2l", "advanced", "Cạnh kẹt 6",
        "Trường hợp 38/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U'", "R'", "U'", "R", "U", "R'", "U2", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U2", "R", "U'", "R'", "U", "R", "U", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-39", "f2l", "advanced", "Cạnh kẹt 7",
        "Trường hợp 39/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U'", "R'", "U", "R", "U2", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U2", "R'", "U'", "R", "U", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-40", "f2l", "advanced", "Cạnh kẹt 8",
        "Trường hợp 40/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("F'", "U", "F", "U2", "R", "U", "R'", "U", "R", "U'", "R'"),
        listOf("R", "U", "R'", "U'", "R", "U'", "R'", "U2", "F'", "U'", "F"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
    TrainingCase(
        "f2l-41", "f2l", "advanced", "Cạnh kẹt 9",
        "Trường hợp 41/41 từ cuberesolver.com. Quan sát 2 viên được highlight (các viên khác xám).",
        listOf("R", "U", "R'", "U'", "R", "U'", "R'", "U2", "y'", "R'", "U'", "R"),
        listOf("R'", "U", "R", "y", "U2", "R", "U", "R'", "U", "R", "U'", "R'"),
        "Công thức gốc từ cuberesolver.com — y/y' giúp đưa về công thức cơ bản, học thuộc để thành phản xạ."
    ),
)

// ------------------------------------------------------------ OLL (2-look)
val OLL_CASES: List<TrainingCase> = listOf(
    TrainingCase("eo-line", "oll", "basic", "Đường thẳng", "2 cạnh đối nhau chưa lật (tạo thành đường thẳng trên mặt vàng).", listOf("F", "R", "U", "R'", "U'", "F'"), listOf("F", "U", "R", "U'", "R'", "F'"), "Nhìn 2 cạnh vàng chưa lên trên: chúng nằm đối nhau thành 1 đường thẳng."),
    TrainingCase("eo-l", "oll", "basic", "Chữ L", "2 cạnh kề nhau chưa lật (tạo thành hình chữ L trên mặt vàng).", listOf("F", "U", "R", "U'", "R'", "F'"), listOf("F", "R", "U", "R'", "U'", "F'"), "2 cạnh chưa lật nằm kề nhau, tạo thành góc chữ L."),
    TrainingCase("eo-dot", "oll", "basic", "Chấm (4 cạnh)", "Cả 4 cạnh đều chưa lật (không có cạnh vàng nào trên mặt vàng).", listOf("F", "R", "U", "R'", "U'", "F'", "U2", "F", "U", "R", "U'", "R'", "F'"), listOf("F", "R", "U", "R'", "U'", "F'", "U2", "F", "U", "R", "U'", "R'", "F'"), "Làm công thức đường thẳng, xoay U2, rồi làm công thức chữ L."),
    TrainingCase("co-sune", "oll", "basic", "Sune", "1 góc đã có màu vàng ở trên, 3 góc còn lại xoắn theo chiều kim đồng hồ.", listOf("R", "U", "R'", "U", "R", "U2", "R'"), listOf("R", "U2", "R'", "U'", "R", "U'", "R'"), "Trường hợp hay gặp nhất. Nhớ: góc đúng ở phía trước-bên trái."),
    TrainingCase("co-antisune", "oll", "basic", "Anti-Sune", "1 góc đã đúng, 3 góc còn lại xoắn ngược chiều kim đồng hồ.", listOf("R", "U2", "R'", "U'", "R", "U'", "R'"), listOf("R", "U", "R'", "U", "R", "U2", "R'"), "Ngược của Sune."),
    TrainingCase("co-h", "oll", "basic", "Chữ H", "Không có góc nào vàng trên mặt trên — cả 4 góc đều xoắn.", listOf("R", "U", "R'", "U", "R", "U'", "R'", "U", "R", "U2", "R'"), listOf("R", "U2", "R'", "U'", "R", "U", "R'", "U'", "R", "U'", "R'"), "Nhìn mặt vàng: không thấy góc vàng nào thì đây là H."),
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

val ALL_TRAINING_CASES: List<TrainingCase> = (F2L_CASES + OLL_CASES + PLL_CASES).map {
    // F2L: giữ y/y' (theo yêu cầu 2026-11-05 — y giúp đưa về công thức cơ bản,
    // dễ học hơn), chỉ loại x/z. OLL/PLL: loại hết x/y/z như cũ.
    if (it.stage == "f2l") {
        it.copy(
            algorithm = CubeOrientation.eliminateXZRotations(it.algorithm),
            setup = CubeOrientation.eliminateXZRotations(it.setup)
        )
    } else {
        it.copy(
            algorithm = CubeOrientation.eliminateRotations(it.algorithm),
            setup = CubeOrientation.eliminateRotations(it.setup)
        )
    }
}

// ------------------------------------------------------------ Theory
val THEORY_F2L = """
F2L là gì? Sau khi làm xong chữ thập trắng ở mặt dưới, bạn cần đưa 4 cặp
(1 góc có màu trắng + 1 viên cạnh) vào 4 khe ở 2 tầng dưới.
Ví dụ: cặp góc trắng/xanh lá/cam + cạnh xanh lá/cam → khe trước-phải.

KHUNG HÌNH CỐ ĐỊNH: xanh lá luôn ở trước mặt, cam luôn ở bên phải.
Mọi công thức trong phần này đều theo khung đó — không xoay cả khối.

CHỈ CÓ 3 BƯỚC, LẶP LẠI CHO 4 CẶP:

BƯỚC 1 — Đưa 2 viên lên tầng trên (tầng vàng):
Tìm 2 viên của cặp. Có 3 khả năng:

a) Cả 2 viên đã ở tầng trên → tốt, sang Bước 2 luôn.

b) Góc đang KẸT ở khe dưới (nằm trong khe nhưng sai hướng):
   Làm R U R' để hất nó lên tầng trên.
   (R: mở khe ra, U: đẩy viên lên, R': đóng khe lại.
   Chữ thập trắng KHÔNG bị phá.)

c) Cạnh đang KẸT ở khe dưới:
   Làm F' U F để hất nó lên tầng trên.
   (Tương tự: mở khe bằng F', đẩy lên bằng U, đóng lại bằng F.)

Nếu cả góc và cạnh đều kẹt (hiếm), hất từng viên một.

BƯỚC 2 — Xoay U/U' để đưa về trường hợp cơ bản:
Khi cả 2 viên đã ở tầng trên, xoay mặt vàng (U hoặc U') để đưa
viên GÓC về vị trí ngay trên khe trước-phải.
Nhìn mặt TRẮNG của viên góc để nhận diện:
- Trắng hướng sang PHẢI (nhìn bên phải thấy trắng) → đúng bài Cơ bản.
- Trắng hướng LÊN TRÊN hoặc ra TRƯỚC → cứ xoay U/U' thử từng vị trí,
  khi nào thấy trắng sang phải thì dừng. (Trong thực tế, sau khi hất
  viên kẹt lên đúng cách, trắng thường đã ở mặt phải.)

BƯỚC 3 — Áp dụng công thức cơ bản:
Khi góc đã ở trên khe, trắng hướng sang phải, cạnh ở tầng trên:
   R U R'
(R: mở khe, U: thả cạnh vào, R': đóng khe — cặp tự rơi vào đúng chỗ.)
Xong 1 cặp! Lặp lại 3 bước cho 3 cặp còn lại.

VÌ SAO KHÔNG CẦN HỌC 41 CÔNG THỨC?
Anh đoán đúng đấy. 41 công thức F2L bản chất chỉ là biến thể của nhau:
- Mỗi công thức dài thực ra là "xoay U vài cái để đưa về trường hợp cơ bản,
  rồi làm R U R'".
- Nếu anh luôn đưa cặp về khe trước-phải và dùng U/U' để căn chỉnh,
  mọi trường hợp đều quay về 1-2 hình cơ bản.
- Học thuộc cách làm (hất lên → xoay U → R U R') thì 41 cái kia
  tự nhiên sẽ thuộc theo, không cần học vẹt từng cái.

Mẹo: Đừng cố nhớ vị trí từng viên. Chỉ cần nhớ: hất lên → xoay U → R U R'.
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
