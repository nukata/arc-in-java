// H22.09.22/R02.05.02 (鈴)
package arc;

import java.io.IOException;
import java.io.UncheckedIOException;

/** 文字イテレータ (C# 風)
 */
public class CharEnumerator implements AutoCloseable
{
    private final ILines lines;
    private String line = null; // 現在の行
    private int lineNumber = 0;
    private char ch = '\n';             // 先読みした１文字
    private boolean hasSeenEOF = false; //  EOF を先読みしたか？
    private int i = -1;                 // 現在の行の中の文字の位置
    private boolean showPrompt = false;

    /** 与えられた ILines インスタンスから次々と文字を読み取るように構築する。
     */
    public CharEnumerator (ILines lines) {
        this.lines = lines;
    }

    /** １次プロンプトを表示するためのフラグを立てる。 
     * もしも次の next() で行を読み取るならば１次プロンプトが表示される
     * ようにする。
     */
    public void showPrompt() {
        showPrompt = (i < 0);
    }

    /** 次の文字を先読みする。
     * @return EOF を先読みしたら false
     */
    public boolean moveNext() {
        if (hasSeenEOF)
            return false;
        if (i < 0) {
            try {
                line = lines.readLine(showPrompt);
            } catch (IOException ex) {
                throw new UncheckedIOException (ex);
            }
            showPrompt = false;
            lineNumber++;
            if (line == null) {
                hasSeenEOF = true;
                return false;
            }
            i = 0;
        }
        if (i < line.length()) {
            ch = line.charAt(i);
            i++;
        } else {
            i = -1;
            ch = '\n';          // 行末文字
        }
        return true;
    }

    /** 先読みしておいた文字を得る。
     * @throws EOFException EOF を先読みしていた。
     */
    public char current() throws EOFException {
        if (hasSeenEOF)
            throw new EOFException ();
        else
            return ch;
    }

    /** 文字列 s を読まなかったことにする。
     * 現在の文字 (current()) の直前の既読文字列 s まで読まなかったことにする。
     * current() は s の先頭文字と等しくなる。行をまたぐことはできない。
     * @throws UnsupportedOperationException 戻すことができない状態だった。
     */
    public void moveBackTo(String s) {
        if (! hasSeenEOF) {
            if (i < 0)
                i = line.length() + 1; // 仮想的な改行文字のさらに次の位置
            int n = s.length();
            if (n < i && s.equals(line.substring(i - n - 1, i - 1))) {
                i -= n;
                ch = line.charAt(i - 1);
                return;
            }
        }
        throw new UnsupportedOperationException (s + ", " + i + "@" + line);
    }

    /** 現在の行番号を得る。
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /** 現在の行を得る。
     */
    public String getLine() {
        return line;
    }

    @Override
    public void close() throws Exception {
        lines.close();
    }


    /** ファイルの終端に達したことを表す例外 */
    public static class EOFException extends Exception
    {
        EOFException () {
            super ("unexpected EOF");
        }
    } // EOFException
} // CharEnumerator
