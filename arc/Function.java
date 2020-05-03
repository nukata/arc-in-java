// H22.09.08/R02.05.01 (鈴)
package arc;

/** Arc から普通に使われる関数の共通基底クラス */
public abstract class Function
{
    /** 引数の個数 (＝フレームの大きさ)。
     * 固定引数と省略可能引数をあわせた個数。rest があればさらに 1 を足す。*/
    private final int arity;

    /** 固定引数の個数 */
    final int fixedArgs;

    /** 省略可能引数の省略時値の式からなる配列 (または null) */
    final Object[] defaultExps;

    /** rest があれば真 */
    final boolean hasRest;

    /**
     * 固定引数の個数と，省略可能引数のデフォルト値の式の配列と
     * rest の有無を指定する。
     */
    public Function (int fixedArgs, Object[] defaultExps, boolean hasRest) {
        this.fixedArgs = fixedArgs;
        this.defaultExps = defaultExps;
        this.hasRest = hasRest;
        int n = fixedArgs;
        if (defaultExps != null)
            n += defaultExps.length;
        if (hasRest)
            n += 1;
        this.arity = n;
    }

    /** 引数の個数を (rest 引数があるときは符号を反転して) 返す。
     */
    public final int carity() { // combined arity: 名前に深い意味はない
        return (hasRest) ? -arity : arity;
    }

    /** 実引数の並びからローカル変数のフレームを作る。
     * ただし，nullary ならば何もせずに null を返す。
     * 省略可能引数に対する省略時値は Default でラップする。
     * @param list Arc のリストによる実引数の並び
     * @return 新しいローカル変数のフレーム。
     * @throws EvalException 引数の個数不一致などの形式上の誤りがあった。
     */
    public final Object[] makeFrame(Cell list)
        throws EvalException
    {
        if (arity == 0) {       // nullary?
            if (list != null)
                throw new EvalException ("no args expected", list);
            return null;
        }
        Object[] frame = new Object[arity];
        Cell c = list;
        for (int i = 0; i < fixedArgs; i++) { // 固定引数
            if (c == null) {
                String msg = argOrArgs(fixedArgs) + " expected" + atLeast()
                    + "for " + shortName();
                throw new EvalException (msg, list);
            }
            frame[i] = c.car;
            c = c.getCdrCell();
        }
        if (defaultExps != null)
            for (int i = 0; i < defaultExps.length; i++) { // 省略可能引数
                if (c == null) {
                    frame[i + fixedArgs] = new Default (defaultExps[i]);
                } else {
                    frame[i + fixedArgs] = c.car;
                    c = c.getCdrCell();
                }
            }
        if (hasRest) {          // rest 引数
            frame[arity - 1] = c;
            c = null;
        }
        if (c != null) {
            String msg = argOrArgs(arity) + " expected" + atMost()
                + "for " + shortName();
            throw new EvalException (msg, list);
        }
        return frame;          // これがローカル変数のフレームになる。
    }

    private String argOrArgs(int n) {
        return (n == 1) ? "1 arg" : (n + " args");
    }

    private String atLeast() {
        return (arity == fixedArgs) ? " " : " at least ";
    }

    private String atMost() {
        return (arity == fixedArgs) ? " " : " at most ";
    }

    private String shortName() {
        String s = toString();
        if (s.length() > 30)
            s = s.substring(0, 30) + "...";
        return s;
    }

     /** もしあればフレーム内の省略時値の式を評価する。
      * @param frame ローカル変数のフレーム
      * @param interp 評価に使われるインタープリタ
      * @param env 評価に使われる環境。関数本体と同じ環境を与える。
      * @throws EvalException 各式の評価時に発生した例外
      */
    public final void evalDefaults(Object[] frame, IInterp interp, Cell env)
        throws EvalException
    {
        if (defaultExps != null) {
            for (int i = 0; i < defaultExps.length; i++) {
                Object e = frame[i + fixedArgs];
                if (e instanceof Default) {
                    e = ((Default) e).value;
                    e = interp.eval(e, env);
                    frame[i + fixedArgs] = e;
                }
            }
        }
    }

    /** 省略時値を示すためのラッパ。
     * 例えば (fn (x (o y x) (o z y)) (list x y z)) では，省略時値の式を
     * 本体と同じようにコンパイルし，本体と同じように順に評価する必要がある。
     */
    protected static class Default {
        final Object value;

        Default (Object value) {
            this.value = value;
        }
    } // Default
} // Function
