// H22.09.29/R02.04.30 (鈴)
package arc;

import java.util.ArrayList;
import java.math.BigInteger;

/** Arc 式の読み取り器
*/
public class ArcReader
{
    private final Lexer lex;
    private boolean erred = false;

    /** 与えられた引数から次々と Arc 式を読み取るように構築する。
     */
    public ArcReader (CharEnumerator chars) {
        lex = new Lexer (chars);
    }

    /** １個の Arc 式を読む。
     * 行が尽きたら LL.EOF をいつまでも返す。
     * @see LL#EOF
     */
    public Object read() throws EvalException
    {
        if (erred) { // 前回が構文エラーだったならば次の行まで読み飛ばす。
            int n = lex.getLineNumber();
            do {
                if (! lex.showPromptAndMoveNext())
                    return LL.EOF;
            } while (lex.getLineNumber() == n);
            erred = false;
        } else {
            if (! lex.showPromptAndMoveNext())
                return LL.EOF;
        }
        try {
            return parseExpression();
        } catch (SyntaxException ex) {
            throw makeEvalException(ex);
        } catch (CharEnumerator.EOFException ex) {
            throw makeEvalException(ex);
        }
    }

    private EvalException makeEvalException(Exception ex) {
        erred = true;
        return new EvalException ("SyntaxError: " + ex.getMessage() +
                                  " -- " + lex.getLineNumber() +
                                  ": " + lex.getLine());
    }

    private Object parseExpression()
        throws SyntaxException, CharEnumerator.EOFException
    {
        Object e = parseExpression1();
        for (;;) {
            char ch = lex.peekNextChar();
            if (ch == ':') {    // x:y:z => (compose x y z)
                var xs = new ArrayList<Object> ();
                xs.add(LL.S_COMPOSE);
                xs.add(e);
                do {
                    lex.skipNextChar(); // 「:」をスキップして
                    lex.next();         // 次のトークンへと進む
                    e = parseExpression1();
                    xs.add(e);
                } while (lex.peekNextChar() == ':');
                return LL.mapcar(xs, null);
            } else if (ch == '.') { // x.y.z => ((x y) z)
                lex.skipNextChar();
                lex.next();
                Object e2 = parseExpression1();
                e = LL.list(e, e2);
            } else if (ch == '!') { // x!y!z => ((x 'y) 'z)
                lex.skipNextChar();
                lex.next();
                Object e2 = parseExpression1();
                e = LL.list(e, LL.list(LL.S_QUOTE, e2));
            } else {
                return e;
            }
        }
    }

    private Object parseExpression1()
        throws SyntaxException, CharEnumerator.EOFException
    {
        Object token = lex.current();
        if (token == Token.DOT || token == Token.RPAREN ||
            token == Token.RBRACKET) {
            throw new SyntaxException ("unexpected " + token);
        } else if (token == Token.LPAREN) {
            lex.next();
            return parseListBody();
        } else if (token == Token.QUOTE) {
            lex.next();
            return LL.list(LL.S_QUOTE, parseExpression());
        } else if (token == Token.TILDE) {
            lex.next();
            return LL.list(LL.S_COMPLEMENT, parseExpression());
        } else if (token == Token.BQUOTE) {
            lex.next();
            return LL.list(LL.S_QUASIQUOTE, parseExpression());
        } else if (token == Token.COMMA) {
            lex.next();
            return LL.list(LL.S_UNQUOTE, parseExpression());
        } else if (token == Token.COMMA_AT) {
            lex.next();
            return LL.list(LL.S_UNQUOTE_SPLICING, parseExpression());
        } else if (token == Token.LBRACKET) {
            lex.next();
            return parseBracketBody();
        } else {
            return token;
        }
    }

    private Object parseListBody()
        throws SyntaxException, CharEnumerator.EOFException
    {
        if (lex.current() == Token.RPAREN) {
            return null;
        } else {
            Object e1 = parseExpression();
            lex.next();
            Object e2;
            if (lex.current() == Token.DOT) {
                lex.next();
                e2 = parseExpression();
                lex.next();
                if (lex.current() != Token.RPAREN)
                    throw new SyntaxException ("\")\" expected: "
                                               + lex.current());

            } else {
                e2 = parseListBody();
            }
            return new Cell (e1, e2);
        }
    }

    // [ ... _ ... ] => (fn (_) (... _ ...))
    private Cell parseBracketBody()
        throws SyntaxException, CharEnumerator.EOFException
    {
        var xs = new ArrayList<Object> ();
        while (lex.current() != Token.RBRACKET) {
            Object e = parseExpression();
            xs.add(e);
            lex.next();
        }
        return LL.list(LL.S_FN, LL.list(LL.S_UNDERSCORE),
                       LL.mapcar(xs, null));
    }


    /** 字句解析器 */
    private static final class Lexer
    {
        private final CharEnumerator ce;
        private Object token = "Ling!"; // 先読みしたトークン
        // token の初期値は EOFException 型でなければ何でもよい。

        Lexer (CharEnumerator chars) {
            ce = chars;
        }

        /** 先読みしておいたトークンを返す。
         * @throws SyntaxException 先読みしたトークンに対して例外があった。
         * @throws EOFException  EOF を先読みしていた
         */
        Object current()
            throws SyntaxException, CharEnumerator.EOFException
        {
            if (token instanceof SyntaxException)
                throw (SyntaxException) token;
            else if (token instanceof CharEnumerator.EOFException)
                throw (CharEnumerator.EOFException) token;
            else
                return token;
        }

        /** 次のトークンを先読みする。読んだトークンは current() で得られる。
         * 今までのトークンは捨てられる。
         * @return EOF を先読みしたら false
         */
        boolean next() {
            if (token instanceof CharEnumerator.EOFException) {
                return false;
            } else {
                try {
                    token = nextToken();
                    return true;
                } catch (CharEnumerator.EOFException ex) {
                    token = ex;
                    return false;
                }
            }
        }

        // 次のトークンを読む
        private Object nextToken()
            throws CharEnumerator.EOFException 
        {
            for (;;) {
                skipSpaces();
                if (ce.current() != '\n')
                    break;
                ce.moveNext();
            }
            char ch = ce.current();
            if (ch == ',') {
                ce.moveNext();
                if (ce.current() == '@') {
                    ce.moveNext();
                    return Token.COMMA_AT;
                } else {
                    return Token.COMMA;
                }
            } else if (ch == '(') {
                ce.moveNext();
                return Token.LPAREN;
            } else if (ch == ')') {
                ce.moveNext();
                return Token.RPAREN;
            } else if (ch == '.') {
                ce.moveNext();
                return Token.DOT;
            } else if (ch == '\'') {
                ce.moveNext();
                return Token.QUOTE;
            } else if (ch == '~') {
                ce.moveNext();
                return Token.TILDE;
            } else if (ch == '`') {
                ce.moveNext();
                return Token.BQUOTE;
            } else if (ch == '[') {
                ce.moveNext();
                return Token.LBRACKET;
            } else if (ch == ']') {
                ce.moveNext();
                return Token.RBRACKET;
            } else if (ch == '"') {
                Object s = getString(ce);
                ce.moveNext();
                return s;
            } else {
                var sb = new StringBuilder ();
                boolean continued = true;
                sb.append(ch);
                if (ch == '#') {
                    if (ce.moveNext()) {
                        ch = ce.current();
                        if (Character.isWhitespace(ch)) {
                            continued = false;
                        } else {
                            sb.append(ch);
                            if (ch == '\\') {
                                if (ce.moveNext()) {
                                    ch = ce.current();
                                    if (Character.isWhitespace(ch))
                                        continued = false;
                                    sb.append(ch);
                                } else {
                                    continued = false;
                                }
                            }
                        }
                    } else {
                        continued = false;
                    }
                }
                if (continued)
                    while (ce.moveNext()) {
                        ch = ce.current();
                        if (ch == '(' || ch == ')' || ch == '\'' ||
                            ch == '[' || ch == ']' || ch == '"' ||
                            ch == '~' || ch == ':' || ch == '!' ||
                            Character.isWhitespace(ch))
                            break;
                        sb.append(ch);
                    }
                String tk = sb.toString();
                Character chara = tryToParseAsCharacter(tk);
                if (chara != null)
                    return chara;
                Object num = tryToParseAsNumber(tk);
                if (num != null)
                    return num;
                // シンボルとして解釈する。ピリオドがあれば，
                // それ以降はまだ読まなかったことにする。
                int i = tk.indexOf('.');
                if (i == 0) {   // あり得ない。cf. Token.DOT
                    return new SyntaxException ("impossible dot: " + tk);
                } else if (i > 0) {
                    ce.moveBackTo(tk.substring(i));
                    tk = tk.substring(0, i);
                }
                if ("nil".equals(tk))
                    return null;
                else if (checkSymbol(tk)) // 妥当なシンボルか？
                    return Symbol.of(tk);
                else
                    return new SyntaxException ("bad token: " + tk);
            }
        }

        // 空白とコメントをたかだか行末まで読み飛ばす
        private void skipSpaces()
            throws CharEnumerator.EOFException 
        {
            char ch;
            for (;;) {
                ch = ce.current();
                if (ch == '\n')
                    return;
                else if (! Character.isWhitespace(ch))
                    break;
                ce.moveNext();
            }
            if (ch == ';') {    // ; コメント
                ce.moveNext();
                while (ce.current() != '\n')
                    ce.moveNext();
            }
        }

        /** 可能ならば１次プロンプトを表示し，next() をする。
         * @return EOF を先読みしたら false
         */
        boolean showPromptAndMoveNext() {
            try {
                skipSpaces();
            } catch (CharEnumerator.EOFException ex) {
                return false;   // EOF を先読みした
            }
            ce.showPrompt();
            return next();
        }

        /** 現在の行番号 */
        int getLineNumber() {
            return ce.getLineNumber();
        }

        /** 現在の行 */
        String getLine() {
            return ce.getLine(); 
        }

        /** 先読みしている文字を返す。ただし EOF ならば '\0' を返す。*/
        char peekNextChar() {
            try {
                return ce.current();
            } catch (CharEnumerator.EOFException ex) {
                return '\0';
            }
        }

        /** 今先読みしている文字をスキップする。
         * @return EOF に突き当たったら false
         */
        boolean skipNextChar() {
            return ce.moveNext();
        }
    } // Lexer


    // 字句解析の下請け関数

    /** 文字列を読み取る。
     * char[] または SyntaxException を返す。
     * ce.current() が '"' を指している状態で呼び出される。
     * ce.current() が次の '"' を指している状態で終わる。
     */
    private static Object getString(CharEnumerator ce)
        throws CharEnumerator.EOFException
    {
        var sb = new StringBuilder ();
        ce.moveNext();          // '"' の次へ進む
        for (;;) {
            char ch = ce.current();
            if (ch == '"') {
                return LL.toCharArray(sb);
            } else if (ch == '\n') {
                return new SyntaxException
                    ("string not terminated: " + LL.str(LL.toCharArray(sb)));
            } else if (ch == '\\') {
                ce.moveNext();
                switch (ce.current()) {
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    sb.append(getOctChar(ce));
                    continue;   // 八進文字の次を先読み済みだから
                case 'x':
                    sb.append(getHexChar(ce));
                    continue;   // 十六進文字の次を先読み済みだから
                case '\"':
                    ch = '\"'; break;
                case '\\':
                    ch = '\\'; break;
                case 'a':
                    ch = '\007'; break; // Java には \a がないから
                case 'b':
                    ch = '\b'; break;
                case 'f':
                    ch = '\f'; break;
                case 'n':
                    ch = '\n'; break;
                case 'r':
                    ch = '\r'; break;
                case 't':
                    ch = '\t'; break;
                case 'v':
                    ch = '\013'; break; // Java には \v がないから
                default:
                    return new SyntaxException
                        ("bad escape: " + ce.current());
                }
            }
            sb.append(ch);
            ce.moveNext();
        }

    }

    /** 文字列の中で \ に続く八進数で表現された文字を読み取る。
     * ce は \ の次の数字を指しているとする。
     * 数の次の文字を先読みした状態で終わる。
     */
    private static char getOctChar(CharEnumerator ce)
        throws CharEnumerator.EOFException
    {
        int result = ce.current() - '0';
        for (int i = 0; ce.moveNext() && i < 2; i++) {
            char ch = ce.current();
            if ('0' <= ch && ch <= '9')
                result = (result << 3) + ch - '0';
            else
                break;
        }
        return (char) result;
    }

    /** 文字列の中で \x に続く十六進数で表現された文字を読み取る。
     * ce は \ の次の x を指しているとする。
     * 数の次の文字を先読みした状態で終わる。
     */
    private static char getHexChar(CharEnumerator ce)
        throws CharEnumerator.EOFException
    {
        int result = 0;
        for (int i = 0; ce.moveNext() && i < 2; i++) {
            char ch = ce.current();
            if ('0' <= ch && ch <= '9')
                result = (result << 4) + ch - '0';
            else if ('A' <= ch && ch <= 'F')
                result = (result << 4) + ch - 'A' + 10;
            else if ('a' <= ch && ch <= 'f')
                result = (result << 4) + ch - 'a' + 10;
            else
                break;
        }
        return (char) result;
    }


    /** 文字列に対して文字定数としての解釈を試みる。
     * 失敗時には null を返す。
     */
    private static Character tryToParseAsCharacter(String s) {
        int len = s.length();
        if (s.startsWith("#\\") && len >= 3) {
            if (len == 3)
                return s.charAt(2);
            else if (s.equals("#\\space"))
                return ' ';
            else if (s.equals("#\\newline"))
                return '\n';
            else if (s.equals("#\\tab"))
                return '\t';
            else if (s.equals("#\\return"))
                return '\r';
        }
        return null;
    }

    /** 文字列に対して数としての解釈を試みる。
     * 不正な数ならば SyntaxException を返す。
     * 数でないならば null を返す。
     */
    private static Object tryToParseAsNumber(String s) {
        int radix = 10;
        if (s.startsWith("#") && s.length() >= 3) {
            switch (s.charAt(1)) {
            case 'b': case 'B':
                radix = 2; break;
            case 'o': case 'O':
                radix = 8; break;
            case 'x': case 'X':
                radix = 16; break;
            default:
                return null;
            }
            s = s.substring(2);
        }
        boolean negative = false;
        if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1);
        }
        String[] ss = s.split("/", 2);
        Number a = tryToParseAs1Number(ss[0], radix, negative);
        if (ss.length == 1) {
            return a;
        } else {
            Number b = tryToParseAs1Number(ss[1], radix, false);
            if (a == null || b == null) {
                return null;
            } else if (a instanceof Double || b instanceof Double) {
                return new SyntaxException ("invalid rational: " + s);
            } else {
                try {
                    return Rational.result(a, b);
                } catch (ArithmeticException ex) {
                    return new SyntaxException ("not a number: " + s);
                }
            }
        }
    }

    private static Number tryToParseAs1Number(String s, int radix,
                                              boolean negative) {
        if (s.startsWith("-"))
            return null;
        if (negative)
            s = "-" + s;
        try {
            return Integer.valueOf(s, radix);
        } catch (NumberFormatException ex) {}
        try {
            return new BigInteger (s, radix);
        } catch (NumberFormatException ex) {}
        if (radix == 10) {
            try {
                return Double.valueOf(s); // new Double(s) は deprecated
            } catch (NumberFormatException ex) {}
        }
        return null;
    }

    /** 引数がシンボルとして適切かどうか判定する。
     */
    private static boolean checkSymbol(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (! (('a' <= c && c <= 'z') ||
                   ('A' <= c && c <= 'Z') ||
                   ('0' <= c && c <= '9') ||
                   c == '_' || c == '&' || c == '$' ||
                   c == '*' || c == '/' || c == '%' ||
                   c == '+' || c == '-' || c == '<' ||
                   c == '>' || c == '=' || c == '?'))
                return false;
        }
        return true;
    }


    /* 構文の組立てに使われるトークン
       これらが結果の Arc 式に出現することはない。
       ここではデバッグの便宜のため金物表現と一致するシンボルを使うが，
       一意に識別できるオブジェクトであれば何でもよい。
    */
    private static class Token {
        static final Symbol BQUOTE = Symbol.of("`");
        static final Symbol COMMA = Symbol.of(",");
        static final Symbol COMMA_AT = Symbol.of(",@");
        static final Symbol DOT = Symbol.of(".");
        static final Symbol LBRACKET = Symbol.of("[");
        static final Symbol LPAREN = Symbol.of("(");
        static final Symbol RBRACKET = Symbol.of("]");
        static final Symbol RPAREN = Symbol.of(")");
        static final Symbol QUOTE = Symbol.of("'");
        static final Symbol TILDE = Symbol.of("~");
    } // Token

    /** 構文上の誤りを表す例外 */
    private static class SyntaxException extends Exception
    {
        SyntaxException (String message) {
            super (message);
        }
    } // SyntaxException
} // ArcReader
