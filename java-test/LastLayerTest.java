package vn.cfop.rubik.solver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Công thức PLL ở màn Giải (LastLayerHuman) phải GIỐNG HỆT màn Luyện tập (TrainingData.kt),
 * và phải đúng bản chất: công thức đổi cạnh không được làm lệch góc, công thức góc A không
 * được làm lệch cạnh.
 * Chạy: javac -encoding UTF-8 -d out $(find ../android -path '*solver*' -name '*.java') LastLayerTest.java
 *       java -cp out vn.cfop.rubik.solver.LastLayerTest [đường-dẫn-TrainingData.kt]
 */
public class LastLayerTest {
    static int failures = 0;

    static void check(boolean ok, String msg) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0]
                : "../android/app/src/main/java/vn/cfop/rubik/ui/TrainingData.kt";
        String kt = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        String[][] map = {{"pll-aa", "Aa"}, {"pll-ab", "Ab"}, {"pll-y", "Y"}, {"pll-ua", "Ua"},
                {"pll-ub", "Ub"}, {"pll-h", "H"}, {"pll-z", "Z"}};
        for (String[] m : map) {
            int i = kt.indexOf("\"" + m[0] + "\"");
            check(i >= 0, "không thấy " + m[0] + " trong TrainingData.kt");
            if (i < 0) {
                continue;
            }
            int a = kt.indexOf("listOf(", i);
            int b = kt.indexOf(')', a);
            Matcher mt = Pattern.compile("\"([^\"]+)\"").matcher(kt.substring(a, b));
            StringBuilder sb = new StringBuilder();
            while (mt.find()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(mt.group(1));
            }
            List<String> solver = LastLayerHuman.pllFormula(m[1]);
            String solverStr = String.join(" ", solver);
            check(sb.toString().equals(solverStr),
                    m[1] + ": màn Luyện tập '" + sb + "' khác màn Giải '" + solverStr + "'");

            Cube c = new Cube();
            c.applySeq(solver);
            boolean edgePerm = m[1].equals("Ua") || m[1].equals("Ub") || m[1].equals("H") || m[1].equals("Z");
            if (edgePerm) {
                check(LastLayerHuman.uCornersCorrect(c), m[1] + ": công thức đổi cạnh không được làm lệch góc");
            } else if (m[1].equals("Aa") || m[1].equals("Ab")) {
                check(LastLayerHuman.uEdgesCorrect(c), m[1] + ": A-perm không được làm lệch cạnh");
            }
        }
        System.out.println(failures == 0 ? "LAST LAYER TESTS PASSED" : failures + " FAILURES");
        if (failures != 0) {
            System.exit(1);
        }
    }
}
