// H22.09.01/R02.04.30 (鈴)
package arc;

/** 準引用 (Quasi-Quotation)
 */
public class QQ
{
    // このクラスはインスタンスを作らない
    private QQ () {}

    // (quasiquote ...) 等のリストを一般のリストから判別するための関数
    private static boolean checkFor(Symbol sym, Cell xc) {
        Object fn = xc.car;
        if (fn == sym) {
            Cell cdr = xc.getCdrCell();
            if (cdr == null || cdr.cdr != null)
                throw new EvalException ("bad " + sym);
            return true;
        } else {
            return false;
        }
    }

    /** 引数が (quote ...) か調べる。
     * そうならば，その引数は厳密に１個でなければならない。
     */
    public static boolean checkForQuote(Cell xc) {
        return checkFor(LL.S_QUOTE, xc);
    }

    /** 引数が (quasiquote ...) か調べる。
     * そうならば，その引数は厳密に１個でなければならない。
     */
    public static boolean checkForQuasiquote(Cell xc) {
        return checkFor(LL.S_QUASIQUOTE, xc);
    }

    private static boolean checkForUnquote(Cell xc) {
        return checkFor(LL.S_UNQUOTE, xc);
    }

    private static boolean checkForUnquoteSplicing(Cell xc) {
        return checkFor(LL.S_UNQUOTE_SPLICING, xc);
    }


    /** 準引用式を展開する。
     * 準引用式 `x の x を受け取り，`x と等価だが準引用を含まない式を返す。
     */
    public static Object expand(Object x) {
        if (x instanceof Cell) {
            Cell xc = (Cell) x;
            if (checkForQuasiquote(xc)) {
                Object e = ((Cell) xc.cdr).car;
                Object y = expand(e);
                return expand(y);
            } else if (checkForUnquote(xc)) {
                return ((Cell) xc.cdr).car;
            } else if (checkForUnquoteSplicing(xc)) {
                throw new EvalException ("unquote-splicing not in list", x);
            } else {
                Cell t = expand1(x);
                if ((t.car instanceof Cell) && (t.cdr == null)) {
                    Cell k = (Cell) t.car;
                    if (k.car == LL.S_LIST || k.car == LL.S_CONS)
                        return k;
                }
                return new Cell (LL.S_APPEND, t);
            }
        } else {
            return quote(x);
        }
    }

    // 引数をクォートする。ただし nil，数，文字列はわざわざクォートしない。
    private static Object quote(Object x) {
        if (x == null || x instanceof Number || x instanceof char[])
            return x;
        else
            return LL.list(LL.S_QUOTE, x);
    }

    // `x の x を append の引数として使えるように展開する。
    // 例 1: (,a b) => h=(list a) t=((list 'b)) => ((list a 'b))
    // 例 2: (,a ,@(cons 2 3)) => h=(list a) t=((cons 2 3)) 
    //                         => ((cons a (cons 2 3)))
    private static Cell expand1(Object x) {
        if (x instanceof Cell) {
            Cell xc = (Cell) x;
            if (checkForQuasiquote(xc)) {
                Object e = ((Cell) xc.cdr).car;
                Object y = expand(e);
                return LL.list(expand(y));
            } else if (checkForUnquote(xc)) {
                Object e = ((Cell) xc.cdr).car;
                return LL.list(e);
            } else if (checkForUnquoteSplicing(xc)) {
                throw new EvalException ("unquote-splicing not in list", x);
            } else {
                Object h = expand2(xc.car);
                Object t = expand1(xc.cdr);
                if (t instanceof Cell) {
                    Cell tc = (Cell) t;
                    if (tc.car == null && tc.cdr == null) {
                        return LL.list(h);
                    } else if (h instanceof Cell) {
                        Cell hc = (Cell) h;
                        if (hc.car == LL.S_LIST) {
                            if (tc.car instanceof Cell) {
                                Cell tcar = (Cell) tc.car;
                                if (tcar.car == LL.S_LIST) {
                                    Object hh = concat(hc, tcar.cdr);
                                    return new Cell (hh, tc.cdr);
                                }
                            }
                            if (hc.cdr instanceof Cell) {
                                Object hh = consCons((Cell) hc.cdr, tc.car);
                                return new Cell (hh, tc.cdr);
                            }
                        }
                    }
                }
                return new Cell (h, t);
            }
        } else {
            return LL.list(quote(x));
        }
    }

    // concat(LL.list(1, 2), LL.list(3, 4)) => (1 2 3 4)
    private static Object concat(Cell x, Object y) {
        if (x == null)
            return y;
        else
            return new Cell (x.car, concat((Cell) x.cdr, y));
    }

    // consCons(LL.list(1, 2, 3), "a") => (cons 1 (cons 2 (cons 3 "a")))
    private static Object consCons(Cell x, Object y) {
        if (x == null)
            return y;
        else
            return LL.list(LL.S_CONS, x.car, consCons((Cell) x.cdr, y));
    }

    // `x の x.car を append の１引数として使えるように展開する。
    // 例 ,a => (list a);  ,@(foo 1 2) => (foo 1 2); b => (list 'b)
    private static Object expand2(Object x) {
        if (x instanceof Cell) {
            Cell xc = (Cell) x;
            if (checkForQuasiquote(xc)) {
                Object e = ((Cell) xc.cdr).car;
                Object y = expand(e);
                return LL.list(LL.S_LIST, expand(y));
            } else if (checkForUnquote(xc)) {
                Object e = ((Cell) xc.cdr).car;
                return LL.list(LL.S_LIST, e);
            } else if (checkForUnquoteSplicing(xc)) {
                return ((Cell) xc.cdr).car;
            }
        } 
        return LL.list(LL.S_LIST, expand(x));
    }


    /** 与えられた式の中にある準引用式を等価な式へと展開する。
     */
    public static Object resolve(Object x) { // cf. Interp#scanForArgs
        for (;;) {
            if (x instanceof Cell) {
                Cell xc = (Cell) x;
                if (QQ.checkForQuote(xc)) { // (quote e) ?
                    return xc;
                } else if (QQ.checkForQuasiquote(xc)) { // (quasiquote e) ?
                    Object e = ((Cell) xc.cdr).car;
                    x = QQ.expand(e);
                } else {
                    return xc.mapcar((a)-> resolve(a));
                }
            } else {
                return x;
            }
        }
    }
} // QQ
