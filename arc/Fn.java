// H22.09.28/R02.05.02 (鈴)
package arc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** コンパイル後の (fn ...) 
 */
public class Fn extends Function
{
    /** 関数本体。実体は Arc 式からなるリスト。中の変数名は解決済み。*/
    final Cell body;

    /** 関数本体をとりまく環境。
        関数本体のなかに出現する大域変数以外の自由変数の値を与える。*/
    final Cell env;

    /** 一から作る。*/
    public Fn (int fixedArgs, Object[] defaultExps, boolean hasRest,
               Cell body, Cell env) {
        super (fixedArgs, defaultExps, hasRest);
        this.body = body;
        this.env = env;
    }

    /** 環境だけ新しく与えて，コピーを作る。*/
    public Fn (Fn orig, Cell env) {
        this (orig.fixedArgs, orig.defaultExps, orig.hasRest, orig.body, env);
    }

    /** 環境だけ新しく与えて，自分のコピーを作る。*/
    public Fn copyWith(Cell newEnv) {
        return new Fn (this, newEnv);
    }

    String reprHead() {
        return "fn";
    }

    String repr(int recLevel, Set<Object> printed) {
        var sb = new StringBuilder ();
        sb.append("#<" + reprHead() + ":" + carity());
        if (defaultExps == null) {
            if (env != null)
                sb.append("::" + LL.str(env, true, recLevel, printed));
        } else {
            sb.append(":" + LL.str(defaultExps, true, recLevel, printed));
            if (env != null)
                sb.append(":" + LL.str(env, true, recLevel, printed));
        }
        if (recLevel > 2)
            recLevel = 2;
        sb.append(":" + LL.str(body, true, recLevel, printed));
        sb.append(">");
        return sb.toString();
    }

    @Override public String toString() {
        return LL.str(this);
    }


    /** コンパイル後の (macro ...)
     */
    public static class Macro extends Fn
    {
        /** マクロを展開する。
         * @param list 実引数の並び. 各引数は評価されない。
         * @param interp マクロの本体の各式を評価するためのインタープリタ
         * @return 展開結果
         */
        public Object expandWith(Cell list, IInterp interp) {
            Object[] frame = makeFrame(list);
            Cell newEnv = (frame == null) ? env : new Cell(frame, env);
            if (defaultExps != null)
                evalDefaults(frame, interp, newEnv);
            Cell x = new Cell(LL.S_DO, this.body); // (_do e1 e2 ...)
            return interp.eval(x, newEnv);
        }

        /** 一から作る。*/
        public Macro (int fixedArgs, Object[] defaultExps, boolean hasRest,
                      Cell body, Cell env) {
            super (fixedArgs, defaultExps, hasRest, body, env);
        }

        /** 環境だけ新しく与えて，コピーを作る。*/
        public Macro (Macro orig, Cell env) {
            super (orig, env);
        }

        /** 環境だけ新しく与えて，自分のコピーを作る。*/
        @Override public Fn copyWith(Cell newEnv) {
            return new Macro (this, newEnv);
        }

        @Override String reprHead() {
            return "macro";
        }
    } // Macro
} // Fn
