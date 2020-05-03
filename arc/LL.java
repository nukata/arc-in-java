// H22.09.28/R02.05.02 (鈴)
package arc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/** 大域的な定数と関数等の置き場.
 * LL は l2lisp https://github.com/nukata/l2lisp-in-java に由来する。
 */
public class LL
{
    /** このクラスはインスタンスを作らない。*/
    private LL () {}

    /** 初期化 Arc スクリプト名 */
    static String PRELUDE = "arc.arc";

    /** 再帰的に印字する深さ */
    static int MAX_EXPANSIONS = 10;

    /** 静的にマクロ展開する深さ */
    static int MAX_MACRO_EXPS = 200;


    /** 初期化 Arc スクリプト名をセットする。 */
    public static void setPrelude(String value) { PRELUDE = value; }

    /** 再帰的に印字する深さをセットする。 */
    public static void setMaxExpansions(int value) { MAX_EXPANSIONS = value; }

    /** 静的にマクロ展開する深さをセットする。 */
    public static void setMaxMacroExps(int value) { MAX_MACRO_EXPS = value; }


    // シンボルの定数
    static final Symbol
        S_ASSIGN = Symbol.Keyword.of("assign"),
        S_FN = Symbol.Keyword.of("fn"),
        S_IF = Symbol.Keyword.of("if"),
        S_MACRO = Symbol.Keyword.of("macro"),
        S_QUOTE = Symbol.Keyword.of("quote"),
        S_QUASIQUOTE = Symbol.Keyword.of("quasiquote"),
        S_DO = Symbol.Keyword.of("_do"),

        S_APPEND = Symbol.of("append"),
        S_APPLY = Symbol.of("apply"),
        S_COMPLEMENT = Symbol.of("complement"),
        S_COMPOSE = Symbol.of("compose"),
        S_CONS = Symbol.of("cons"),
        S_LIST = Symbol.of("_list"),
        S_O = Symbol.of("o"),
        S_T = Symbol.of("t"),
        S_UNDERSCORE = Symbol.of("_"),
        S_UNQUOTE = Symbol.of("unquote"),
        S_UNQUOTE_SPLICING = Symbol.of("unquote-splicing"),

        S_CHAR = Symbol.of("char"),
        S_EXCEPTION = Symbol.of("exception"),
        S_INPUT = Symbol.of("input"),
        S_INT = Symbol.of("int"),
        S_MAC = Symbol.of("mac"),
        S_NUM = Symbol.of("num"),
        S_OUTPUT = Symbol.of("output"),
        S_SOCKET = Symbol.of("socket"),
        S_STRING = Symbol.of("string"),
        S_SYM = Symbol.of("sym"),
        S_TABLE = Symbol.of("table");

    /** Arc の中で EOF を表す値 */
    public static final Object EOF = new Object () {
            public String toString() {
                return "#<eof>";
            }
        };

    /** ラムダ式が評価前であることを示す仮の環境値 */
    static final Cell NONE = new Cell (new Object () {
            public String toString() { return "#<none>"; }
        }, null);

    /** Arc の中で apply 関数を表す値 */
    static final Object APPLY_VAL =
        new Intrinsic("apply", 2, "(apply fn (arg...)) => (fn arg...)", null);

    /** Arc の中で ccc (call/cc) 関数を表す値 */
    static final Object CCC_VAL = 
        new Intrinsic("ccc", 1, "(ccc fn) => (fn current-continuation)", null);

    /** 変数があるべき場所に，変数がなかったことを知らせる例外
     */
    static class VariableExpectedException extends EvalException
    {
        VariableExpectedException (Object exp) {
            super ("variable expected", exp);
        }
    }


    // 文字列化関数

    /** Arc としての値を文字列化する。文字列を引用符で囲む。
     */
    public static String str(Object arg) {
        return str(arg, true);
    }

    /** Arc としての値を文字列化する。
     *  @param arg この値が文字列化される。
     *  @param printQuote 真ならば文字列を引用符で囲む。
     *    ただしリスト等を文字列化するとき，その要素として出現する文字列は，
     *    このフラグにかかわらず，つねに引用符で囲まれる。
     */
    public static String str(Object arg, boolean printQuote) {
        return str(arg, printQuote, MAX_EXPANSIONS, new HashSet<Object> ());
    }

    private static String strQ(Symbol sym, String repr,
                               Cell xc, int recLevel, Set<Object> printed) {
        if (xc.car == sym && xc.cdr instanceof Cell) {
            Cell cdr = (Cell) xc.cdr;
            if (cdr.cdr == null)
                return repr + str(cdr.car, true, recLevel, printed);
        }
        return null;
    }

    /** 2 引数 str の下請け
     */
    static String str(Object x, boolean printQuote, int recLevel,
                      Set<Object> printed) {
        if (x == null) {
            return "nil";
        } else if (x instanceof Cell) {
            Cell xc = (Cell) x;
            String s = strQ(S_QUOTE, "'", xc, recLevel, printed);
            if (s == null) {
                s = strQ(S_QUASIQUOTE, "`", xc, recLevel, printed);
                if (s == null) {
                    s = strQ(S_UNQUOTE, ",", xc, recLevel, printed);
                    if (s == null) {
                        s = strQ(S_UNQUOTE_SPLICING, ",@",
                                 xc, recLevel, printed);
                        if (s == null) {
                            s = "(" + xc.repr(recLevel, printed) + ")";
                        }
                    }
                }
            }
            return s;
        } else if (x instanceof Fn) {
            return ((Fn) x).repr(recLevel, printed);
        } else if (x instanceof Table) {
            return ((Table) x).repr(recLevel, printed);
        } else if (x instanceof String) {
            String s = (String) x;
            if (printQuote) {
                char[] xs = s.toCharArray();
                return "#<String:" + str(xs, true, recLevel, printed) + ">";
            } else {
                return s;
            }
        } else if (x instanceof char[]) {
            char[] xs = (char[]) x;
            if (printQuote) {
                var sb = new StringBuilder ();
                sb.append('"');
                for (char c: xs) {
                    if (c == '"')
                        sb.append("\\\"");
                    else if (c == '\\')
                        sb.append("\\\\");
                    else if (c == '\n')
                        sb.append("\\n");
                    else if (c == '\r')
                        sb.append("\\r");
                    else if (c == '\t')
                        sb.append("\\t");
                    else if (Character.isISOControl(c))
                        sb.append(String.format("\\x%02x", (int) c));
                    else
                        sb.append(c);
                }
                sb.append('"');
                return sb.toString();
            } else {
                return new String(xs);
            }
        } else if (x instanceof Character) {
            if (printQuote) {
                char ch = (Character) x;
                if (ch == '\n')
                    return "#\\newline";
                else if (ch == ' ')
                    return "#\\space";
                else if (ch == '\t')
                    return "#\\tab";
                else if (ch == '\r')
                    return "#\\return";
                else
                    return "#\\" + ch;
            } else {
                return x.toString();
            }
        } else {
            if (x instanceof Object[]) // 参照型の配列ならば…
                x = Arrays.asList((Object[]) x);
            if (x instanceof Iterable) {
                var xl = (Iterable) x;
                if (! printed.add(xl)) { // 重複していたならば…
                    recLevel--;
                    if (recLevel == 0)
                        return "#(...)";
                }
                var sb = new StringBuilder ();
                sb.append("#(");
                boolean first = true;
                for (Object e: xl) {
                    if (first)
                        first = false;
                    else
                        sb.append(" ");
                    sb.append(str(e, true, recLevel, printed));
                }
                sb.append(")");
                return sb.toString();
            } else {
                return
                    (x instanceof long[])   ? Arrays.toString((long[]) x) :
                    (x instanceof int[])    ? Arrays.toString((int[]) x) :
                    (x instanceof short[])  ? Arrays.toString((short[]) x) :
                    // (x instanceof char[])   ? Arrays.toString((char[]) x) :
                    (x instanceof byte[])   ? Arrays.toString((byte[]) x) :
                    (x instanceof boolean[])? Arrays.toString((boolean[]) x) :
                    (x instanceof float[])  ? Arrays.toString((float[]) x) :
                    (x instanceof double[]) ? Arrays.toString((double[]) x) :
                    x.toString();
            }
        }
        // Java では小数点以下 0 の double 値の文字列表現は ".0" が付随する。
        // C# と異なり，そのような値に対し陽に + ".0" とする必要はない。
    }


    // その他の関数

    /** Arc の list 関数に相当する。
     */
    public static Cell list(Object... args) {
        return mapcar(Arrays.asList(args), null);
    }

    /** list 引数の各要素に fn 引数を適用した Arc のリストを作る。
     * @param list 任意のオブジェクトの並びまたは null
     * @param fn 各要素に適用する関数，ただし null ならば恒等関数とみなす。
     * @return 各要素に関数を適用した結果からなるリスト
     * @see Cell#mapcar
     */
    public static <T> Cell mapcar(Iterable<T> list, UnaryOperator<T> fn) {
        if (list == null)
            return null;
        Cell z = null;
        Cell y = null;
        for (T e: list) {
            if (fn != null)
                e = fn.apply(e);
            Cell x = new Cell (e, null);
            if (z == null)
                z = x;
            else
                y.cdr = x;
            y = x;
        }
        return z;
    }

    /** 文字(の)列から，等価な char[] を新しく作る。
     */
    public static char[] toCharArray(CharSequence s) {
        int len = s.length();
        char[] a = new char[len];
        for (int i = 0; i < len; i++)
            a[i] = s.charAt(i);
        return a;
    }
} // LL
