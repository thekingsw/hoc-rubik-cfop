package vn.cfop.rubik.solver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CFOP 2-look solver. Faithful port of the Python reference solver
 * (workspace/rubik-cfop/python/solver.py), which is tested against
 * hundreds of random scrambles.
 *
 * Stages: white cross (on D) -> 4 F2L pairs -> OLL (2-look) -> PLL (2-look).
 * The last layer is the U face, whose colour is yellow (3).
 */
public final class CfopSolver {

    private CfopSolver() {
    }

    /** One solving stage: a key, a Vietnamese title, and its moves. */
    public static final class Stage {
        public final String key;
        public final String title;
        public final List<String> moves;
        /** F2L pair index (0..3) for "f2l" stages, -1 otherwise. */
        public final int pairIndex;
        /** Lời giải thích cho người học (F2L: trường hợp nào trong 41, vì sao). Có thể rỗng. */
        public final String note;

        public Stage(String key, String title, List<String> moves) {
            this(key, title, moves, -1);
        }

        public Stage(String key, String title, List<String> moves, int pairIndex) {
            this(key, title, moves, pairIndex, "");
        }

        public Stage(String key, String title, List<String> moves, int pairIndex, String note) {
            this.key = key;
            this.title = title;
            this.moves = moves;
            this.pairIndex = pairIndex;
            this.note = note;
        }

        public int moveCount() {
            return moves.size();
        }
    }

    // ------------------------------------------------------------ slots

    private static final int[][] EDGE_SLOTS = {
            {5, 10}, {7, 19}, {3, 37}, {1, 46},     // UR UF UL UB
            {32, 16}, {28, 25}, {30, 43}, {34, 52}, // DR DF DL DB
            {23, 12}, {21, 41}, {48, 14}, {50, 39}  // FR FL BR BL
    };

    private static final int[][] CORNER_SLOTS = {
            {8, 9, 20}, {6, 18, 38}, {0, 36, 47}, {2, 45, 11},  // URF UFL ULB UBR
            {29, 26, 15}, {27, 44, 24}, {33, 53, 42}, {35, 17, 51} // DFR DLF DBL DRB
    };

    // ------------------------------------------------------------ cross

    // white edges: (other colour, goal facelet of the white sticker)
    // order matches CROSS_GOAL (DF, DR, DB, DL): WG, white-orange, WB, white-red
    private static final int[][] WHITE_EDGES = {{2, 28}, {4, 32}, {5, 34}, {1, 30}};
    private static final int[] CROSS_GOAL = {28, 32, 34, 30};

    /** Face turns only: M/E/S would move the centre stickers. */
    private static final String[] FACE_MOVES = {
            "U", "U'", "U2", "D", "D'", "D2", "R", "R'", "R2",
            "L", "L'", "L2", "F", "F'", "F2", "B", "B'", "B2"};

    private static int[] findWhiteStickers(Cube cube) {
        int[] out = new int[4];
        for (int k = 0; k < 4; k++) {
            int other = WHITE_EDGES[k][0];
            int found = -1;
            for (int[] slot : EDGE_SLOTS) {
                int a = slot[0], b = slot[1];
                if ((cube.s[a] == 0 && cube.s[b] == other)
                        || (cube.s[a] == other && cube.s[b] == 0)) {
                    found = cube.s[a] == 0 ? a : b;
                    break;
                }
            }
            if (found < 0) {
                throw new IllegalStateException("white edge not found");
            }
            out[k] = found;
        }
        return out;
    }

    private static String crossKey(int[] w) {
        return w[0] + "," + w[1] + "," + w[2] + "," + w[3];
    }

    /** BFS for the white cross on D. */
    public static List<String> crossSolution(Cube cube) {
        int[] start = findWhiteStickers(cube);
        String goal = crossKey(CROSS_GOAL);
        if (crossKey(start).equals(goal)) {
            return new ArrayList<>();
        }
        Map<String, String> parent = new HashMap<>();
        Map<String, String> parentMove = new HashMap<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        parent.put(crossKey(start), null);
        q.add(start);
        while (!q.isEmpty()) {
            int[] st = q.poll();
            for (String m : FACE_MOVES) {
                int[] inv = Cube.INV.get(m);
                int[] nxt = new int[4];
                for (int i = 0; i < 4; i++) {
                    nxt[i] = inv[st[i]];
                }
                String nk = crossKey(nxt);
                if (!parent.containsKey(nk)) {
                    parent.put(nk, crossKey(st));
                    parentMove.put(nk, m);
                    if (nk.equals(goal)) {
                        List<String> moves = new ArrayList<>();
                        String cur = nk;
                        while (parent.get(cur) != null) {
                            moves.add(0, parentMove.get(cur));
                            cur = parent.get(cur);
                        }
                        return moves;
                    }
                    q.add(nxt);
                }
            }
        }
        throw new IllegalStateException("cross BFS failed");
    }

    public static boolean crossSolved(Cube cube) {
        int[] s = cube.s;
        return s[28] == 0 && s[25] == 2
                && s[32] == 0 && s[16] == 4
                && s[34] == 0 && s[52] == 5
                && s[30] == 0 && s[43] == 1;
    }

    // ------------------------------------------------------------ F2L

    // (corner colours, corner target facelets, edge colours, edge target facelets)
    // Corner colours are (white, F/B colour, R/L colour); R=orange(4), L=red(1).
    private static final int[][] F2L_CORNER_COLOURS = {
            {0, 2, 4}, {0, 2, 1}, {0, 5, 4}, {0, 5, 1}};
    private static final int[][] F2L_CORNER_TARGETS = {
            {29, 26, 15}, {27, 24, 44}, {35, 51, 17}, {33, 53, 42}};
    private static final int[][] F2L_EDGE_COLOURS = {
            {2, 4}, {2, 1}, {5, 4}, {5, 1}};
    private static final int[][] F2L_EDGE_TARGETS = {
            {23, 12}, {21, 41}, {48, 14}, {50, 39}};

    static boolean pairSolved(Cube cube, int pairIdx) {
        int[] cc = F2L_CORNER_COLOURS[pairIdx];
        int[] ct = F2L_CORNER_TARGETS[pairIdx];
        int[] ec = F2L_EDGE_COLOURS[pairIdx];
        int[] et = F2L_EDGE_TARGETS[pairIdx];
        int[] s = cube.s;
        return s[ct[0]] == cc[0] && s[ct[1]] == cc[1] && s[ct[2]] == cc[2]
                && s[et[0]] == ec[0] && s[et[1]] == ec[1];
    }

    public static boolean f2lSolved(Cube cube) {
        if (!crossSolved(cube)) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!pairSolved(cube, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Facelet indices of an F2L pair's two pieces in the given state:
     * the corner's 3 facelets, then the edge's 2 facelets.
     * Used by the UI to highlight the pair being solved.
     */
    public static int[] f2lPairFacelets(int[] s, int pairIdx) {
        int[] cc = F2L_CORNER_COLOURS[pairIdx];
        int[] ec = F2L_EDGE_COLOURS[pairIdx];
        int[] out = new int[5];
        boolean foundCorner = false;
        for (int[] slot : CORNER_SLOTS) {
            int a = s[slot[0]], b = s[slot[1]], c = s[slot[2]];
            if ((a == cc[0] || a == cc[1] || a == cc[2])
                    && (b == cc[0] || b == cc[1] || b == cc[2])
                    && (c == cc[0] || c == cc[1] || c == cc[2])
                    && a != b && b != c && a != c) {
                out[0] = slot[0];
                out[1] = slot[1];
                out[2] = slot[2];
                foundCorner = true;
                break;
            }
        }
        boolean foundEdge = false;
        for (int[] slot : EDGE_SLOTS) {
            int a = s[slot[0]], b = s[slot[1]];
            if ((a == ec[0] && b == ec[1]) || (a == ec[1] && b == ec[0])) {
                out[3] = slot[0];
                out[4] = slot[1];
                foundEdge = true;
                break;
            }
        }
        if (!foundCorner || !foundEdge) {
            throw new IllegalStateException("F2L pair pieces not found: " + pairIdx);
        }
        return out;
    }

    // ------------------------------------------------------------ OLL / PLL highlights
    // U-layer slots (U facelet first) and their solved colours, for the
    // UI to highlight what still needs fixing in each last-layer stage.

    private static final int[][] U_EDGES = {{5, 10}, {7, 19}, {3, 37}, {1, 46}};
    private static final int[][] U_CORNERS = {{8, 9, 20}, {6, 18, 38}, {0, 36, 47}, {2, 45, 11}};
    private static final int[][] U_EDGE_SOLVED = {{3, 4}, {3, 2}, {3, 1}, {3, 5}};
    private static final int[][] U_CORNER_SOLVED = {{3, 4, 2}, {3, 2, 1}, {3, 1, 5}, {3, 5, 4}};

    private static int[] toIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /**
     * Facelets of the U edges that are not oriented yet (U sticker is not
     * yellow). Both facelets of each such edge. Shows the EO pattern
     * (line / L / dot) the user must recognise.
     */
    public static int[] ollEoFacelets(int[] s) {
        List<Integer> out = new ArrayList<>();
        for (int[] slot : U_EDGES) {
            if (s[slot[0]] != 3) {
                out.add(slot[0]);
                out.add(slot[1]);
            }
        }
        return toIntArray(out);
    }

    /**
     * Facelets of the U corners that are not oriented yet (U sticker is
     * not yellow). All 3 facelets of each such corner.
     */
    public static int[] ollCoFacelets(int[] s) {
        List<Integer> out = new ArrayList<>();
        for (int[] slot : U_CORNERS) {
            if (s[slot[0]] != 3) {
                out.add(slot[0]);
                out.add(slot[1]);
                out.add(slot[2]);
            }
        }
        return toIntArray(out);
    }

    /**
     * Facelets of the U corners that are not in their solved position yet
     * (for PLL corners). All 3 facelets of each such corner.
     */
    public static int[] pllCornersFacelets(int[] s) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int[] slot = U_CORNERS[i];
            int[] want = U_CORNER_SOLVED[i];
            if (s[slot[0]] != want[0] || s[slot[1]] != want[1] || s[slot[2]] != want[2]) {
                out.add(slot[0]);
                out.add(slot[1]);
                out.add(slot[2]);
            }
        }
        return toIntArray(out);
    }

    /**
     * Facelets of the U edges that are not in their solved position yet
     * (for PLL edges). Both facelets of each such edge.
     */
    public static int[] pllEdgesFacelets(int[] s) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int[] slot = U_EDGES[i];
            int[] want = U_EDGE_SOLVED[i];
            if (s[slot[0]] != want[0] || s[slot[1]] != want[1]) {
                out.add(slot[0]);
                out.add(slot[1]);
            }
        }
        return toIntArray(out);
    }

    // ------------------------------------------------------------ last layer

    private static final List<String> LINE_ALG = Cube.parseMoves("F R U R' U' F'");
    private static final List<String> L_ALG = Cube.parseMoves("F U R U' R' F'");

    // NB: the textbook 'T' (R U R' U' R' F R F') and 'L'
    // (F R' F' R U R U' R') flip two U edges, so they are left out;
    // the Sune fallback in solveCo still reaches every case.
    private static final String[] CORNER_ALG_NAMES = {
            "Sune", "Anti-Sune", "H", "Pi", "U"};
    private static final List<List<String>> CORNER_ALGS = Arrays.asList(
            Cube.parseMoves("R U R' U R U2 R'"),
            Cube.parseMoves("R U2 R' U' R U' R'"),
            Cube.parseMoves("R U R' U R U' R' U R U2 R'"),
            Cube.parseMoves("R U2 R2 U' R2 U' R2 U2 R"),
            Cube.parseMoves("R2 D R' U2 R D' R' U2 R'"));

    private static final String[] PLL_CORNER_NAMES = {"Aa", "Ab", "Y"};
    private static final List<List<String>> PLL_CORNER_ALGS = Arrays.asList(
            Cube.parseMoves("x R' U R' D2 R U' R' D2 R2 x'"),
            Cube.parseMoves("x R2 D2 R U R' D2 R U' R x'"),
            Cube.parseMoves("F R U' R' U' R U R' F' R U R' U' R' F R F'"));

    private static final String[] PLL_EDGE_NAMES = {"Ua", "Ub", "H", "Z"};
    private static final List<List<String>> PLL_EDGE_ALGS = Arrays.asList(
            Cube.parseMoves("R U' R U R U R U' R' U' R2"),
            Cube.parseMoves("R2 U R U R' U' R' U' R' U R'"),
            Cube.parseMoves("M2 U M2 U2 M2 U M2"),
            Cube.parseMoves("M2 U M2 U M' U2 M2 U2 M' U2"));

    private interface Check {
        boolean test(Cube c);
    }

    private static int uEdgesOriented(Cube cube) {
        // Last layer is the U face, whose colour is yellow (3).
        int n = 0;
        for (int i : new int[]{1, 3, 5, 7}) {
            if (cube.s[i] == 3) {
                n++;
            }
        }
        return n;
    }

    private static int uCornersOriented(Cube cube) {
        int n = 0;
        for (int i : new int[]{0, 2, 6, 8}) {
            if (cube.s[i] == 3) {
                n++;
            }
        }
        return n;
    }

    private static boolean uCornersCorrect(Cube cube) {
        // Solved U face (yellow): URF yellow/orange/green, UFL yellow/green/red,
        // ULB yellow/red/blue, UBR yellow/blue/orange.
        int[] s = cube.s;
        return s[8] == 3 && s[9] == 4 && s[20] == 2
                && s[6] == 3 && s[18] == 2 && s[38] == 1
                && s[0] == 3 && s[36] == 1 && s[47] == 5
                && s[2] == 3 && s[45] == 5 && s[11] == 4;
    }

    private static boolean uEdgesCorrect(Cube cube) {
        int[] s = cube.s;
        return s[5] == 3 && s[10] == 4 && s[7] == 3 && s[19] == 2
                && s[3] == 3 && s[37] == 1 && s[1] == 3 && s[46] == 5;
    }

    /**
     * Try each alg with 0..3 U turns before AND after it. Both matter:
     * the pre-turn lines the algorithm up with the pieces, and the
     * post-turn (AUF) rotates the layer back into place afterwards.
     * Also tries a bare U turn first: sometimes the pieces only need the
     * layer rotated into place and no algorithm is needed at all.
     * Returns the moves or null.
     */
    private static List<String> tryAlgs(Cube cube, List<List<String>> algs, Check check) {
        for (int r = 0; r < 4; r++) {
            Cube test = cube.copy();
            List<String> moves = new ArrayList<>();
            for (int i = 0; i < r; i++) {
                moves.add("U");
            }
            test.applySeq(moves);
            if (check.test(test)) {
                return Cube.simplifyMoves(moves);
            }
        }
        for (List<String> alg : algs) {
            for (int r = 0; r < 4; r++) {
                for (int q = 0; q < 4; q++) {
                    Cube test = cube.copy();
                    List<String> moves = new ArrayList<>();
                    for (int i = 0; i < r; i++) {
                        moves.add("U");
                    }
                    moves.addAll(alg);
                    for (int i = 0; i < q; i++) {
                        moves.add("U");
                    }
                    test.applySeq(moves);
                    if (check.test(test)) {
                        return Cube.simplifyMoves(moves);
                    }
                }
            }
        }
        return null;
    }

    private static List<String> solveEo(Cube cube) {
        List<String> moves = new ArrayList<>();
        for (int iter = 0; iter < 3; iter++) {
            int n = uEdgesOriented(cube);
            if (n == 4) {
                return moves;
            }
            if (n == 0) {
                cube.applySeq(LINE_ALG);
                moves.addAll(LINE_ALG);
                continue;
            }
            List<String> got = tryAlgs(cube, Arrays.asList(LINE_ALG, L_ALG),
                    c -> uEdgesOriented(c) == 4);
            if (got == null) {
                throw new IllegalStateException("EO failed");
            }
            cube.applySeq(got);
            moves.addAll(got);
        }
        if (uEdgesOriented(cube) != 4) {
            throw new IllegalStateException("EO failed");
        }
        return moves;
    }

    private static List<String> solveCo(Cube cube) {
        List<String> moves = new ArrayList<>();
        for (int iter = 0; iter < 3; iter++) {
            if (uCornersOriented(cube) == 4) {
                return moves;
            }
            List<String> got = tryAlgs(cube, CORNER_ALGS,
                    c -> uCornersOriented(c) == 4);
            if (got != null) {
                cube.applySeq(got);
                moves.addAll(got);
                continue;
            }
            cube.applySeq(CORNER_ALGS.get(0)); // Sune once, then retry
            moves.addAll(CORNER_ALGS.get(0));
        }
        if (uCornersOriented(cube) != 4) {
            throw new IllegalStateException("CO failed");
        }
        return moves;
    }

    private static List<String> solvePllCorners(Cube cube) {
        if (uCornersCorrect(cube)) {
            return new ArrayList<>();
        }
        List<String> got = tryAlgs(cube, PLL_CORNER_ALGS, CfopSolver::uCornersCorrect);
        if (got == null) {
            throw new IllegalStateException("PLL corners failed");
        }
        cube.applySeq(got);
        return got;
    }

    private static List<String> solvePllEdges(Cube cube) {
        if (uEdgesCorrect(cube)) {
            return new ArrayList<>();
        }
        List<String> got = tryAlgs(cube, PLL_EDGE_ALGS,
                c -> uEdgesCorrect(c) && uCornersCorrect(c));
        if (got == null) {
            throw new IllegalStateException("PLL edges failed");
        }
        cube.applySeq(got);
        return got;
    }

    // ------------------------------------------------------------ full solve

    /**
     * Solve with CFOP 2-look. Returns the stages in order.
     * The input cube is not modified.
     */
    public static List<Stage> solve(Cube cube) {
        Cube c = cube.copy();
        List<Stage> stages = new ArrayList<>();

        List<String> mv = crossSolution(c);
        c.applySeq(mv);
        if (!mv.isEmpty()) {
            stages.add(new Stage("cross", "Chữ thập trắng", mv));
        }
        if (!crossSolved(c)) {
            throw new IllegalStateException("cross did not solve");
        }

        // F2L theo cách người chơi: mỗi lần chọn cặp dễ nhất còn lại, (nếu cần) hất viên
        // kẹt lên, xoay U cho khớp 1 trong 41 trường hợp, rồi áp công thức chuẩn của
        // trường hợp đó. Không dùng tìm kiếm của máy. Xem F2lHuman / F2lCases.
        for (int done = 0; done < 4; done++) {
            F2lHuman.Plan plan = F2lHuman.easiest(c);
            if (plan == null) {
                break;
            }
            int pairNo = done + 1;
            String head = "Cặp F2L " + pairNo + "/4: " + F2lHuman.PAIR_LABELS[plan.pair];
            int stepNo = 0;
            for (F2lHuman.Step st : plan.steps) {
                stepNo++;
                String what;
                if (st.kind.equals("extract")) {
                    what = "Hất viên kẹt lên tầng trên";
                } else if (st.kind.equals("align")) {
                    what = "Xoay U về hình Case " + plan.fcase.number;
                } else {
                    what = "Công thức Case " + plan.fcase.number + "/41";
                }
                stages.add(new Stage("f2l", head + " — Bước " + stepNo + ": " + what,
                        st.moves, plan.pair, st.note));
            }
            c.applySeq(plan.all);
        }
        if (!f2lSolved(c)) {
            throw new IllegalStateException("F2L did not solve");
        }

        mv = solveEo(c);
        if (!mv.isEmpty()) {
            stages.add(new Stage("oll_eo", "OLL: định hướng cạnh", mv));
        }
        mv = solveCo(c);
        if (!mv.isEmpty()) {
            stages.add(new Stage("oll_co", "OLL: định hướng góc", mv));
        }
        mv = solvePllCorners(c);
        if (!mv.isEmpty()) {
            stages.add(new Stage("pll_c", "PLL: hoán vị góc", mv));
        }
        mv = solvePllEdges(c);
        if (!mv.isEmpty()) {
            stages.add(new Stage("pll_e", "PLL: hoán vị cạnh", mv));
        }

        if (!c.isSolved()) {
            throw new IllegalStateException("cube not solved at the end");
        }
        return stages;
    }
}
