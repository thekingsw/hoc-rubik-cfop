import vn.cfop.rubik.solver.*;
import java.util.*;

/**
 * Decisive check for the "keep verbatim" fix:
 * Pattern D' = setup from the CONVERTED (fixed-frame) algorithm, so the
 * diagram shows the pair at FR/top exactly like cuberesolver.com.
 * Algorithm shown = VERBATIM (with y/d) as the site prints it.
 * Question: does verbatim A applied to D' solve the pair, ending in a
 * solved cube up to whole-cube rotation? (A y-rotation at the end just
 * turns the solved cube in your hand — normal for a human cuber.)
 * Reports for each case: D' pair config, and A(D') = rot(solved)?
 */
public class VerifyF2L41d {
    static final String[] VERBATIM = {
        "U R U' R'", "y' U' R' U R", "y' R' U' R", "R U R'",
        "U' R U' R' U y' R' U' R", "U' R U R' U R U R'",
        "U' R U2 R' U y' R' U' R", "R' U2 R2 U R2 U R",
        "y' U R' U R U' R' U' R", "U' R U' R' U R U R'",
        "U' R U R' U2 R U' R'", "y' U R' U' R U2 R' U R",
        "U' R U2 R' U2 R U' R'", "y' U R' U2 R U2 R' U R",
        "U R U2 R' U R U' R'", "y' U' R' U2 R U' R' U R",
        "U2 R U R' U R U' R'", "y' U2 R' U' R U' R' U R",
        "y' R' U R U2 y R U R'", "R U' R' U2 y' R' U' R",
        "R U2 R' U' R U R'", "y' R' U2 R U R' U' R",
        "U R U' R' U' R U' R' U R U' R'", "y' U' R' U R U R' U R U' R' U R",
        "U' F' R U R' U' R' F R", "U R U' R' U' F' U F",
        "R U' R' U R U' R'", "y' R' U R U' R' U R",
        "y' R' U' R U R' U' R", "R U R' U' R U R'",
        "U' R' F R F' R U' R'", "U R U' R' U R U' R' U R U' R'",
        "U' R U' R' U2 R U' R'", "U R U R' U2 R U R'",
        "U' R U R' d R' U' R", "U F' U' F U' R U R'",
        "R U' R' d R' U2 R U2 R' U R", "R U' R' U' R U R' U2 R U' R'",
        "R U' R' U R U2 R' U R U' R'", "F' U F U2 R U R' U R U' R'",
        "R U R' U' R U' R' U2 y' R' U' R",
    };
    // Converted (fixed-frame) algorithms from f2l_cases.kt (ef828e7).
    // Cases 35, 37 have none yet (used 'd') -> null.
    static final String[] CONVERTED = {
        "U R U' R'", "U' F' U F", "F' U' F", "R U R'",
        "U' R U' R' U F' U' F", "U' R U R' U R U R'",
        "U' R U2 R' U F' U' F", "R' U2 R2 U R2 U R",
        "U F' U F U' F' U' F", "U' R U' R' U R U R'",
        "U' R U R' U2 R U' R'", "U F' U' F U2 F' U F",
        "U' R U2 R' U2 R U' R'", "U F' U2 F U2 F' U F",
        "U R U2 R' U R U' R'", "U' F' U2 F U' F' U F",
        "U2 R U R' U R U' R'", "U2 F' U' F U' F' U F",
        "F' U F U2 F U F'", "R U' R' U2 F' U' F",
        "R U2 R' U' R U R'", "F' U2 F U F' U' F",
        "U R U' R' U' R U' R' U R U' R'", "U' F' U F U F' U F U' F' U F",
        "U' F' R U R' U' R' F R", "U R U' R' U' F' U F",
        "R U' R' U R U' R'", "F' U F U' F' U F",
        "F' U' F U F' U' F", "R U R' U' R U R'",
        "U' R' F R F' R U' R'", "U R U' R' U R U' R' U R U' R'",
        "U' R U' R' U2 R U' R'", "U R U R' U2 R U R'",
        null, "U F' U' F U' R U R'",
        null, "R U' R' U' R U R' U2 R U' R'",
        "R U' R' U R U2 R' U R U' R'", "F' U F U2 R U R' U R U' R'",
        "R U R' U' R U' R' U2 F' U' F",
    };

    static final String[] CORNER_NAMES = {"URF","UFL","ULB","UBR","DFR","DLF","DBL","DRB"};
    static final int[][] CORNER_SLOTS = {
        {8,9,20},{6,18,38},{0,36,47},{2,45,11},
        {29,26,15},{27,44,24},{33,53,42},{35,17,51}};
    static final String[] EDGE_NAMES = {"UR","UF","UL","UB","DR","DF","DL","DB","FR","FL","BR","BL"};
    static final int[][] EDGE_SLOTS = {
        {5,10},{7,19},{3,37},{1,46},{32,16},{28,25},{30,43},{34,52},
        {23,12},{21,41},{48,14},{50,39}};

    static String faceName(int idx) {
        return new String[]{"U","R","F","D","L","B"}[idx / 9];
    }

    static String pairPos(int[] p) {
        String corner = "?", edge = "?";
        for (int k = 0; k < 8; k++) {
            int a = p[CORNER_SLOTS[k][0]], b = p[CORNER_SLOTS[k][1]], d = p[CORNER_SLOTS[k][2]];
            if (((a==0||a==2||a==4) && (b==0||b==2||b==4) && (d==0||d==2||d==4)) && a!=b && b!=d && a!=d) {
                int w = (a==0)?CORNER_SLOTS[k][0]:(b==0)?CORNER_SLOTS[k][1]:CORNER_SLOTS[k][2];
                corner = CORNER_NAMES[k] + "(w:" + faceName(w) + ")";
                break;
            }
        }
        for (int k = 0; k < 12; k++) {
            int a = p[EDGE_SLOTS[k][0]], b = p[EDGE_SLOTS[k][1]];
            if ((a==2&&b==4)||(a==4&&b==2)) { edge = EDGE_NAMES[k]; break; }
        }
        return corner + "/" + edge;
    }

    /** Returns rotation name if state == rot(solved), else null. */
    static String rotationOf(int[] s) {
        String[] rots = {"y", "y'", "y2"};
        for (String r : rots) {
            Cube c = new Cube();
            c.apply(r);
            if (Arrays.equals(c.s, s)) return r;
        }
        return null;
    }

    public static void main(String[] args) {
        int okRot = 0, okExact = 0, tested = 0;
        for (int i = 0; i < 41; i++) {
            if (CONVERTED[i] == null) {
                System.out.println("Case " + (i+1) + " | NO CONVERTED (d-case, pending)");
                continue;
            }
            tested++;
            List<String> conv = Arrays.asList(CONVERTED[i].split(" "));
            List<String> verb = Arrays.asList(VERBATIM[i].split(" "));
            // D' = inv(converted)(solved)
            Cube d = new Cube();
            d.applySeq(Cube.invertMoves(conv));
            String pos = pairPos(d.s);
            boolean cross = CfopSolver.crossSolved(d);
            boolean others = d.s[27]==0&&d.s[44]==1&&d.s[24]==2 && d.s[21]==2&&d.s[41]==1
                && d.s[33]==0&&d.s[53]==5&&d.s[42]==1 && d.s[50]==5&&d.s[39]==1
                && d.s[35]==0&&d.s[51]==5&&d.s[17]==4 && d.s[48]==5&&d.s[14]==4;
            // verbatim alg on D'
            Cube c2 = new Cube();
            c2.applySeq(Cube.invertMoves(conv));
            c2.applySeq(verb);
            boolean exact = Arrays.equals(c2.s, Cube.SOLVED);
            String rot = exact ? "exact" : rotationOf(c2.s);
            if (rot != null) { okRot++; if (exact) okExact++; }
            System.out.println("Case " + (i+1) + " | D' pair=" + pos + " cross=" + cross + " others=" + others
                + " | verbatim(D')=" + (rot == null ? "NOT-SOLVED-UP-TO-ROT" : rot));
        }
        System.out.println("\nSUMMARY tested=" + tested + " verbatim solves D' up to rotation: " + okRot + " (exact: " + okExact + ")");
    }
}
