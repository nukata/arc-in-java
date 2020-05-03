// R01.06.08/R02.05.03 (鈴)
// from https://github.com/nukata/little-scheme-in-java - Continuation.java
package arc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 継続における演算子 */
enum ContOp { // internal
    THEN, DO, ASSIGN, APPLY, APPLY_FUN, EVAL_ARG, CONS_ARGS, RESTORE_ENV,
    EVAL_AGAIN, EVAL_VAL, RESULT_VAL, DEFER;
}

/** 継続における１ステップ */
class Step { // internal
    final ContOp op;
    final Object val;

    Step(ContOp op, Object val) {
        this.op = op;
        this.val = val;
    }
}

/** ステップのスタックとして構成された Arc の継続 */
public class Continuation {
    private final Deque<Step> stack;

    /** 空の継続をつくる。 */
    public Continuation() {
        stack = new ArrayDeque<Step> ();
    }

    /** 他の継続のコピーをつくる。 */
    public Continuation(Continuation other) {
        stack = new ArrayDeque<Step> (other.stack);
    }

    /** この継続にステップが無ければ真。 */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** この継続のステップ数 */
    public int size() {
        return stack.size();
    }

    @Override public String toString() {
        var ss = new ArrayList<String> ();
        for (Step step: stack)
            ss.add(step.op + " " + LL.str(step.val));
        return "#<" + String.join("\n\t  ", ss) + ">";
    }

    /** 継続の末尾に１ステップを加える。 */
    void push(ContOp op, Object value) { // internal
        stack.push(new Step(op, value));
    }

    /** 継続の末尾から１ステップを取り出す。 */
    Step pop() {                // internal
        return stack.pop();
    }

    /** 末尾呼び出しでなければ継続の末尾に RESTORE_ENV を加える。 */
    void pushRestoreEnv(Cell env) { // internal
        Step last = stack.peek();
        if (last == null || last.op != ContOp.RESTORE_ENV)
            push(ContOp.RESTORE_ENV, env);
    }
}
