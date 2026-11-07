package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * F2L theo cách người chơi — KHÔNG tìm kiếm bằng máy.
 *
 * Mỗi cặp được giải theo đúng thứ tự một người chơi sẽ làm:
 *   1. (nếu cần) Hất viên đang kẹt ở khe khác lên tầng trên bằng R U R'.
 *   2. (nếu cần) Xoay U cho cặp khớp hình của một trong 41 trường hợp.
 *   3. Áp công thức chuẩn của trường hợp đó (bảng trong {@link F2lCases}).
 * Khe không phải trước-phải thì xoay cả khối (y) để khe ra trước-phải, làm
 * xong xoay lại — nên mọi công thức đều quen thuộc (R U F), không có nước B.
 * Cặp được chọn là cặp dễ nhất còn lại (ít nước nhất).
 */
final class F2lHuman {

    private F2lHuman() {
    }

    static final String[] PAIR_LABELS = {
            "xanh lá - cam", "xanh lá - đỏ", "xanh dương - cam", "xanh dương - đỏ"};
    /** Tên khe nhà của từng cặp (cùng thứ tự với PAIR_LABELS). */
    static final String[] SLOT_NAMES = {"trước-phải", "trước-trái", "sau-phải", "sau-trái"};
    /** Xoay cả khối để khe đó ra trước-phải. */
    private static final String[][] SLOT_ROT = {{}, {"y'"}, {"y"}, {"y2"}};

    private static final int[][] CORNER_COLOURS = {{0, 2, 4}, {0, 2, 1}, {0, 5, 4}, {0, 5, 1}};
    private static final int[][] EDGE_COLOURS = {{2, 4}, {2, 1}, {5, 4}, {5, 1}};
    private static final int[] CENTRES = {4, 13, 22, 31, 40, 49};

    /** Một bước hướng dẫn. */
    static final class Step {
        final String kind;   // "extract" | "align" | "formula"
        final List<String> moves;
        final String note;
        Step(String kind, List<String> moves, String note) {
            this.kind = kind;
            this.moves = moves;
            this.note = note;
        }
    }

    /** Kế hoạch giải một cặp. */
    static final class Plan {
        final int pair;
        final List<Step> steps = new ArrayList<>();
        F2lCases.F2lCase fcase;
        int cost;
        /** Chuỗi nước đầy đủ (để áp lên khối). */
        final List<String> all = new ArrayList<>();
        Plan(int pair) {
            this.pair = pair;
        }
    }

    // ------------------------------------------------------------ tiện ích xoay
    private static Cube rotated(Cube c, String[] rot) {
        Cube r = c.copy();
        for (String m : rot) {
            r.apply(m);
        }
        return r;
    }

    /** Nước y/y2/y' đưa khối về hướng chuẩn (tâm đúng chỗ); null nếu không có. */
    private static String fixRotation(Cube c) {
        String[] names = {"", "y", "y2", "y'"};
        for (int k = 0; k < 4; k++) {
            Cube t = c.copy();
            for (int i = 0; i < k; i++) {
                t.apply("y");
            }
            boolean ok = true;
            for (int f = 0; f < 6; f++) {
                if (t.s[CENTRES[f]] != Cube.SCHEME[f]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return names[k];
            }
        }
        return null;
    }

    private static int yTurns(String m) {
        return m.endsWith("'") ? 3 : m.endsWith("2") ? 2 : 1;
    }

    /** Gộp các nước xoay y liền nhau (y' y' -> y2, y y' -> bỏ). */
    static List<String> mergeRotations(List<String> in) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < in.size()) {
            String m = in.get(i);
            if (m.charAt(0) == 'y') {
                int total = 0;
                while (i < in.size() && in.get(i).charAt(0) == 'y') {
                    total += yTurns(in.get(i));
                    i++;
                }
                total %= 4;
                if (total == 1) {
                    out.add("y");
                } else if (total == 2) {
                    out.add("y2");
                } else if (total == 3) {
                    out.add("y'");
                }
            } else {
                out.add(m);
                i++;
            }
        }
        return out;
    }

    /** Nước thật (không tính xoay cả khối). */
    private static int faceMoves(List<String> moves) {
        int n = 0;
        for (String m : moves) {
            if (m.charAt(0) != 'y') {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------ viên kẹt
    /** Khe (0=FR,1=FL,2=BR,3=BL) của góc nếu nó nằm ở tầng dưới; -1 nếu ở tầng trên. */
    private static int cornerBottomSlot(Cube c, int pair) {
        int[] cc = CORNER_COLOURS[pair];
        int[][] slots = {{29, 26, 15}, {27, 44, 24}, {35, 17, 51}, {33, 53, 42}}; // DFR DLF DRB DBL
        // thứ tự khớp với 0=FR,1=FL,2=BR,3=BL
        for (int s = 0; s < 4; s++) {
            if (hasColours(c, slots[s], cc)) {
                return s;
            }
        }
        return -1;
    }

    /** Khe của cạnh nếu nó nằm ở khe giữa; -1 nếu ở tầng trên. */
    private static int edgeMiddleSlot(Cube c, int pair) {
        int[] ec = EDGE_COLOURS[pair];
        int[][] slots = {{23, 12}, {21, 41}, {48, 14}, {50, 39}}; // FR FL BR BL
        for (int s = 0; s < 4; s++) {
            if (hasColours(c, slots[s], ec)) {
                return s;
            }
        }
        return -1;
    }

    private static boolean hasColours(Cube c, int[] slot, int[] colours) {
        for (int col : colours) {
            boolean found = false;
            for (int f : slot) {
                if (c.s[f] == col) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /** Khe cần hất ra (viên của cặp đang kẹt ở khe khác khe nhà), -1 nếu không có. */
    private static int stuckSlot(Cube c, int pair) {
        int cs = cornerBottomSlot(c, pair);
        if (cs >= 0 && cs != pair) {
            return cs;
        }
        int es = edgeMiddleSlot(c, pair);
        if (es >= 0 && es != pair) {
            return es;
        }
        return -1;
    }

    // ------------------------------------------------------------ lập kế hoạch
    /** Kế hoạch giải cặp pair từ khối c (c không bị đổi). null nếu cặp đã xong hoặc không lập được. */
    static Plan plan(Cube c, int pair) {
        if (CfopSolver.pairSolved(c, pair)) {
            return null;
        }
        Cube w = c.copy();
        Plan plan = new Plan(pair);
        List<String> extract = new ArrayList<>();
        List<String> extractSlots = new ArrayList<>();
        F2lCases.Match m = null;
        for (int iter = 0; iter < 6; iter++) {
            m = F2lCases.recognize(rotated(w, SLOT_ROT[pair]));
            if (m != null) {
                break;
            }
            int s = stuckSlot(w, pair);
            if (s < 0) {
                return null;
            }
            List<String> ex = new ArrayList<>();
            for (String r : SLOT_ROT[s]) {
                ex.add(r);
            }
            ex.add("R");
            ex.add("U");
            ex.add("R'");
            for (String r : SLOT_ROT[s]) {
                ex.add(r.equals("y2") ? "y2" : r.equals("y") ? "y'" : "y");
            }
            ex = mergeRotations(ex);
            w.applySeq(ex);
            String fr = fixRotation(w);
            if (fr == null || !fr.isEmpty()) {
                return null;
            }
            extract.addAll(ex);
            extractSlots.add(SLOT_NAMES[s]);
        }
        if (m == null) {
            return null;
        }
        String pairName = PAIR_LABELS[pair];
        if (!extract.isEmpty()) {
            plan.steps.add(new Step("extract", extract,
                    "Viên của cặp " + pairName + " đang kẹt ở khe " + join(extractSlots)
                            + " (không phải khe nhà " + SLOT_NAMES[pair] + "). Hất nó lên tầng trên bằng R U R' "
                            + "ở khe đó: xoay cả khối (y) cho khe ra trước-phải, làm R U R', rồi xoay lại như cũ. "
                            + "Sau bước này cả hai viên đã ở tầng trên và cặp thuộc 1 trong 41 trường hợp."));
            plan.all.addAll(extract);
        }
        F2lCases.F2lCase fc = m.fcase;
        plan.fcase = fc;
        List<String> align = m.alignMoves();
        if (!align.isEmpty()) {
            plan.steps.add(new Step("align", align,
                    "Xoay tầng trên (U) cho góc và cạnh nằm đúng như hình mẫu của Case " + fc.number
                            + "/41. " + fc.recognition));
            plan.all.addAll(align);
        }
        List<String> formula = new ArrayList<>();
        for (String r : SLOT_ROT[pair]) {
            formula.add(r);
        }
        formula.addAll(fc.alg);
        // áp thử để biết khối xoay lệch bao nhiêu sau công thức rồi xoay lại cho thẳng.
        Cube after = w.copy();
        after.applySeq(align);
        after.applySeq(formula);
        String fix = fixRotation(after);
        if (fix == null) {
            return null;
        }
        if (!fix.isEmpty()) {
            formula.add(fix);
        }
        formula = mergeRotations(formula);
        after = w.copy();
        after.applySeq(align);
        after.applySeq(formula);
        if (!CfopSolver.pairSolved(after, pair) || !CfopSolver.crossSolved(after) || fixRotation(after) == null
                || !fixRotation(after).isEmpty()) {
            return null;
        }
        StringBuilder note = new StringBuilder();
        note.append("Case ").append(fc.number).append("/41 — ").append(F2lCases.GROUP_NAMES[fc.group]).append(". ");
        note.append(fc.recognition).append(" ");
        if (pair != 0) {
            note.append("Khe ").append(SLOT_NAMES[pair])
                    .append(": xoay cả khối (y) để khe ra trước-phải, làm công thức, rồi xoay lại như cũ. ");
        }
        note.append("Công thức chuẩn: ").append(join(fc.alg, " ")).append(".");
        plan.steps.add(new Step("formula", formula, note.toString()));
        plan.all.addAll(formula);
        plan.cost = faceMoves(extract) * 3 + (align.isEmpty() ? 0 : 1) + faceMoves(formula);
        return plan;
    }

    private static String join(List<String> l) {
        return join(l, " và ");
    }

    private static String join(List<String> l, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(l.get(i));
        }
        return sb.toString();
    }

    /** Cặp dễ nhất còn lại (ít nước nhất). null nếu cả 4 cặp đã xong. */
    static Plan easiest(Cube c) {
        Plan best = null;
        for (int p = 0; p < 4; p++) {
            if (CfopSolver.pairSolved(c, p)) {
                continue;
            }
            Plan pl = plan(c, p);
            if (pl == null) {
                throw new IllegalStateException("F2L: không lập được kế hoạch cho cặp " + p);
            }
            if (best == null || pl.cost < best.cost) {
                best = pl;
            }
        }
        return best;
    }
}
