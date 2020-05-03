// H22.09.21/R02.04.29 (鈴)
package arc;

/** コンパイル後の束縛変数。
 * ラムダ式やマクロ式の本体に含まれる。
 * 静的リンクの深さ (level) とフレーム内のオフセット (offset) を持つ。
 * 静的リンクは Cell によるリストで，フレームは Object[] でそれぞれ
 * 表現されるものとする。
 */
public final class Arg
{
    final int level;
    final int offset;
    final Symbol symbol;

    Arg (int level, int offset, Symbol symbol) {
        this.level = level;
        this.offset = offset;
        this.symbol = symbol;
    }

    @Override public String toString() {
        return "#" + level + ":" + offset + ":" + symbol;
    }

    void setValue(Object x, Cell env) {
        for (int i = 0; i < level; i++)
            env = (Cell) env.cdr;
        ((Object[]) env.car)[offset] = x;
    }

    Object getValue(Cell env) {
        for (int i = 0; i < level; i++)
            env = (Cell) env.cdr;
        return ((Object[]) env.car)[offset];
    }
} // Arg
