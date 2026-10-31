import java.util.List;
import vn.cfop.rubik.solver.CfopSolver;
import vn.cfop.rubik.solver.Cube;

/** Diagnose per-stage timing for one scramble. */
public class DiagSolver {
    public static void main(String[] args) {
        // First scramble from TestSolver seed 42
        List<String> scr = TestSolver.randomScramble(new java.util.Random(42), 22);
        System.out.println("scramble: " + String.join(" ", scr));
        Cube c = new Cube();
        c.applySeq(scr);
        long t0 = System.currentTimeMillis();
        List<String> cross = CfopSolver.crossSolution(c);
        c.applySeq(cross);
        System.out.println("cross: " + cross.size() + " moves, "
                + (System.currentTimeMillis() - t0) + "ms; solved=" + CfopSolver.crossSolved(c));
        // Solve via the public solve() but with timing per stage by re-doing stages
        t0 = System.currentTimeMillis();
        List<CfopSolver.Stage> stages = CfopSolver.solve(new Cube(c.s));
        System.out.println("full solve from cross state: "
                + (System.currentTimeMillis() - t0) + "ms, stages=" + stages.size());
        for (CfopSolver.Stage st : stages) {
            System.out.println("  " + st.title + ": " + String.join(" ", st.moves));
        }
        System.out.println("DIAG DONE");
    }
}
