// R01.06.08/R02.05.05 (鈴)
// from https://github.com/nukata/little-scheme-in-java - Continuation.java
package arc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;

/** 継続における演算子 */
enum ContOp { // internal
    THEN, DO, ASSIGN, APPLY, APPLY_FUN, EVAL_ARG, CONS_ARGS, RESTORE_ENV,
    EVAL_AGAIN, EVAL_VAL, RESULT_VAL, DEFER, POP_WIND;
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
    private Deque<Step> stack;
    private Cell winds;

    /** 空の継続をつくる。 */
    public Continuation() {
        stack = new ArrayDeque<Step> ();
        winds = null;
    }

    /** 他の継続のコピーをつくる。 */
    public Continuation(Continuation other) {
        stack = new ArrayDeque<Step> (other.stack);
        winds = other.winds;
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

    /** dynamic-wind の thunk1 と thunk3 を winds に push する。
     * 継続に POP_WIND thunk3 を積む。
     */
    void pushWind(Object thunk1, Object thunk3) { // internal
        winds = new Cell(new Cell(thunk1, thunk3), winds);
        push(ContOp.POP_WIND, thunk3);
    }

    /** (thunk1 . thunk3) を winds から pop する。 */
    Cell popWind() {            // internal
        var top = (Cell) winds.car;
        winds = (Cell) winds.cdr;
        return top;
    }

    /** 他の継続のコピーへと切り替える。
     * このとき this.winds の thunk3 が入れ子の内から外の順に，
     * そして other.winds の thunk1 が入れ子の外から内の順に，
     * 継続に DEFER が積まれる。
     */
    void copyFrom(Continuation other) { // internal
        stack = new ArrayDeque<Step> (other.stack);
        if (winds != other.winds) {
            if (winds == null) {
                pushThunk1s(null, other.winds);
            } else if (other.winds == null) {
                pushThunk3s(winds, null);
            } else {
                var others = new HashSet<Cell> ();
                Cell j;
                for (j = other.winds; j != null; j = (Cell) j.cdr)
                    others.add(j);
                for (j = winds; j != null; j = (Cell) j.cdr)
                    if (others.contains(j))
                        break;
                // winds と other.winds は j 以降で共通
                pushThunk1s(j, other.winds);
                pushThunk3s(winds, j);
            }
            winds = other.winds;
        }
    }

    private void pushThunk1s(Cell j, Cell to) {
        if (j != to) {
            var thunk1 = ((Cell) to.car).car;
            push(ContOp.DEFER, thunk1);
            pushThunk1s(j, (Cell) to.cdr);
        }
    }

    private void pushThunk3s(Cell from, Cell j) {
        if (from != j) {
            pushThunk3s((Cell) from.cdr, j);
            var thunk3 = ((Cell) from.car).cdr;
            push(ContOp.DEFER, thunk3);
        }
    }

    /*
      旧継続 from と新継続 to ともに
      (dynamic-wind thunk1 thunk2 thunk3) の thunk1 と thunk3 が
      A (B (C)) のように入れ子になっているとする。

        thunk1A
          thunk1B
            thunk1C
            thunk3C
          thunk3B
        thunk3A

      このとき両継続の winds は下記のように構成されている。

      from: ((_ . thunk3C) (_ . thunk3B) (_ . thunk3A) . nil)
      to:   ((thunk1C . _) (thunk1B . _) (thunk1A . _) . nil)

      from から thunk3C, thunk3B, thunk3A をしてから
      to から   thunk1A, thunk1B, thunk1C をする必要がある。

      そこで stack には下記の順に push する。
　　　　 push thunk1C;
         push thunk1B;
         push thunk1A;
         push thunk3A;
         push thunk3B;
         push thunk3C

      一般に

      from: ****+++++++++
      to:   ****-----

      のように **** が共通基底になっているとき **** を実行する必要はない。
     */
}
