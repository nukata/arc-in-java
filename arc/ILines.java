// H22.08.30/R02.05.02 (鈴)
package arc;

import java.io.IOException;

/** Arc 式を読み込むために使われるストリーム
 */
public interface ILines extends AutoCloseable
{
    /** 入力行を１行読み込む。
     * @param showPrompt1 Arc 式の読込み開始のプロンプト
     *   (いわゆる１次プロンプト) を表示すべきならば true。
     *   対話入力でだけ意味がある。
     * @return 行の内容を含む文字列。ただし末尾の改行文字は含めない。
     *   入力ストリームの終わりに達している場合は null を返す。
     */
    String readLine(boolean showPrompt1) throws IOException;
}
