// H22.09.30/R02.05.05 (鈴)
package arc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/** (outstring) を実装するクラス
 */
class StringPrintWriter extends PrintWriter
{
    StringPrintWriter () {
        super (new StringWriter ());
    }

    /** これまでこれに出力された内容の新しいコピーを返す。*/
    char[] inside() {
        StringWriter w = (StringWriter) out;
        StringBuffer buf = w.getBuffer();
        return LL.toCharArray(buf);
    }
} // StringPrintWriter


/** 組込み関数の集合体
 */
public class Builtins extends BuiltinUtil
{
    /** このクラスはインスタンスを作らない。*/
    private Builtins () {}

    /** atomic-invoke のための排他ロック */
    static final ReentrantLock LOCK = new ReentrantLock();

    /** atomic-invoke の排他ロックのための式 */
    static final Cell DO_LOCK =
        new Cell(c("_lock", 0, null,
                   (a)-> {
                       LOCK.lock();
                       return null;
                   }), null);

    /** atomic-invoke の排他ロック解除のための式 */
    static final Cell DO_UNLOCK =
        new Cell(c("_unlock", 0, null,
                   (a)-> {
                       LOCK.unlock();
                       return null;
                   }), null);

    /** call-w/stdin, call-w/stdout のための隠し組み込み関数 */
    static final Intrinsic PUTSYMVAL =
        c("_putsymval", 2, null,
          (a, eval)-> {
              var symbols = eval.interp.getSymbolTable();
              var sym = (Symbol) a[0];
              var val = a[1];
              symbols.put(sym, val);
              return null;
          });

    static private Intrinsic _F1 = null;

    /** maptable のループのための隠し組み込み関数 */
    static final Intrinsic MAPTABLE1 =
        c("_maptable1", 2, null,
          (a, eval)-> {
              @SuppressWarnings("unchecked")
              var iter = (Iterator<Map.Entry<Object, Object>>) a[0];
              var fun = a[1];
              if (iter.hasNext()) {
                  var entry = iter.next();
                  var key = new Cell(LL.S_QUOTE,
                                     new Cell(entry.getKey(), null));
                  var val = new Cell(LL.S_QUOTE,
                                     new Cell(entry.getValue(), null));
                  eval.k.push(ContOp.EVAL_VAL, LL.list(_F1, iter, fun));
                  eval.k.push(ContOp.EVAL_VAL, LL.list(fun, key, val));
              }
              return null;
          });

    static {
        _F1 = MAPTABLE1;
    }

    /** 省略時値としての (stdout) からなる配列  */
    static final Object[] DEFAULTS_TO_STDOUT = new Object[] {
        LL.list(Symbol.of("stdout"))
    };

    private static Intrinsic c(String name, int arity, String doc,
                               Intrinsic.Body body) {
        return new Intrinsic (name, arity, doc, body);
    }

    private static Intrinsic c(String name, int arity, String doc,
                               Intrinsic.Body2 body2) {
        return new Intrinsic (arity, null, false, name, doc, null, body2);
    }

    private static Intrinsic c(String name, int fixedArgs,
                               Object[] defaultExps, boolean hasRest, 
                               String doc, Intrinsic.Body body) {
        return new Intrinsic (fixedArgs, defaultExps, hasRest,
                              name, doc, body, null);
    }

    private static Intrinsic c(String name, int fixedArgs,
                               Object[] defaultExps, boolean hasRest,
                               String doc, Intrinsic.Body2 body2) {
        return new Intrinsic (fixedArgs, defaultExps, hasRest,
                              name, doc, null, body2);
    }

    /** 組込み関数からなる配列 */
    public static final Intrinsic[] FUNCTIONS = new Intrinsic[] {
        c("car", 1, "(car '(a b c)) => a; (car nil) => nil",
          (a)-> (a[0] == null) ? null : ((Cell) a[0]).car),
        
        c("cdr", 1, "(cdr '(a b c)) => (b c); (cdr nil) => nil",
          (a)-> (a[0] == null) ? null : ((Cell) a[0]).cdr),

        c(LL.S_CONS.name, 2, "(cons 'a '(b c)) => (a b c)",
          (a)-> new Cell (a[0], a[1])),

        c("is", 2, "(is x y) => 同じ整数/実数/文字/文字列/参照か？",
          (a)-> isSame(a[0], a[1]) ? LL.S_T : null),

        c("type", 1, "(type x) => 型を表すシンボル",
          (a)-> {
              Object x = a[0];
              return
                  (x == null) ? LL.S_SYM :
                  (x instanceof Cell) ? LL.S_CONS :
                  (x instanceof Symbol.Keyword) ? LL.S_MAC :
                  (x instanceof Symbol) ? LL.S_SYM :
                  (x instanceof Fn.Macro) ? LL.S_MAC :
                  (x instanceof Function) ? LL.S_FN :
                  (x instanceof Character) ? LL.S_CHAR :
                  (x instanceof char[]) ? LL.S_STRING :
                  (x instanceof Integer) ? LL.S_INT :
                  (x instanceof BigInteger) ? LL.S_INT :
                  (x instanceof Number) ? LL.S_NUM :
                  (x instanceof PrintWriter) ? LL.S_OUTPUT :
                  (x instanceof CharEnumerator) ? LL.S_INPUT :
                  (x instanceof Table) ? LL.S_TABLE :
                  (x instanceof Arg) ? LL.S_SYM : // XXX
                  x.getClass();                   // XXX
          }),
          
        c("bound", 1, "(bound name) => name が大域的に束縛されているか？",
          (a, eval)-> {
              var symbols = eval.interp.getSymbolTable();
              var key = (Symbol) a[0];
              return (symbols.containsKey(key)) ? LL.S_T : null;
          }),

        c(LL.S_LIST.name, 0, null, true,
          "(" + LL.S_LIST + " 'a 'b ...) => (a b ...)",
          (a)-> a[0]),

        c("table", 0, "(table) =>  a new table",
          (a)-> new Table ()),

        c("newstring", 1, new Object[] { '\0' }, false,
          "(newstring length [char]) => a new string",
          (a)-> {
              int n = (Integer) a[0];
              char ch = (Character) a[1];
              char[] s = new char[n];
              for (int i = 0; i < n; i++)
                  s[i] = ch;
              return s;
          }),

        c("stdin", 0, "(stdin) => input-port",
          (a, eval)-> eval.interp.getReader()),

        c("stdout", 0, "(stdout) => output-port",
          (a, eval)-> eval.interp.getWriter()),

        c("stderr", 0, "(stderr) => output-port",
          (a, eval)-> eval.interp.getErrorWriter()),

        c("call-w/stdin", 2, "(call-w/stdin input-port nullary-function)",
          (a, eval)-> {
              var symbols = eval.interp.getSymbolTable();
              var sym = Symbol.of("stdin");
              var old_val = symbols.get(sym);
              var port = a[0];
              var new_val = c("_tmp_stdin", 0, null, (x)-> port);
              symbols.put(sym, new_val);
              eval.k.pushWind(LL.list(PUTSYMVAL, sym, new_val),
                              LL.list(PUTSYMVAL, sym, old_val));
              eval.k.push(ContOp.EVAL_VAL, new Cell(a[1], null));
              return null;
          }),

        c("call-w/stdout", 2, "(call-w/stdout output-port nullary-function)",
          (a, eval)-> {
              var symbols = eval.interp.getSymbolTable();
              var sym = Symbol.of("stdout");
              var old_val = symbols.get(sym);
              var port = a[0];
              var new_val = c("_tmp_stdout", 0, null, (x)-> port);
              symbols.put(sym, new_val);
              eval.k.pushWind(LL.list(PUTSYMVAL, sym, new_val),
                              LL.list(PUTSYMVAL, sym, old_val));    
              eval.k.push(ContOp.EVAL_VAL, new Cell(a[1], null));
              return null;
          }),

        c("infile", 1, "(infile filename) => input-port (in UTF-8)",
          (a)-> {
              var s = (char[]) a[0];
              var rf = new FileInputStream (new String (s));
              var input = new LinesFromInputStream (rf);
              return new CharEnumerator (input);
          }),

        c("outfile", 1, "(outfile filename) => output-port (in UTF-8)",
          (a)-> {
              var s = (char[]) a[0];
              var os = new FileOutputStream (new String (s));
              var wr = new OutputStreamWriter (os, "UTF-8");
              return new PrintWriter (wr, true);
          }),
        
        c("file-exists", 1, "(file-exits filename)",
          (a)-> {
              var s = (char[]) a[0];
              var file = new File (new String (s));
              return file.exists() ? s : null;
          }),
            
        c("instring", 1, "(instring string) => input-string-port",
          (a)-> {
              var s = (char[]) a[0];
              var input = new LinesFromString (new String (s));
              return new CharEnumerator (input);
          }),

        c("outstring", 0, "(outstring) => output-string-port",
          (a)-> new StringPrintWriter ()),

        c("inside", 1, "(inside output-string-port) => string",
          (a)-> {
              var w = (StringPrintWriter) a[0];
              return w.inside();
          }),

        c("close", 1, "(close port)",
          (a)-> {
              var p = (AutoCloseable) a[0];
              p.close();
              return null;
          }),

        c("writec", 1, DEFAULTS_TO_STDOUT, false,
          "(writec char [port]): 文字を印字する",
          (a)-> {
              var pw = (PrintWriter) a[1];
              pw.print((Character) a[0]);
              pw.flush();
              return null;
          }),

        c("write", 1, DEFAULTS_TO_STDOUT, false,
          "(write x [port]): x を印字する (文字列は引用符付き)",
          (a)-> {
              var pw = (PrintWriter) a[1];
              pw.print(LL.str(a[0], true));
              pw.flush();
              return null;
          }),

        c("disp", 1, DEFAULTS_TO_STDOUT, false,
          "(disp x [port]): x を印字する (文字列は引用符なし)",
          (a)-> {
              var pw = (PrintWriter) a[1];
              pw.print(LL.str(a[0], false));
              pw.flush();
              return null;
          }),

        c("sread", 2, "(sread port eof)",
          (a)-> {
              var chars = (CharEnumerator) a[0];
              var eof = a[1];
              var ar = new ArcReader (chars);
              var x = ar.read();
              return (x == LL.EOF) ? eof : x;
          }),

        c("readc", 1, "(readc port)",
          (a)-> {
              var chars = (CharEnumerator) a[0];
              try {
                  char ch = chars.current();
                  chars.moveNext();
                  return ch;
              } catch (CharEnumerator.EOFException ex) {
                  return null;
              }
          }),

        c("peekc", 1, "(peekc port)",
          (a)-> {
              var chars = (CharEnumerator) a[0];
              try {
                  return chars.current();
              } catch (CharEnumerator.EOFException ex) {
                  return null;
              }
          }),

        c("+", 0, null, true,
          "(算術加算/文字列連結/リスト連結 (+ ...); (+) => 0",
          (a)-> {
              Cell xs = (Cell) a[0];
              if (xs != null) {
                  Object x = xs.car;
                  if (x == null || x instanceof Cell)
                      return appendLists(xs);
                  else if (x instanceof char[])
                      return appendStrings(xs);
              }
              return BuiltinMath.add(xs);
          }),

        c(LL.S_APPEND.name, 0, null, true,
          "(" + LL.S_APPEND + " '(a b) '(c d) ...) => (a b c d ...)",
          (a)-> appendLists((Cell) a[0])),

        c("*", 0, null, true,
          "算術乗算 (* ...); (* 2 3) => 6; (*) => 1",
          (a)-> BuiltinMath.multiply((Cell) a[0])),

        c("/", 1, null, true,
          "実数除算 (/ x ...); (/ 6) => 1/6",
          (a)-> {
              Number x = (Number) a[0];
              Cell yy = (Cell) a[1];
              if (yy == null) {
                  return BuiltinMath.divideReal(1, x);
              } else {
                  for (Object y: yy)
                      x = BuiltinMath.divideReal(x, (Number) y);
                  return x;
              }
          }),

        c("quotient", 2, "整数除算 (quotient x y); (quotient 6 5) => 1",
          (a)-> BuiltinMath.divideInt((Number) a[0], (Number) a[1])),

        c("-", 1, null, true, 
          "算術減算 (- x ...); (- 10) => -10; (- 10 2) => 8",
          (a)-> {
              Number x = (Number) a[0];
              Cell yy = (Cell) a[1];
              return BuiltinMath.subtract(x, yy);
          }),

        c("mod", 2, "Modulo (mod a b); (mod 5 7) => 5; (mod 13 -4) => -3",
          (a)-> BuiltinMath.modulo((Number) a[0], (Number) a[1])),

        c("<", 2, null, true,
          "算術/文字列/シンボル/文字比較 (< a b ...)",
          (a)-> {
              Cell b = new Cell (a[1], a[2]);
              return lessThan(a[0], b) ? LL.S_T : null;
          }),

        c(">", 2, null, true,
          "算術/文字列/シンボル/文字比較 (> a b ...)",
          (a)-> {
              Cell b = new Cell (a[1], a[2]);
              return greaterThan(a[0], b) ? LL.S_T : null;
          }),

        c("trunc", 1, "(trunc 数) => 数をゼロの方向へ丸めた整数",
          (a)-> BuiltinMath.trunc((Number) a[0])),

        c("sqrt", 1, "正の平方根 (sqrt 4) => 2",
          (a)-> BuiltinMath.sqrt((Number) a[0])),

        c("expt", 2, "(expt 2 3) => 8",
          (a)-> BuiltinMath.expt((Number) a[0], (Number) a[1])),

        c("eval", 1, "(eval x) => evaluated x",
          (a, eval)-> {
              eval.k.push(ContOp.EVAL_VAL, a[0]);
              return null;
          }),

        c("macex1", 1, "(macex1 expr) => expr を１回マクロ展開する",
          (a, eval)-> eval.interp.expandMacros(a[0], -1)),

        c("macex", 1, "(macex1 expr) => expr をマクロ展開する",
          (a, eval)-> eval.interp.expandMacros(a[0], LL.MAX_MACRO_EXPS)),

        c("uniq", 0, "(uniq) => インターンされていない一意的なシンボル",
          (a)-> Symbol.generateUninterned()),

        c("err", 1, null, true,
          "(err msg arg...)",
          (a)-> {
              String msg = new String ((char[]) a[0]);
              Cell arg = (Cell) a[1];
              if (arg == null)
                  throw new EvalException (msg);
              else if (arg.cdr == null)
                  throw new EvalException (msg, arg.car);
              else
                  throw new EvalException (msg, arg);
          }),

        c("protect", 2, "(protect during after): try {during} finally {after}",
          (a, eval)-> {
              eval.k.pushWind(null, new Cell(a[1], null));
              eval.k.push(ContOp.EVAL_VAL, new Cell(a[0], null));
              return null;
          }),

        // 引数の関数を評価しているときだけ排他ロックをかける。
        c("atomic-invoke", 1, "(atomic-invoke nullary-function)",
          (a, eval)-> {
              LOCK.lock();
              eval.k.pushWind(DO_LOCK, DO_UNLOCK);
              eval.k.push(ContOp.EVAL_VAL, new Cell(a[0], null));
              return null;
          }),

        c("new-thread", 1, "(new-thread nullary-function)",
          (a, eval)-> {
              var fn = a[0];
              IInterp interp = eval.interp;
              Cell env = eval.env;
              Thread th = new Thread () {
                      public void run() {
                          interp.eval(new Cell(fn, null), env);
                      }
                  };
              th.start();
              return th;
          }),

        c("scar", 2, "(scar pair value): pair の car を value で置き換える",
          (a)-> {
              Object x = a[0];
              ((Cell) x).car = a[1];
              return a[1];
          }),
        
        c("scdr", 2, "(scdr pair value): pair の cdr を value で置き換える",
          (a)-> {
              Object x = a[0];
              ((Cell) x).cdr = a[1];
              return a[1];
          }),

        c("sref", 3, "(sref x value index): x[index] := value",
          (a)-> {
              Object x = a[0];
              Object value = a[1];
              Object index = a[2];
              //
              if (x instanceof Cell) {
                  int n = (Integer) index;
                  Cell xc = (Cell) x;
                  for (int i = 0; i < n; i++)
                      xc = xc.getCdrCell();
                  xc.car = value;
              } else if (x instanceof char[]) {
                  int i = (Integer) index;
                  ((char[]) x)[i] = (Character) value;
              } else {
                  ((Table) x).put(index, value);
              }
              return value;
          }),

        c("len", 1, "(len x): 要素数またはエントリ数",
          (a)-> len(a[0])),
        
        // table の各エントリに２引数関数 (fn (key val) ...) を適用する。
        c("maptable", 2, "(maptable (fn (key val) ...) table)",
          (a, eval)-> {
              var fun = a[0];
              var table = (Table) a[1];
              var iter = table.iterator();
              eval.k.push(ContOp.RESULT_VAL, table);
              eval.k.push(ContOp.EVAL_VAL, LL.list(MAPTABLE1, iter, fun));
              return null;
          }),

        c("coerce", 2, new Object[] { null }, false,
          "(coerce x type-symbol [radix])",
          (a)-> {
              var x = a[0];
              var typ = (Symbol) a[1];
              var option = a[2];
              return coerce(x, typ, option);
          }),

        // XXX ここでは (annotate 'mac (fn ..)) だけ実装する
        c("annotate", 2, "(annotate 'mac (fn ...) => (mac ...)",
          (a)-> {
              if (a[0] != LL.S_MAC)
                  throw new UnsupportedOperationException ();
              Fn f = (Fn) a[1];
              return new Fn.Macro (f.fixedArgs, f.defaultExps, f.hasRest,
                                   f.body, f.env);
          }),

        // ~x や x:y は読取り時に展開済みだから，これはつねに nil
        c("ssyntax", 1, "(ssyntax x) => nil",
          (a)-> null),

        // ~x や x:y は読取り時に展開済みだから，これはつねに恒等関数
        c("ssexpand", 1, "(ssexpand x) => x",
          (a)-> a[0]),

        c("msec", 0, "(msec)",
          (a)-> BuiltinMath.reg(System.currentTimeMillis())),

        c("seconds", 0, "(seconds)",
          (a)-> {
              long msec = System.currentTimeMillis();
              return BuiltinMath.reg(msec / 1000);
          }),
        
        c("rand", 0, new Object[] { null }, false,
          "(rand [k])",
          (a)-> {
              if (a[0] == null) {
                  return Math.random(); // 0.0 以上 1.0 未満の乱数
              } else {
                  int k = (Integer) a[0];
                  double r = Math.random();
                  return (int) (k * r); // 0 以上 k 未満の乱数
              }
          }),
            
        c("quit", 1, "(quit exit-code): プロセスを終了する",
          (a)-> {
              int code = (Integer) a[0];
              System.exit(code);
              throw new RuntimeException ("ここには到達しない");
          }),

        c("dump", 0, "(dump) => (大域的に定義されたシンボルの集合 環境 継続)",
          (a, eval)-> {
              var map = eval.interp.getSymbolTable();
              var keys = map.keySet();
              var symbols = keys.toArray();
              Arrays.sort(symbols);
              return LL.list(symbols, eval.env, new Continuation(eval.k));
          }),

        c("java-def", 1, "(java-def クラス名): 動的に Intrinsic を取り込む",
          (a, eval)-> {
              var className = new String ((char[]) a[0]);
              var klass = Class.forName(className);
              var ctor = klass.getDeclaredConstructor();
              var f = (Intrinsic) ctor.newInstance();
              eval.interp.load(new Intrinsic[] {f});
              return f;
          }),

        c("help", 1, "(help intrinsic): intrinsic を説明する",
          (a, eval)-> {
              if (a[0] instanceof Intrinsic) {
                  var f = (Intrinsic) a[0];
                  if (f.doc != null) {
                      var pw = eval.interp.getWriter();
                      pw.println(f.doc);
                  }
              }
              return a[0];
          })
    };
} // Builtins
