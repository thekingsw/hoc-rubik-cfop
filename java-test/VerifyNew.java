import vn.cfop.rubik.solver.*;
import java.util.*;
public class VerifyNew {
  static final String[][] ALGS = {
    {"U","R","U'","R'"}, // f2l-01 Trường hợp 1
    {"y'","U'","R'","U","R"}, // f2l-02 Trường hợp 2
    {"y'","R'","U'","R"}, // f2l-03 Trường hợp 3
    {"R","U","R'"}, // f2l-04 Trường hợp 4
    {"U'","R","U'","R'","U","y'","R'","U'","R"}, // f2l-05 Trường hợp 5
    {"U'","R","U","R'","U","R","U","R'"}, // f2l-06 Trường hợp 6
    {"U'","R","U2","R'","U","y'","R'","U'","R"}, // f2l-07 Trường hợp 7
    {"R'","U2","R2","U","R2","U","R"}, // f2l-08 Trường hợp 8
    {"y'","U","R'","U","R","U'","R'","U'","R"}, // f2l-09 Trường hợp 9
    {"U'","R","U'","R'","U","R","U","R'"}, // f2l-10 Trường hợp 10
    {"U'","R","U","R'","U2","R","U'","R'"}, // f2l-11 Trường hợp 11
    {"y'","U","R'","U'","R","U2","R'","U","R"}, // f2l-12 Trường hợp 12
    {"U'","R","U2","R'","U2","R","U'","R'"}, // f2l-13 Trường hợp 13
    {"y'","U","R'","U2","R","U2","R'","U","R"}, // f2l-14 Trường hợp 14
    {"U","R","U2","R'","U","R","U'","R'"}, // f2l-15 Trường hợp 15
    {"y'","U'","R'","U2","R","U'","R'","U","R"}, // f2l-16 Trường hợp 16
    {"U2","R","U","R'","U","R","U'","R'"}, // f2l-17 Trường hợp 17
    {"y'","U2","R'","U'","R","U'","R'","U","R"}, // f2l-18 Trường hợp 18
    {"y'","R'","U","R","U2","y","R","U","R'"}, // f2l-19 Trường hợp 19
    {"R","U'","R'","U2","y'","R'","U'","R"}, // f2l-20 Trường hợp 20
    {"R","U2","R'","U'","R","U","R'"}, // f2l-21 Trường hợp 21
    {"y'","R'","U2","R","U","R'","U'","R"}, // f2l-22 Trường hợp 22
    {"U","R","U'","R'","U'","R","U'","R'","U","R","U'","R'"}, // f2l-23 Trường hợp 23
    {"y'","U'","R'","U","R","U","R'","U","R","U'","R'","U","R"}, // f2l-24 Trường hợp 24
    {"U'","F'","R","U","R'","U'","R'","F","R"}, // f2l-25 Trường hợp 25
    {"U","R","U'","R'","U'","F'","U","F"}, // f2l-26 Trường hợp 26
    {"R","U'","R'","U","R","U'","R'"}, // f2l-27 Trường hợp 27
    {"y'","R'","U","R","U'","R'","U","R"}, // f2l-28 Trường hợp 28
    {"y'","R'","U'","R","U","R'","U'","R"}, // f2l-29 Trường hợp 29
    {"R","U","R'","U'","R","U","R'"}, // f2l-30 Trường hợp 30
    {"U'","R'","F","R","F'","R","U'","R'"}, // f2l-31 Trường hợp 31
    {"U","R","U'","R'","U","R","U'","R'","U","R","U'","R'"}, // f2l-32 Trường hợp 32
    {"U'","R","U'","R'","U2","R","U'","R'"}, // f2l-33 Trường hợp 33
    {"U","R","U","R'","U2","R","U","R'"}, // f2l-34 Trường hợp 34
    {"U'","R","U","R'","d","R'","U'","R"}, // f2l-35 Trường hợp 35
    {"U","F'","U'","F","U'","R","U","R'"}, // f2l-36 Trường hợp 36
    {"R","U'","R'","d","R'","U2","R","U2","R'","U","R"}, // f2l-37 Trường hợp 37
    {"R","U'","R'","U'","R","U","R'","U2","R","U'","R'"}, // f2l-38 Trường hợp 38
    {"R","U'","R'","U","R","U2","R'","U","R","U'","R'"}, // f2l-39 Trường hợp 39
    {"F'","U","F","U2","R","U","R'","U","R","U'","R'"}, // f2l-40 Trường hợp 40
    {"R","U","R'","U'","R","U'","R'","U2","y'","R'","U'","R"}, // f2l-41 Trường hợp 41
  };
  static final String[][] SETUPS = {
    {"R","U","R'","U'"}, // f2l-01
    {"F'","U'","F","U"}, // f2l-02
    {"F'","U","F"}, // f2l-03
    {"R","U'","R'"}, // f2l-04
    {"F'","U","F","U'","R","U","R'","U"}, // f2l-05
    {"R","U'","R'","U'","R","U'","R'","U"}, // f2l-06
    {"F'","U","F","U'","R","U2","R'","U"}, // f2l-07
    {"R'","U'","R2","U'","R2","U2","R"}, // f2l-08
    {"F'","U","F","U","F'","U'","F","U'"}, // f2l-09
    {"R","U'","R'","U'","R","U","R'","U"}, // f2l-10
    {"R","U","R'","U2","R","U'","R'","U"}, // f2l-11
    {"F'","U'","F","U2","F'","U","F","U'"}, // f2l-12
    {"R","U","R'","U2","R","U2","R'","U"}, // f2l-13
    {"F'","U'","F","U2","F'","U2","F","U'"}, // f2l-14
    {"R","U","R'","U'","R","U2","R'","U'"}, // f2l-15
    {"F'","U'","F","U","F'","U2","F","U"}, // f2l-16
    {"R","U","R'","U'","R","U'","R'","U2"}, // f2l-17
    {"F'","U'","F","U","F'","U","F","U2"}, // f2l-18
    {"R","U'","R'","y'","U2","R'","U'","R","y"}, // f2l-19
    {"F'","U","F","U2","R","U","R'"}, // f2l-20
    {"R","U'","R'","U","R","U2","R'"}, // f2l-21
    {"F'","U","F","U'","F'","U2","F"}, // f2l-22
    {"R","U","R'","U'","R","U","R'","U","R","U","R'","U'"}, // f2l-23
    {"F'","U'","F","U","F'","U'","F","U'","F'","U'","F","U"}, // f2l-24
    {"R'","F'","R","U","R","U'","R'","F","U"}, // f2l-25
    {"F'","U'","F","U","R","U","R'","U'"}, // f2l-26
    {"R","U","R'","U'","R","U","R'"}, // f2l-27
    {"F'","U'","F","U","F'","U'","F"}, // f2l-28
    {"F'","U","F","U'","F'","U","F"}, // f2l-29
    {"R","U'","R'","U","R","U'","R'"}, // f2l-30
    {"R","U","R'","F","R'","F'","R","U"}, // f2l-31
    {"R","U","R'","U'","R","U","R'","U'","R","U","R'","U'"}, // f2l-32
    {"R","U","R'","U2","R","U","R'","U"}, // f2l-33
    {"R","U'","R'","U2","R","U'","R'","U'"}, // f2l-34
    {"R'","U","R","d'","R","U'","R'","U"}, // f2l-35
    {"R","U'","R'","U","F'","U","F","U'"}, // f2l-36
    {"R'","U'","R","U2","R'","U2","R","d'","R","U","R'"}, // f2l-37
    {"R","U","R'","U2","R","U'","R'","U","R","U","R'"}, // f2l-38
    {"R","U","R'","U'","R","U2","R'","U'","R","U","R'"}, // f2l-39
    {"R","U","R'","U'","R","U'","R'","U2","F'","U'","F"}, // f2l-40
    {"F'","U","F","U2","R","U","R'","U","R","U'","R'"}, // f2l-41
  };
  static final String[] NAMES = {
    "f2l-01",
    "f2l-02",
    "f2l-03",
    "f2l-04",
    "f2l-05",
    "f2l-06",
    "f2l-07",
    "f2l-08",
    "f2l-09",
    "f2l-10",
    "f2l-11",
    "f2l-12",
    "f2l-13",
    "f2l-14",
    "f2l-15",
    "f2l-16",
    "f2l-17",
    "f2l-18",
    "f2l-19",
    "f2l-20",
    "f2l-21",
    "f2l-22",
    "f2l-23",
    "f2l-24",
    "f2l-25",
    "f2l-26",
    "f2l-27",
    "f2l-28",
    "f2l-29",
    "f2l-30",
    "f2l-31",
    "f2l-32",
    "f2l-33",
    "f2l-34",
    "f2l-35",
    "f2l-36",
    "f2l-37",
    "f2l-38",
    "f2l-39",
    "f2l-40",
    "f2l-41",
  };

  static final String[] CN = {"URF","UFL","ULB","UBR","DFR","DLF","DBL","DRB"};
  static final int[][] CS = {{8,9,20},{6,18,38},{0,36,47},{2,45,11},{29,26,15},{27,44,24},{33,53,42},{35,17,51}};
  static final String[] EN = {"UR","UF","UL","UB","DR","DF","DL","DB","FR","FL","BR","BL"};
  static final int[][] ES = {{5,10},{7,19},{3,37},{1,46},{32,16},{28,25},{30,43},{34,52},{23,12},{21,41},{48,14},{50,39}};
  static String fn(int i){ return new String[]{"U","R","F","D","L","B"}[i/9]; }
  static String pairPos(int[] p){
    String corner="?"; String edge="?";
    for(int k=0;k<8;k++){int a=p[CS[k][0]],b=p[CS[k][1]],d=p[CS[k][2]];
      if(((a==0||a==2||a==4)&&(b==0||b==2||b==4)&&(d==0||d==2||d==4))&&a!=b&&b!=d&&a!=d){
        int w=(a==0)?CS[k][0]:(b==0)?CS[k][1]:CS[k][2]; corner=CN[k]+"(w:"+fn(w)+")"; break;}}
    for(int k=0;k<12;k++){int a=p[ES[k][0]],b=p[ES[k][1]];
      if((a==2&&b==4)||(a==4&&b==2)){edge=EN[k];break;}}
    return corner+"/"+edge;
  }
  static String rotOf(int[] s){
    for(String r: new String[]{"y","y'","y2"}){ Cube c=new Cube(); c.apply(r); if(Arrays.equals(c.s,s)) return r; }
    return null;
  }
  public static void main(String[] a){
    int okPattern=0, okAlg=0;
    for(int i=0;i<41;i++){
      Cube c=new Cube(); c.applySeq(Arrays.asList(SETUPS[i]));
      String pos=pairPos(c.s);
      boolean cross=CfopSolver.crossSolved(c);
      boolean others=c.s[27]==0&&c.s[44]==1&&c.s[24]==2&&c.s[21]==2&&c.s[41]==1
        &&c.s[33]==0&&c.s[53]==5&&c.s[42]==1&&c.s[50]==5&&c.s[39]==1
        &&c.s[35]==0&&c.s[51]==5&&c.s[17]==4&&c.s[48]==5&&c.s[14]==4;
      boolean patOk = cross && others;
      if(patOk) okPattern++;
      Cube c2=new Cube(); c2.applySeq(Arrays.asList(SETUPS[i])); c2.applySeq(Arrays.asList(ALGS[i]));
      boolean exact=Arrays.equals(c2.s,Cube.SOLVED);
      String rot=exact?"exact":rotOf(c2.s);
      if(rot!=null) okAlg++;
      System.out.println(NAMES[i]+" | pattern pair="+pos+" cross="+cross+" others="+others
        +" | alg->"+(rot==null?"FAIL":rot));
    }
    System.out.println("SUMMARY clean patterns: "+okPattern+"/41; alg solves (exact or up-to-y-rot): "+okAlg+"/41");
  }
}
