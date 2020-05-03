// H22.08.30/R02.05.01 (鈴)
package arc;

/** 文字列から１行ずつ入力するためのクラス
 */
public class LinesFromString implements ILines
{
    private final String[] lines;
    private int index = 0;

    /** 引数を改行ごとに分割したコピーを内部に格納する。
     */
    public LinesFromString (String s) {
        lines = s.split("\r?\n");
    }

    /** 行ごとに分割したコピーを一つずつ与える。このとき，
     * 資源回収のため，そのコピーへの内部からの参照を消す。
     */
    @Override
    public String readLine(boolean showPrompt1) {
        if (index < lines.length) {
            String s = lines[index];
            lines[index] = null;
            index++;
            return s;
        } else {
            return null;
        }
    }

    /** 資源回収のため，コピーへの内部からの参照を消す。 
     */
    @Override
    public void close() {
        while (index < lines.length) {
            lines[index] = null;
            index++;
        }
    }
} // LinesFromString
