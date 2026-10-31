import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import vn.cfop.rubik.solver.CfopSolver;
import vn.cfop.rubik.solver.Cube;

/** Standalone test for the Java solver. Compile against the solver sources. */
public class TestSolver {

    static void check(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }

    static void testModel() {
        for (String m : Cube.MOVE_NAMES) {
            Cube c = new Cube();
            for (int i = 0; i < 4; i++) {
                c.apply(m);
            }
            check(c.isSolved(), "move " + m + " x4 not identity");
        }
        Cube c = new Cube();
        List<String> sexy = Cube.parseMoves("R U R' U'");
        for (int i = 0; i < 6; i++) {
            c.applySeq(sexy);
        }
        check(c.isSolved(), "sexy move order != 6");
        Random rng = new Random(1);
        for (int t = 0; t < 50; t++) {
            Cube c2 = new Cube();
            List<String> mv = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                mv.add(Cube.MOVE_NAMES[rng.nextInt(Cube.MOVE_NAMES.length)]);
            }
            c2.applySeq(mv);
            check(!c2.isSolved(), "scramble solved?");
            c2.applySeq(Cube.invertMoves(mv));
            check(c2.isSolved(), "inverse failed");
        }
        System.out.println("model OK");
    }

    static void testCross() {
        Random rng = new Random(7);
        int worst = 0;
        for (int t = 0; t < 30; t++) {
            Cube c = new Cube();
            List<String> scr = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                scr.add(Cube.MOVE_NAMES[rng.nextInt(Cube.MOVE_NAMES.length)]);
            }
            c.applySeq(scr);
            List<String> mv = CfopSolver.crossSolution(c);
            check(mv.size() <= 8, "cross too long: " + mv.size());
            worst = Math.max(worst, mv.size());
            c.applySeq(mv);
            check(CfopSolver.crossSolved(c), "cross not solved");
        }
        System.out.println("cross OK (worst " + worst + " moves)");
    }

    static List<String> randomScramble(Random rng, int n) {
        String[] faces = {"U", "D", "R", "L", "F", "B"};
        List<String> out = new ArrayList<>();
        String prev = null;
        String[] suf = {"", "'", "2"};
        for (int i = 0; i < n; i++) {
            String f;
            do {
                f = faces[rng.nextInt(faces.length)];
            } while (f.equals(prev));
            prev = f;
            out.add(f + suf[rng.nextInt(3)]);
        }
        return out;
    }

    static void testFullSolve(int n, long seed) {
        Random rng = new Random(seed);
        int total = 0;
        int worst = 0;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            List<String> scr = randomScramble(rng, 22);
            Cube c = new Cube();
            c.applySeq(scr);
            List<CfopSolver.Stage> stages = CfopSolver.solve(c);
            check(!stages.isEmpty(), "no stages");
            Cube c2 = new Cube();
            c2.applySeq(scr);
            for (CfopSolver.Stage st : stages) {
                c2.applySeq(st.moves);
            }
            check(c2.isSolved(), "not solved for scramble " + scr);
            int moves = 0;
            for (CfopSolver.Stage st : stages) {
                moves += st.moves.size();
            }
            total += moves;
            worst = Math.max(worst, moves);
            if (i == 0) {
                System.out.println("example scramble: " + String.join(" ", scr));
                for (CfopSolver.Stage st : stages) {
                    System.out.println("  " + st.title + ": " + String.join(" ", st.moves));
                }
            }
        }
        long dt = System.currentTimeMillis() - t0;
        System.out.printf("full solve OK: n=%d, avg %.1f moves, worst %d, %dms total (%.1fms/solve)%n",
                n, (double) total / n, worst, dt, (double) dt / n);
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        testModel();
        testCross();
        testFullSolve(n, 42);
        System.out.println("ALL JAVA TESTS PASSED");
    }
}
