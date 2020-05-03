// H22.09.21/R02.04.29 (鈴)
package arc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Arc の table (ハッシュ表) を実装する。
 * char[] で表現された文字列をキーとするとき，自動的にキーを String に
 * 変換することで，ハッシュが適切に機能するようにする。
 */
public class Table implements Iterable<Map.Entry<Object, Object>>
{
    private final Map<Object, Object> map = new HashMap<> ();

    /** java.util.Map#put(key, value) と同様。
     * ただし，value が null ならば java.util.Map#remove(key) と同様。
     */
    public Object put(Object key, Object value) {
        if (key instanceof char[])
            key = new String ((char[]) key);
        return (value == null) ?
            map.remove(key) :
            map.put(key, value);
    }

    /** java.util.Map#get と同様。
     */
    public Object get(Object key) {
        if (key instanceof char[])
            key = new String ((char[]) key);
        return map.get(key);
    }

    /** java.util.Map#size と同様。
     */
    public int size() {
        return map.size();
    }

    /** エントリを次々と与えるイテレータを作って返す。
     */
    @Override public Iterator<Map.Entry<Object, Object>> iterator() {
        var entries = map.entrySet();
        final var iter = entries.iterator();
        return new Iterator<Map.Entry<Object, Object>> () {
            public boolean hasNext() {
                return iter.hasNext();
            }

            public Map.Entry<Object, Object> next() {
                final var entry = iter.next();
                Object ky = entry.getKey();
                if (ky instanceof String) {
                    final char[] key = ((String) ky).toCharArray();
                    return new Map.Entry<Object, Object> () {
                        public boolean equals(Object x) {
                            return entry.equals(x);
                        }
                        public Object getKey() {
                            return key;
                        }
                        public Object getValue() {
                            return entry.getValue();
                        }
                        public int hashCode() {
                            return entry.hashCode();
                        }
                        public Object setValue(Object value) {
                            return entry.setValue(value);
                        }
                    };
                } else {
                    return entry;
                }
            }

            public void remove() {
                iter.remove();
            }
        };
    }

    /** arc の table らしい文字列表現を得る。
     * #hash((k1 . v1) (k2 . v2) ...) のような形式の文字列を返す。
     */
    @Override public String toString() {
        return LL.str(this);
    }

    /** LL.str の補助関数
     */
    String repr(int recLevel, Set<Object> printed) {
        var sb = new StringBuilder ();
        sb.append("#hash(");
        boolean first = true;
        for (Map.Entry<Object, Object> entry: this) {
            Object key = entry.getKey();
            Object val = entry.getValue();
            if (key instanceof String)
                key = ((String) key).toCharArray();
            Cell c = new Cell (key, val);
            if (first)
                first = false;
            else
                sb.append(" ");
            sb.append(LL.str(c, true, recLevel, printed));
        }
        sb.append(")");
        return sb.toString();
    }
} // Table
