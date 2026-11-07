package vn.cfop.rubik.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 41 trường hợp F2L chuẩn — NGUỒN DUY NHẤT cho cả màn Luyện tập lẫn bộ giải.
 *
 * Quy ước khung hình: khe trống ở TRƯỚC-PHẢI (xanh lá ở trước, cam ở phải),
 * mặt trắng ở dưới, chữ thập và 3 khe còn lại đã xong. Mỗi trường hợp chỉ
 * lưu CÔNG THỨC của người chơi (ký hiệu R U F, có y khi cần). Mọi thứ khác
 * (thế mẫu, nhóm, mô tả cách nhận ra, khoá nhận diện) được TÍNH ra từ công
 * thức đó, nên không thể lệch nhau.
 *
 * Nhận diện: thế của cặp được mô tả đầy đủ bởi (vị trí mặt trắng của góc,
 * vị trí mặt "trước" của cạnh). Xoay U tối đa 3 lần là khớp với đúng một
 * trong 41 thế mẫu (xem {@link #recognize}). Không có tìm kiếm của máy.
 */
public final class F2lCases {

    private F2lCases() {
    }

    /** Một trường hợp F2L. */
    public static final class F2lCase {
        public final int number;            // 1..41
        public final String id;             // "f2l-07"
        public final int group;             // 1..4, xem GROUP_NAMES
        public final String name;
        public final String recognition;
        public final String tip;
        /** Công thức người chơi: chỉ có U R F L và y (d đã đổi thành U + y'). */
        public final List<String> alg;
        /** Thế mẫu tạo từ khối đã giải (không xoay cả khối), để tập trên khối thật. */
        public final List<String> setup;
        /** Thế mẫu (54 facelet) = khối đã giải + setup. */
        public final int[] pattern;
        final int rawKey;

        F2lCase(int number, int group, String name, String recognition, String tip,
                List<String> alg, List<String> setup, int[] pattern, int rawKey) {
            this.number = number;
            this.id = String.format("f2l-%02d", number);
            this.group = group;
            this.name = name;
            this.recognition = recognition;
            this.tip = tip;
            this.alg = alg;
            this.setup = setup;
            this.pattern = pattern;
            this.rawKey = rawKey;
        }
    }

    public static final String[] GROUP_NAMES = {
            "",
            "Góc và cạnh đều ở tầng trên",
            "Góc nằm trong khe, cạnh ở tầng trên",
            "Cạnh nằm trong khe giữa, góc ở tầng trên",
            "Góc và cạnh đều nằm trong khe",
    };

    private static final String[] GROUP_SHORT = {
            "", "cả hai ở trên", "góc trong khe", "cạnh trong khe", "cả hai trong khe",
    };

    private static final String[] GROUP_TIPS = {
            "",
            "Cả hai viên đang ở tầng trên. Xoay U cho góc và cạnh nằm đúng như hình (nhìn mặt trắng của góc quay về đâu và cạnh nằm cạnh/đối diện góc ra sao), rồi làm công thức.",
            "Góc đang nằm sẵn trong khe nhưng chưa đúng chiều, cạnh ở tầng trên. Nhìn chiều quay của mặt trắng trên góc và vị trí cạnh rồi làm công thức.",
            "Cạnh đang nằm trong khe giữa, góc ở tầng trên. Nhìn mặt trắng của góc quay về đâu và mặt trước của cạnh đang hướng ra ngoài hay không rồi làm công thức.",
            "Cả góc và cạnh đều đang trong khe (ghép sai hoặc chưa đúng chiều). Công thức sẽ lấy chúng ra và ghép lại trong một lần.",
    };

    // ------------------------------------------------------------ dữ liệu gốc
    // {số thứ tự, công thức người chơi} — công thức gốc từ cuberesolver.com.
    private static final String[][] RAW = {
        {"01", "U R U' R'"},
        {"02", "y' U' R' U R"},
        {"03", "y' R' U' R"},
        {"04", "R U R'"},
        {"05", "U' R U' R' U y' R' U' R"},
        {"06", "U' R U R' U R U R'"},
        {"07", "U' R U2 R' U y' R' U' R"},
        {"08", "R' U2 R2 U R2 U R"},
        {"09", "y' U R' U R U' R' U' R"},
        {"10", "U' R U' R' U R U R'"},
        {"11", "U' R U R' U2 R U' R'"},
        {"12", "y' U R' U' R U2 R' U R"},
        {"13", "U' R U2 R' U2 R U' R'"},
        {"14", "y' U R' U2 R U2 R' U R"},
        {"15", "U R U2 R' U R U' R'"},
        {"16", "y' U' R' U2 R U' R' U R"},
        {"17", "U2 R U R' U R U' R'"},
        {"18", "y' U2 R' U' R U' R' U R"},
        {"19", "y' R' U R U2 y R U R'"},
        {"20", "R U' R' U2 y' R' U' R"},
        {"21", "R U2 R' U' R U R'"},
        {"22", "y' R' U2 R U R' U' R"},
        {"23", "U R U' R' U' R U' R' U R U' R'"},
        {"24", "y' U' R' U R U R' U R U' R' U R"},
        {"25", "U' F' R U R' U' R' F R"},
        {"26", "U R U' R' U' F' U F"},
        {"27", "R U' R' U R U' R'"},
        {"28", "y' R' U R U' R' U R"},
        {"29", "y' R' U' R U R' U' R"},
        {"30", "R U R' U' R U R'"},
        {"31", "U' R' F R F' R U' R'"},
        {"32", "U R U' R' U R U' R' U R U' R'"},
        {"33", "U' R U' R' U2 R U' R'"},
        {"34", "U R U R' U2 R U R'"},
        {"35", "U' R U R' d R' U' R"},
        {"36", "U F' U' F U' R U R'"},
        {"37", "R U' R' d R' U2 R U2 R' U R"},
        {"38", "R U' R' U' R U R' U2 R U' R'"},
        {"39", "R U' R' U R U2 R' U R U' R'"},
        {"40", "F' U F U2 R U R' U R U' R'"},
        {"41", "R U R' U' R U' R' U2 y' R' U' R"},
    };

    // ------------------------------------------------------------ vị trí viên
    private static final int[][] CORNER_SLOTS = {
            {8, 9, 20}, {6, 18, 38}, {0, 36, 47}, {2, 45, 11},  // URF UFL ULB UBR
            {29, 26, 15}, {27, 44, 24}, {33, 53, 42}, {35, 17, 51} // DFR DLF DBL DRB
    };
    private static final int[][] EDGE_SLOTS = {
            {5, 10}, {7, 19}, {3, 37}, {1, 46},     // UR UF UL UB
            {32, 16}, {28, 25}, {30, 43}, {34, 52}, // DR DF DL DB
            {23, 12}, {21, 41}, {48, 14}, {50, 39}  // FR FL BR BL
    };
    private static final String[] TOP_CORNER_NAMES = {"trước-phải", "trước-trái", "sau-trái", "sau-phải"};
    private static final String[] TOP_EDGE_NAMES = {"bên phải", "phía trước", "bên trái", "phía sau"};

    /** Mặt của facelet f: 0=U 1=R 2=F 3=D 4=L 5=B. */
    private static String faceWord(int f) {
        switch (f / 9) {
            case 0: return "hướng lên trên";
            case 1: return "hướng sang phải";
            case 2: return "hướng ra trước";
            case 3: return "hướng xuống dưới";
            case 4: return "hướng sang trái";
            default: return "hướng ra sau";
        }
    }

    /** Vị trí (slot) góc có màu {0, cF, cR}; trả {chỉ số slot, facelet mặt trắng}. */
    static int[] findCorner(int[] s, int cF, int cR) {
        for (int i = 0; i < CORNER_SLOTS.length; i++) {
            int[] sl = CORNER_SLOTS[i];
            boolean ok = true;
            int white = -1;
            int mask = 0;
            for (int f : sl) {
                int col = s[f];
                if (col == 0) {
                    white = f;
                    mask |= 1;
                } else if (col == cF) {
                    mask |= 2;
                } else if (col == cR) {
                    mask |= 4;
                } else {
                    ok = false;
                }
            }
            if (ok && mask == 7) {
                return new int[]{i, white};
            }
        }
        return null;
    }

    /** Vị trí (slot) cạnh {cF, cR}; trả {chỉ số slot, facelet mặt cF}. */
    static int[] findEdge(int[] s, int cF, int cR) {
        for (int i = 0; i < EDGE_SLOTS.length; i++) {
            int[] sl = EDGE_SLOTS[i];
            if (s[sl[0]] == cF && s[sl[1]] == cR) {
                return new int[]{i, sl[0]};
            }
            if (s[sl[0]] == cR && s[sl[1]] == cF) {
                return new int[]{i, sl[1]};
            }
        }
        return null;
    }

    /**
     * Khoá thô của cặp ở khe trước-phải: màu mặt trước/phải lấy từ chính tâm
     * khối (nên dùng được cả khi khối đã xoay y). -1 nếu không tìm thấy viên.
     */
    public static int rawKey(int[] s) {
        int cF = s[22];
        int cR = s[13];
        int[] c = findCorner(s, cF, cR);
        int[] e = findEdge(s, cF, cR);
        if (c == null || e == null) {
            return -1;
        }
        return c[1] * 54 + e[1];
    }

    /** Khoá chuẩn: nhỏ nhất qua 4 lần xoay U (cùng một trường hợp, khác độ xoay U). */
    public static int canonicalKey(int[] s) {
        Cube t = new Cube(s);
        int best = Integer.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            int key = rawKey(t.s);
            if (key < 0) {
                return -1;
            }
            best = Math.min(best, key);
            t.apply("U");
        }
        return best;
    }

    // ------------------------------------------------------------ bỏ xoay cả khối
    /** Đổi d thành "U y'" để mọi nước đều là nước xoay mặt hoặc xoay cả khối. */
    static List<String> expandWide(List<String> pro) {
        List<String> out = new ArrayList<>();
        for (String m : pro) {
            if (m.equals("d")) {
                out.add("U");
                out.add("y'");
            } else if (m.equals("d'")) {
                out.add("U'");
                out.add("y");
            } else if (m.equals("d2")) {
                out.add("U2");
                out.add("y2");
            } else {
                out.add(m);
            }
        }
        return out;
    }

    private static final char[] FACES = {'U', 'R', 'F', 'D', 'L', 'B'};

    /**
     * Viết lại công thức có y thành công thức không xoay cả khối, cùng hiệu
     * quả lên khối (chỉ khác ở chỗ khối không bị xoay đi). Dùng để dựng thế mẫu.
     */
    static List<String> withoutRotations(List<String> pro) {
        Map<Character, Character> m = new HashMap<>();
        for (char f : FACES) {
            m.put(f, f);
        }
        List<String> out = new ArrayList<>();
        for (String mv : expandWide(pro)) {
            char b = mv.charAt(0);
            String suffix = mv.substring(1);
            if (b == 'y') {
                int times = suffix.equals("'") ? 3 : suffix.equals("2") ? 2 : 1;
                for (int t = 0; t < times; t++) {
                    Map<Character, Character> n = new HashMap<>(m);
                    // y: nội dung mặt F chuyển sang L, L sang B, B sang R, R sang F.
                    n.put('L', m.get('F'));
                    n.put('B', m.get('L'));
                    n.put('R', m.get('B'));
                    n.put('F', m.get('R'));
                    m = n;
                }
            } else {
                out.add(m.get(b) + suffix);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ dựng bảng
    public static final List<F2lCase> ALL;
    private static final Map<Integer, F2lCase> BY_RAW = new HashMap<>();

    static {
        List<F2lCase> list = new ArrayList<>();
        for (String[] row : RAW) {
            int number = Integer.parseInt(row[0]);
            List<String> pro = Cube.parseMoves(row[1]);
            List<String> alg = expandWide(pro);
            List<String> flat = withoutRotations(pro);
            List<String> setup = Cube.simplifyMoves(Cube.invertMoves(flat));
            Cube p = new Cube();
            p.applySeq(setup);
            int[] pattern = p.s.clone();

            int[] c = findCorner(pattern, 2, 4);
            int[] e = findEdge(pattern, 2, 4);
            int group;
            boolean cornerTop = c[0] < 4;
            boolean edgeTop = e[0] < 4;
            if (cornerTop && edgeTop) {
                group = 1;
            } else if (!cornerTop && edgeTop) {
                group = 2;
            } else if (cornerTop) {
                group = 3;
            } else {
                group = 4;
            }
            String recog = describe(c, e, cornerTop, edgeTop);
            String name = "Case " + number + " · " + GROUP_SHORT[group];
            F2lCase fc = new F2lCase(number, group, name, recog, GROUP_TIPS[group],
                    Collections.unmodifiableList(alg), Collections.unmodifiableList(setup),
                    pattern, c[1] * 54 + e[1]);
            list.add(fc);
        }
        ALL = Collections.unmodifiableList(list);
        for (F2lCase fc : list) {
            BY_RAW.put(fc.rawKey, fc);
        }
    }

    private static String describe(int[] c, int[] e, boolean cornerTop, boolean edgeTop) {
        StringBuilder sb = new StringBuilder("Hình mẫu (xanh lá ở trước, cam ở phải). ");
        if (cornerTop) {
            sb.append("Góc ở tầng trên (vị trí ").append(TOP_CORNER_NAMES[c[0]])
                    .append("), mặt trắng ").append(faceWord(c[1])).append(". ");
        } else if (c[1] == 29) {
            sb.append("Góc nằm trong khe, mặt trắng hướng xuống dưới. ");
        } else {
            sb.append("Góc nằm trong khe, mặt trắng ").append(faceWord(c[1])).append(". ");
        }
        if (edgeTop) {
            sb.append("Cạnh ở tầng trên (").append(TOP_EDGE_NAMES[e[0]]).append("), mặt xanh lá ")
                    .append(faceWord(e[1])).append(".");
        } else if (e[1] == 23) {
            sb.append("Cạnh nằm trong khe giữa, đúng chiều (xanh lá ở mặt trước).");
        } else {
            sb.append("Cạnh nằm trong khe giữa, bị lật (xanh lá ở mặt phải).");
        }
        return sb.toString();
    }

    public static F2lCase byNumber(int number) {
        return ALL.get(number - 1);
    }

    // ------------------------------------------------------------ nhận diện
    /** Kết quả nhận diện: trường hợp + số lần xoay U cần làm trước công thức. */
    public static final class Match {
        public final F2lCase fcase;
        /** 0..3: xoay U bao nhiêu lần (theo chiều kim đồng hồ) để về đúng hình mẫu. */
        public final int uTurns;

        Match(F2lCase fcase, int uTurns) {
            this.fcase = fcase;
            this.uTurns = uTurns;
        }

        /** Nước xoay U tương ứng (rỗng nếu 0 lần). */
        public List<String> alignMoves() {
            List<String> out = new ArrayList<>();
            if (uTurns == 1) {
                out.add("U");
            } else if (uTurns == 2) {
                out.add("U2");
            } else if (uTurns == 3) {
                out.add("U'");
            }
            return out;
        }
    }

    /**
     * Nhận diện cặp ở khe trước-phải của khối r (đã xoay để khe cần giải ở
     * trước-phải). Trả null nếu cặp chưa thuộc 41 trường hợp (còn kẹt ở khe khác)
     * hoặc đã giải xong.
     */
    public static Match recognize(Cube r) {
        Cube t = r.copy();
        for (int u = 0; u < 4; u++) {
            int key = rawKey(t.s);
            if (key < 0) {
                return null;
            }
            F2lCase fc = BY_RAW.get(key);
            if (fc != null) {
                return new Match(fc, u);
            }
            t.apply("U");
        }
        return null;
    }

    /** Bảng key chuẩn -> số trường hợp (dùng cho test: phải đủ 41 khoá khác nhau). */
    public static Map<Integer, Integer> canonicalKeyTable() {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (F2lCase fc : ALL) {
            m.put(canonicalKey(fc.pattern), fc.number);
        }
        return m;
    }
}
