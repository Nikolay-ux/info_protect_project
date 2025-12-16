package org.graphUtils;

import java.io.*;
import java.util.*;

public class GenerateGraph {
    private static final String[] COLORS = {"red", "blue", "yellow"};
    private static final Random random = new Random();

    private int n;
    private int m;
    private List<Integer>[] adjacencyList;
    private String[] vertexColors;
    private List<int[]> edgesList;

    public GenerateGraph(int n) {
        this.n = n;
        this.adjacencyList = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adjacencyList[i] = new ArrayList<>();
        }
        this.vertexColors = new String[n];
        this.edgesList = new ArrayList<>();
    }

    public GenerateGraph(String filename) throws IOException {
        loadFromFile(filename);
    }

    public void generateGraph(int maxEdges) {
        int maxPossibleEdges = Math.min(maxEdges, n * (n - 1) / 2);

        generateValidColoring();

        int edgesAdded = 0;
        int attempts = 0;
        int maxAttempts = maxPossibleEdges * 10;

        edgesList.clear();
        for (int i = 0; i < n; i++) {
            adjacencyList[i].clear();
        }

        while (edgesAdded < maxPossibleEdges && attempts < maxAttempts) {
            int u = random.nextInt(n);
            int v = random.nextInt(n);

            if (canAddEdge(u, v)) {
                adjacencyList[u].add(v);
                adjacencyList[v].add(u);
                edgesList.add(new int[]{u, v});
                edgesAdded++;
            }
            attempts++;
        }

        this.m = edgesAdded;
    }

    private void generateValidColoring() {
        for (int i = 0; i < n; i++) {
            vertexColors[i] = COLORS[random.nextInt(3)];
        }
    }

    private boolean canAddEdge(int u, int v) {
        return u != v &&
                !adjacencyList[u].contains(v) &&
                !vertexColors[u].equals(vertexColors[v]);
    }

    public void writeToFile(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(n + " " + m);

            boolean[][] writtenEdges = new boolean[n][n];
            for (int u = 0; u < n; u++) {
                for (int v : adjacencyList[u]) {
                    if (!writtenEdges[u][v] && !writtenEdges[v][u]) {
                        writer.println((u + 1) + " " + (v + 1));
                        writtenEdges[u][v] = true;
                        writtenEdges[v][u] = true;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                writer.println((i + 1) + " " + vertexColors[i]);
            }
        }
    }

    private void loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String[] firstLine = reader.readLine().trim().split("\\s+");
            this.n = Integer.parseInt(firstLine[0]);
            this.m = Integer.parseInt(firstLine[1]);

            this.adjacencyList = new ArrayList[n];
            for (int i = 0; i < n; i++) {
                adjacencyList[i] = new ArrayList<>();
            }
            this.vertexColors = new String[n];
            this.edgesList = new ArrayList<>();

            for (int i = 0; i < m; i++) {
                String line = reader.readLine().trim();
                if (line.isEmpty()) {
                    i--;
                    continue;
                }

                String[] parts = line.split("\\s+");
                int u = Integer.parseInt(parts[0]) - 1;
                int v = Integer.parseInt(parts[1]) - 1;

                adjacencyList[u].add(v);
                adjacencyList[v].add(u);

                edgesList.add(new int[]{u, v});
            }

            for (int i = 0; i < n; i++) {
                String line = reader.readLine().trim();
                if (line.isEmpty()) {
                    i--;
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                int vertex = Integer.parseInt(parts[0]) - 1;
                String color = parts[1].trim();

                if (!Arrays.asList(COLORS).contains(color)) {
                    throw new IOException("Неверный цвет вершины " + (vertex + 1) + ": " + color);
                }

                vertexColors[vertex] = color;
            }

            for (int i = 0; i < n; i++) {
                if (vertexColors[i] == null) {
                    throw new IOException("Отсутствует цвет для вершины " + (i + 1));
                }
            }
        }
    }

    public boolean validateColoring() {
        for (int u = 0; u < n; u++) {
            for (int v : adjacencyList[u]) {
                if (vertexColors[u].equals(vertexColors[v])) {
                    System.err.println("Нарушение раскраски: вершины " + (u+1) +
                            " и " + (v+1) + " имеют одинаковый цвет " + vertexColors[u]);
                    return false;
                }
            }
        }
        return true;
    }

    public List<int[]> getEdges() {
        return edgesList;
    }

    public String[] getColoring() {
        return vertexColors.clone();
    }

    public int getVertexCount() {
        return n;
    }

    public int getEdgeCount() {
        return m;
    }

    public static void main(String[] args) {
        try {
            System.out.println("Генерация нового графа...");
            GenerateGraph generator = new GenerateGraph(1000);
            generator.generateGraph(500);
            generator.writeToFile("graph.txt");
            System.out.println("Граф записан в graph.txt");
            System.out.println("Вершин: " + generator.getVertexCount());
            System.out.println("Ребер: " + generator.getEdgeCount());
            System.out.println("Проверка раскраски: " + (generator.validateColoring() ? "OK" : "ERROR"));

            System.out.println("\nЗагрузка графа из файла...");
            GenerateGraph loadedGraph = new GenerateGraph("graph.txt");
            System.out.println("Граф загружен из graph.txt");
            System.out.println("Вершин: " + loadedGraph.getVertexCount());
            System.out.println("Ребер: " + loadedGraph.getEdgeCount());
            System.out.println("Проверка раскраски: " + (loadedGraph.validateColoring() ? "OK" : "ERROR"));

        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}