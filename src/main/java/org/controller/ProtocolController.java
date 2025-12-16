package org.controller;

import org.graphColoring.ZeroKnowledgeColoringProtocol;
import org.graphUtils.GenerateGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class ProtocolController {

    private static final String GRAPH_FILE = "graph.txt";
    private static final String[] VALID_COLORS = {"red", "blue", "yellow"};

    private int vertexCount;
    private int edgeCount;
    private List<int[]> edges;
    private String[] coloring;

    private ZeroKnowledgeColoringProtocol protocol;
    private boolean protocolRunning;
    private Thread protocolThread;

    private List<ProtocolListener> listeners = new ArrayList<>();

    public interface ProtocolListener {
        void onLogMessage(String message);
        void onProgressUpdate(int current, int total);
        void onResultUpdate(String result, Color color);
        void onGraphInfoUpdate(String info);
        void onProtocolStarted();
        void onProtocolStopped();
        void onProtocolCompleted(boolean success, int successfulRounds, int totalRounds, long totalTime);
        void onGraphLoaded();
    }

    public enum Color {
        GREEN, RED, ORANGE
    }

    public void addListener(ProtocolListener listener) {
        listeners.add(listener);
    }

    private void notifyLog(String message) {
        for (ProtocolListener listener : listeners) {
            listener.onLogMessage(message);
        }
    }

    private void notifyProgress(int current, int total) {
        for (ProtocolListener listener : listeners) {
            listener.onProgressUpdate(current, total);
        }
    }

    private void notifyResult(String result, Color color) {
        for (ProtocolListener listener : listeners) {
            listener.onResultUpdate(result, color);
        }
    }

    private void notifyGraphInfo(String info) {
        for (ProtocolListener listener : listeners) {
            listener.onGraphInfoUpdate(info);
        }
    }

    private void notifyProtocolStarted() {
        for (ProtocolListener listener : listeners) {
            listener.onProtocolStarted();
        }
    }

    private void notifyProtocolStopped() {
        for (ProtocolListener listener : listeners) {
            listener.onProtocolStopped();
        }
    }

    private void notifyProtocolCompleted(boolean success, int successfulRounds, int totalRounds, long totalTime) {
        for (ProtocolListener listener : listeners) {
            listener.onProtocolCompleted(success, successfulRounds, totalRounds, totalTime);
        }
    }

    private void notifyGraphLoaded() {
        for (ProtocolListener listener : listeners) {
            listener.onGraphLoaded();
        }
    }

    /**
     * Генерация нового графа
     */
    public void generateGraph(int n, int maxEdges) {
        try {
            notifyLog("\n=== ГЕНЕРАЦИЯ НОВОГО ГРАФА ===");
            notifyLog("Количество вершин: " + n);
            notifyLog("Максимальное количество ребер: " + maxEdges);

            GenerateGraph graphGenerator = new GenerateGraph(n);
            graphGenerator.generateGraph(maxEdges);

            edges = graphGenerator.getEdges();
            coloring = graphGenerator.getColoring();
            vertexCount = n;
            edgeCount = edges.size();

            graphGenerator.writeToFile(GRAPH_FILE);

            updateGraphInfo();
            notifyLog("Граф успешно сгенерирован и сохранен в " + GRAPH_FILE);
            notifyLog("Количество ребер: " + edgeCount);

            if (validateColoring()) {
                notifyLog("✓ Проверка раскраски: граф правильно раскрашен");
                notifyResult("Граф сгенерирован. Готов к выполнению протокола.", Color.GREEN);
            } else {
                notifyLog("✗ ВНИМАНИЕ: Сгенерированный граф имеет неправильную раскраску!");
                notifyResult("Граф сгенерирован, но есть ошибки раскраски!", Color.ORANGE);
            }

            notifyGraphLoaded();

        } catch (Exception e) {
            notifyLog("Ошибка при генерации графа: " + e.getMessage());
            notifyResult("Ошибка генерации графа!", Color.RED);
            e.printStackTrace();
        }
    }

    public void loadGraphFromFile() {
        try {
            notifyLog("\n=== ЗАГРУЗКА ГРАФА ИЗ ФАЙЛА ===");
            notifyLog("Загрузка из файла: " + GRAPH_FILE);

            BufferedReader reader = new BufferedReader(new FileReader(GRAPH_FILE));

            String line = reader.readLine().trim();
            String[] firstLine = line.split("\\s+");
            if (firstLine.length != 2) {
                throw new IOException("Неверный формат первой строки: " + line);
            }

            vertexCount = Integer.parseInt(firstLine[0]);
            edgeCount = Integer.parseInt(firstLine[1]);

            notifyLog("Вершин: " + vertexCount);
            notifyLog("Ребер: " + edgeCount);

            edges = new ArrayList<>();
            coloring = new String[vertexCount];

            for (int i = 0; i < edgeCount; i++) {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("Неожиданный конец файла при чтении ребер");
                }
                line = line.trim();
                if (line.isEmpty()) {
                    i--;
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    throw new IOException("Неверный формат ребра: " + line);
                }

                int u = Integer.parseInt(parts[0]) - 1;
                int v = Integer.parseInt(parts[1]) - 1;

                if (u < 0 || u >= vertexCount || v < 0 || v >= vertexCount) {
                    throw new IOException("Неверный номер вершины в ребре: " + line);
                }

                edges.add(new int[]{u, v});
            }

            int verticesRead = 0;
            while ((line = reader.readLine()) != null && verticesRead < vertexCount) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) {
                    throw new IOException("Неверный формат цвета вершины: " + line);
                }

                int vertexIndex = Integer.parseInt(parts[0]) - 1;
                String color = parts[1].toLowerCase();

                boolean validColor = false;
                for (String valid : VALID_COLORS) {
                    if (valid.equals(color)) {
                        validColor = true;
                        break;
                    }
                }

                if (!validColor) {
                    throw new IOException("Неверный цвет для вершины " + (vertexIndex + 1) + ": " + color);
                }

                if (vertexIndex < 0 || vertexIndex >= vertexCount) {
                    throw new IOException("Неверный номер вершины: " + (vertexIndex + 1));
                }

                coloring[vertexIndex] = color;
                verticesRead++;
            }

            reader.close();

            for (int i = 0; i < vertexCount; i++) {
                if (coloring[i] == null) {
                    throw new IOException("Отсутствует цвет для вершины " + (i + 1));
                }
            }

            updateGraphInfo();
            notifyLog("Граф успешно загружен из " + GRAPH_FILE);

            if (validateColoring()) {
                notifyLog("✓ Проверка раскраски: граф правильно раскрашен");
                notifyResult("Граф загружен. Готов к выполнению протокола.", Color.GREEN);
            } else {
                notifyLog("✗ ВНИМАНИЕ: Загруженный граф имеет неправильную раскраску!");
                notifyResult("Граф загружен, но имеет ошибки раскраски!", Color.ORANGE);
            }

            notifyGraphLoaded();

        } catch (IOException e) {
            notifyLog("Ошибка при загрузке графа: " + e.getMessage());
            notifyResult("Ошибка загрузки графа!", Color.RED);
        } catch (NumberFormatException e) {
            notifyLog("Ошибка формата числа в файле: " + e.getMessage());
            notifyResult("Ошибка формата файла!", Color.RED);
        }
    }

    /**
     * Проверка правильности раскраски
     */
    private boolean validateColoring() {
        if (edges == null || coloring == null) {
            return false;
        }

        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];

            if (u >= coloring.length || v >= coloring.length) {
                notifyLog("Ошибка: вершина за пределами массива цветов");
                return false;
            }

            if (coloring[u] == null || coloring[v] == null) {
                notifyLog("Ошибка: отсутствует цвет для вершины");
                return false;
            }

            if (coloring[u].equals(coloring[v])) {
                notifyLog("Нарушение раскраски: вершины " + (u+1) +
                        " и " + (v+1) + " имеют одинаковый цвет " + coloring[u]);
                return false;
            }
        }

        return true;
    }

    private void updateGraphInfo() {
        String info = String.format("Граф: %d вершин, %d ребер", vertexCount, edgeCount);
        notifyGraphInfo(info);
    }

    public void executeTestRound(int rsaBitLength) {
        if (edges == null || coloring == null) {
            notifyResult("Сначала загрузите или сгенерируйте граф", Color.RED);
            return;
        }

        protocol = new ZeroKnowledgeColoringProtocol(
                vertexCount,
                edges,
                coloring,
                rsaBitLength
        );

        notifyLog("\n=== ТЕСТОВЫЙ РАУНД (один) ===");
        notifyLog("Параметры:");
        notifyLog("  Вершин: " + vertexCount);
        notifyLog("  Ребер: " + edgeCount);
        notifyLog("  RSA бит: " + rsaBitLength);

        long startTime = System.currentTimeMillis();

        try {
            boolean result = protocol.executeRound();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            notifyLog("\nВремя выполнения: " + duration + " мс");

            if (result) {
                notifyLog("✓ Проверка пройдена: цвета смежных вершин различны");
                notifyResult("Тест пройден успешно за " + duration + " мс", Color.GREEN);
            } else {
                notifyLog("✗ Обнаружена попытка обмана!");
                notifyLog("  Возможные причины:");
                notifyLog("  1. Граф неправильно раскрашен");
                notifyLog("  2. Ошибка в реализации протокола");
                notifyLog("  3. Проблемы с генерацией RSA ключей");
                notifyResult("Обнаружена попытка обмана!", Color.RED);
            }

        } catch (Exception e) {
            notifyLog("✗ Ошибка при выполнении раунда: " + e.getMessage());
            notifyResult("Ошибка при выполнении!", Color.RED);
            e.printStackTrace();
        }
    }

    public void executeFullProtocol(int rsaBitLength, double a) {
        if (edges == null || coloring == null) {
            notifyResult("Сначала загрузите или сгенерируйте граф", Color.RED);
            return;
        }

        protocol = new ZeroKnowledgeColoringProtocol(
                vertexCount,
                edges,
                coloring,
                rsaBitLength
        );

        int totalRounds = (int)(a * edgeCount);

        if (totalRounds > 1000) {
            totalRounds = 1000;
            notifyLog("Внимание: количество раундов ограничено 1000 для производительности");
        }

        if (totalRounds < 1) {
            totalRounds = 1;
        }

        notifyLog("\n=== ЗАПУСК ПОЛНОГО ПРОТОКОЛА ===");
        notifyLog("Параметры протокола:");
        notifyLog("  Вершин в графе: " + vertexCount);
        notifyLog("  Ребер в графе: " + edgeCount);
        notifyLog("  Количество раундов: " + totalRounds + " (a|E|, где a=" + a + ")");
        notifyLog("  Длина RSA ключей: " + rsaBitLength + " бит");

        protocolRunning = true;
        notifyProtocolStarted();

        int finalTotalRounds = totalRounds;
        protocolThread = new Thread(() -> {
            boolean allPassed = true;
            int successfulRounds = 0;
            long startTime = System.currentTimeMillis();

            try {
                for (int round = 0; round < finalTotalRounds && protocolRunning; round++) {
                    final int currentRound = round + 1;

                    notifyProgress(currentRound, finalTotalRounds);

                    boolean roundResult = false;
                    try {
                        roundResult = protocol.executeRound();
                    } catch (Exception e) {
                        notifyLog("✗ Раунд " + currentRound + ": исключение - " + e.getMessage());
                        allPassed = false;
                        break;
                    }

                    if (roundResult) {
                        successfulRounds++;
                    } else {
                        allPassed = false;
                        notifyLog("✗ Раунд " + currentRound + ": обнаружена попытка обмана!");
                        break;
                    }
                }
            } finally {
                long totalTime = System.currentTimeMillis() - startTime;
                protocolRunning = false;

                notifyProtocolCompleted(allPassed, successfulRounds, finalTotalRounds, totalTime);
                notifyProtocolStopped();
            }
        });

        protocolThread.start();
    }

    public void stopProtocol() {
        if (protocolRunning && protocolThread != null) {
            protocolRunning = false;
            protocolThread.interrupt();
            notifyLog("Протокол остановлен пользователем");
            notifyResult("Протокол остановлен", Color.ORANGE);
        }
    }

    public boolean isGraphLoaded() {
        return edges != null && coloring != null;
    }
}