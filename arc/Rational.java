// H22.10.01/R02.04.29 (鈴)
package arc;

import java.math.BigInteger;
import static arc.BuiltinMath.intAdd;
import static arc.BuiltinMath.intCompare;
import static arc.BuiltinMath.intDivide;
import static arc.BuiltinMath.intGCD;
import static arc.BuiltinMath.intMultiply;
import static arc.BuiltinMath.intNegate;
import static arc.BuiltinMath.intSqrt;
import static arc.BuiltinMath.intSubtract;

/** 有理数。
 * 
 * 浮動小数点演算で結果が Infinity になるような場合は 1/0 を結果とする。
 * 浮動小数点演算で結果が -Infinity になるような場合は -1/0 を結果とする。
 * 浮動小数点演算で結果が NaN になるような場合は RuntimeException を送出
 * する。
 */
public final class Rational extends Number implements Comparable<Rational>
{
    private final Number numerator;
    private final Number denominator;

    private Rational (Number numerator, Number denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }


    /** 引数に等しい有理数を返す。
     * @param x Integer, BigInteger, または Rational
     */
    public static Rational of(Number x) {
        if (x instanceof Integer ||x instanceof BigInteger)
            return new Rational (x, 1);
        else
            return (Rational) x;
    }


    /** 二つの整数から有理数，できれば整数を返す。
     * @param x Integer または BigInteger
     */
    public static Number result(Number x, Number y) {
        if (intCompare(y, 0) < 0) { // 分母を正にそろえる
            x = intNegate(x);
            y = intNegate(y);
        }
        Number gcd = intGCD(x, y);
        x = intDivide(x, gcd);
        y = intDivide(y, gcd);
        if (y.equals(1))
            return x;
        else
            return new Rational (x, y);
    }

    // 可能な限り演算結果は Integer または BigInteger にする

    /** 加算 */
    public Number add(Rational x) { // a/b + c/d = (ad + bc)/bd
        Number a = numerator;
        Number b = denominator;
        Number c = x.numerator;
        Number d = x.denominator;
        if (b.equals(d)) {      // a/b + c/b = (a + c)/b
            Number a_c = intAdd(a, c);
            return result(a_c, b);
        } else {
            Number ad_bc = intAdd(intMultiply(a, d), intMultiply(b, c));
            Number bd = intMultiply(b, d);
            return result(ad_bc, bd);
        }
    }

    /** 減算 */
    public Number subtract(Rational x) { // a/b - c/d = (ad - bc)/bd
        Number a = numerator;
        Number b = denominator;
        Number c = x.numerator;
        Number d = x.denominator;
        if (b.equals(d)) {      // a/b - c/b = (a - c)/b
            Number a_c = intSubtract(a, c);
            return result(a_c, b);
        } else {
            Number ad_bc = intSubtract(intMultiply(a, d), intMultiply(b, c));
            Number bd = intMultiply(b, d);
            return result(ad_bc, bd);
        }
    }

    /** 乗算 */
    public Number multiply(Rational x) { // a/b * c/d = ac/bd
        Number a = numerator;
        Number b = denominator;
        Number c = x.numerator;
        Number d = x.denominator;
        Number ac = intMultiply(a, c);
        Number bd = intMultiply(b, d);
        return result(ac, bd);
    }

    /** 除算 */
    public Number divide(Rational x) { // a/b / c/d = ad/bc
        Number a = numerator;
        Number b = denominator;
        Number c = x.numerator;
        Number d = x.denominator;
        Number ac = intMultiply(a, d);
        Number bd = intMultiply(b, c);
        return result(ac, bd);
    }

    /** 符号逆転 */
    public Rational negate() {
        Number x = intNegate(numerator);
        return new Rational (x, denominator);
    }

    /** 数をゼロの方向へ丸めた整数を得る。*/
    public Number trunc() {
        return intDivide(numerator, denominator);
    }

    /** 剰余 */
    public Number remainder(Rational x) {
        Number div = this.divide(x);
        if (div instanceof Rational) {
            Number q = ((Rational) div).trunc();
            Number m = x.multiply(Rational.of(q));
            return this.subtract(Rational.of(m));
        } else {                // 割った結果が整数ならば
            return 0;
        }
    }

    /** 平方根。結果は Double かもしれない。*/
    public Number sqrt() {
        Number a = intSqrt(numerator);
        Number b = intSqrt(denominator);
        if (a instanceof Double || b instanceof Double)
            return a.doubleValue() / b.doubleValue();
        else
            return result(a, b);
    }

    // a/b - c/d = (ad - bc)/bd <=> 0
    @Override public int compareTo(Rational x) {
        Number a = numerator;
        Number b = denominator; // result メソッドにより b >= 0
        Number c = x.numerator;
        Number d = x.denominator; // result メソッドにより d >= 0
        if (b.equals(d)) {        // a/b - c/b = (a - c)/b <=> 0
            return intCompare(a, c);
        } else {
            return intCompare(intMultiply(a, d), intMultiply(b, c));
        }
    }


    @Override public double doubleValue() {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    /** 未実装 */
    @Override public float floatValue() {
        throw new UnsupportedOperationException ();
    }

    /** 未実装 */
    @Override public int intValue() {
        throw new UnsupportedOperationException ();
    }

    /** 未実装 */
    @Override public long longValue() {
        throw new UnsupportedOperationException ();
    }

    @Override public String toString() {
        return numerator + "/" + denominator;
    }

    /** 既約であると仮定して等しいかどうか調べる。 */
    @Override public boolean equals(Object x) {
        if (x instanceof Rational) {
            Rational r = (Rational) x;
            return (numerator.equals(r.numerator) &&
                    denominator.equals(r.denominator));
        } else {
            return false;
        }
    }

    /** 既約であると仮定してハッシュ値を求める。*/
    @Override public int hashCode() {
        return numerator.hashCode() ^ (denominator.hashCode() << 5);
    }
} // Rational
