import java.util.*;
import vn.cfop.rubik.solver.*;

/**
 * Kiểm tra F2L theo cách người chơi (bảng 41 case trong F2lCases + F2lHuman).
 * Chạy:  javac -encoding UTF-8 -d out $(find ../android -name '*.java' -path '*solver*') F2lTest.java
 *        java -cp out F2lTest
 */
public class F2lTest {
    static int failures = 0;

    static void check(boolean ok, String msg) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    public static void main(String[] args) {
        // ---- 1. Bảng 41 case
        check(F2lCases.ALL.size() == 41, "phải có đúng 41 case");
        Map<Integer, Integer> keys = F2lCases.canonicalKeyTable();
        check(new HashSet<>(keys.keySet()).size() == 41, "41 case phải có 41 khoá nhận diện khác nhau");
        int[] groups = new int[5];
        for (F2lCases.F2lCase fc : F2lCases.ALL) {
            groups[fc.group]++;
            Cube p = new Cube(fc.pattern);
            check(CfopSolver.crossSolved(p), "case " + fc.number + ": thế mẫu phải còn nguyên chữ thập");
            // công thức (có y) đưa thế mẫu về đã giải (chấp nhận khối bị xoay y)
            Cube r = p.copy();
            r.applySeq(fc.alg);
            boolean solved = false;
            for (int k = 0; k < 4 && !solved; k++) {
                Cube t = r.copy();
                for (int i = 0; i < k; i++) {
                    t.apply("y");
                }
                solved = t.isSolved();
            }
            check(solved, "case " + fc.number + ": công thức phải giải được thế mẫu");
            for (String m : fc.alg) {
                check(!m.startsWith("d") && !m.startsWith("B") && !m.startsWith("D"),
                        "case " + fc.number + ": không dùng d/B/D (animation không hỗ trợ d)");
            }
            F2lCases.Match m = F2lCases.recognize(new Cube(fc.pattern));
            check(m != null && m.fcase.number == fc.number && m.uTurns == 0,
                    "case " + fc.number + ": nhận diện lại chính thế mẫu của nó");
            // xoay U bất kỳ vẫn ra đúng case
            for (int u = 1; u < 4; u++) {
                Cube t = new Cube(fc.pattern);
                for (int i = 0; i < u; i++) {
                    t.apply("U");
                }
                F2lCases.Match mm = F2lCases.recognize(t);
                check(mm != null && mm.fcase.number == fc.number, "case " + fc.number + ": U^" + u);
            }
        }
        check(groups[1] == 24 && groups[2] == 6 && groups[3] == 6 && groups[4] == 5,
                "nhóm phải là 24/6/6/5, đang là " + Arrays.toString(groups));

        // ---- 2. Giải ngẫu nhiên
        Random rng = new Random(2026);
        String[] names = {"U", "U'", "U2", "R", "R'", "R2", "L", "L'", "L2", "F", "F'", "F2",
                "B", "B'", "B2", "D", "D'", "D2"};
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        Set<Integer> hit = new HashSet<>();
        for (int t = 0; t < n; t++) {
            Cube c = new Cube();
            List<String> mv = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                mv.add(names[rng.nextInt(names.length)]);
            }
            c.applySeq(mv);
            List<CfopSolver.Stage> stages = CfopSolver.solve(c);
            for (CfopSolver.Stage s : stages) {
                if (!s.key.equals("f2l")) {
                    continue;
                }
                check(s.note != null && !s.note.isEmpty(), "bước F2L phải có lời giải thích");
                check(!s.title.contains("Đặc biệt"), "không còn kiểu 'đặc biệt' do máy tìm");
                for (String m : s.moves) {
                    char b = m.charAt(0);
                    check(b == 'U' || b == 'R' || b == 'L' || b == 'F' || b == 'y',
                            "F2L chỉ dùng U R L F và y, gặp " + m);
                }
                int i = s.title.indexOf("Công thức Case ");
                if (i >= 0) {
                    hit.add(Integer.parseInt(s.title.substring(i + 15, s.title.indexOf("/41"))));
                }
            }
        }
        System.out.println("giải " + n + " khối, số case gặp: " + hit.size() + "/41");
        if (n >= 300) {
            check(hit.size() == 41, "300+ khối phải gặp đủ 41 case");
        }

        System.out.println(failures == 0 ? "F2L TESTS PASSED" : failures + " FAILURES");
        if (failures != 0) {
            System.exit(1);
        }
    }
}
