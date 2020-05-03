// H22.09.21/R02.04.30 (鈴)
package arc;

import java.util.*;
import java.util.function.UnaryOperator;

/** cons セル */
public final class Cell implements Iterable<Object>
{
    Object car;
    Object cdr;

    /** Arc の (cons car cdr) に相当 */
    public Cell (Object car, Object cdr) {
        this.car = car;
        this.cdr = cdr;
    }

    /** 第１要素の getter */
    public Object getCar() {
        return car;
    }
    /** 第１要素の setter */
    public void setCar(Object value) {
        car = value;
    }

    /** 第２要素の getter */
    public Object getCdr() {
        return cdr;
    }
    /** 第２要素の setter */
    public void setCdr(Object value) {
        cdr = value;
    }

    /** 第２要素の getter。ただし Cell または null として。
     * @throws ImproperListException Cell または null ではなかった。
     */
    public Cell getCdrCell() throws ImproperListException {
        if (cdr instanceof Cell) {
            return (Cell) cdr;
        } else if (cdr == null) {
            return null;
        } else {
            throw new ImproperListException (this);
        }
    }

    /** Arc のリストとして各要素を与えるイテレータを作る。
     * proper list でなければ最後に ImproperListException の例外を
     * 発生させる。
     */
    @Override public Iterator<Object> iterator() {
        return new Iterator<Object> () {
            private Object j = Cell.this;

            public boolean hasNext() {
                if (j instanceof Cell) {
                    return true;
                } else if (j == null) {
                    return false;
                } else {
                    throw new ImproperListException (Cell.this);
                }
            }

            /** リストの次の要素を返す。内部のポインタを次のセルへと進める。
             */
            public Object next() {
                if (j == null) {
                    throw new NoSuchElementException ();
                } else {
                    Cell c = (Cell) j;
                    j = c.cdr;
                    return c.car;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException ();
            }
        };
    }

    /** Arc のリストとしての各要素に関数を適用した新しいリストを作る。
     * 汎用の LL.mapcar よりも効率がよい。improper list にも使える。
     * @param fn 各要素に適用する関数，ただし null ならば恒等関数とみなす。
     * @return 各要素に関数を適用した結果からなるリスト
     * @see LL#mapcar
     */
    public Cell mapcar(UnaryOperator<Object> fn) {
        Cell z = null;
        Cell y = null;
        Cell j = this;          // != null
        for (;;) {
            Object e = j.car;
            if (fn != null)
                e = fn.apply(e);
            Cell x = new Cell (e, null);
            if (z == null)
                z = x;
            else
                y.cdr = x;
            y = x;
            Object k = j.cdr;
            if (k == null) {
                break;
            } else if (k instanceof Cell) {
                j = (Cell) k;
            } else {
                if (fn != null)
                    k = fn.apply(k);
                y.cdr = k;
                break;
            }
        }
        return z;
    }

    /** Arc のリストとしての文字列表現を返す。
     */
    @Override public String toString() {
        return LL.str(this);
    }

    /** LL.str の補助関数
     */
    String repr(int recLevel, Set<Object> printed) {
        if (! printed.add(this)) { // 重複していたならば…
            recLevel--;
            if (recLevel == 0)
                return "...";
        }
        Object kdr = cdr;
        if (kdr == null) {
            return LL.str(car, true, recLevel, printed);
        } else {
            String s = LL.str(car, true, recLevel, printed);
            if (kdr instanceof Cell) {
                String t = ((Cell) kdr).repr(recLevel, printed);
                return s + " " + t;
            } else {
                String t = LL.str(kdr, true, recLevel, printed);
                return s + " . " + t;
            }
        }
    }


    /** proper list ではなかったことを知らせる例外
     */
    public static class ImproperListException extends EvalException
    {
        public ImproperListException (Object exp) {
            super ("proper list expected", exp);
        }
    } // ImproperListException
} // Cell
