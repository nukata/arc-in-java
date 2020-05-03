// H22.09.29/R02.05.01 (鈴)
package arc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;

/** Arc インタープリタ本体
 */
public class Interp implements IInterp
{
    // シンボルから大域変数値への表
    private final Map<Symbol, Object> symbols = new NameMap<Object> ();

    private CharEnumerator reader; // Arc から (read) するときに使う
    private PrintWriter writer;    // Arc から印字するときに使う
    private PrintWriter errorWriter; // Arc からエラー印字するときに使う

    /** 名前表，ただしキーワードは除外する
     */
    private static class NameMap<T> extends HashMap<Symbol, T>
    {
        @Override public T put(Symbol k, T v) {
            if (k instanceof Symbol.Keyword)
                throw new EvalException ("keyword not expected", k);
            return super.put(k, v);
        }
    } // NameMap<T>

    // コンストラクタ

    /** 入出力に System.in, System.out, System.err を指定して構築する。
     */
    public Interp () {
        this (new CharEnumerator (new LinesFromConsole ("", "", null)),
              new PrintWriter (System.out, true),
              new PrintWriter (System.err, true));
    }

    /** 入力と出力を指定して構築する。
     * @param input  {@link #getReader()} の値となる。
     * @param output {@link #getWriter()} の値となる。
     * @param errOutput {@link #getErrorWriter()} の値となる。
     */
    public Interp (CharEnumerator input,
                   PrintWriter output, PrintWriter errOutput) {
        reader = input;
        writer = output;
        errorWriter = errOutput;
        symbols.put(LL.S_T, LL.S_T);
        symbols.put(Symbol.of("sig"), new Table ());
        symbols.put(LL.S_APPLY, LL.APPLY_VAL);
        symbols.put(Symbol.of("ccc"), LL.CCC_VAL);
    }

    @Override public Map<Symbol, Object> getSymbolTable() {
        return symbols;
    }

    @Override public CharEnumerator getReader() {
        return reader;
    }

    @Override public PrintWriter getWriter() {
        return writer;
    }

    @Override public PrintWriter getErrorWriter() {
        return errorWriter;
    }

    @Override public void load(Intrinsic[] functions) {
        for (var f: functions) {
            var sym = Symbol.of(f.getName());
            symbols.put(sym, f);
        }
    }

    @Override public Object eval(Object x, Cell env) {
        var evl = new Eval(x, env, this);
        return evl.evaluate();
    }

    
    @Override
    public Fn compile(Cell j, Cell env) {
        final Cell arg = j.getCdrCell();
        if (arg == null)
            throw new EvalException ("arglist and body expected");
        final var table = new NameMap<Arg> ();
        final var defaults = new ArrayList<Object> ();
        final var nestedArgs = new ArrayList<Cell> ();
        final boolean hasRest = makeArgTable(arg.car, table, defaults,
                                             nestedArgs);
        final int arity = table.size();

        int fixedArgs = arity;
        final Object[] defaultExps;
        if (defaults.isEmpty()) {
            defaultExps = null;
        } else {
            fixedArgs -= defaults.size();
            defaultExps = defaults.toArray();
        }
        if (hasRest)
            fixedArgs--;

        if (defaultExps != null) // 省略時値の式をコンパイル
            for (int i = 0; i < defaultExps.length; i++)
                defaultExps[i] = compileBody(defaultExps[i], table, arity);

        Cell body = arg.getCdrCell();
        for (Cell cell: nestedArgs) { // 入れ子の引数リストを解決
            var gensym = (Symbol) cell.car; // e.g. $G1
            var nestedList = (Cell) cell.cdr; // e.g. (x y)
            var fn = new Cell (LL.S_FN, new Cell (nestedList, body));
            body = LL.list(LL.list(LL.S_APPLY, fn, gensym));
                                // e.g. ((apply (fn (x y) ...) $G1))
        }
        body = (Cell) compileBody(body, table, arity); // 本体をコンパイル

        if (j.car == LL.S_FN)
            return new Fn (fixedArgs, defaultExps, hasRest, body, env);
        else if (j.car == LL.S_MACRO)
            return new Fn.Macro (fixedArgs, defaultExps, hasRest, body, env);
        else
            throw new UnsupportedOperationException (LL.str(j.car));
    }

    private Object compileBody(Object body, Map<Symbol, Arg> table, int arity)
    {
        if (arity == 0)         // nullary?
            body = QQ.resolve(body);
        else
            body = scanForArgs(body, table);
        body = expandMacros(body, LL.MAX_MACRO_EXPS);
        body = compileInners(body);
        return body;
    }

    @Override
    public Object expandMacros(Object j, final int count) {
        if (j instanceof Cell) {
            Cell jc = (Cell) j;
            Object k = jc.car;
            if (k == LL.S_QUOTE || k == LL.S_FN || k == LL.S_MACRO) {
                return j;
            } else {
                if (k instanceof Symbol && symbols.containsKey(k))
                    k = symbols.get(k);
                if (k instanceof Fn.Macro) {
                    if (count == 0) {
                        throw new EvalException ("macros too nested", jc);
                    } else {
                        Cell jcdr = jc.getCdrCell();
                        Object z = ((Fn.Macro) k).expandWith(jcdr, this);
                        if (count == -1)
                            return z;
                        else
                            return expandMacros(z, count - 1);
                    }
                } else {
                    return jc.mapcar((x)-> expandMacros(x, count));
                }
            }
        } else {
            return j;
        }
    }

    /** 入れ子のラムダ式を Fn インスタンスに置き換える。
     * マクロ式についても同様。環境には NONE を与える。
     * @param j 元の式
     * @return 置き換えた式
     */
    private Object compileInners(Object j) {
        if (j instanceof Cell) {
            Cell jc = (Cell) j;
            Object k = jc.car;
            if (k == LL.S_QUOTE)
                return j;
            else if (k == LL.S_FN || k == LL.S_MACRO)
                return compile(jc, LL.NONE);
            else
                return jc.mapcar((x)-> compileInners(x));
        } else {
            return j;
        }
    }

    /** 仮引数の表を作る。
     * より正確には，仮引数のシンボルをキーとし，
     * そのコンパイル結果の Arg インスタンスを値とする表を作る。
     * @param j  仮引数の並び
     * @param table この仮引数の表に内容が追加される。
     * @param defaults 省略可能引数の省略時値の式が追加される。
     * @param 入れ子の引数を記録するためのリスト
     * @return rest 引数の有無
     */
    private static boolean makeArgTable(Object j,
                                        Map<Symbol, Arg> table,
                                        List<Object> defaults,
                                        List<Cell> nestedArgs) {
        int offset = 0;    // 仮引数に割り当てるフレーム内オフセット値
        while (j instanceof Cell) {
            Cell jc = (Cell) j;
            Object v = jc.car;
            if (v instanceof Cell) { // 入れ子のリストか？
                Cell vc = (Cell) v;
                if (vc.car == LL.S_O) { // 省略可能引数 (o name ...) か？
                    vc = vc.getCdrCell();
                    if (vc == null)
                        throw new LL.VariableExpectedException (v);
                    Object name = vc.car;
                    vc = vc.getCdrCell();
                    if (vc == null) // (o name) なら nil を省略時値にする。
                        defaults.add(null);
                    else if (vc.cdr == null) // (o name expression) ?
                        defaults.add(vc.car);
                    else
                        throw new EvalException
                            ("something after default value", v);
                    v  = name;
                } else {        // 入れ子のリストに仮に名前をつける。
                    if (! defaults.isEmpty())
                        throw new EvalException ("unexpected nested arg", v);
                    Symbol gensym = Symbol.generateUninterned();
                    nestedArgs.add(new Cell (gensym, v));
                    v = gensym;
                }
            } else if (! defaults.isEmpty()) {
                throw new EvalException ("unexpected positional arg", v);
            }
            addArgToTable(v, offset, table);
            offset++;
            j = jc.cdr;
        }
        if (j == null) {
            return false;
        } else {                // rest 引数の表への追加
            addArgToTable(j, offset, table);
            return true;
        }
    }

    private static void addArgToTable(Object j, int offset,
                                      Map<Symbol, Arg> table) {
        Symbol sym;
        if (j instanceof Symbol) {
            sym = (Symbol) j;
        } else if (j instanceof Arg) {
            sym = ((Arg) j).symbol;
        } else {
            throw new LL.VariableExpectedException (j);
        }
        table.put(sym, new Arg (0, offset, sym));
    }

    /** 仮引数を求めて式をスキャンする。
     * 準引用式を等価な式へと展開してから，
     * 式の中のシンボルのうち仮引数表に該当があるものを Arg 値に置き換える。
     * もともと Arg 値だったものについては，そのシンボルを表から探し，
     * あれば表の Arg 値に置き換える (入れ子の同名の変数だったら，最内
     * のものとみなす)。なければ，Arg 値のレベルを 1 だけ上げる (入れ子
     * の外の変数とみなす)。
     * @param j スキャン対象となる式
     * @param 仮引数表
     * @param スキャンして置換した結果の式
     * @see QQ#resolve(Object)
     */
    private static Object scanForArgs(Object j,
                                      final Map<Symbol, Arg> table) {
        for (;;) {
            if (j instanceof Symbol) {
                Arg k = table.get(j);
                return (k == null) ? j : k;
            } else if (j instanceof Arg) {
                Arg ja = (Arg) j;
                Arg k = table.get(ja.symbol);
                return (k == null) ?
                    new Arg (ja.level + 1, ja.offset, ja.symbol) : k;
            } else if (j instanceof Cell) {
                Cell jc = (Cell) j;
                if (QQ.checkForQuote(jc)) { // (quote e) ?
                    return jc;
                } else if (QQ.checkForQuasiquote(jc)) { // (quasiquote e) ?
                    Object e = ((Cell) jc.cdr).car;
                    j = QQ.expand(e);
                } else {
                    return jc.mapcar((x)-> scanForArgs(x, table));
                }
            } else {
                return j;
            }
        }
    }
} // Interp
