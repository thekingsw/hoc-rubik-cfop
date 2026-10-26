package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rubik's cube model: 54 facelets.
 * Faces: U=0..8, R=9..17, F=18..26, D=27..35, L=36..44, B=45..53.
 * Each face is indexed row-major as seen from outside that face.
 * Colours: 0=white, 1=red, 2=green, 3=yellow, 4=orange, 5=blue.
 * Colour scheme (standard CFOP working orientation): U=yellow(3),
 * R=orange(4), F=green(2), D=white(0), L=red(1), B=blue(5).
 * White cross on D, yellow last layer on U.
 *
 * Move permutations are derived from 3D coordinates (no memorised
 * tables): every facelet has a (position, normal); a move rotates one
 * layer about an axis.
 */
public final class Cube {

    /** Colour of each face, faces U,R,F,D,L,B. */
    public static final int[] SCHEME = {3, 4, 2, 0, 1, 5};

    public static final int[] FACE_OF = new int[54];
    public static final int[] SOLVED = new int[54];

    static {
        for (int i = 0; i < 54; i++) {
            FACE_OF[i] = i / 9;
            SOLVED[i] = SCHEME[i / 9];
        }
    }

    /** All move names, including slice moves. */
    public static final String[] MOVE_NAMES = {
            "U", "U'", "U2", "D", "D'", "D2", "R", "R'", "R2",
            "L", "L'", "L2", "F", "F'", "F2", "B", "B'", "B2",
            "M", "M'", "M2", "E", "E'", "E2", "S", "S'", "S2"
    };

    /** Permutation for each move: newState[i] = oldState[perm[i]]. */
    public static final Map<String, int[]> PERM = new HashMap<>();
    /** Inverse permutations: sticker at f moves to INV[m][f]. */
    public static final Map<String, int[]> INV = new HashMap<>();

    static {
        // (axis, value, rotation) for each clockwise base move.
        // Clockwise is judged looking at the face from outside.
        String[] names = {"U", "D", "R", "L", "F", "B", "M", "E", "S"};
        int[][] defs = {
                {1, 1, 5},   // U: top layer, -90 deg about +y
                {1, -1, 4},  // D: bottom layer, +90 deg about +y
                {0, 1, 3},   // R: right layer, -90 deg about +x
                {0, -1, 2},  // L: left layer, +90 deg about +x
                {2, 1, 7},   // F: front layer, -90 deg about +z
                {2, -1, 6},  // B: back layer, +90 deg about +z
                {0, 0, 2},   // M: middle slice, follows L
                {1, 0, 4},   // E: middle slice, follows D
                {2, 0, 7},   // S: middle slice, follows F
        };
        // rotation ids: 2=rotX+, 3=rotX-, 4=rotY+, 5=rotY-, 6=rotZ+, 7=rotZ-
        for (int k = 0; k < names.length; k++) {
            int[] p = layerPerm(defs[k][0], defs[k][1], defs[k][2]);
            int[] p2 = compose(p, p);
            int[] pinv = compose(p2, p);
            PERM.put(names[k], p);
            PERM.put(names[k] + "2", p2);
            PERM.put(names[k] + "'", pinv);
        }
        // Whole-cube rotations (x follows R, y follows U, z follows F).
        String[] rotNames = {"x", "x'", "y", "y'", "z", "z'"};
        int[] rotIds = {3, 2, 5, 4, 7, 6};
        for (int k = 0; k < rotNames.length; k++) {
            int[] p = rotationPerm(rotIds[k]);
            PERM.put(rotNames[k], p);
        }
        PERM.put("x2", compose(PERM.get("x"), PERM.get("x")));
        PERM.put("y2", compose(PERM.get("y"), PERM.get("y")));
        PERM.put("z2", compose(PERM.get("z"), PERM.get("z")));
        for (Map.Entry<String, int[]> e : PERM.entrySet()) {
            int[] p = e.getValue();
            int[] inv = new int[54];
            for (int i = 0; i < 54; i++) {
                inv[p[i]] = i;
            }
            INV.put(e.getKey(), inv);
        }
    }

    private static int[] rot(int[] v, int rotId) {
        int x = v[0], y = v[1], z = v[2];
        switch (rotId) {
            case 2: return new int[]{x, -z, y};    // rotX+
            case 3: return new int[]{x, z, -y};    // rotX-
            case 4: return new int[]{z, y, -x};    // rotY+
            case 5: return new int[]{-z, y, x};    // rotY-
            case 6: return new int[]{-y, x, z};    // rotZ+
            case 7: return new int[]{y, -x, z};    // rotZ-
            default: throw new IllegalArgumentException("rot " + rotId);
        }
    }

    private static int[][] faceletPn(int idx) {
        int[] pos;
        int[] nrm;
        if (idx < 9) {            // U
            pos = new int[]{idx % 3 - 1, 1, idx / 3 - 1};
            nrm = new int[]{0, 1, 0};
        } else if (idx < 18) {    // R
            int j = idx - 9;
            pos = new int[]{1, 1 - j / 3, 1 - j % 3};
            nrm = new int[]{1, 0, 0};
        } else if (idx < 27) {    // F
            int j = idx - 18;
            pos = new int[]{j % 3 - 1, 1 - j / 3, 1};
            nrm = new int[]{0, 0, 1};
        } else if (idx < 36) {    // D
            int j = idx - 27;
            pos = new int[]{j % 3 - 1, -1, 1 - j / 3};
            nrm = new int[]{0, -1, 0};
        } else if (idx < 45) {    // L
            int j = idx - 36;
            pos = new int[]{-1, 1 - j / 3, j % 3 - 1};
            nrm = new int[]{-1, 0, 0};
        } else {                  // B
            int j = idx - 45;
            pos = new int[]{1 - j % 3, 1 - j / 3, -1};
            nrm = new int[]{0, 0, -1};
        }
        return new int[][]{pos, nrm};
    }

    private static String key(int[] pos, int[] nrm) {
        return pos[0] + "," + pos[1] + "," + pos[2] + "|" + nrm[0] + "," + nrm[1] + "," + nrm[2];
    }

    private static int[] layerPerm(int axis, int value, int rotId) {
        Map<String, Integer> pnToIdx = new HashMap<>();
        for (int i = 0; i < 54; i++) {
            int[][] pn = faceletPn(i);
            pnToIdx.put(key(pn[0], pn[1]), i);
        }
        int[] perm = new int[54];
        for (int i = 0; i < 54; i++) {
            perm[i] = i;
        }
        for (int i = 0; i < 54; i++) {
            int[][] pn = faceletPn(i);
            if (pn[0][axis] == value) {
                int[] newPos = rot(pn[0], rotId);
                int[] newNrm = rot(pn[1], rotId);
                perm[pnToIdx.get(key(newPos, newNrm))] = i;
            }
        }
        return perm;
    }

    /** Permutation for a whole-cube rotation: every facelet moves. */
    private static int[] rotationPerm(int rotId) {
        Map<String, Integer> pnToIdx = new HashMap<>();
        for (int i = 0; i < 54; i++) {
            int[][] pn = faceletPn(i);
            pnToIdx.put(key(pn[0], pn[1]), i);
        }
        int[] perm = new int[54];
        for (int i = 0; i < 54; i++) {
            int[][] pn = faceletPn(i);
            int[] newPos = rot(pn[0], rotId);
            int[] newNrm = rot(pn[1], rotId);
            perm[pnToIdx.get(key(newPos, newNrm))] = i;
        }
        return perm;
    }

    /** Permutation for applying a then b. */
    public static int[] compose(int[] a, int[] b) {
        int[] out = new int[54];
        for (int i = 0; i < 54; i++) {
            out[i] = a[b[i]];
        }
        return out;
    }

    public static List<String> parseMoves(String text) {
        List<String> out = new ArrayList<>();
        for (String t : text.replace("’", "'").trim().split("\\s+")) {
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public static List<String> invertMoves(List<String> moves) {
        List<String> out = new ArrayList<>();
        for (int i = moves.size() - 1; i >= 0; i--) {
            String m = moves.get(i);
            if (m.endsWith("'")) {
                out.add(m.substring(0, 1));
            } else if (m.endsWith("2")) {
                out.add(m);
            } else {
                out.add(m + "'");
            }
        }
        return out;
    }

    private static int quarterTurns(String m) {
        if (m.endsWith("'")) return 3;
        if (m.endsWith("2")) return 2;
        return 1;
    }

    /** Combine consecutive moves on the same face. */
    public static List<String> simplifyMoves(List<String> moves) {
        List<String> out = new ArrayList<>();
        for (String m : moves) {
            if (!out.isEmpty() && out.get(out.size() - 1).charAt(0) == m.charAt(0)) {
                String prev = out.remove(out.size() - 1);
                int total = (quarterTurns(prev) + quarterTurns(m)) % 4;
                if (total == 1) {
                    out.add(String.valueOf(m.charAt(0)));
                } else if (total == 2) {
                    out.add(m.charAt(0) + "2");
                } else if (total == 3) {
                    out.add(m.charAt(0) + "'");
                }
            } else {
                out.add(m);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ instance

    public int[] s;

    public Cube() {
        s = SOLVED.clone();
    }

    public Cube(int[] state) {
        s = state.clone();
    }

    public Cube copy() {
        return new Cube(s);
    }

    public Cube apply(String move) {
        int[] p = PERM.get(move);
        int[] next = new int[54];
        for (int i = 0; i < 54; i++) {
            next[i] = s[p[i]];
        }
        s = next;
        return this;
    }

    public Cube applySeq(List<String> moves) {
        for (String m : moves) {
            apply(m);
        }
        return this;
    }

    public boolean isSolved() {
        return Arrays.equals(s, SOLVED);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cube && Arrays.equals(s, ((Cube) o).s);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(s);
    }
}
