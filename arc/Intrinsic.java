// H22.09.06/R02.05.02 (鈴)
package arc;

/** 組込み関数 */
public class Intrinsic extends Function
{
    /** 関数名 */
    final String name;

    /** ドキュメンテーション文字列 */
    final String doc;

    /** Java による関数の本体を呼び出す。
     * 関数が rest 引数をとる (つまり hasRest() が true) ならば，
     * args の最後の要素として rest 引数が Arc のリストで渡される。
     * つまり rest 引数の個数が 0 ならば null が渡され，
     * 1 以上ならば Cell インスタンスが渡される。
     * <p>
     * 任意の例外を投げてよい。
     * @see Function#makeFrame(Cell)
     */
    @FunctionalInterface public static interface Body {
        Object call(Object[] args) throws Exception;
    }

    /** Java による関数の本体を呼び出す。
     * Arc 式の評価器を追加の引数として取る以外は Body と同じ。
     */
    @FunctionalInterface public static interface Body2 {
        Object call(Object[] args, Eval eval) throws Exception;
    }

    final Body body;

    final Body2 body2;

    /** オプションなしで構築する。
     * @param name 関数名
     * @param arity 固定引数の個数
     * @param doc ドキュメンテーション文字列
     * @param body 関数本体
     */
    public Intrinsic (String name, int arity, String doc, Body body) {
        super (arity, null, false);
        this.name = name;
        this.doc = doc;
        this.body = body;
        this.body2 = null;
    }

    /** フルオプションで構築する。
     * ただし body と body2 はどちらかを null とすること。
     * @param fixedArgs 固定引数の個数
     * @param defaultExps 省略可能引数の省略時値からなる配列 または null
     * @param hasRest  rest をとるならば true
     * @param name 関数名
     * @param doc ドキュメンテーション文字列
     * @param body 関数本体
     * @param body2 関数本体
     */
    public Intrinsic (int fixedArgs, Object[] defaultExps, boolean hasRest, 
                      String name, String doc, Body body, Body2 body2) {
        super (fixedArgs, defaultExps, hasRest);
        this.name = name;
        this.doc = doc;
        this.body = body;
        this.body2 = body2;
    }

    /** Body を呼び出す。Body がなければ Body2 を呼び出す。
     * 例外が発生したらキャッチしてEvalException でラップする。
     * 省略可能引数に対して省略時値が使われたときは，まずそれを順に評価する。
     */
    public Object call(Object[] args, Eval eval) {
        if (defaultExps != null)
            evalDefaults(args, eval.interp, eval.env);
        try {
            if (body != null)return body.call(args);
            return body2.call(args, eval);
        } catch (Exception ex) {
            var sb = new StringBuilder ();
            sb.append(ex + " -- " + this + " " + LL.str(args));
            String here = Intrinsic.class.getName();
            for (var e: ex.getStackTrace()) {
                sb.append("\n  @ ");
                sb.append(e.toString());
                if (here.equals(e.getClassName()))
                    break;
            }
            throw new EvalException (sb.toString(), ex);
        }
    }

    /** 関数名を返す。 */
    public final String getName() {
        return name;
    }

    @Override public String toString() {
        return String.format("#<%s:%d>", name, carity());
    }
} // Intrinsic
