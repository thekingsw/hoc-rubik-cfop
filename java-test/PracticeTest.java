package vn.cfop.rubik.solver;

import java.util.List;

/**
 * Thẻ luyện F2L: 41 case x 4 khe x 4 độ xoay U = 656 thẻ, thẻ nào cũng phải
 * nhận đúng case, và làm theo align + formula thì khối giải xong hoàn toàn.
 */
public class PracticeTest {
    public static void main(String[] args) {
        int bad = 0;
        int n = 0;
        for (int num = 1; num <= 41; num++) {
            for (int slot = 0; slot < 4; slot++) {
                for (int u = 0; u < 4; u++) {
                    n++;
                    F2lPractice.Drill d = F2lPractice.make(num, slot, u);
                    if (d == null) {
                        bad++;
                        System.out.println("FAIL null: case " + num + " slot " + slot + " u " + u);
                        continue;
                    }
                    // Thế dựng lại từ setup phải trùng state.
                    Cube c = new Cube();
                    c.applySeq(d.setup);
                    if (!java.util.Arrays.equals(c.s, d.state)) {
                        bad++;
                        System.out.println("FAIL state: case " + num + " slot " + slot);
                    }
                    if (c.isSolved()) {
                        bad++;
                        System.out.println("FAIL thế đã giải sẵn: case " + num);
                    }
                    // Chỉ chữ thập + 3 khe còn lại nguyên: cặp khác phải đã xong.
                    for (int p = 0; p < 4; p++) {
                        if (p != slot && !CfopSolver.pairSolved(c, p)) {
                            bad++;
                            System.out.println("FAIL cặp " + p + " bị động: case " + num + " slot " + slot);
                        }
                    }
                    // Làm theo lời giải thì xong hẳn khối.
                    c.applySeq(d.align);
                    c.applySeq(d.formula);
                    if (!c.isSolved()) {
                        bad++;
                        System.out.println("FAIL không giải xong: case " + num + " slot " + slot + " u " + u);
                    }
                    for (String m : d.setup) {
                        char b = m.charAt(0);
                        if ("URFLBD".indexOf(b) < 0) {
                            bad++;
                            System.out.println("FAIL nước lạ trong setup: " + m);
                        }
                    }
                }
            }
        }
        // Khe khác trước-phải thì công thức phải có xoay cả khối y.
        F2lPractice.Drill d1 = F2lPractice.make(2, 1, 0);
        if (d1 == null || !d1.formula.get(0).startsWith("y")) {
            bad++;
            System.out.println("FAIL: khe trước-trái phải bắt đầu bằng y");
        }
        System.out.println(bad == 0 ? "PRACTICE TESTS PASSED (" + n + " thẻ)" : bad + " FAILURES");
        if (bad != 0) {
            System.exit(1);
        }
    }
}
