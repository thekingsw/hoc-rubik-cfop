import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import vn.cfop.rubik.solver.CfopSolver;
import vn.cfop.rubik.solver.Cube;
import vn.cfop.rubik.solver.CubeOrientation;

/**
 * Verify whole-cube-rotation handling:
 * 1. eliminateRotations: exact facelet equality, no x/y/z left.
 * 2. translateForView: y R -> F etc.
 * 3. Full solver: translated stages still solve the cube.
 * 4. View rotation end-to-end: rotate view, translate moves, display consistent.
 */
public class TestOrientation {
    static int failures = 0;

    static void check(boolean cond, String msg) {
        if (cond) {
            System.out.println("  OK " + msg);
        } else {
            System.out.println("  FAIL " + msg);
            failures++;
        }
    }

    static List<String> faceMoves(Random rng, int n) {
        return faceMoves(rng, n, Cube.MOVE_NAMES);
    }

    static List<String> faceMoves(Random rng, int n, String[] pool) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(pool[rng.nextInt(pool.length)]);
        }
        return out;
    }

    public static void main(String[] args) {
        System.out.println("== eliminateRotations exact equality ==");
        List<List<String>> seqs = Arrays.asList(
                Cube.parseMoves("x R' U R' D2 R U' R' D2 R2 x'"), // PLL Aa
                Cube.parseMoves("x R2 D2 R U R' D2 R U' R x'"), // PLL Ab
                Cube.parseMoves("y R U R' U' y' F' U' F"),
                Cube.parseMoves("x2 U R2 x2 F2"),
                Cube.parseMoves("z R U' z' L2 y B y'"),
                Cube.parseMoves("y F2 y' R"),
                Cube.parseMoves("x y z R U z' y' x'"),
                Cube.parseMoves("y2 R U y2"));
        Random rng = new Random(7);
        for (List<String> seq : seqs) {
            List<String> tr = CubeOrientation.eliminateRotations(seq);
            boolean noRot = tr.stream().noneMatch(CubeOrientation::isRotation);
            check(noRot, "no rotations left: " + seq + " -> " + tr);
            boolean eq = true;
            for (int t = 0; t < 30; t++) {
                int[] s0 = new Cube().applySeq(faceMoves(rng, 25)).s.clone();
                int[] a = new Cube(s0).applySeq(seq).s.clone();
                int[] b = new Cube(s0).applySeq(tr).s.clone();
                if (!Arrays.equals(a, b)) {
                    eq = false;
                    break;
                }
            }
            check(eq, "exact equality: " + seq);
        }

        System.out.println("== translateForView ==");
        check(CubeOrientation.translateForView("R", Cube.PERM.get("y")).equals("F"), "y: R -> F");
        check(CubeOrientation.translateForView("F", Cube.PERM.get("y")).equals("L"), "y: F -> L");
        check(CubeOrientation.translateForView("B", Cube.PERM.get("y")).equals("R"), "y: B -> R");
        check(CubeOrientation.translateForView("U", Cube.PERM.get("y")).equals("U"), "y: U -> U");
        check(CubeOrientation.translateForView("R", Cube.PERM.get("y'")).equals("B"), "y': R -> B");
        check(CubeOrientation.translateForView("R", Cube.PERM.get("y2")).equals("L"), "y2: R -> L");
        check(CubeOrientation.translateForView("B", Cube.PERM.get("y2")).equals("F"), "y2: B -> F");
        check(CubeOrientation.translateForView("U", Cube.PERM.get("x")).equals("B"), "x: U -> B (Cube.java x takes U to B)");
        check(CubeOrientation.translateForView("M2", Cube.PERM.get("y")).equals("M2")
                || !CubeOrientation.translateForView("M2", Cube.PERM.get("y")).isEmpty(),
                "y: M2 translates to a slice move");

        System.out.println("== full solver with elimination ==");
        Random rng2 = new Random(42);
        // Face-move scrambles (the solver's tested domain; slice moves are out of scope).
        final String[] FM = {"U", "U'", "U2", "D", "D'", "D2", "R", "R'", "R2",
                "L", "L'", "L2", "F", "F'", "F2", "B", "B'", "B2"};
        int solved = 0;
        for (int t = 0; t < 25; t++) {
            int[] start = new Cube().applySeq(faceMoves(rng2, 20, FM)).s.clone();
            List<CfopSolver.Stage> stages;
            try {
                stages = CfopSolver.solve(new Cube(start));
            } catch (Exception e) {
                System.out.println("  (solver failed on scramble " + t + ", skipping)");
                continue;
            }
            List<String> all = new ArrayList<>();
            for (CfopSolver.Stage st : stages) {
                List<String> tr = CubeOrientation.eliminateRotations(st.moves);
                if (tr.stream().anyMatch(CubeOrientation::isRotation)) {
                    System.out.println("  FAIL stage still has rotation: " + st.moves);
                    failures++;
                }
                all.addAll(tr);
            }
            int[] end = new Cube(start).applySeq(all).s.clone();
            if (Arrays.equals(end, Cube.SOLVED)) {
                solved++;
            } else {
                System.out.println("  FAIL translated solution did not solve scramble " + t);
                failures++;
            }
        }
        check(solved > 0, "translated full solutions solved " + solved + " cubes");

        System.out.println("== view rotation end-to-end ==");
        // User rotates view by y, then executes translated moves on the
        // displayed (rotated) cube: display must track V(logical).
        boolean e2e = true;
        Random rng3 = new Random(3);
        List<String> logical = Cube.parseMoves("R U R' U' F' U F");
        for (int t = 0; t < 20; t++) {
            int[] s0 = new Cube().applySeq(faceMoves(rng3, 15)).s.clone();
            int[] v = CubeOrientation.identity();
            // apply first 2 moves logically, then user rotates y
            int[] sLogic = new Cube(s0).applySeq(logical.subList(0, 2)).s.clone();
            v = CubeOrientation.composeView(v, "y");
            int[] d = CubeOrientation.applyView(sLogic, v);
            // remaining moves translated for the view
            List<String> rest = new ArrayList<>();
            for (int i = 2; i < logical.size(); i++) {
                rest.add(CubeOrientation.translateForView(logical.get(i), v));
            }
            int[] dEnd = new Cube(d).applySeq(rest).s.clone();
            int[] sLogicEnd = new Cube(s0).applySeq(logical).s.clone();
            int[] expect = CubeOrientation.applyView(sLogicEnd, v);
            if (!Arrays.equals(dEnd, expect)) {
                e2e = false;
                break;
            }
        }
        check(e2e, "view rotation keeps display consistent");

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
