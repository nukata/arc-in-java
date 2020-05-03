// H22.08.30/R02.05.02 (鈴)
package arc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** コンソールから対話的に１行ずつ入力するためのクラス.
 * System.in と System.out を使う。
 */
public class LinesFromConsole implements ILines
{
    private final BufferedReader br;
    private final String ps1;
    private final String ps2;
    private final String farewell;
    private boolean isOpen = true;

    /** 与えられたコンソールをラップする。
     * @param ps1 １次プロンプト
     * @param ps2 ２次プロンプト
     * @param farewell わかれのあいさつ または null
     */
    public LinesFromConsole (String ps1, String ps2, String farewell) {
        this.br = new BufferedReader (new InputStreamReader (System.in));
        this.ps1 = ps1;
        this.ps2 = ps2;
        this.farewell = farewell;
    }

    /** System.in から１行読む。 ただし showPrompt1 が true ならば，
     * その前にコンソールに１次プロンプトを表示する。
     * そうでなければ２次プロンプトを表示する。
     */
    @Override
    public String readLine(boolean showPrompt1) throws IOException {
        System.out.print((showPrompt1) ? ps1 : ps2);
        System.out.flush();
        return br.readLine();
    }

    /** もしあれば，System.out にさよならを表示して改行する。
     * ただし，２回目以降呼び出されたときは何もしない。
     */
    @Override
    public void close() {
        if (isOpen) {
            isOpen = false;
            if (farewell != null)
                System.out.println(farewell);
            System.out.flush();
        }
    }
} // LinesFromConsole
