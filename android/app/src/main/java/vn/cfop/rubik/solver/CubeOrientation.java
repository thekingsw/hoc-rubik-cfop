package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CubeOrientation {
    private CubeOrientation() {
    }

    private static final Map<String, String> PERM_TO_MOVE = new HashMap<>();

    static {
        for (String m : Cube.MOVE_NAMES) {
            int[] p = Cube.PERM.get(m);
            PERM_TO_MOVE.put(key(p), m);
        }
    }

    private static String key(int[] p) {
        StringBuilder sb = new StringBuilder(160);
        for (int v : p) {
            sb.append(v).append(',');
        }
        return sb.toString();
    }

    public static int[] identity() {
        int[] id = new int[54];
        for (int i = 0; i < 54; i++) {
            id[i] = i;
        }
        return id;
    }

    private static int[] invert(int[] p) {
        int[] inv = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            inv[p[i]] = i;
        }
        return inv;
    }

    private static int[] composeApply(int[] a, int[] b) {
        int[] c = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[b[i]];
        }
        return c;
    }

    public static boolean isRotation(String move) {
        if (move == null || move.isEmpty()) {
            return false;
        }
        char c = move.charAt(0);
        return c == 'x' || c == 'y' || c == 'z';
    }

    public static List<String> eliminateRotations(List<String> moves) {
        int[] acc = identity();
        List<String> out = new ArrayList<>(moves.size());
        for (String m : moves) {
            if (isRotation(m)) {
                acc = composeApply(acc, Cube.PERM.get(m));
            } else {
                int[] accInv = invert(acc);
                int[] pm = Cube.PERM.get(m);
                int[] conj = new int[54];
                for (int i = 0; i < 54; i++) {
                    conj[i] = acc[pm[accInv[i]]];
                }
                String m2 = PERM_TO_MOVE.get(key(conj));
                if (m2 == null) {
                    return new ArrayList<>(moves);
                }
                out.add(m2);
            }
        }
        for (int i = 0; i < 54; i++) {
            if (acc[i] != i) {
                return new ArrayList<>(moves);
            }
        }
        return out;
    }

    public static List<String> eliminateXZRotations(List<String> moves) {
        List<String> out = new ArrayList<>(moves.size());
        int[] acc = identity();
        for (String m : moves) {
            char c = m.charAt(0);
            if (c == 'x' || c == 'z') {
                acc = composeApply(acc, Cube.PERM.get(m));
            } else if (c == 'y') {
                int[] accInv = invert(acc);
                int[] pm = Cube.PERM.get(m);
                int[] conj = new int[54];
                for (int i = 0; i < 54; i++) {
                    conj[i] = acc[pm[accInv[i]]];
                }
                String m2 = PERM_TO_MOVE.get(key(conj));
                if (m2 == null || !isRotation(m2) || m2.charAt(0) != 'y') {
                    return new ArrayList<>(moves);
                }
                out.add(m2);
                acc = Cube.PERM.get(m2).clone();
            } else {
                int[] accInv = invert(acc);
                int[] pm = Cube.PERM.get(m);
                int[] conj = new int[54];
                for (int i = 0; i < 54; i++) {
                    conj[i] = acc[pm[accInv[i]]];
                }
                String m2 = PERM_TO_MOVE.get(key(conj));
                if (m2 == null) {
                    return new ArrayList<>(moves);
                }
                out.add(m2);
            }
        }
        for (String m : out) {
            char c = m.charAt(0);
            if (c == 'x' || c == 'z') {
                return new ArrayList<>(moves);
            }
        }
        return out;
    }

    public static String translateForView(String move, int[] viewPerm) {
        if (isRotation(move)) {
            return move;
        }
        int[] vi = invert(viewPerm);
        int[] pm = Cube.PERM.get(move);
        int[] conj = new int[54];
        for (int i = 0; i < 54; i++) {
            conj[i] = vi[pm[viewPerm[i]]];
        }
        String m2 = PERM_TO_MOVE.get(key(conj));
        return m2 != null ? m2 : move;
    }

    public static int[] composeView(int[] viewPerm, String rot) {
        return composeApply(viewPerm, Cube.PERM.get(rot));
    }

    public static int[] applyView(int[] state, int[] viewPerm) {
        int[] out = new int[54];
        for (int i = 0; i < 54; i++) {
            out[i] = state[viewPerm[i]];
        }
        return out;
    }
}
