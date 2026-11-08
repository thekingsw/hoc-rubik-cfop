package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Tạo "thẻ luyện" F2L cho người học: một trường hợp trong 41 case, đặt ở một trong
 * 4 khe và với độ xoay U ngẫu nhiên — để người học không học vẹt theo một hướng.
 *
 * Mọi thứ đều được kiểm lại bằng chính bộ nhận diện của bộ giải (F2lHuman.plan):
 * thế dựng ra phải được nhận đúng case, không cần hất viên kẹt, và công thức phải
 * giải xong khối. Nếu không khớp thì trả null (test bắt buộc không bao giờ null).
 */
public final class F2lPractice {

    private F2lPractice() {
    }

    /** Một thẻ luyện. */
    public static final class Drill {
        public final int caseNumber;
        /** Khe cần giải: 0=trước-phải 1=trước-trái 2=sau-phải 3=sau-trái. */
        public final int slot;
        public final String slotName;
        /** Số lần U thêm vào sau thế mẫu (0..3). */
        public final int uTurns;
        /** Nước xáo từ khối đã giải (xanh lá ở trước, vàng ở trên) để có đúng thế này. */
        public final List<String> setup;
        /** Xoay U để cặp về đúng hình của case (có thể rỗng). */
        public final List<String> align;
        /** Công thức, gồm cả nước xoay cả khối (y) vào/ra khe nếu khe không ở trước-phải. */
        public final List<String> formula;
        /** 54 facelet sau khi làm xong setup. */
        public final int[] state;

        Drill(int caseNumber, int slot, int uTurns, List<String> setup, List<String> align,
              List<String> formula, int[] state) {
            this.caseNumber = caseNumber;
            this.slot = slot;
            this.slotName = F2lHuman.SLOT_NAMES[slot];
            this.uTurns = uTurns;
            this.setup = setup;
            this.align = align;
            this.formula = formula;
            this.state = state;
        }
    }

    // Đổi tên mặt khi xoay cả dãy nước quanh trục dọc để khe 0 (trước-phải) thành khe slot.
    // Hàng: mặt gốc R F L B; cột: khe 0..3.
    private static char mapFace(char face, int slot) {
        switch (face) {
            case 'R':
                return new char[]{'R', 'F', 'B', 'L'}[slot];
            case 'F':
                return new char[]{'F', 'L', 'R', 'B'}[slot];
            case 'L':
                return new char[]{'L', 'B', 'F', 'R'}[slot];
            case 'B':
                return new char[]{'B', 'R', 'L', 'F'}[slot];
            default:
                return face; // U và D không đổi
        }
    }

    /** Đổi các nước của khung trước-phải sang khung của khe slot. */
    static List<String> translate(List<String> moves, int slot) {
        List<String> out = new ArrayList<>();
        for (String m : moves) {
            out.add(mapFace(m.charAt(0), slot) + m.substring(1));
        }
        return out;
    }

    /** Thế dựng thẻ; null nếu không khớp (không được xảy ra). */
    public static Drill make(int caseNumber, int slot, int uTurns) {
        if (caseNumber < 1 || caseNumber > F2lCases.ALL.size() || slot < 0 || slot > 3
                || uTurns < 0 || uTurns > 3) {
            return null;
        }
        F2lCases.F2lCase fc = F2lCases.byNumber(caseNumber);
        // Nhóm 4 (cả góc và cạnh đã nằm trong khe): xoay U không đổi gì với cặp, bỏ qua.
        if (fc.group == 4) {
            uTurns = 0;
        }
        List<String> setup = new ArrayList<>(translate(fc.setup, slot));
        if (uTurns == 1) {
            setup.add("U");
        } else if (uTurns == 2) {
            setup.add("U2");
        } else if (uTurns == 3) {
            setup.add("U'");
        }
        setup = Cube.simplifyMoves(setup);

        Cube c = new Cube();
        c.applySeq(setup);
        F2lHuman.Plan plan = F2lHuman.plan(c, slot);
        if (plan == null || plan.fcase == null || plan.fcase.number != caseNumber) {
            return null;
        }
        List<String> align = new ArrayList<>();
        List<String> formula = null;
        for (F2lHuman.Step st : plan.steps) {
            if (st.kind.equals("extract")) {
                return null;
            } else if (st.kind.equals("align")) {
                align.addAll(st.moves);
            } else {
                formula = new ArrayList<>(st.moves);
            }
        }
        if (formula == null) {
            return null;
        }
        return new Drill(caseNumber, slot, uTurns, setup, align, formula, c.s.clone());
    }
}
