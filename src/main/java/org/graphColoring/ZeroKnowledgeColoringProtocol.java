package org.graphColoring;

import org.mathUtils.FastModularExponentiation;
import org.rsaGenerator.RSAKeyGenerator;

import java.math.BigInteger;
import java.util.*;

/**
 * Реализация протокола доказательства с нулевым разглашением
 * для задачи раскраски графа в 3 цвета.
 */
public class ZeroKnowledgeColoringProtocol {
    private static final String[] COLORS = {"red", "blue", "yellow"};

    private int n;
    private List<int[]> edges;
    private String[] originalColoring;

    private int rsaBitLength;
    private Random random;

    public ZeroKnowledgeColoringProtocol(int n, List<int[]> edges,
                                         String[] originalColoring,
                                         int rsaBitLength) {
        this.n = n;
        this.edges = edges;
        this.originalColoring = originalColoring;
        this.rsaBitLength = rsaBitLength;
        this.random = new Random();
    }

    /**
     * Шаг 1: Генерация случайной перестановки цветов
     */
    private Map<String, String> generateColorPermutation() {
        List<String> shuffledColors = new ArrayList<>(Arrays.asList(COLORS));
        Collections.shuffle(shuffledColors, random);

        Map<String, String> permutation = new HashMap<>();
        for (int i = 0; i < COLORS.length; i++) {
            permutation.put(COLORS[i], shuffledColors.get(i));
        }

        return permutation;
    }

    /**
     * Извлечение двух последних битов из числа
     */
    private int getLastTwoBits(BigInteger number) {
        return number.and(BigInteger.valueOf(3)).intValue();
    }

    /**
     * Выполнение одного раунда протокола
     */
    public boolean executeRound() {
        // Шаг 1: Генерация перестановки цветов
        Map<String, String> permutation = generateColorPermutation();

        // Шаг 2: Генерация случайных чисел с цветами
        BigInteger[] rValues = new BigInteger[n];
        for (int v = 0; v < n; v++) {
            String permutedColor = permutation.get(originalColoring[v]);

            int colorCode = -1;
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i].equals(permutedColor)) {
                    colorCode = i;
                    break;
                }
            }

            // Генерируем число с учетом длины RSA ключа
            BigInteger r = new BigInteger(rsaBitLength - 10, random).shiftLeft(2);

            if (colorCode == 1) {
                r = r.setBit(0); // 01 - синий
            } else if (colorCode == 2) {
                r = r.setBit(1); // 10 - желтый
            }
            // 00 - красный (ничего не делаем)

            rValues[v] = r;
        }

        // Шаг 3: Генерация RSA ключей
        RSAKeyGenerator.RSAKeyPair[] rsaKeys = new RSAKeyGenerator.RSAKeyPair[n];
        for (int v = 0; v < n; v++) {
            rsaKeys[v] = RSAKeyGenerator.generateKeyPair(rsaBitLength);

            // Убеждаемся, что r < N
            if (rValues[v].compareTo(rsaKeys[v].n) >= 0) {
                int colorBits = getLastTwoBits(rValues[v]);

                BigInteger newR;
                do {
                    newR = new BigInteger(rsaBitLength - 10, random).shiftLeft(2);
                    if (colorBits == 1) {
                        newR = newR.setBit(0);
                    } else if (colorBits == 2) {
                        newR = newR.setBit(1);
                    }
                } while (newR.compareTo(rsaKeys[v].n) >= 0);

                rValues[v] = newR;
            }
        }

        // Шаг 4: Вычисление Z_v = r_v^{d_v} mod N_v
        BigInteger[] zValues = new BigInteger[n];
        for (int v = 0; v < n; v++) {
            zValues[v] = FastModularExponentiation.powMod(
                    rValues[v],
                    rsaKeys[v].d,
                    rsaKeys[v].n
            );
        }

        // Шаг 5: Боб выбирает случайное ребро
        int edgeIndex = random.nextInt(edges.size());
        int v1 = edges.get(edgeIndex)[0];
        int v2 = edges.get(edgeIndex)[1];

        // Шаг 6-7: Проверка
        BigInteger r1Prime = FastModularExponentiation.powMod(
                zValues[v1], rsaKeys[v1].e, rsaKeys[v1].n
        );
        BigInteger r2Prime = FastModularExponentiation.powMod(
                zValues[v2], rsaKeys[v2].e, rsaKeys[v2].n
        );

        int bits1 = getLastTwoBits(r1Prime);
        int bits2 = getLastTwoBits(r2Prime);

        // Цвета должны быть различны
        return bits1 != bits2;
    }
}