// H22.08.30/R02.04.29 (鈴)
package arc;

import java.util.List;
import java.util.ArrayList;

/** 評価時例外
 */
public class EvalException extends RuntimeException
{
    private final List<Object> trace = new ArrayList<> ();
    
    /**
     * @param message 例外の説明
     * @param exp 例外を引き起こした Arc 式
     */
    public EvalException (String message, Object exp) {
        super (message + ": " + LL.str(exp));
    }

    /**
     * @param message 例外の説明
     * @param inner 入れ子の例外
     */
    public EvalException (String message, Exception inner) {
        super (message, inner);
    }

    /**
     * @param message 例外の説明
     */
    public EvalException (String message) {
        super (message);
    }

    /** Arc の評価トレースを返す。
     */
    public List<Object> getTrace() {
        return trace;
    }

    /** メッセージに評価トレースを加えた文字列を返す。
     */
    @Override public String toString() {
        var sb = new StringBuilder ();
        sb.append("*** " + getMessage());
        int i = 0;
        for (Object t: trace) {
            sb.append(String.format("\n%3d: %s", i, t));
            i++;
        }
        return sb.toString();
    }
} // EvalException
