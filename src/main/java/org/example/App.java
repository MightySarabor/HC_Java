package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jtransforms.fft.DoubleFFT_1D;

public class App extends JFrame {

    public App(String title, List<Long> memoryUsage, double samplingRate, int blockSize, int shift) {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        XYSeries series = new XYSeries("Current Usage");
        for (int i = 0; i < memoryUsage.size(); i++) {
            series.add(i * blockSize / samplingRate, memoryUsage.get(i) / (1024 * 1024.0));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Memory Usage Over Time",
                "Time (seconds)",
                "Current Usage (MB)",
                dataset
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        String filePath = "src/main/java/org/example/audio.wav";
        int blockSize = 512;
        int shift = 1;
        int duration = 60;

        List<Long> memoryUsage = blockFourierAnalysis(filePath, blockSize, shift, duration);
        double samplingRate = getSamplingRate(filePath);
        SwingUtilities.invokeLater(() -> new App("Memory Usage Plot", memoryUsage, samplingRate, blockSize, shift));
    }

    private static List<Long> blockFourierAnalysis(String filePath, int blockSize, int shift, int duration) throws Exception {
        List<Long> memoryUsage = new ArrayList<>();

        long startTime = System.nanoTime();

        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int n = Math.min((int) (format.getSampleRate() * duration), (int) audioInputStream.getFrameLength());
        byte[] audioBytes = new byte[n * bytesPerSample];
        audioInputStream.read(audioBytes);

        double[] data = new double[n];
        for (int i = 0; i < n; i++) {
            int sample = 0;
            for (int j = 0; j < bytesPerSample; j++) {
                sample |= (audioBytes[i * bytesPerSample + j] & 0xFF) << (j * 8);
            }
            data[i] = sample;
        }

        int totalIterations = (n - blockSize + 1) / shift;

        for (int i = 0; i <= n - blockSize; i += shift) {
            double[] block = new double[blockSize];
            System.arraycopy(data, i, block, 0, blockSize);

            DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
            fft.realForward(block);

            long current = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            memoryUsage.add(current);

            double progress = (i / (double) totalIterations) * 100;
            System.out.printf("Progress: %.2f%%\n", progress);
        }

        return memoryUsage;
    }

    private static double getSamplingRate(String filePath) throws Exception {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();
        return format.getSampleRate();
    }
}
