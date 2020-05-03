// H22.10.01/R02.05.01 (鈴)
package arc;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Builtins のためのユーティリティ集 (算術演算を除く)
 * @see Builtins
 * @see BuiltinMath
 */
class BuiltinUtil
{
    // 二つの引数が同じ整数，実数，文字または文字列または参照か？
    static boolean isSame(Object a, Object b) {
        if (a == b)             // 同じ参照または nil か？
            return true;
        else if (a instanceof char[] && b instanceof char[]) // 文字列か？
            return compare((char[]) a, (char[]) b) == 0;
        else if (a == null)
            return false;
        else
            return a.equals(b);
    }

    /// 二つの char[] を文字列として比較する。
    static int compare(char[] s, char[] t) {
        for (int i = 0; i < s.length && i < t.length; i++)
            if (s[i] != t[i])
                return s[i] - t[i];
        return s.length - t.length;
    }

    // x < y < ... ならば true
    static boolean lessThan(Object x, Cell yy) {
        for (Object e: yy) {
            if (x instanceof Number) {
                if (BuiltinMath.compare((Number) x, (Number) e) >= 0)
                    return false;
            } else if (x instanceof char[]) {
                if (compare((char[]) x, (char[]) e) >= 0)
                    return false;
            } else if (x instanceof Symbol) {
                if (((Symbol) x).compareTo((Symbol) e) >= 0)
                    return false;
            } else {
                if (((Character) x).compareTo((Character) e) >= 0)
                    return false;
            }
            x = e;
        }
        return true;
    }

    // x > y > ... ならば true
    static boolean greaterThan(Object x, Cell yy) {
        for (Object e: yy) {
            if (x instanceof Number) {
                if (BuiltinMath.compare((Number) x, (Number) e) <= 0)
                    return false;
            } else if (x instanceof char[]) {
                if (compare((char[]) x, (char[]) e) <= 0)
                    return false;
            } else if (x instanceof Symbol) {
                if (((Symbol) x).compareTo((Symbol) e) <= 0)
                    return false;
            } else {
                if (((Character) x).compareTo((Character) e) <= 0)
                    return false;
            }
            x = e;
        }
        return true;
    }


    // 文字列からなるリストの各文字列を連結する。
    static char[] appendStrings(Cell ss) {
        StringBuilder sb = new StringBuilder ();
        for (Object e: ss)
            sb.append((char[]) e);
        return LL.toCharArray(sb);
    }

    // リストからなるリストの各リストを連結する。
    // 最後の要素はリストでなくてもよい。
    static Object appendLists(Cell xs) {
        if (xs == null) {
            return null;
        } else {
            Cell cdr = xs.getCdrCell();
            if (cdr == null) {
                return xs.car;
            } else {
                Object t = appendLists(cdr);
                return add2Lists((Cell) xs.car, t);
            }
        }
    }

    // 二つのリストを連結する。
    // head のコピーを作成してその末尾を tail につなげる。
    static Object add2Lists(Cell head, Object tail) {
        Object z = null;
        Cell y = null;
        if (head != null)
            for (Object e: head) {
                Cell x = new Cell (e, null);
                if (z == null)
                    z = x;
                else
                    y.cdr = x;
                y = x;
            }
        if (z == null)
            z = tail;
        else
            y.cdr = tail;
        return z;
    }

    // 要素数またはエントリ数
    static int len(Object x) {
        if (x == null) {
            return 0;
        } else if (x instanceof Cell) {
            int i = 0;
            for (Object e: (Cell) x)
                i++;
            return i;
        } else if (x instanceof char[]) {
            return ((char[]) x).length;
        } else {
            return ((Table) x).size();
        }
    }


    // char[] を Iterable にするラッパを作って返す。
    private static Iterable<Character> iterableChars(final char[] s) {
        return new Iterable<Character> () {
            public Iterator<Character> iterator() {
                return new Iterator<Character> () {
                    int i = 0;

                    public boolean hasNext() {
                        return i < s.length;
                    }

                    public Character next() {
                        if (i < s.length) {
                            char ch = s[i];
                            i++;
                            return ch;
                        } else {
                            throw new NoSuchElementException ();
                        }
                    }
            
                    public void remove() {
                        throw new UnsupportedOperationException ();
                    }
                };
            }
        };
    }

    // "23" => 23
    private static Number stringToInt(char[] s, Object option) {
        int radix = (option == null) ? 10 : (Integer) option;
        BigInteger x = new BigInteger (new String (s), radix);
        return BuiltinMath.reg(x);
    }

    // 23 => "23"
    private static char[] intToString(int i, Object option) {
        int radix = (option == null) ? 10 : (Integer) option;
        String s = Integer.toString(i, radix);
        return s.toCharArray();
    }

    private static char[] bignumToString(BigInteger i, Object option) {
        int radix = (option == null) ? 10 : (Integer) option;
        String s = i.toString(radix);
        return s.toCharArray();
    }

    // "444.36" => 444.36
    private static Number stringToNum(char[] s, Object option) {
        if (option == null || (Integer) option == 10)
            try {
                var input = new LinesFromString (new String (s));
                try (var chars = new CharEnumerator (input)) {
                    var reader = new ArcReader (chars);
                    Object x = reader.read();
                    return (Number) x;
                }
            } catch (Exception ex) {
                throw new EvalException ("failed to convert to number", ex);
            }
        else
            throw new UnsupportedOperationException ();
    }

    // (#\額 #\田) => "額田"
    private static char[] consToString(Cell list) {
        StringBuilder sb = new StringBuilder ();
        for (Object e: list) {
            char[] cc = (char[]) coerce(e, LL.S_STRING, null);
            sb.append(cc);
        }
        return LL.toCharArray(sb);
    }

    // (coerce x typ) の実装
    static Object coerce(Object x, Symbol typ, Object option) {
        if (x == null) {
            if (typ == LL.S_SYM || typ == LL.S_CONS)
                return null;
            else if (typ == LL.S_STRING)
                return new char[0];
        }
        else if (x instanceof Cell) {
            Cell j = (Cell) x;
            if (typ == LL.S_CONS)
                return j;
            else if (typ == LL.S_STRING)
                return consToString(j);
        }
        else if (x instanceof Symbol) {
            Symbol s = (Symbol) x;
            if (typ == LL.S_SYM)
                return s;
            else if (typ == LL.S_STRING)
                return s.name.toCharArray();
        }
        else if (x instanceof Character) {
            Character ch = (Character) x;
            if (typ == LL.S_CHAR)
                return ch;
            else if (typ == LL.S_STRING)
                return new char[] { ch };
            else if (typ == LL.S_INT)
                return (int) ch;
        }
        else if (x instanceof char[]) {
            char[] s = (char[]) x;
            if (typ == LL.S_CONS)
                return LL.mapcar(iterableChars(s), null);
            else if (typ == LL.S_SYM) {
                if (s.length == 0) {
                    return null;
                } else {
                    String ss = new String (s);
                    return ss.equals("nil") ? null : Symbol.of(ss);
                }
            } 
            else if (typ == LL.S_STRING)
                return s;
            else if (typ == LL.S_CHAR && s.length == 1)
                return s[0];
            else if (typ == LL.S_INT)
                return stringToInt(s, option);
            else if (typ == LL.S_NUM)
                return stringToNum(s, option);
        }
        else if (x instanceof Integer) {
            int i = (Integer) x;
            if (typ == LL.S_CHAR)
                return (char) i;
            else if (typ == LL.S_STRING)
                return intToString(i, option);
            else if (typ == LL.S_INT || typ == LL.S_NUM)
                return i;
        }
        else if (x instanceof BigInteger) {
            BigInteger i = (BigInteger) x;
            if (typ == LL.S_STRING)
                return bignumToString(i, option);
            else if (typ == LL.S_INT || typ == LL.S_NUM)
                return i;
        }
        else if (x instanceof Number) {
            Number n = (Number) x;
            if (typ == LL.S_STRING)
                return n.toString().toCharArray();
            else if (typ == LL.S_NUM)
                return n;
        }
        else if (x instanceof Arg) {
            return coerce(((Arg) x).symbol, typ, option);
        }
        throw new UnsupportedOperationException ();
    }
} // BuiltinUtil
