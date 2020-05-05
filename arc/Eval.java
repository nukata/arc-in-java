// H22.09.29/R02.05.05 (鈴)
// cf. https://github.com/nukata/little-scheme-in-java - Eval.java
package arc;

/** 左辺値を表す。与えられた値を所定の変数に代入する。 */
@FunctionalInterface interface Setter {
    void set(Object args);
}

/** Arc 式の評価器 */
public class Eval {
    Object exp;
    Cell env;
    Continuation k;
    final IInterp interp;

    /** 式 exp を環境 env のもとで評価する評価器を作る。
     */
    public Eval(Object exp, Cell env, IInterp interp) {
        this.exp = exp;
        this.env = env;
        this.interp = interp;
        this.k = new Continuation();
    }

    /** この評価器が属するインタープリタ */
    public IInterp getInterp() {
        return interp;
    }

    /** 与えられた式を与えられた環境で評価する。
     */
    public Object evaluate() {
        var symbols = interp.getSymbolTable();
        try {
            for (;;) {
                for (;;) {
                    if (exp instanceof Symbol) {
                        if (symbols.containsKey(exp))
                            exp = symbols.get(exp);
                        else if (! (exp instanceof Symbol.Keyword))
                            throw new EvalException ("void variable", exp);
                        break;
                    } else if (exp instanceof Arg) {
                        exp = ((Arg) exp).getValue(env);
                        break;
                    } else if (exp instanceof Cell) {
                        Cell xc = (Cell) exp;
                        Object fn = xc.car;
                        Cell arg = xc.getCdrCell();
                        if (fn instanceof Symbol.Keyword) {
                            if (fn == LL.S_QUOTE) { // (quote e)
                                if (arg == null || arg.cdr != null)
                                    throw new EvalException ("bad quote");
                                exp = arg.car;
                                break;
                            } else if (fn == LL.S_IF) { // (if c1 e1 [c2...en])
                                exp = arg.car;
                                k.push(ContOp.THEN, arg.cdr);
                            } else if (fn == LL.S_DO) { // (_do e1 e2 ...)
                                if (arg == null) {
                                    exp = null;
                                    break;
                                } else {
                                    exp = arg.car;
                                    if (arg.cdr != null)
                                        k.push(ContOp.DO, arg.cdr);
                                }
                            } else if (fn == LL.S_ASSIGN) { // (assign v e)
                                Object lval = arg.car;
                                Setter setter;
                                if (lval instanceof Symbol) {
                                    var sym = (Symbol) lval;
                                    setter = (x)-> symbols.put(sym, x);
                                } else if (lval instanceof Arg) {
                                    var arg1 = (Arg) lval;
                                    var env1 = env;
                                    setter = (x)-> arg1.setValue(x, env1);
                                } else {
                                    throw new LL.VariableExpectedException
                                        (lval);
                                }
                                Cell j = arg.getCdrCell();
                                if (j == null || j.cdr != null)
                                    throw new EvalException
                                        ("one RHS expected");
                                exp = j.car;
                                k.push(ContOp.ASSIGN, setter);
                            } else if (fn == LL.S_FN || fn == LL.S_MACRO) {
                                exp = interp.compile(xc, env);
                                break;
                            } else if (fn == LL.S_QUASIQUOTE) {
                                if (arg == null || arg.cdr != null)
                                    throw new EvalException ("bad quasiquote");
                                exp = QQ.expand(arg.car);
                            } else {
                                throw new EvalException ("bad keyword", fn);
                            }
                        } else {    // (fun arg...)
                            exp = fn;
                            k.push(ContOp.APPLY, arg);
                        }
                    } else if (exp instanceof Fn) {
                        Fn f = (Fn) exp;
                        if (f.env == LL.NONE) // まだ評価前のラムダ式ならば
                            exp = f.copyWith(env); // 現時点の環境を捕捉する
                        break;
                    } else {
                        break;      // 数や文字列や null など
                    }
                }
                LOOP2:
                for (;;) {
                    if (k.isEmpty())
                        return exp;
                    Step step = k.pop();
                    Object x = step.val;
                    switch (step.op) {
                    case THEN: // x は (e1 c2 e2 .. en) か (e1 en) か (e1)
                        {  
                            Cell c = (Cell) x;
                            if (exp != null) {
                                // (if t e1 ..) => e1
                                exp = c.car;
                                break LOOP2;
                            } else {
                                c = c.getCdrCell();
                                if (c == null) {
                                    // (if nil e1) => nil
                                    exp = null;
                                    break;
                                } else if (c.cdr == null) {
                                    // (if nil e1 en) => en
                                    exp = c.car;
                                    break LOOP2;
                                } else {
                                    // (if nil e1 c2 e2 ..) => (if c2 e2 ..)
                                    exp = c.car;
                                    k.push(ContOp.THEN, c.cdr);
                                    break LOOP2;
                                }
                            }
                        }
                    case DO:     // x は (e ...)
                        {
                            Cell c = (Cell) x;
                            if (c.cdr != null)
                                k.push(ContOp.DO, c.cdr);
                            exp = c.car;
                            break LOOP2;
                        }
                    case ASSIGN: // exp を代入関数 x で変数に代入する
                        {
                            var setter = (Setter) x;
                            setter.set(exp);
                            break;
                        }
                    case APPLY: // exp は評価済み関数, x は未評価の引数列
                        if (exp instanceof Fn.Macro) {
                            k.push(ContOp.EVAL_AGAIN, null);
                            applyFunction(exp, (Cell) x);
                            break;
                        } else if (x == null) {
                            applyFunction(exp, null);
                            break;
                        } else {
                            k.push(ContOp.APPLY_FUN, exp);
                            Cell c = (Cell) x;
                            exp = c.car;
                            pushArgs(c.getCdrCell());
                            k.push(ContOp.CONS_ARGS, null);
                            break LOOP2;
                        }
                    case CONS_ARGS: // exp は新しく評価した引数
                        {           // x は評価済みの引数列
                            Cell args = new Cell(exp, x);
                            step = k.pop();
                            exp = step.val;
                            switch (step.op) {
                            case EVAL_ARG: // exp は次に評価すべき引数
                                k.push(ContOp.CONS_ARGS, args);
                                break LOOP2;
                            case APPLY_FUN: // exp は評価済み関数
                                args = nreverse(args);
                                applyFunction(exp, args);
                                break;
                            default:
                                throw new EvalException ("bad op: " + step.op);
                            }
                            break;
                        }
                    case RESTORE_ENV: // 現在の環境を捨てて x を環境とする
                        env = (Cell) x;
                        break;
                    case EVAL_AGAIN: // 現在の exp を再び評価にかける
                        break LOOP2;
                    case EVAL_VAL: // 現在の exp を捨てて x を評価にかける
                        exp = x;
                        break LOOP2;
                    case RESULT_VAL: // 現在の exp を捨てて x を結果とする
                        exp = x;
                        break;
                    case POP_WIND:
                        {
                            Cell w = k.popWind();
                            if (x != w.cdr)
                                throw new RuntimeException
                                    ("bad wind " + LL.str(w) + ": " +
                                     LL.str(x));
                        }
                        // POP_WIND はそのまま DEFER へと続く。
                    case DEFER: // 現在の exp を結果としつつ x を呼び出す
                        if (x != null) {
                            k.push(ContOp.RESULT_VAL, exp);
                            Cell c = (Cell) x;
                            applyFunction(c.car, (Cell) c.cdr);
                        }
                        break;
                    default:
                        throw new EvalException ("unexpected op: " + step.op);
                    }
                }
            }
        } catch (RuntimeException ex) {
            var x = (ex instanceof EvalException) ?
                ((EvalException) ex) :
                new EvalException(ex.toString(), ex);
            x.getTrace().add(LL.str(exp) + " in " + LL.str(env)
                             + "\n\t" + LL.str(k));
            throw x;
        } finally {
            // 継続に含まれる winds 呼び出しを実行する。
            // この呼び出しは環境非依存であることを仮定する。
            if (! k.isEmpty()) {
                k.copyFrom(new Continuation());
                exp = null;
                evaluate(); // XXX DEFER で ccc の継続を呼び出したら？
            }
        }
    }


    // (a b c) => k.push(EVAL_ARG, c); k.push(EVAL_ARG, b); k.push(EVAL_ARG, a)
    // arc.arc の pr 関数の実装は実引数が左から順に評価されることを仮定して
    // いるから，スタックには逆順に評価前実引数を積み上げる。
    private void pushArgs(Cell j) {
        if (j != null) {
            pushArgs(j.getCdrCell());
            k.push(ContOp.EVAL_ARG, j.car);
        }
    }

    // 実引数列が逆順に並んでいるから破壊的に正順に直す。
    private static Cell nreverse(Cell j) {
        Cell temp = null;
        Cell prev = null;
        Cell curr = j;
        while (curr != null) {
            temp = (Cell) curr.cdr;
            curr.cdr = prev;
            prev = curr;
            curr = temp;
        }
        return prev; // 今や先頭要素となった car を持つ cons セルを返す
    }

    // (a b (c d e)) => (a b c d e)
    private static Cell constructApplyArg(Cell j) {
        if (j.cdr == null)
            return (Cell) j.car;
        else
            return new Cell(j.car, constructApplyArg((Cell) j.cdr));
    }

    // 評価済み関数 fun を評価済み引数列 arg に適用する。
    private void applyFunction(Object fun, Cell arg) {
        for (;;) {
            if (fun == LL.CCC_VAL) {
                k.pushRestoreEnv(env);
                fun = arg.car;
                var cont = new Continuation(k);
                arg = new Cell(cont, null);
            } else if (fun == LL.APPLY_VAL) {
                fun = arg.car;
                arg = constructApplyArg((Cell) arg.cdr);
            } else {
                break;
            }
        }
        if (fun instanceof Function) {
            Object[] frame = ((Function) fun).makeFrame(arg);
            if (fun instanceof Fn) {
                Fn fn = (Fn) fun;
                k.pushRestoreEnv(env);
                env = (frame == null) ? fn.env : new Cell(frame, fn.env);
                Cell body = fn.body;
                if (body != null)
                    k.push(ContOp.DO, body);
                if (fn.defaultExps != null)
                    pushEvalDefaults(fn, frame);
                exp = null;
                // System.err.println("k = " + LL.str(k)); // DEBUG
                // System.err.println("env = " + LL.str(env)); // DEBUG
            } else {
                exp = ((Intrinsic) fun).call(frame, this);
            }
        } else if (fun instanceof Continuation) {
            k.copyFrom((Continuation) fun);
            exp = arg.car;
        } else {
            exp = evalElementAccess(fun, arg);
        }
    }

    // フレーム内の省略時値の評価を継続に追加する。
    // 省略時値が左から順に評価されるようにスタックする。
    private void pushEvalDefaults(Fn fn, Object[] frame) {
        for (int i = fn.defaultExps.length - 1; i >= 0; i--) {
            int index = fn.fixedArgs + i;
            var e = frame[index];
            if (e instanceof Fn.Default) {
                Setter setter = (result)-> frame[index] = result;
                k.push(ContOp.ASSIGN, setter);
                var val = ((Fn.Default) e).value;
                k.push(ContOp.EVAL_VAL, val);
            }
        }
    }

    // ("abc" 0) => #\a
    private static Object evalElementAccess(Object x, Cell arg) {
        if (arg != null && arg.cdr == null) {
            Object index = arg.car;
            if (x == null) {  // NB (car nil) => nil, (cdr nil) => nil
                if (index instanceof Integer)
                    return null;
            } else if (x instanceof Cell) {
                Cell xc = (Cell) x;
                if (index instanceof Integer) {
                    int n = (Integer) index;
                    for (int i = 0; i < n; i++) {
                        xc = xc.getCdrCell();
                        if (xc == null)
                            return null;
                    }
                    return xc.car;
                }
            } else if (x instanceof char[]) {
                char[] s = (char[]) x;
                if (index instanceof Integer) {
                    int i = (Integer) index;
                    try {
                        return s[i];
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new EvalException
                            ("index " + i + " for " + LL.str(s));
                    }
                }
            } else if (x instanceof Table) {
                return ((Table) x).get(index);
            }
        }
        throw new EvalException ("not applicable", x);
    }
} // Eval
