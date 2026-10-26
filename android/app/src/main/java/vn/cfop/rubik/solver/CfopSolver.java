package vn.cfop.rubik.solver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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

        public Stage(String key, String title, List<String> moves) {
            this(key, title, moves, -1);
        }

        public Stage(String key, String title, List<String> moves, int pairIndex) {
            this.key = key;
            this.title = title;
            this.moves = moves;
            this.pairIndex = pairIndex;
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

    // (corner colours, corner target facelets, edge colours, edge target facelets, label)
    // Corner colours are (white, F/B colour, R/L colour); R=orange(4), L=red(1).
    private static final int[][] F2L_CORNER_COLOURS = {
            {0, 2, 4}, {0, 2, 1}, {0, 5, 4}, {0, 5, 1}};
    private static final int[][] F2L_CORNER_TARGETS = {
            {29, 26, 15}, {27, 24, 44}, {35, 51, 17}, {33, 53, 42}};
    private static final int[][] F2L_EDGE_COLOURS = {
            {2, 4}, {2, 1}, {5, 4}, {5, 1}};
    private static final int[][] F2L_EDGE_TARGETS = {
            {23, 12}, {21, 41}, {48, 14}, {50, 39}};
    private static final String[] F2L_LABELS = {
            "xanh lá - cam", "xanh lá - đỏ", "xanh dương - cam", "xanh dương - đỏ"};
    private static final String[] F2L_MOVES = {
            "U", "U'", "U2", "R", "R'", "R2", "L", "L'", "L2",
            "F", "F'", "F2", "B", "B'", "B2"};

    /** Facelets in the top layer: U face + top rows of the side faces. */
    private static boolean isTopLayerFacelet(int f) {
        if (f < 9) {
            return true;
        }
        return f == 9 || f == 10 || f == 11
                || f == 18 || f == 19 || f == 20
                || f == 36 || f == 37 || f == 38
                || f == 45 || f == 46 || f == 47;
    }

    // ------------------------------------------------------------ pattern databases (PDB) for F2L
    // A pair's state is fully described by the facelets holding its two
    // primary stickers (corner: yellow; edge: F/B colour). BFS over that
    // pair of facelets, from the solved pair, gives the EXACT minimum
    // moves to solve the pair ignoring everything else -- a much stronger
    // heuristic than single-piece distances. Same idea for the cross
    // (4 white stickers).

    private static final Map<String, int[][]> PDB_PAIR_CACHE = new HashMap<>();
    private static Map<Integer, Integer> PDB_CROSS = null;

    private static int[][] pdbPair(int cornerTarget, int edgeTarget) {
        String key = cornerTarget + "," + edgeTarget;
        int[][] cached = PDB_PAIR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int[][] dist = new int[54][54];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        dist[cornerTarget][edgeTarget] = 0;
        q.add(new int[]{cornerTarget, edgeTarget});
        while (!q.isEmpty()) {
            int[] st = q.poll();
            int d = dist[st[0]][st[1]];
            for (String m : F2L_MOVES) {
                int[] inv = Cube.INV.get(m);
                int nfc = inv[st[0]];
                int nfe = inv[st[1]];
                if (dist[nfc][nfe] < 0) {
                    dist[nfc][nfe] = d + 1;
                    q.add(new int[]{nfc, nfe});
                }
            }
        }
        PDB_PAIR_CACHE.put(key, dist);
        return dist;
    }

    private static int encodeCross(int w0, int w1, int w2, int w3) {
        return w0 | (w1 << 6) | (w2 << 12) | (w3 << 18);
    }

    private static Map<Integer, Integer> pdbCross() {
        if (PDB_CROSS != null) {
            return PDB_CROSS;
        }
        Map<Integer, Integer> dist = new HashMap<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        int goal = encodeCross(CROSS_GOAL[0], CROSS_GOAL[1], CROSS_GOAL[2], CROSS_GOAL[3]);
        dist.put(goal, 0);
        q.add(CROSS_GOAL.clone());
        while (!q.isEmpty()) {
            int[] st = q.poll();
            int d = dist.get(encodeCross(st[0], st[1], st[2], st[3]));
            for (String m : F2L_MOVES) {
                int[] inv = Cube.INV.get(m);
                int[] nxt = new int[]{inv[st[0]], inv[st[1]], inv[st[2]], inv[st[3]]};
                int nk = encodeCross(nxt[0], nxt[1], nxt[2], nxt[3]);
                if (!dist.containsKey(nk)) {
                    dist.put(nk, d + 1);
                    q.add(nxt);
                }
            }
        }
        PDB_CROSS = dist;
        return dist;
    }

    private static int piecePrimaryFacelet(Cube cube, int[] colours) {
        int want0 = colours[0];
        int[][] slots = new int[EDGE_SLOTS.length + CORNER_SLOTS.length][];
        System.arraycopy(EDGE_SLOTS, 0, slots, 0, EDGE_SLOTS.length);
        System.arraycopy(CORNER_SLOTS, 0, slots, EDGE_SLOTS.length, CORNER_SLOTS.length);
        for (int[] slot : slots) {
            boolean match = true;
            for (int c : colours) {
                boolean has = false;
                for (int i : slot) {
                    if (cube.s[i] == c) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    match = false;
                    break;
                }
            }
            if (!match || slot.length != colours.length) {
                continue;
            }
            // exact colour set match
            boolean exact = true;
            for (int i : slot) {
                boolean inColours = false;
                for (int c : colours) {
                    if (cube.s[i] == c) {
                        inColours = true;
                        break;
                    }
                }
                if (!inColours) {
                    exact = false;
                    break;
                }
            }
            if (exact) {
                for (int i : slot) {
                    if (cube.s[i] == want0) {
                        return i;
                    }
                }
            }
        }
        throw new IllegalStateException("piece not found: " + Arrays.toString(colours));
    }

    private static boolean pairSolved(Cube cube, int pairIdx) {
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

    /** IDA* search for one F2L pair, preserving the cross and solved pairs. */
    private static final class F2lSearch {
        private final Cube start;
        private final int pairIdx;
        private final List<Integer> solvedPairs;
        // tracked primary facelets, in order:
        // [pair corner, pair edge, white G, white R, white B, white O,
        //  solved pair 0 corner, solved pair 0 edge, ...]
        private int[] pos;
        // subproblems: (kind, pdb, pos indices). kind 0 = pair pdb (int[][]),
        // kind 1 = cross pdb (Map<Integer,Integer>).
        private final List<int[]> subKinds = new ArrayList<>();
        private final List<Object> subPdbs = new ArrayList<>();
        private final List<int[]> subIdxs = new ArrayList<>();

        F2lSearch(Cube cube, int pairIdx, List<Integer> solvedPairs) {
            this.start = cube.copy();
            this.pairIdx = pairIdx;
            this.solvedPairs = solvedPairs;
            List<int[]> colours = new ArrayList<>();
            colours.add(F2L_CORNER_COLOURS[pairIdx]);
            colours.add(F2L_EDGE_COLOURS[pairIdx]);
            for (int[] we : WHITE_EDGES) {
                colours.add(new int[]{0, we[0]});
            }
            for (int pi : solvedPairs) {
                colours.add(F2L_CORNER_COLOURS[pi]);
                colours.add(F2L_EDGE_COLOURS[pi]);
            }
            pos = new int[colours.size()];
            for (int i = 0; i < colours.size(); i++) {
                pos[i] = piecePrimaryFacelet(start, colours.get(i));
            }
            subKinds.add(new int[]{0});
            subPdbs.add(pdbPair(F2L_CORNER_TARGETS[pairIdx][0],
                    F2L_EDGE_TARGETS[pairIdx][0]));
            subIdxs.add(new int[]{0, 1});
            subKinds.add(new int[]{1});
            subPdbs.add(pdbCross());
            subIdxs.add(new int[]{2, 3, 4, 5});
            for (int k = 0; k < solvedPairs.size(); k++) {
                int pi = solvedPairs.get(k);
                subKinds.add(new int[]{0});
                subPdbs.add(pdbPair(F2L_CORNER_TARGETS[pi][0],
                        F2L_EDGE_TARGETS[pi][0]));
                int base = 6 + 2 * k;
                subIdxs.add(new int[]{base, base + 1});
            }
        }

        private int heuristicFor(int[] p) {
            int h = 0;
            for (int s = 0; s < subPdbs.size(); s++) {
                int d;
                if (subKinds.get(s)[0] == 0) {
                    int[][] pdb = (int[][]) subPdbs.get(s);
                    int[] idx = subIdxs.get(s);
                    d = pdb[p[idx[0]]][p[idx[1]]];
                    if (d < 0) {
                        d = 99;
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    Map<Integer, Integer> pdb = (Map<Integer, Integer>) subPdbs.get(s);
                    int[] idx = subIdxs.get(s);
                    Integer dd = pdb.get(encodeCross(p[idx[0]], p[idx[1]],
                            p[idx[2]], p[idx[3]]));
                    d = dd == null ? 99 : dd;
                }
                if (d > h) {
                    h = d;
                }
            }
            return h;
        }

        private int heuristic() {
            return heuristicFor(pos);
        }

        private boolean goal(Cube cube) {
            if (!crossSolved(cube)) {
                return false;
            }
            for (int pi : solvedPairs) {
                if (!pairSolved(cube, pi)) {
                    return false;
                }
            }
            return pairSolved(cube, pairIdx);
        }

        private static String posKey(int[] p) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < p.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(p[i]);
            }
            return sb.toString();
        }

        /**
         * A* over the tracked-piece positions (pos), with a closed set.
         * The goal is fully determined by pos, and moves act on pos
         * deterministically, so each pos-state expands once -- unlike
         * IDA*, which re-explores the tree at every bound.
         */
        List<String> search(int maxDepth) {
            int[] startPos = pos.clone();
            int h0 = heuristicFor(startPos);
            if (h0 == 0) {
                if (goal(start)) {
                    return new ArrayList<>();
                }
                throw new IllegalStateException("F2L search failed for pair " + pairIdx);
            }
            // Priority queue ordered by (f, -h, g): smallest f first,
            // deeper h preferred on ties.
            java.util.PriorityQueue<long[]> open = new java.util.PriorityQueue<>(
                    (a, b) -> {
                        if (a[0] != b[0]) return Long.compare(a[0], b[0]);
                        if (a[1] != b[1]) return Long.compare(a[1], b[1]);
                        return Long.compare(a[2], b[2]);
                    });
            // Each entry: {f, -h, g, stateId}. States stored separately.
            Map<String, int[]> states = new HashMap<>();
            Map<String, Integer> bestG = new HashMap<>();
            Map<String, String> parent = new HashMap<>();
            Map<String, String> parentMove = new HashMap<>();
            String startKey = posKey(startPos);
            states.put(startKey, startPos);
            bestG.put(startKey, 0);
            parent.put(startKey, null);
            open.add(new long[]{h0, -h0, 0, 0});
            // stateId -> key mapping via a list (queue entries carry an index)
            List<String> keys = new ArrayList<>();
            keys.add(startKey);
            Map<String, Integer> keyToId = new HashMap<>();
            keyToId.put(startKey, 0);
            while (!open.isEmpty()) {
                long[] node = open.poll();
                int g = (int) node[2];
                String key = keys.get((int) node[3]);
                Integer bg = bestG.get(key);
                if (bg == null || bg < g) {
                    continue;
                }
                int h = (int) (-node[1]);
                int[] state = states.get(key);
                if (h == 0) {
                    List<String> path = new ArrayList<>();
                    String cur = key;
                    while (parent.get(cur) != null) {
                        path.add(0, parentMove.get(cur));
                        cur = parent.get(cur);
                    }
                    List<String> moves = Cube.simplifyMoves(path);
                    Cube check = start.copy();
                    check.applySeq(moves);
                    if (!goal(check)) {
                        throw new IllegalStateException("F2L A* solution failed goal check");
                    }
                    return moves;
                }
                for (String m : F2L_MOVES) {
                    int[] inv = Cube.INV.get(m);
                    int[] ns = new int[state.length];
                    for (int i = 0; i < state.length; i++) {
                        ns[i] = inv[state[i]];
                    }
                    int ng = g + 1;
                    String nk = posKey(ns);
                    Integer oldG = bestG.get(nk);
                    if (oldG != null && oldG <= ng) {
                        continue;
                    }
                    int nh = heuristicFor(ns);
                    if (ng + nh > maxDepth) {
                        continue;
                    }
                    bestG.put(nk, ng);
                    states.put(nk, ns);
                    parent.put(nk, key);
                    parentMove.put(nk, m);
                    int id;
                    Integer existing = keyToId.get(nk);
                    if (existing == null) {
                        id = keys.size();
                        keys.add(nk);
                        keyToId.put(nk, id);
                    } else {
                        id = existing;
                    }
                    open.add(new long[]{ng + nh, -nh, ng, id});
                }
            }
            throw new IllegalStateException("F2L search failed for pair " + pairIdx);
        }
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

        List<Integer> solvedPairs = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        // Greedy pair order: at each step solve the pair whose pieces are
        // most in the top layer (easiest to insert while few pairs are locked).
        while (!remaining.isEmpty()) {
            int bestPair = -1;
            F2lSearch bestSearch = null;
            int bestTop = -1;
            int bestH = Integer.MAX_VALUE;
            for (int i : remaining) {
                int fc = piecePrimaryFacelet(c, F2L_CORNER_COLOURS[i]);
                int fe = piecePrimaryFacelet(c, F2L_EDGE_COLOURS[i]);
                int topCount = 0;
                if (isTopLayerFacelet(fc)) {
                    topCount++;
                }
                if (isTopLayerFacelet(fe)) {
                    topCount++;
                }
                F2lSearch search = new F2lSearch(c, i, solvedPairs);
                int h = search.heuristic();
                if (topCount > bestTop || (topCount == bestTop && h < bestH)) {
                    bestTop = topCount;
                    bestH = h;
                    bestPair = i;
                    bestSearch = search;
                }
            }
            mv = bestSearch.search(14);
            c.applySeq(mv);
            solvedPairs.add(bestPair);
            remaining.remove((Integer) bestPair);
            stages.add(new Stage("f2l", "Cặp F2L " + solvedPairs.size() + "/4: "
                    + F2L_LABELS[bestPair], mv, bestPair));
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
