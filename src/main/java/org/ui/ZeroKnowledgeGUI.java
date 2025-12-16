package org.ui;

import org.controller.ProtocolController;
import org.controller.ProtocolController.Color;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Графический интерфейс для демонстрации протокола
 * доказательства с нулевым разглашением
 */
public class ZeroKnowledgeGUI extends JFrame implements ProtocolController.ProtocolListener {

    private JTextArea logArea;
    private JButton startProtocolButton;
    private JButton singleRoundButton;
    private JButton generateGraphButton;
    private JButton loadFromFileButton;
    private JButton stopProtocolButton;
    private JSpinner vertexSpinner;
    private JSpinner edgeSpinner;
    private JSpinner rsaBitSpinner;
    private JSpinner aParamSpinner;
    private JProgressBar progressBar;
    private JLabel resultLabel;
    private JLabel graphInfoLabel;
    private JLabel timeLabel;

    private ProtocolController controller;

    public ZeroKnowledgeGUI() {
        setTitle("Zero-Knowledge Proof: Graph 3-Coloring");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        controller = new ProtocolController();
        controller.addListener(this);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.WEST);

        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.CENTER);

        JPanel resultPanel = createResultPanel();
        add(resultPanel, BorderLayout.SOUTH);

        pack();
        setSize(1100, 700);
        setLocationRelativeTo(null);

        checkGraphFileOnStartup();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Управление протоколом"));

        JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        paramPanel.add(new JLabel("Вершин:"));
        vertexSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        paramPanel.add(vertexSpinner);

        paramPanel.add(new JLabel("Ребер:"));
        edgeSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 2000, 50));
        paramPanel.add(edgeSpinner);

        paramPanel.add(new JLabel("RSA бит:"));
        rsaBitSpinner = new JSpinner(new SpinnerNumberModel(128, 64, 512, 32));
        paramPanel.add(rsaBitSpinner);

        paramPanel.add(new JLabel("Параметр a:"));
        aParamSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.01, 1.0, 0.05));
        paramPanel.add(aParamSpinner);

        JPanel perfPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        perfPanel.add(new JLabel("Рекомендации:"));
        JLabel recommendationLabel = new JLabel("<html><font color='blue'>Для быстрого теста: ≤100 вершин, ≤200 ребер, RSA 128 бит, a=0.1</font></html>");
        perfPanel.add(recommendationLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        generateGraphButton = new JButton("Сгенерировать граф");
        generateGraphButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateGraph();
            }
        });
        buttonPanel.add(generateGraphButton);

        loadFromFileButton = new JButton("Загрузить из graph.txt");
        loadFromFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGraphFromFile();
            }
        });
        buttonPanel.add(loadFromFileButton);

        singleRoundButton = new JButton("Тестовый раунд");
        singleRoundButton.setEnabled(false);
        singleRoundButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeTestRound();
            }
        });
        buttonPanel.add(singleRoundButton);

        startProtocolButton = new JButton("Начать протокол");
        startProtocolButton.setEnabled(false);
        startProtocolButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeFullProtocol();
            }
        });
        buttonPanel.add(startProtocolButton);

        stopProtocolButton = new JButton("Остановить");
        stopProtocolButton.setEnabled(false);
        stopProtocolButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopProtocol();
            }
        });
        buttonPanel.add(stopProtocolButton);

        panel.add(paramPanel);
        panel.add(perfPanel);
        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Информация о графе"));
        panel.setPreferredSize(new Dimension(300, 200));

        graphInfoLabel = new JLabel("<html><center>Граф не загружен<br>"
                + "Сгенерируйте новый или загрузите из файла</center></html>");
        graphInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        graphInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JTextArea detailsArea = new JTextArea(10, 25);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setText("Детали графа:\n-------------\nВершин: 0\nРебер: 0\nРаскраска: Не проверена");

        JScrollPane scrollPane = new JScrollPane(detailsArea);

        panel.add(graphInfoLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ход выполнения протокола"));

        logArea = new JTextArea(20, 70);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = new JButton("Очистить лог");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logControlPanel.add(clearLogButton);

        timeLabel = new JLabel("Время: --:--");
        logControlPanel.add(timeLabel);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(logControlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.NORTH);

        resultLabel = new JLabel("Готов к работе");
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(resultLabel, BorderLayout.CENTER);

        return panel;
    }

    private void checkGraphFileOnStartup() {
        try {
            java.io.File file = new java.io.File("graph.txt");
            if (file.exists()) {
                log("Обнаружен файл graph.txt. Для загрузки нажмите 'Загрузить из graph.txt'");
            } else {
                log("Файл graph.txt не найден. Сгенерируйте новый граф или создайте файл вручную.");
            }
        } catch (Exception e) {
            log("Ошибка при проверке файла: " + e.getMessage());
        }
    }

    private void generateGraph() {
        int n = (int) vertexSpinner.getValue();
        int maxEdges = (int) edgeSpinner.getValue();
        controller.generateGraph(n, maxEdges);
    }

    private void loadGraphFromFile() {
        controller.loadGraphFromFile();
    }

    private void executeTestRound() {
        int rsaBitLength = (int) rsaBitSpinner.getValue();
        controller.executeTestRound(rsaBitLength);
    }

    private void executeFullProtocol() {
        int rsaBitLength = (int) rsaBitSpinner.getValue();
        double a = (double) aParamSpinner.getValue();
        controller.executeFullProtocol(rsaBitLength, a);
    }

    private void stopProtocol() {
        controller.stopProtocol();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onLogMessage(String message) {
        log(message);
    }

    @Override
    public void onProgressUpdate(int current, int total) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(total);
            progressBar.setValue(current);
            progressBar.setString(current + "/" + total);
        });
    }

    @Override
    public void onResultUpdate(String result, Color color) {
        SwingUtilities.invokeLater(() -> {
            resultLabel.setText(result);
            switch (color) {
                case GREEN:
                    resultLabel.setForeground(java.awt.Color.GREEN);
                    break;
                case RED:
                    resultLabel.setForeground(java.awt.Color.RED);
                    break;
                case ORANGE:
                    resultLabel.setForeground(java.awt.Color.ORANGE);
                    break;
            }
        });
    }

    @Override
    public void onGraphInfoUpdate(String info) {
        SwingUtilities.invokeLater(() -> {
            graphInfoLabel.setText("<html><center>" + info.replace("\n", "<br>") + "</center></html>");
        });
    }

    @Override
    public void onProtocolStarted() {
        SwingUtilities.invokeLater(() -> {
            startProtocolButton.setEnabled(false);
            stopProtocolButton.setEnabled(true);
            singleRoundButton.setEnabled(false);
            generateGraphButton.setEnabled(false);
            loadFromFileButton.setEnabled(false);
        });
    }

    @Override
    public void onProtocolStopped() {
        SwingUtilities.invokeLater(() -> {
            stopProtocolButton.setEnabled(false);
            startProtocolButton.setEnabled(true);
            singleRoundButton.setEnabled(true);
            generateGraphButton.setEnabled(true);
            loadFromFileButton.setEnabled(true);
        });
    }

    @Override
    public void onProtocolCompleted(boolean success, int successfulRounds, int totalRounds, long totalTime) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                log("\n✓ ПРОТОКОЛ УСПЕШНО ЗАВЕРШЕН!");
                log("  Все " + totalRounds + " раундов пройдены успешно.");
                log("  Общее время выполнения: " + totalTime + " мс");
                log("  Среднее время на раунд: " + (totalTime / (double) totalRounds) + " мс");

                double deceptionProbability = Math.exp(-((double)aParamSpinner.getValue()));
                log(String.format("  Вероятность успешного обмана: %.10f", deceptionProbability));
                log(String.format("  Достоверность доказательства: %.10f", 1 - deceptionProbability));

                resultLabel.setText("Протокол успешно завершен за " + totalTime + " мс!");
                resultLabel.setForeground(java.awt.Color.GREEN);
            } else {
                log("\n✗ ПРОТОКОЛ ПРЕРВАН!");
                log("  Успешных раундов до прерывания: " + successfulRounds + "/" + totalRounds);
                log("  Общее время выполнения: " + totalTime + " мс");

                resultLabel.setText("Протокол прерван: доказательство не принято!");
                resultLabel.setForeground(java.awt.Color.RED);
            }
        });
    }

    @Override
    public void onGraphLoaded() {
        SwingUtilities.invokeLater(() -> {
            singleRoundButton.setEnabled(true);
            startProtocolButton.setEnabled(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            ZeroKnowledgeGUI gui = new ZeroKnowledgeGUI();
            gui.setVisible(true);

            java.io.File graphFile = new java.io.File("graph.txt");
            if (graphFile.exists() && graphFile.length() > 0) {
                int response = JOptionPane.showConfirmDialog(gui,
                        "Обнаружен файл graph.txt.\nЗагрузить граф из этого файла?",
                        "Автозагрузка графа",
                        JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    gui.loadGraphFromFile();
                }
            }
        });
    }
}