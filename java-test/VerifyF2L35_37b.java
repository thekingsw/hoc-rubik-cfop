import vn.cfop.rubik.solver.*;
import java.util.*;

/**
 * Brute force: for the d-algorithms (cases 35, 37), find EVERY pair
 * configuration (corner slot x orientation, edge slot x orientation, rest
 * solved) from which the verbatim algorithm solves the pair exactly.
 * This tells us what diagram state the site's algorithm actually belongs to.
 */
public class VerifyF2L35_37b {
    static final int[][] CORNER_SLOTS = {
        {8,9,20},{6,18,38},{0,36,47},{2,45,11},
        {29,26,15},{27,44,24},{33,53,42},{35,17,51}};
    static final String[] CORNER_NAMES = {"URF","UFL","ULB","UBR","DFR","DLF","DBL","DRB"};
    static final int[][] EDGE_SLOTS = {
        {5,10},{7,19},{3,37},{1,46},{32,16},{28,25},{30,43},{34,52},
        {23,12},{21,41},{48,14},{50,39}};
    static final String[] EDGE_NAMES = {"UR","UF","UL","UB","DR","DF","DL","DB","FR","FL","BR","BL"};

    public static void main(String[] args) {
        test("35", Arrays.asList("U'","R","U","R'","d","R'","U'","R"));
        test("37", Arrays.asList("R","U'","R'","d","R'","U2","R","U2","R'","U","R"));
    }

    static void test(String num, List<String> alg) {
        System.out.println("=== Case " + num + " ===");
        int hits = 0;
        for (int cs = 0; cs < 8; cs++) {
            for (int w = 0; w < 3; w++) {
                int wf = CORNER_SLOTS[cs][w];
                for (int es = 0; es < 12; es++) {
                    for (int g = 0; g < 2; g++) {
                        int gf = EDGE_SLOTS[es][g];
                        int[] s = stateWithPair(cs, wf, es, gf);
                        if (s == null) continue;
                        Cube c = new Cube();
                        c.s = s;
                        c.applySeq(alg);
                        boolean exact = Arrays.equals(c.s, Cube.SOLVED);
                        if (exact) {
                            hits++;
                            System.out.println("  HIT corner=" + CORNER_NAMES[cs] + " white@"
                                + faceName(wf) + " edge=" + EDGE_NAMES[es] + " green@"
                                + faceName(gf));
                        }
                    }
                }
            }
        }
        System.out.println("  total HITs: " + hits);
    }

    static String faceName(int idx) {
        return new String[]{"U","R","F","D","L","B"}[idx / 9];
    }

    static int[] stateWithPair(int cornerSlot, int whiteFacelet, int edgeSlot, int greenFacelet) {
        int[] s = Cube.SOLVED.clone();
        int[] home = CORNER_SLOTS[4]; // DFR
        int[] tgt = CORNER_SLOTS[cornerSlot];
        int[] homeStickers = {s[home[0]], s[home[1]], s[home[2]]};
        int[] tgtStickers = {s[tgt[0]], s[tgt[1]], s[tgt[2]]};
        for (int k = 0; k < 3; k++) {
            s[home[k]] = tgtStickers[k];
            s[tgt[k]] = homeStickers[k];
        }
        for (int tries = 0; tries < 3; tries++) {
            int wi = -1;
            for (int k = 0; k < 3; k++) if (s[tgt[k]] == 0) wi = k;
            if (tgt[wi] == whiteFacelet) break;
            int tmp = s[tgt[0]];
            s[tgt[0]] = s[tgt[1]]; s[tgt[1]] = s[tgt[2]]; s[tgt[2]] = tmp;
        }
        boolean ok = false;
        for (int k = 0; k < 3; k++) if (tgt[k] == whiteFacelet && s[tgt[k]] == 0) ok = true;
        if (!ok) return null;
        int[] ehome = {23, 12};
        int[] etgt = EDGE_SLOTS[edgeSlot];
        int a = s[ehome[0]], b = s[ehome[1]];
        int c = s[etgt[0]], d = s[etgt[1]];
        s[ehome[0]] = c; s[ehome[1]] = d;
        s[etgt[0]] = a; s[etgt[1]] = b;
        if (s[greenFacelet] != 2) {
            int t = s[etgt[0]]; s[etgt[0]] = s[etgt[1]]; s[etgt[1]] = t;
        }
        if (s[greenFacelet] != 2) return null;
        return s;
    }
}
