package org.rsaGenerator;

import org.mathUtils.FastModularExponentiation;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class RSAKeyGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

    public static class RSAKeyPair {
        public final BigInteger n; // Модуль
        public final BigInteger e; // Открытая экспонента
        public final BigInteger d; // Закрытая экспонента
        public final BigInteger p; // Первое простое число (секретное)
        public final BigInteger q; // Второе простое число (секретное)

        public RSAKeyPair(BigInteger n, BigInteger e, BigInteger d,
                          BigInteger p, BigInteger q) {
            this.n = n;
            this.e = e;
            this.d = d;
            this.p = p;
            this.q = q;
        }
    }

    private static boolean isProbablePrime(BigInteger n, int iterations) {
        if (n.compareTo(BigInteger.valueOf(2)) < 0) return false;
        if (n.equals(BigInteger.valueOf(2))) return true;
        if (n.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO)) return false;

        // Записываем n-1 = d * 2^s
        BigInteger d = n.subtract(BigInteger.ONE);
        int s = 0;
        while (d.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO)) {
            d = d.divide(BigInteger.valueOf(2));
            s++;
        }

        for (int i = 0; i < iterations; i++) {
            BigInteger a = getRandomBigInteger(BigInteger.valueOf(2), n.subtract(BigInteger.ONE));
            BigInteger x = FastModularExponentiation.powMod(a, d, n);

            if (x.equals(BigInteger.ONE) || x.equals(n.subtract(BigInteger.ONE))) {
                continue;
            }

            boolean composite = true;
            for (int r = 0; r < s - 1; r++) {
                x = x.multiply(x).mod(n);
                if (x.equals(n.subtract(BigInteger.ONE))) {
                    composite = false;
                    break;
                }
            }

            if (composite) return false;
        }

        return true;
    }

    public static BigInteger generatePrime(int bitLength) {
        BigInteger prime;
        do {
            prime = new BigInteger(bitLength, random);
            prime = prime.setBit(bitLength - 1); // Гарантируем нужную длину
            prime = prime.or(BigInteger.ONE); // Гарантируем нечетность
        } while (!isProbablePrime(prime, 10)); // 10 итераций теста Миллера-Рабина

        return prime;
    }

    public static RSAKeyPair generateKeyPair(int bitLength) {
        BigInteger p = generatePrime(bitLength / 2);
        BigInteger q;

        do {
            q = generatePrime(bitLength / 2);
        } while (p.equals(q));

        BigInteger n = p.multiply(q);

        BigInteger phi = p.subtract(BigInteger.ONE)
                .multiply(q.subtract(BigInteger.ONE));

        BigInteger e = PUBLIC_EXPONENT;

        BigInteger d = e.modInverse(phi);

        return new RSAKeyPair(n, e, d, p, q);
    }

    private static BigInteger getRandomBigInteger(BigInteger min, BigInteger max) {
        BigInteger range = max.subtract(min);
        int bitLength = range.bitLength();
        BigInteger result;

        do {
            result = new BigInteger(bitLength, random);
        } while (result.compareTo(range) >= 0);

        return result.add(min);
    }
}
