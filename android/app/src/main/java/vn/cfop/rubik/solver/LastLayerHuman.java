package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tầng cuối (OLL 2-look + PLL 2-look) theo cách người chơi.
 *
 * Mỗi "look" được tách thành các bước như người chơi làm:
 *   1. Xoay U (nếu cần) để đưa thế về đúng hướng của công thức.
 *   2. Công thức có tên (Sune, Aa, Ua, ...), kèm cách nhận ra.
 *   3. Xoay U cuối (AUF) để các viên khớp màu tâm.
 * Không có tìm kiếm của máy: chỉ thử các công thức người chơi đã biết, mỗi
 * công thức với 0–3 lần xoay U trước/sau, và chọn cách ít xoay U nhất.
 */
final class LastLayerHuman {

    private LastLayerHuman() {
    }

    private interface Goal {
        boolean test(Cube c);
    }

    private static final class Look {
        final String name;
        final List<String> alg;
        final String recognition;

        Look(String name, String alg, String recognition) {
            this.name = name;
            this.alg = Cube.parseMoves(alg);
            this.recognition = recognition;
        }
    }

    // ------------------------------------------------------------ OLL: cạnh
    private static final Look EO_DOT = new Look("Chấm",
            "F R U R' U' F'",
            "Mặt vàng chỉ có ô tâm vàng, không cạnh nào vàng hướng lên (hình “chấm”). "
                    + "Làm F R U R' U' F' một lần sẽ ra chữ L hoặc đường thẳng, rồi nhìn lại.");
    private static final Look EO_LINE = new Look("Đường thẳng",
            "F R U R' U' F'",
            "Có 2 cạnh vàng đối diện nhau, nối với tâm thành một đường thẳng. "
                    + "Cầm sao cho đường nằm NGANG (trái–phải) rồi làm F R U R' U' F'.");
    private static final Look EO_L = new Look("Chữ L",
            "F U R U' R' F'",
            "Có 2 cạnh vàng kề nhau, cùng tâm tạo thành chữ L. "
                    + "Cầm sao cho chữ L nằm ở góc sau-trái rồi làm F U R U' R' F'.");

    // ------------------------------------------------------------ OLL: góc
    private static final Look SUNE = new Look("Sune", "R U R' U R U2 R'", null);
    private static final Look ANTI_SUNE = new Look("Anti-Sune", "R U2 R' U' R U' R'", null);
    private static final Look CO_H = new Look("H", "R U R' U R U' R' U R U2 R'", null);
    private static final Look CO_PI = new Look("Pi", "R U2 R2 U' R2 U' R2 U2 R", null);
    private static final Look CO_U = new Look("U", "R2 D R' U2 R D' R' U2 R'", null);

    // ------------------------------------------------------------ PLL
    private static final Look AA = new Look("Aa", "x R' U R' D2 R U' R' D2 R2 x'", null);
    private static final Look AB = new Look("Ab", "x R2 D2 R U R' D2 R U' R x'", null);
    private static final Look Y = new Look("Y",
            "F R U' R' U' R U R' F' R U R' U' R' F R F'", null);
    private static final Look UA = new Look("Ua", "R U' R U R U R U' R' U' R2", null);
    private static final Look UB = new Look("Ub", "R2 U R U R' U' R' U' R' U R'", null);
    private static final Look PH = new Look("H", "M2 U M2 U2 M2 U M2", null);
    private static final Look PZ = new Look("Z", "M2 U M2 U M' U2 M2 U2 M'", null);

    // ------------------------------------------------------------ đo trạng thái
    static int uEdgesOriented(Cube c) {
        int n = 0;
        for (int i : new int[]{1, 3, 5, 7}) {
            if (c.s[i] == 3) {
                n++;
            }
        }
        return n;
    }

    static int uCornersOriented(Cube c) {
        int n = 0;
        for (int i : new int[]{0, 2, 6, 8}) {
            if (c.s[i] == 3) {
                n++;
            }
        }
        return n;
    }

    static boolean uCornersCorrect(Cube c) {
        int[] s = c.s;
        return s[8] == 3 && s[9] == 4 && s[20] == 2
                && s[6] == 3 && s[18] == 2 && s[38] == 1
                && s[0] == 3 && s[36] == 1 && s[47] == 5
                && s[2] == 3 && s[45] == 5 && s[11] == 4;
    }

    static boolean uEdgesCorrect(Cube c) {
        int[] s = c.s;
        return s[5] == 3 && s[10] == 4 && s[7] == 3 && s[19] == 2
                && s[3] == 3 && s[37] == 1 && s[1] == 3 && s[46] == 5;
    }

    // ------------------------------------------------------------ tên mặt / vị trí
    private static final String[] SIDE_NAMES = {"bên phải", "phía trước", "bên trái", "phía sau"};
    // Với mỗi mặt bên (R F L B): 2 ô góc hàng trên, ô cạnh hàng trên, ô tâm.
    private static final int[][] SIDE = {
            {9, 11, 10, 13}, {18, 20, 19, 22}, {36, 38, 37, 40}, {45, 47, 46, 49}};
    private static final int[][] TOP_CORNERS = {{8, 9, 20}, {6, 18, 38}, {0, 36, 47}, {2, 45, 11}};
    private static final String[] TOP_CORNER_NAMES = {"trước-phải", "trước-trái", "sau-trái", "sau-phải"};

    private static String faceDir(int facelet) {
        switch (facelet / 9) {
            case 0: return "hướng lên";
            case 1: return "sang phải";
            case 2: return "ra trước";
            case 4: return "sang trái";
            default: return "ra sau";
        }
    }

    /** Mặt có "đèn pha" (2 góc kề nhau cùng màu); -1 nếu không có hoặc có nhiều hơn một. */
    private static int headlightFace(Cube c) {
        int found = -1;
        for (int f = 0; f < 4; f++) {
            if (c.s[SIDE[f][0]] == c.s[SIDE[f][1]]) {
                if (found >= 0) {
                    return -1;
                }
                found = f;
            }
        }
        return found;
    }

    /** Mặt có cạnh tầng trên đã đúng màu tâm; -1 nếu không có hoặc có nhiều hơn một. */
    private static int solvedEdgeFace(Cube c) {
        int found = -1;
        for (int f = 0; f < 4; f++) {
            if (c.s[SIDE[f][2]] == c.s[SIDE[f][3]]) {
                if (found >= 0) {
                    return -1;
                }
                found = f;
            }
        }
        return found;
    }

    /** Mô tả các góc của hình mẫu OLL góc (đã xoay U đúng hướng). */
    private static String describeCorners(Cube c) {
        StringBuilder up = new StringBuilder();
        StringBuilder other = new StringBuilder();
        int nUp = 0;
        for (int i = 0; i < 4; i++) {
            int yellow = -1;
            for (int f : TOP_CORNERS[i]) {
                if (c.s[f] == 3) {
                    yellow = f;
                }
            }
            if (yellow / 9 == 0) {
                nUp++;
                if (up.length() > 0) {
                    up.append(", ");
                }
                up.append(TOP_CORNER_NAMES[i]);
            } else {
                if (other.length() > 0) {
                    other.append("; ");
                }
                other.append("góc ").append(TOP_CORNER_NAMES[i]).append(" vàng ").append(faceDir(yellow));
            }
        }
        StringBuilder sb = new StringBuilder();
        if (nUp == 0) {
            sb.append("Không góc nào có mặt vàng hướng lên. ");
        } else {
            sb.append("Góc vàng hướng lên: ").append(up).append(". ");
        }
        if (other.length() > 0) {
            sb.append("Các góc còn lại: ").append(other).append(".");
        }
        return sb.toString().trim();
    }

    // ------------------------------------------------------------ chọn công thức
    private static final class Pick {
        Look look;
        int pre;
        int post;
        int cost;
    }

    private static List<String> uTurns(int k) {
        List<String> out = new ArrayList<>();
        if (k == 1) {
            out.add("U");
        } else if (k == 2) {
            out.add("U2");
        } else if (k == 3) {
            out.add("U'");
        }
        return out;
    }

    private static Cube withU(Cube c, int k) {
        Cube t = c.copy();
        for (int i = 0; i < k; i++) {
            t.apply("U");
        }
        return t;
    }

    /** Công thức + số lần xoay U trước/sau ít nhất; null nếu không công thức nào hợp. */
    private static Pick pick(Cube c, List<Look> looks, Goal goal, boolean allowPost) {
        Pick best = null;
        for (Look look : looks) {
            for (int r = 0; r < 4; r++) {
                for (int q = 0; q < (allowPost ? 4 : 1); q++) {
                    Cube t = withU(c, r);
                    t.applySeq(look.alg);
                    t = withU(t, q);
                    if (!goal.test(t)) {
                        continue;
                    }
                    int cost = (r > 0 ? 1 : 0) + (q > 0 ? 1 : 0);
                    if (best == null || cost < best.cost) {
                        best = new Pick();
                        best.look = look;
                        best.pre = r;
                        best.post = q;
                        best.cost = cost;
                    }
                }
            }
        }
        return best;
    }

    /** Chỉ cần xoay U (không công thức): trả số lần, hoặc -1. */
    private static int bareU(Cube c, Goal goal) {
        int[] order = {0, 1, 3, 2};
        for (int r : order) {
            if (goal.test(withU(c, r))) {
                return r;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------ dựng stage
    private static void addStage(List<CfopSolver.Stage> out, String key, String title, List<String> moves,
                                 String note) {
        out.add(new CfopSolver.Stage(key, title, moves, -1, note));
    }

    /**
     * Áp một look đã chọn lên c và thêm các stage: xoay U trước, công thức, xoay U cuối.
     * featureHint: câu chỉ cách xoay U trước (tính từ thế thật), có thể null.
     */
    private static void emit(Cube c, List<CfopSolver.Stage> out, String key, String prefix, Pick p,
                             String recognition, String alignHint, String aufHint) {
        int step = 0;
        if (p.pre > 0) {
            step++;
            List<String> mv = uTurns(p.pre);
            addStage(out, key, prefix + " — Bước " + step + ": Xoay U cho đúng thế",
                    mv, "Xoay tầng trên (U) " + (alignHint == null ? "để thế khớp với công thức." : alignHint));
            c.applySeq(mv);
        }
        step++;
        addStage(out, key, prefix + " — Bước " + step + ": Công thức " + p.look.name,
                new ArrayList<>(p.look.alg),
                recognition + " Công thức " + p.look.name + ": " + join(p.look.alg) + ".");
        c.applySeq(p.look.alg);
        if (p.post > 0) {
            step++;
            List<String> mv = uTurns(p.post);
            addStage(out, key, prefix + " — Bước " + step + ": Xoay U cuối",
                    mv, aufHint);
            c.applySeq(mv);
        }
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(l.get(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ OLL 2-look
    private static void solveEo(Cube c, List<CfopSolver.Stage> out) {
        String prefix = "OLL 1/2 · Cạnh vàng";
        for (int iter = 0; iter < 3; iter++) {
            int n = uEdgesOriented(c);
            if (n == 4) {
                return;
            }
            if (n == 0) {
                Pick p = new Pick();
                p.look = EO_DOT;
                emit(c, out, "oll_eo", prefix + ": " + EO_DOT.name, p, EO_DOT.recognition, null, null);
                continue;
            }
            Pick p = pick(c, Arrays.asList(EO_LINE, EO_L), cc -> uEdgesOriented(cc) == 4, false);
            if (p == null) {
                throw new IllegalStateException("EO failed");
            }
            String hint = p.look == EO_LINE
                    ? "cho đường vàng nằm ngang (trái–phải)."
                    : "cho chữ L vàng nằm ở góc sau-trái.";
            emit(c, out, "oll_eo", prefix + ": " + p.look.name, p, p.look.recognition, hint, null);
        }
        if (uEdgesOriented(c) != 4) {
            throw new IllegalStateException("EO failed");
        }
    }

    private static void solveCo(Cube c, List<CfopSolver.Stage> out) {
        String prefix = "OLL 2/2 · Góc vàng";
        List<Look> looks = Arrays.asList(SUNE, ANTI_SUNE, CO_H, CO_PI, CO_U);
        for (int iter = 0; iter < 3; iter++) {
            if (uCornersOriented(c) == 4) {
                return;
            }
            int up = uCornersOriented(c);
            String family = up == 1 ? "Có 1 góc vàng hướng lên → Sune hoặc Anti-Sune. "
                    : up == 0 ? "Không góc vàng nào hướng lên → H hoặc Pi. "
                    : "Có 2 góc vàng hướng lên → U (hoặc T/L). ";
            Pick p = pick(c, looks, cc -> uCornersOriented(cc) == 4, false);
            if (p != null) {
                Cube ref = withU(c, p.pre);
                String rec = family + "Hình mẫu của " + p.look.name + ": " + describeCorners(ref);
                emit(c, out, "oll_co", prefix + ": " + p.look.name, p, rec,
                        "cho các góc vàng khớp hình mẫu của " + p.look.name + " (xem bước sau).", null);
                continue;
            }
            // T/L (không có trong 5 công thức): làm Sune 1 lần rồi nhìn lại.
            Pick s = new Pick();
            s.look = SUNE;
            emit(c, out, "oll_co", prefix + ": T/L → Sune 1 lần", s,
                    family + "Trường hợp T/L: làm Sune một lần rồi nhìn lại, sẽ ra 1 trong 5 hình trên.",
                    null, null);
        }
        if (uCornersOriented(c) != 4) {
            throw new IllegalStateException("CO failed");
        }
    }

    // ------------------------------------------------------------ PLL 2-look
    private static void solvePllCorners(Cube c, List<CfopSolver.Stage> out) {
        if (uCornersCorrect(c)) {
            return;
        }
        String prefix = "PLL 1/2 · Hoán vị góc";
        int r = bareU(c, LastLayerHuman::uCornersCorrect);
        if (r >= 0) {
            List<String> mv = uTurns(r);
            addStage(out, "pll_c", prefix + ": đã đúng chỗ — chỉ xoay U", mv,
                    "4 góc đã đúng vị trí với nhau, chỉ cần xoay tầng trên cho mỗi góc khớp màu tâm.");
            c.applySeq(mv);
            return;
        }
        Pick p = pick(c, Arrays.asList(AA, AB, Y), LastLayerHuman::uCornersCorrect, true);
        if (p == null) {
            throw new IllegalStateException("PLL corners failed");
        }
        Cube ref = withU(c, p.pre);
        String rec;
        String alignHint;
        if (p.look == Y) {
            rec = "Không có “đèn pha” (không có 2 góc kề nhau cùng màu ở cùng một mặt): 2 góc chéo nhau đổi chỗ → Y-perm.";
            alignHint = "để thế khớp với công thức Y.";
        } else {
            int face = headlightFace(ref);
            rec = "Có đúng một “đèn pha” (2 góc kề nhau cùng màu trên một mặt) → A-perm "
                    + "(Aa hoặc Ab tuỳ chiều hoán đổi của 3 góc còn lại). ";
            alignHint = face >= 0
                    ? "cho đèn pha nằm ở " + SIDE_NAMES[face] + "."
                    : "để thế khớp với công thức.";
            if (face >= 0) {
                rec += "Với " + p.look.name + " ở thế này, đèn pha nằm ở " + SIDE_NAMES[face] + ".";
            }
        }
        emit(c, out, "pll_c", prefix + ": " + p.look.name, p, rec, alignHint,
                "Xoay tầng trên (U) cho 4 góc khớp màu tâm của các mặt bên.");
    }

    private static void solvePllEdges(Cube c, List<CfopSolver.Stage> out) {
        if (uEdgesCorrect(c)) {
            return;
        }
        String prefix = "PLL 2/2 · Hoán vị cạnh";
        Goal goal = cc -> uEdgesCorrect(cc) && uCornersCorrect(cc);
        int r = bareU(c, goal);
        if (r >= 0) {
            List<String> mv = uTurns(r);
            addStage(out, "pll_e", prefix + ": đã đúng chỗ — chỉ xoay U", mv,
                    "Các cạnh đã đúng vị trí với nhau, chỉ cần xoay tầng trên một chút là xong.");
            c.applySeq(mv);
            return;
        }
        Pick p = pick(c, Arrays.asList(UA, UB, PH, PZ), goal, true);
        if (p == null) {
            throw new IllegalStateException("PLL edges failed");
        }
        Cube ref = withU(c, p.pre);
        String rec;
        String alignHint;
        if (p.look == UA || p.look == UB) {
            int face = solvedEdgeFace(ref);
            rec = "Có đúng 1 cạnh đã đúng chỗ, 3 cạnh còn lại xoay vòng → U-perm "
                    + "(Ua và Ub là hai chiều xoay ngược nhau). ";
            alignHint = face >= 0
                    ? "cho cạnh đã đúng nằm ở " + SIDE_NAMES[face] + "."
                    : "để thế khớp với công thức.";
            if (face >= 0) {
                rec += "Với " + p.look.name + " ở thế này, cạnh đúng nằm ở " + SIDE_NAMES[face] + ".";
            }
        } else if (p.look == PH) {
            rec = "Không cạnh nào đúng chỗ, 2 cặp cạnh đối diện đổi chỗ cho nhau (trước↔sau, trái↔phải) → H-perm.";
            alignHint = "để thế khớp với công thức H.";
        } else {
            rec = "Không cạnh nào đúng chỗ, 2 cặp cạnh kề nhau đổi chỗ cho nhau → Z-perm.";
            alignHint = "để thế khớp với công thức Z.";
        }
        emit(c, out, "pll_e", prefix + ": " + p.look.name, p, rec, alignHint,
                "Xoay tầng trên (U) lần cuối cho các mặt bên cùng một màu. Xong!");
    }

    /** Giải tầng cuối trên c (c bị đổi) và thêm các stage. */
    static void solve(Cube c, List<CfopSolver.Stage> out) {
        solveEo(c, out);
        solveCo(c, out);
        solvePllCorners(c, out);
        solvePllEdges(c, out);
    }
}
