import vn.cfop.rubik.solver.*;
import java.util.*;

/**
 * Cases 35 & 37 (d-moves): find the diagram state D' (pair as the site
 * diagrams it, rest solved) that the VERBATIM algorithm solves exactly,
 * then derive a clean setup via the solver's own F2L pair solver.
 * Case 35 (site): corner on top, white on side; edge in FR slot.
 * Case 37 (site): corner in FR slot, white down; edge in FR slot.
 */
public class VerifyF2L35_37 {
    // facelet indices: U=0..8 R=9..17 F=18..26 D=27..35 L=36..44 B=45..53
    // corner slots: URF {8,9,20} UFL {6,18,38} ULB {0,36,47} UBR {2,45,11}
    //               DFR {29,26,15} DLF {27,44,24} DBL {33,53,42} DRB {35,17,51}
    // edge slots: UR {5,10} UF {7,19} UL {3,37} UB {1,46} FR {23,12} FL {21,41}
    static final int[][] CORNER_SLOTS = {
        {8,9,20},{6,18,38},{0,36,47},{2,45,11},
        {29,26,15},{27,44,24},{33,53,42},{35,17,51}};
    static final int[][] EDGE_SLOTS = {
        {5,10},{7,19},{3,37},{1,46},{32,16},{28,25},{30,43},{34,52},
        {23,12},{21,41},{48,14},{50,39}};
    static final String[] CORNER_NAMES = {"URF","UFL","ULB","UBR","DFR","DLF","DBL","DRB"};

    /** Build a state: solved, except the white/green/orange corner is placed
     *  at cornerSlot with white on whiteFacelet, and the green/orange edge is
     *  placed at edgeSlot (green on greenFacelet). The displaced pieces go
     *  where the pair pieces came from (swap). */
    static int[] buildState(int cornerSlot, int whiteFacelet, int edgeSlot, int greenFacelet) {
        int[] s = Cube.SOLVED.clone();
        // corner piece colours in solved position URF: facelets {8:U yellow? no...}
        // Solved URF corner stickers: 8 (U=3 yellow), 9 (R=4 orange), 20 (F=2 green).
        // The pair corner piece is (white 0, green 2, orange 4). In the SOLVED
        // cube it sits at DFR: facelets {29:D=0 white, 26:F=2 green, 15:R=4 orange}.
        // Take it from DFR and put it at cornerSlot with the requested orientation.
        int[] dfr = {29, 26, 15}; // white, green, orange facelets of the piece
        int[] piece = {0, 2, 4};  // colours in DFR order (white, green, orange)
        // The piece currently at cornerSlot must go to DFR (swap).
        int[] target = CORNER_SLOTS[cornerSlot];
        // colours currently at target slot
        int[] targetColours = {s[target[0]], s[target[1]], s[target[2]]};
        // place pair corner at target: white on whiteFacelet; green/orange on the other two
        int wi = -1;
        for (int k = 0; k < 3; k++) if (target[k] == whiteFacelet) wi = k;
        // the other two facelets: assign green then orange in slot order, but keep
        // orientation consistent: we just need SOME placement with white on whiteFacelet;
        // try both orders below via caller (this function places green on the
        // first non-white facelet, orange on the second).
        int gi = -1, oi = -1;
        for (int k = 0; k < 3; k++) {
            if (k == wi) continue;
            if (gi < 0) gi = k; else oi = k;
        }
        s[target[wi]] = 0; s[target[gi]] = 2; s[target[oi]] = 4;
        // displaced corner piece goes to DFR with its original colours in DFR order
        // (white on D, green on F, orange on R) — but must keep the cube a valid
        // permutation: the displaced piece's stickers land on DFR facelets in the
        // same cyclic order as they were at the target slot.
        // Simplest valid swap: rotate the two pieces' stickers by position mapping.
        // We do it by permuting facelets: swap the sticker VALUES between the two
        // slots position-wise, then fix the pair corner orientation as requested.
        // (Recompute from scratch below instead.)
        return s;
    }

    public static void main(String[] args) {
        // Direct approach: enumerate candidate D' states by applying candidate
        // SETUPS (short U/R/F sequences) — instead, brute force over states
        // built by swapping the pair pieces into candidate slots with each
        // orientation, keeping the rest solved, and test the verbatim alg.
        // We build states via explicit piece placement with a proper swap.
        List<String> alg35 = Arrays.asList("U'","R","U","R'","d","R'","U'","R");
        List<String> alg37 = Arrays.asList("R","U'","R'","d","R'","U2","R","U2","R'","U","R");

        System.out.println("=== Case 35: corner top (white side), edge FR ===");
        // corner slots URF/UFL/ULB/UBR (0..3); white on one of its 2 side facelets
        for (int cs = 0; cs < 4; cs++) {
            int[] slot = CORNER_SLOTS[cs];
            for (int w = 0; w < 3; w++) {
                int wf = slot[w];
                if (wf / 9 == 0) continue; // white on U = up, not side; skip
                // edge FR slot {23,12}: green on 23 (F) or 12 (R)
                for (int g = 0; g < 2; g++) {
                    int[] s = stateWithPair(cs, wf, 8, g == 0 ? 23 : 12);
                    if (s == null) continue;
                    Cube c = new Cube();
                    c.s = s;
                    c.applySeq(alg35);
                    boolean exact = Arrays.equals(c.s, Cube.SOLVED);
                    boolean pairOk = c.s[29]==0 && c.s[26]==2 && c.s[15]==4 && c.s[23]==2 && c.s[12]==4;
                    if (exact || pairOk) {
                        System.out.println("  HIT corner=" + CORNER_NAMES[cs] + " white@" + wf
                            + " edgeGreen@" + (g == 0 ? 23 : 12) + " exact=" + exact + " pair=" + pairOk);
                    }
                }
            }
        }
        System.out.println("=== Case 37: corner FR slot, edge FR slot ===");
        // corner DFR (slot 4); white on 29 (D), 26 (F) or 15 (R)
        for (int w = 0; w < 3; w++) {
            int wf = CORNER_SLOTS[4][w];
            for (int g = 0; g < 2; g++) {
                int[] s = stateWithPair(4, wf, 8, g == 0 ? 23 : 12);
                if (s == null) continue;
                Cube c = new Cube();
                c.s = s;
                c.applySeq(alg37);
                boolean exact = Arrays.equals(c.s, Cube.SOLVED);
                boolean pairOk = c.s[29]==0 && c.s[26]==2 && c.s[15]==4 && c.s[23]==2 && c.s[12]==4;
                if (exact || pairOk) {
                    System.out.println("  HIT white@" + wf + " edgeGreen@" + (g == 0 ? 23 : 12)
                        + " exact=" + exact + " pair=" + pairOk);
                }
            }
        }
        System.out.println("done");
    }

    /**
     * Build a valid cube state: solved except the FR pair pieces are moved
     * to the requested slots (corner at cornerSlot with white on whiteFacelet,
     * edge at edgeSlot with green on greenFacelet). The two displaced pieces
     * are swapped into the pair's home slots (DFR corner / FR edge) with an
     * orientation that keeps the permutation valid (we swap sticker-by-sticker
     * between the two slots, then re-orient the pair corner as requested).
     * Returns null if the requested placement is not a valid permutation.
     */
    static int[] stateWithPair(int cornerSlot, int whiteFacelet, int edgeSlot, int greenFacelet) {
        int[] s = Cube.SOLVED.clone();
        // --- corner swap: DFR piece <-> piece at cornerSlot ---
        int[] home = CORNER_SLOTS[4]; // DFR {29,26,15}
        int[] tgt = CORNER_SLOTS[cornerSlot];
        // Save stickers
        int[] homeStickers = {s[home[0]], s[home[1]], s[home[2]]};
        int[] tgtStickers = {s[tgt[0]], s[tgt[1]], s[tgt[2]]};
        // Move target piece to DFR (position-wise), pair piece to target slot.
        for (int k = 0; k < 3; k++) {
            s[home[k]] = tgtStickers[k];
            s[tgt[k]] = homeStickers[k];
        }
        // Now the pair corner (white,green,orange) sits at tgt, but its white
        // may not be on whiteFacelet. Rotate the three stickers within the slot
        // until white is on whiteFacelet (a cyclic rotation of the 3 stickers
        // is a valid re-orientation of the corner in its slot).
        for (int tries = 0; tries < 3; tries++) {
            int wi = -1;
            for (int k = 0; k < 3; k++) if (s[tgt[k]] == 0) wi = k;
            if (tgt[wi] == whiteFacelet) break;
            int tmp = s[tgt[0]];
            s[tgt[0]] = s[tgt[1]]; s[tgt[1]] = s[tgt[2]]; s[tgt[2]] = tmp;
        }
        // verify white placement
        boolean ok = false;
        for (int k = 0; k < 3; k++) if (tgt[k] == whiteFacelet && s[tgt[k]] == 0) ok = true;
        if (!ok) return null;
        // --- edge swap: FR piece <-> piece at edgeSlot ---
        int[] ehome = {23, 12}; // FR
        int[] etgt = EDGE_SLOTS[edgeSlot];
        int a = s[ehome[0]], b = s[ehome[1]];
        int c = s[etgt[0]], d = s[etgt[1]];
        s[ehome[0]] = c; s[ehome[1]] = d;
        s[etgt[0]] = a; s[etgt[1]] = b;
        // orient the pair edge so green is on greenFacelet (flip if needed)
        if (s[greenFacelet] != 2) {
            int o1 = etgt[0], o2 = etgt[1];
            int t = s[o1]; s[o1] = s[o2]; s[o2] = t;
        }
        if (s[greenFacelet] != 2) return null;
        return s;
    }
}
