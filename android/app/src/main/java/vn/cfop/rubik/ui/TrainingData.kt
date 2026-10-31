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
// Phương pháp trực quan 4 bước (theo cách anh hiểu):
// 1. Xoay cả khối (y) đưa khe chưa giải về trước-phải (FR)
// 2. Đưa 2 viên (góc + cạnh) lên tầng trên (nếu kẹt ở dưới thì dùng trigger để hất lên)
// 3. Xoay U/U' để đưa về 1 trong 3 trường hợp chuẩn
// 4. Áp dụng công thức chuẩn của trường hợp đó
//
// Chỉ có 3 trường hợp chuẩn (với cặp xanh lá - cam ở tầng trên, khe FR):
// - Trắng trên: góc có mặt trắng hướng lên trên
// - Trắng phải: góc có mặt trắng hướng sang phải
// - Trắng trước: góc có mặt trắng hướng ra trước
val F2L_CASES: List<TrainingCase> = listOf(
    // --- 3 trường hợp chuẩn ---
    TrainingCase(
        "f2l-std-1", "f2l", "basic", "Chuẩn 1: Trắng trên",
        "Góc có mặt TRẮNG hướng LÊN TRÊN (nhìn thấy màu trắng trên mặt vàng). Cạnh nằm ở tầng trên.",
        listOf("R", "U", "R'"), listOf("R", "U'", "R'"),
        "Thấy trắng trên mặt vàng → đưa góc về trên khe FR (xoay U), rồi R U R'."
    ),
    TrainingCase(
        "f2l-std-2", "f2l", "basic", "Chuẩn 2: Trắng phải",
        "Góc có mặt TRẮNG hướng sang PHẢI (nhìn từ bên phải thấy màu trắng ở viên góc).",
        listOf("F'", "U", "F"), listOf("F'", "U'", "F"),
        "Thấy trắng ở mặt phải → F' U F để đưa cặp vào khe."
    ),
    TrainingCase(
        "f2l-std-3", "f2l", "basic", "Chuẩn 3: Trắng trước",
        "Góc có mặt TRẮNG hướng ra TRƯỚC (nhìn từ phía trước thấy màu trắng ở viên góc).",
        listOf("F", "U'", "F'"), listOf("F", "U", "F'"),
        "Thấy trắng ở mặt trước → F U' F' để đưa cặp vào khe."
    ),
    // --- Hất viên kẹt lên (extraction) ---
    TrainingCase(
        "f2l-ext-1", "f2l", "basic", "Hất góc kẹt lên",
        "Góc đang KẸT ở khe (đã ở vị trí nhưng sai hướng). Dùng R U R' U' để hất nó lên rồi làm lại.",
        listOf("R", "U", "R'", "U'"), listOf("U", "R", "U'", "R'"),
        "Góc kẹt → R U R' U' đưa lên tầng trên, rồi nhận diện 1 trong 3 trường hợp chuẩn."
    ),
    TrainingCase(
        "f2l-ext-2", "f2l", "basic", "Hất cạnh kẹt lên",
        "Cạnh đang KẸT ở khe. Dùng F' U' F U để đưa nó lên tầng trên trước rồi mới ghép cặp.",
        listOf("F'", "U'", "F", "U"), listOf("U'", "F'", "U", "F"),
        "Cạnh kẹt → F' U' F U đưa lên, rồi tìm góc để ghép thành 1 trong 3 trường hợp chuẩn."
    ),
    // --- Xoay khối đưa khe về FR ---
    TrainingCase(
        "f2l-rot-1", "f2l", "basic", "Xoay khối về FR",
        "Khe cần giải đang ở vị trí khác (không phải trước-phải). Xoay cả khối để đưa nó về góc trước-phải rồi làm như bình thường.",
        listOf("R", "U", "R'"), listOf("R", "U'", "R'"),
        "Luôn đưa khe về trước-phải trước khi làm: xoay cả khối trên tay (y / y' / y2), rồi bấm nút xoay tương ứng trong app — công thức sẽ tự đổi ký hiệu theo hướng cầm mới của anh."
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
    // v4: loại bỏ xoay cả khối (x/y/z) khỏi công thức — dịch thành các nước
    // mặt thường để ký hiệu luôn theo hướng người học đang cầm khối.
    // Trạng thái mẫu và tính đúng đắn của công thức được giữ nguyên.
    it.copy(
        algorithm = CubeOrientation.eliminateRotations(it.algorithm),
        setup = CubeOrientation.eliminateRotations(it.setup)
    )
}

// ------------------------------------------------------------ Theory
val THEORY_F2L = """
F2L là gì? Sau khi làm xong chữ thập trắng ở mặt dưới, bạn cần đưa 4 cặp
(1 góc + 1 cạnh) vào 4 khe ở 2 tầng dưới. Mỗi cặp gồm 1 viên góc có màu trắng
và 1 viên cạnh — ví dụ cặp xanh lá - cam.

CHỈ CẦN NHỚ 4 BƯỚC (không cần học 41 công thức):

BƯỚC 1 — Xoay cả khối đưa khe về trước-phải:
Luôn làm việc ở khe TRƯỚC-PHẢI (FR). Nếu khe cần giải đang ở chỗ khác,
xoay cả khối trên tay để đưa nó về FR, rồi bấm nút xoay tương ứng trong
app (Trái/Phải/Sau ra trước) — mọi công thức sẽ tự đổi ký hiệu theo hướng
cầm mới của anh. Ký hiệu luôn theo vị trí (R = bên phải, F = trước mặt),
không theo màu cố định, nên anh cứ nhìn vị trí mà xoay.

BƯỚC 2 — Đưa 2 viên lên tầng trên:
Tìm 2 viên của cặp (góc có trắng + cạnh). Nếu chúng đang ở tầng trên rồi thì tốt.
Nếu 1 viên đang KẸT ở tầng dưới (trong khe), dùng trigger của khe đó để hất lên:
- Kẹt ở khe FR: làm R U R' (hất góc lên) hoặc F' U F (hất cạnh lên)
- Kẹt ở khe khác: xoay y đưa khe đó về FR trước, rồi hất lên
Chữ thập trắng KHÔNG bị phá khi hất.

BƯỚC 3 — Xoay U/U' về trường hợp chuẩn:
Khi cả 2 viên đã ở tầng trên, chỉ có 3 trường hợp. Nhìn mặt TRẮNG của viên góc:
- Trắng hướng LÊN TRÊN (thấy trắng trên mặt vàng) → Trường hợp 1
- Trắng hướng sang PHẢI (thấy trắng ở mặt phải) → Trường hợp 2  
- Trắng hướng ra TRƯỚC (thấy trắng ở mặt trước) → Trường hợp 3
Xoay U hoặc U' để đưa góc về vị trí trên khe FR.

BƯỚC 4 — Áp dụng công thức chuẩn:
- Trường hợp 1 & 2 (trắng trên/phải): R U R'
- Trường hợp 3 (trắng trước): F' U F

Vậy là xong 1 cặp! Lặp lại 4 bước cho 3 cặp còn lại.

Mẹo: Đừng cố nhớ vị trí từng viên. Chỉ cần nhớ: FR → hất lên → nhìn trắng → U → công thức.
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
