// H22.09.07/R02.05.01 (鈴)
package arc;

import java.util.Map;
import java.util.HashMap;

/** Arc のシンボル
 */
public class Symbol implements Comparable<Symbol>
{
    /** シンボルの印字名 */
    final String name;

    /** シンボルの一意性を保つための表 */
    private static final Map<String, Symbol> dict = new HashMap<> ();

    /** generateUninterned のためのカウンタ */
    private static int gensymCounter = 0;

    /** 印字名から未インターンのシンボルを構築する。
     */
    private Symbol (String name) {
        this.name = name;
    }

    /** 同じ印字名に対し，同じシンボルを返す。
     * @param name 印字名
     */
    public static Symbol of(String name) {
        synchronized (dict) {
            Symbol sym = dict.get(name);
            if (sym == null) {
                sym = new Symbol (name);
                dict.put(name, sym);
            }
            return sym;
        }
    }

    /** 未インターンのシンボルを生成する。
     * Arc の (uniq) に使う。
     */
    public static Symbol generateUninterned() {
        int i;
        synchronized (dict) {
            i = ++gensymCounter;
        }
        String name = "$G" + i;
        return new Symbol (name);
    }

    /** 印字名をそのまま返す。
     */
    @Override public String toString() {
        return name;
    }

    /** 印字名で比較する。
     */
    @Override public int compareTo(Symbol s) {
        return name.compareTo(s.name);
    }


    /** キーワードを表すシンボル.
     * スペシャルフォームの構文キーワードをこれで表す。
     */
    public static class Keyword extends Symbol
    {
        private Keyword (String name) {
            super (name);
        }

        /** 印字名に対するシンボルを Keyword インスタンスとして構築する。
         * それぞれの name に対して最初の呼出しでなければならない。
         * ２回目以降は Symbol.of(name) を使うこと。
         * @param name 印字名
         * @throws IllegalArgumentException 
         *   name に対して２度目の呼出しをした。
         */
        public static Keyword of(String name) {
            synchronized (dict) {
                if (dict.containsKey(name)) {
                    throw new IllegalArgumentException (name);
                } else {
                    Keyword sym = new Keyword (name);
                    dict.put(name, sym);
                    return sym;
                }
            }
        }
    } // Keyword
} // Symbol
