// H22.10.01/R02.04.29 (鈴)
package arc;

import java.math.BigInteger;
import java.math.BigDecimal;

/** Builtins と Rational のための算術関数集
 * @see Builtins
 * @see Rational
 */
public class BuiltinMath
{
    /** このクラスはインスタンスを作らない。*/
    private BuiltinMath () {}

    /* 整数は Integer か BigInteger のどちらかであることを仮定する。
       Long は Integer の桁あふれを防ぐため過渡的にだけ使う。*/

    /** 整数加算 */
    public static Number intAdd(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer)
                return reg(x.longValue() + y.longValue());
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).add((BigInteger) y));
    }

    /** 整数減算 */
    public static Number intSubtract(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer)
                return reg(x.longValue() - y.longValue());
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).subtract((BigInteger) y));
    }

    /** 整数乗算 */
    public static Number intMultiply(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer)
                return reg(x.longValue() * y.longValue());
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).multiply((BigInteger) y));
    }

    /** 整数除算 */
    public static Number intDivide(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer) // 注: 8bit演算で -128 / -1  は？
                return reg(x.longValue() / y.longValue());
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).divide((BigInteger) y));
    }

    /** 整数剰余 */
    public static Number intRemainder(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer)
                return x.intValue() % y.intValue();
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).remainder((BigInteger) y));
    }

    /** 整数符号反転 */
    public static Number intNegate(Number x) {
        if (x instanceof Integer)
            return reg(- x.longValue());
        else
            return reg(((BigInteger) x).negate());
    }

    /** 最大公約数 */
    public static Number intGCD(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer) {
                int a = x.intValue();
                int b = y.intValue();
                while (b != 0) {
                    int c = a % b;
                    a = b;
                    b = c;
                }
                return (a < 0) ? -a : a;
            } else {
                x = BigInteger.valueOf(x.longValue());
            }
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return reg(((BigInteger) x).gcd((BigInteger) y));
    }

    /** 整数比較 */
    public static int intCompare(Number x, Number y) {
        if (x instanceof Integer)
            if (y instanceof Integer)
                return ((Integer) x).compareTo((Integer) y);
            else
                x = BigInteger.valueOf(x.longValue());
        else if (y instanceof Integer)
            y = BigInteger.valueOf(y.longValue());
        return ((BigInteger) x).compareTo((BigInteger) y);
    }

    /** 整数の平方根。結果はできれば整数だが Double かもしれない。*/
    public static Number intSqrt(Number x) {
        double a = x.doubleValue();
        a = Math.sqrt(a);
        Number n = trunc(a);
        Number n2 = intMultiply(n, n);
        if (intCompare(n2, x) == 0)
            return n;
        else
            return a;
    }


    /** 算術加算。(+ 2 3) ⇒ 5; (+) ⇒ 0
     */
    public static Number add(Cell j) {
        Number x = (Integer) 0;
        while (j != null) {
            Number y = (Number) j.car;
            if (x instanceof Double || y instanceof Double) {
                x = x.doubleValue() + y.doubleValue();
            } else if (x instanceof Rational || y instanceof Rational) {
                Rational r = Rational.of(x);
                Rational s = Rational.of(y);
                x = r.add(s);
            } else {
                x = intAdd(x, y);
            }
            j = j.getCdrCell();
        }
        return x;
    }

    /** 算術乗算。(* 2 3) ⇒ 6; (*) ⇒ 1
     */
    public static Number multiply(Cell j) {
        Number x = (Integer) 1;
        while (j != null) {
            Number y = (Number) j.car;
            if (x instanceof Double || y instanceof Double) {
                x = x.doubleValue() * y.doubleValue();
            } else if (x instanceof Rational || y instanceof Rational) {
                Rational r = Rational.of(x);
                Rational s = Rational.of(y);
                x = r.multiply(s);
            } else {
                x = intMultiply(x, y);
            }
            j = j.getCdrCell();
        }
        return x;
    }

    /** 実数除算。(/ 6 5) ⇒ 6/5; (/ 6.0 5) ⇒ 1.2
     */
    public static Number divideReal(Number a, Number b) {
        if (a instanceof Double || b instanceof Double) {
            double x = a.doubleValue();
            double y = b.doubleValue();
            return x / y;
        } else {
            Rational r = Rational.of(a);
            Rational s = Rational.of(b);
            return r.divide(s);
        }
    }

    /** 整数として除算する。(quotient 6 5) ⇒ 1
     */
    public static Number divideInt(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            x = x.doubleValue() / y.doubleValue();
            x = trunc(x);
            return x.doubleValue();
        } else if (x instanceof Rational || y instanceof Rational) {
            Rational r = Rational.of(x);
            Rational s = Rational.of(y);
            return trunc(r.divide(s));
        } else {
            return intDivide(x, y);
        }
    }

    /** 算術減算。(- 10) ⇒ -10; (- 10 2) ⇒ 8
     */
    public static Number subtract(Number x, Cell yy) {
        if (yy == null) {
            if (x instanceof Double)
                return - x.doubleValue();
            else if (x instanceof Rational)
                return ((Rational) x).negate();
            else
                return intNegate(x);
        } else {
            for (Cell j = yy; j != null; j = j.getCdrCell()) {
                Number y = (Number) j.car;
                if (x instanceof Double || y instanceof Double) {
                    x = x.doubleValue() - y.doubleValue();
                } else if (x instanceof Rational || y instanceof Rational) {
                    Rational r = Rational.of(x);
                    Rational s = Rational.of(y);
                    x = r.subtract(s);
                } else {
                    x = intSubtract(x, y);
                }
            }
            return x;
        }
    }

    /** 算術剰余 (remainder): 13 remainder -4 = 1
     */
    public static Number remainder(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return x.doubleValue() % y.doubleValue();
        } else if (x instanceof Rational || y instanceof Rational) {
            Rational r = Rational.of(x);
            Rational s = Rational.of(y);
            return r.remainder(s);
        } else {
            return intRemainder(x, y);
        }
    }

    /** 算術剰余 (modulo): 13 modulo -4 = -3
     */
    public static Number modulo(Number x, Number y) {
        Number r = remainder(x, y);
        int xsig = compare(x, 0);
        int ysig = compare(y, 0);
        return (xsig * ysig < 0) ? add(LL.list(r, y)) : r;
    }

    /** 算術比較。２引数 x, y について x が y より小さい/等しい/大きい
     * とき，それぞれ負数/0/正数を返す。
     */
    public static int compare(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            double a = x.doubleValue();
            double b = y.doubleValue();
            return (a < b) ? -1 : (a == b) ? 0 : 1;
        } else if (x instanceof Rational || y instanceof Rational) {
            Rational r = Rational.of(x);
            Rational s = Rational.of(y);
            return r.compareTo(s);
        } else {
            return intCompare(x, y);
        }
    }

    /** 数をゼロの方向へ丸めた整数を得る。
     */
    public static Number trunc(Number x) {
        if (x instanceof Double) {
            BigDecimal d = new BigDecimal (x.doubleValue());
            BigInteger b = d.toBigInteger();
            return reg(b);
        } if (x instanceof Rational) {
            return ((Rational) x).trunc();
        } else {
            return x;
        }
    }

    /** 正の平方根を得る。
     */
    public static Number sqrt(Number x) {
        if (x instanceof Double) {
            double a = x.doubleValue();
            return Math.sqrt(a);
        } else if (x instanceof Rational) {
            return ((Rational) x).sqrt();
        } else {
            return intSqrt(x);
        }
    }

    /** x の y 乗を得る。
     */
    public static Number expt(Number x, Number y) {
        if (x instanceof Double ||
            y instanceof Double ||
            y instanceof Rational) {
            double a = x.doubleValue();
            double b = y.doubleValue();
            return Math.exp(Math.log(a) * b);
        } else {
            Number a = 1;
            int n = (Integer) y;
            if (n < 0) {
                int m = -n;
                for (int i = 0; i < m; i++)
                    a = divideReal(a, x);
            } else if (n == 0) {
                if (x.equals(0) || x.equals(POS_INF) || x.equals(NEG_INF))
                    throw new ArithmeticException ("not a number");
            } else {
                for (int i = 0; i < n; i++)
                    a = multiply(LL.list(a, x));
            }
            return a;
        }
    }

    private static final Number POS_INF = Rational.result(1, 0); // 1/0
    private static final Number NEG_INF = Rational.result(-1, 0); // -1/0

    /** 正規化した数のインスタンスを得る。
     * long 値をできれば int 値に，できなければ BigInteger 値にする。
     */
    public static Number reg(long x) {
        int i = (int) x;
        return (i == x) ? i : BigInteger.valueOf(x);
    }

    /** 正規化した数のインスタンスを得る。
     * BigInteger 値をできれば int 値に，できなければそのままにする。
     */
    public static Number reg(BigInteger x) {
        return (x.bitLength() < 32) ? x.intValue() : x;
    }
} // BuiltinMath
