// H22.08.30/R02.05.02 (鈴)
package arc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/** java.io.InputStream から構築する ILines 実装クラス.
 * 文字エンコーディングには UTF-8 を使う。
 */
public class LinesFromInputStream implements ILines
{
    private final BufferedReader br;

    /** 引数から BufferedReader インスタンスを作成して構築する。 
     */
    public LinesFromInputStream (InputStream stream) {
        try {
            Reader reader = new InputStreamReader (stream, "UTF-8");
            br = new BufferedReader (reader);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException ("Impossible!", ex);
        }
    }

    /** BufferedReader インスタンスの readLine() を呼び出す。 
     * 引数は無視する。
     */
    @Override
    public String readLine(boolean showPrompt1) throws IOException {
        return br.readLine();
    }

    /** BufferedReader インスタンスの close() を呼び出す。
     */
    @Override
    public void close() throws IOException {
        br.close();
    }
} // LinesFromInputStream
