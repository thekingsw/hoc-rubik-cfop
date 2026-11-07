package vn.cfop.rubik.ui

import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube
import vn.cfop.rubik.solver.CubeOrientation
import vn.cfop.rubik.solver.F2lCases

/**
 * Training mode data: fixed CFOP patterns with a fixed algorithm each,
 * plus a setup sequence that turns a solved cube into the pattern so
 * the user can practise on a real cube. Every case is verified: the
 * setup keeps the earlier steps solved, and the algorithm solves the case.
 * F2L: the algorithm is the verbatim cuberesolver.com formula; the setup
 * produces the diagram state (pair at FR/top, rest solved) — NOT the
 * inverse of the algorithm when it contains y/d (that would rotate the
 * whole cube / break the cross). Verified by VerifyF2L41d.
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
// 41 trường hợp F2L: NGUỒN DUY NHẤT là bảng trong solver/F2lCases.java —
// màn Luyện tập này và bộ giải ("Giải khối này") dùng chung đúng bảng đó,
// nên số thứ tự, hình mẫu, công thức luôn khớp nhau. Thế mẫu, nhóm, mô tả
// cách nhận ra đều được TÍNH từ công thức (xem F2lCases), không gõ tay.
// Khung hình: xanh lá ở trước, cam ở phải, khe trống ở trước-phải.
// Cơ bản = 4 trường hợp đầu (ghép cặp rồi cho xuống); còn lại là Nâng cao.
val F2L_CASES: List<TrainingCase> = F2lCases.ALL.map { fc ->
    TrainingCase(
        id = fc.id,
        stage = "f2l",
        level = if (fc.number <= 4) "basic" else "advanced",
        name = fc.name,
        recognition = fc.recognition,
        algorithm = fc.alg,
        setup = fc.setup,
        tip = fc.tip,
    )
}

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
F2L là gì? Sau khi xong chữ thập trắng ở mặt dưới, anh ghép từng CẶP
(1 góc có màu trắng + 1 cạnh cùng màu) rồi cho vào 4 khe ở 2 tầng dưới.
Ví dụ: góc trắng/xanh lá/cam + cạnh xanh lá/cam → khe trước-phải.

41 TRƯỜNG HỢP là gì? Là 41 cách cặp có thể nằm so với khe trống (khi cặp
đã ở tầng trên hoặc ngay trong khe). Mỗi cách có 1 công thức chuẩn.

CÁCH LÀM CỦA NGƯỜI CHƠI — lặp lại cho từng cặp:

1) CHỌN CẶP. Nhìn cả khối, chọn cặp dễ nhất (viên gần nhau, ít nước).
   Nút "Giải khối này" cũng gợi ý đúng cặp dễ nhất.

2) ĐƯA KHE RA TRƯỚC-PHẢI. Xoay cả khối (y) cho khe cần giải ở trước-phải.
   Làm xong công thức thì xoay lại như cũ.

3) HẤT VIÊN KẸT RA (chỉ khi cần). Nếu góc hoặc cạnh đang kẹt ở một khe
   KHÁC, xoay khe đó ra trước-phải và làm R U R' để hất nó lên tầng trên.

4) NHẬN RA CASE. Cả 2 viên đã ở tầng trên (hoặc trong khe) → so với 4 nhóm:
   - Nhóm 1 (case 1–24): góc và cạnh đều ở tầng trên.
   - Nhóm 2 (case 25–30): góc nằm trong khe, cạnh ở tầng trên.
   - Nhóm 3 (case 31–36): cạnh nằm trong khe giữa, góc ở tầng trên.
   - Nhóm 4 (case 37–41): góc và cạnh đều nằm trong khe.
   Xoay U cho cặp khớp hình mẫu của case rồi làm công thức của case đó.

Trong "Giải khối này", mỗi bước hiện đúng như vậy: Bước hất viên kẹt (nếu có),
bước xoay U về hình case, bước công thức, kèm số case và cách nhận ra.

Mẹo học: đừng thuộc vẹt 41 case một lúc. Học nhóm 1 trước (nhất là case 1–4),
tập nhận ra hình trước, công thức sẽ vào dần.
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
