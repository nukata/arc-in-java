// H22.09.28/R02.05.02 (鈴)
package arc;

import java.io.FileInputStream;

/** 主プログラムの置き場 */
public class Main
{
    /** Lisp インタープリタを初期化する。
     * その処理は次のとおりである。
     * <ol>
     * <li> {@link Builtins#FUNCTIONS} を {@link IInterp#load} する。 
     *      car, cdr, cons 等が定義される。
     * <li> このクラスと同じ場所にある PRELUDE ファイルを UTF-8 で読んで
     *      {@link IInterp#run(ILines, IReceiver)} する。
     *      def, let 等が定義される。
     * </ol>
     * @param interp 初期化の対象となる Lisp インタープリタ
     */
    public static void initialize(IInterp interp) throws Exception {
        interp.load(Builtins.FUNCTIONS);
        var prelude = new LinesFromInputStream
            (LL.class.getResourceAsStream(LL.PRELUDE));
        interp.run(prelude, null);
    }

    /** 単独の Lisp インタープリタとしての主プログラムのサンプル実装.
     * その処理は次のとおりである。
     * <ol>
     * <li> {@link Interp} インスタンス interp を構築する。
     * <li> interp に対して {@link #initialize} を適用する。
     * <li> コマンド行引数をそれぞれファイル名として UTF-8 で読んで
     *   {@link IInterp#run(ILines, IReceiver)} する。
     *   ただし，コマンド行引数がないか "-" ならば，
     *   対話セッションとして {@link IInterp#run(ILines, IReceiver)} する。
     * </ol>
     * {@link Interp} の外では本メソッドだけが {@link Interp} に依存する。
     */
    public static void main(String[] args) throws Exception {
        var interp = new Interp ();
        initialize(interp);
        for (String fname: ((args.length == 0) ? new String[] {"-"} : args)) {
            ILines lines;
            IInterp.IReceiver receiver;
            if (fname.equals("-")) { // 対話セッション？
                lines = new LinesFromConsole ("arc> ", "     ", "Goodbye");
                receiver = new IInterp.IReceiver () {
                        public void receiveResult(Object result) {
                            System.out.println(LL.str(result));
                        }
                        public void receiveException(EvalException ex) {
                            System.out.println(ex);
                        }
                    };
            } else {
                lines = new LinesFromInputStream (new FileInputStream (fname));
                receiver = null;
            }
            interp.run(lines, receiver);
        }
    }

    /*
    // 開発用の最低限の主プログラムの実装例
    public static void main(String[] args) throws Exception {
        var interp = new Interp ();
        interp.load(Builtins.FUNCTIONS);
        // var symbols = interp.getSymbolTable();
        // System.out.println(symbols.toString());
        ILines input;
        if (args.length == 1) {
            input = new LinesFromInputStream(new FileInputStream (args[0]));
        } else {
            input = new LinesFromConsole("arc> ", "arc| ", "Goodbye");
        }
        try (var chars = new CharEnumerator(input)) {
            var ar = new ArcReader(chars);
            for (;;) {
                var x = ar.read();
                if (x == LL.EOF) break;
                x = interp.eval(x, null);
                System.out.println(LL.str(x));
            }
        }
    }
    */
} // Main
