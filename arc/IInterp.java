// H22.09.22/R02.05.01 (鈴)
package arc;

import java.util.Map;
// import java.io.IOException;
import java.io.PrintWriter;

/** Arc インタープリタ本体のインタフェース
 */
public interface IInterp
{
    /** シンボルから大域変数値への表を得る。
     */
    Map<Symbol, Object> getSymbolTable();

    /** Arc から入力するときに使う読み取り器を得る。
     */
    CharEnumerator getReader();

    /** Arc から印字するときに使う書き込み器を得る。
     */
     PrintWriter getWriter();

    /** Arc からエラー印字するときに使う書き込み器を得る。
     */
     PrintWriter getErrorWriter();

    /** Java で書かれた関数をロードする。
     * @param functions Java で書かれた関数の集合体。各要素がそれぞれ
     *   {@link Intrinsic#getName()} の値を名前とする大域変数として登録される。
     * @see #getSymbolTable()
     */
    void load(Intrinsic[] functions);

    /** Arc 式を評価する。
     * @param exp 評価される Arc 式
     * @param env 評価するときに使うローカル環境 (null ならば大域変数だけ使う)
     * @return 評価結果
     * @throws EvalException 評価中に例外が発生した。
     */
    Object eval(Object exp, Cell env) throws EvalException;

    /** Arc のリスト (macro ...) または (fn ...) をコンパイルして 
     * Fn.Macro または Fn のインスタンスを作る。このとき，
     * 仮引数を解決する前に準引用を解決し，
     * 仮引数を解決した後にマクロを展開する。
     * @param j コンパイルすべき Arc のリスト
     * @param env 現在の環境
     * @return コンパイルした式
     */
    Fn compile(Cell j, Cell env);

    /** 式の中のマクロを展開する。デバッグ用。
     * 束縛変数の名前解決と連係せずにこれを呼び出した展開結果は必ずしも
     * 評価可能とは限らない。また，その展開結果を評価するとき，本来は
     * 回避されていた名前の衝突が発生するかもしれない。
     * @param exp 対象となる式
     * @param count ここで展開できる入れ子深さの残り。
     *   <ul>
     *   <li> 0 以上の任意の数 n を与えた時は高々 n 重に展開する。
     *        展開し切れないときは {@link EvalException} を発生させる。
     *   <li> -1 を与えたときは 1 重に展開する。
     *        展開し切れなくてもそのまま結果の式を返す。
     *   <li> -2 以下の数を与えたときの動作は定義しない。
     *   </ul>
     * @return 展開結果の式
     */
    Object expandMacros(Object exp, final int count);

    /** 文字列に書かれたスクリプトを評価する。
     * @param text ここに書かれた式を次々と評価する。
     * @return 最後に書かれた式の評価結果
     */
    default public Object run(String text) throws Exception {
        var script = new LinesFromString (text);
        return run(script, null);
    }

    /** スクリプトを評価する。
     * @param script ここに書かれている式が次々と評価される。
     *   正常，異常にかかわらずメソッドが終わるとき close される。
     * @param receiver 
     *   もしも null でなければ，スクリプトのトップレベルの式の評価が終わる
     *   たびに評価結果を引数として {@link IReceiver#receiveResult} が
     *   呼び出される。
     *   評価時に {@link EvalException} が発生したときは，それを引数として
     *   {@link IReceiver#receiveException} が呼び出される。
     * @return トップレベルの最後の式の評価結果
     */
    default Object run(ILines script, IReceiver receiver) throws Exception {
        try (var chars = new CharEnumerator (script)) {
            var ar = new ArcReader (chars);
            Object result = null;
            for (;;) {
                try {
                    Object x = ar.read();
                    if (x == LL.EOF)
                        return result;
                    result = eval(x, null);
                    if (receiver != null)
                        receiver.receiveResult(result);
                } catch (EvalException ex) {
                    if (receiver != null)
                        receiver.receiveException(ex);
                    else
                        throw ex;
                }
            }
        }
    }

    /** スクリプト評価時の結果や例外の受信器
     * @see #run
     */
    interface IReceiver
    {
        /** 結果を受信する。
         * スクリプト評価時，トップレベルの各式の評価結果を受け止める。
         */
        void receiveResult(Object result);

        /** 例外を受信する。
         * スクリプト評価時，トップレベルの各式の評価で発生した例外を
         * 受け止める。
         */
        void receiveException(EvalException ex);
    } // IReceiver
} // IInterp
