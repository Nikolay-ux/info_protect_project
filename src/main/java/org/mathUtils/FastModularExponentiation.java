package org.mathUtils;

import java.math.BigInteger;

public class FastModularExponentiation {

    /**
     * Расширенный алгоритм Евклида для нахождения НОД и коэффициентов Безу.
     *
     * @param a Первое число
     * @param b Второе число
     * @return Массив [НОД, x, y] такие что a*x + b*y = НОД
     */
    public static BigInteger[] extendedGcd(BigInteger a, BigInteger b) {
        if (a.equals(BigInteger.ZERO)) {
            return new BigInteger[]{b, BigInteger.ZERO, BigInteger.ONE};
        }

        BigInteger[] result = extendedGcd(b.mod(a), a);
        BigInteger gcd = result[0];
        BigInteger x = result[2].subtract(b.divide(a).multiply(result[1]));
        BigInteger y = result[1];

        return new BigInteger[]{gcd, x, y};
    }

    /**
     * Нахождение обратного элемента по модулю.
     *
     * @param a Число, для которого ищем обратный элемент
     * @param m Модуль
     * @return Обратный элемент a^-1 mod m
     * @throws IllegalArgumentException если обратный элемент не существует
     */
    public static BigInteger modInverse(BigInteger a, BigInteger m) {
        BigInteger[] gcdResult = extendedGcd(a.mod(m), m);
        BigInteger gcd = gcdResult[0];

        if (!gcd.equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("Обратный элемент не существует");
        }

        BigInteger x = gcdResult[1];
        return x.mod(m).add(m).mod(m);
    }

    /**
     * Быстрое возведение в степень по модулю с поддержкой отрицательных степеней.
     * Использует алгоритм бинарного возведения в степень.
     *
     * @param a Основание
     * @param x Показатель степени (может быть отрицательным)
     * @param p Модуль
     * @return a^x mod p
     */
    public static BigInteger powMod(BigInteger a, BigInteger x, BigInteger p) {
        if (x.compareTo(BigInteger.ZERO) < 0) {
            BigInteger aInverse = modInverse(a, p);
            return powMod(aInverse, x.negate(), p);
        }

        BigInteger y = BigInteger.ONE;
        BigInteger s = a.mod(p);
        String binaryX = x.toString(2);

        for (int i = binaryX.length() - 1; i >= 0; i--) {
            char bit = binaryX.charAt(i);

            if (bit == '1') {
                y = y.multiply(s).mod(p);
            }

            s = s.multiply(s).mod(p);
        }

        return y;
    }

    public static BigInteger powMod(BigInteger a, int x, BigInteger p) {
        return powMod(a, BigInteger.valueOf(x), p);
    }
}