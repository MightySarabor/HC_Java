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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.jtransforms.fft.DoubleFFT_1D;

public class AppParamPara extends JFrame {

    public AppParamPara(String title, List<Long> memoryUsage, double samplingRate, int blockSize, int shift) {
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
        if (args.length < 4) {
            System.err.println("Usage: java AppParamPara <filePath> <blockSize> <shift> <threshold>");
            System.exit(1);
        }

        String filePath = args[0];
        int blockSize = Integer.parseInt(args[1]);
        int shift = Integer.parseInt(args[2]);
        double threshold = Double.parseDouble(args[3]);
        int duration = 60;

        List<Long> memoryUsage = blockFourierAnalysis(filePath, blockSize, shift, duration, threshold);
        double samplingRate = getSamplingRate(filePath);
        SwingUtilities.invokeLater(() -> new AppParamPara("Memory Usage Plot", memoryUsage, samplingRate, blockSize, shift));
    }

    private static List<Long> blockFourierAnalysis(String filePath, int blockSize, int shift, int duration, double threshold) throws Exception {
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

        int totalIterations = (n - blockSize) / shift + 1;
        double[] amplitudeSum = new double[blockSize / 2];
        int blockCount = 0;

        ForkJoinPool pool = new ForkJoinPool();

        for (int i = 0; i <= n - blockSize; i += shift) {
            double[] block = new double[blockSize];
            System.arraycopy(data, i, block, 0, blockSize);

            FFTTask task = new FFTTask(block);
            double[] result = pool.invoke(task);

            for (int j = 0; j < blockSize / 2; j++) {
                double real = result[2 * j];
                double imag = result[2 * j + 1];
                double amplitude = Math.sqrt(real * real + imag * imag);
                amplitudeSum[j] += amplitude;
            }
            blockCount++;

            long current = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            memoryUsage.add(current);

            double progress = ((i / (double) shift) / (totalIterations - 1)) * 100;
            System.out.printf("Progress: %.2f%%\n", progress);
        }

        pool.shutdown();

        System.out.println("Frequency Amplitudes above Threshold:");
        double samplingRate = getSamplingRate(filePath);
        for (int j = 0; j < blockSize / 2; j++) {
            double averageAmplitude = amplitudeSum[j] / blockCount;
            if (averageAmplitude > threshold) {
                double frequency = j * samplingRate / blockSize;
                System.out.printf("Frequency: %.2f Hz, Average Amplitude: %.2f\n", frequency, averageAmplitude);
            }
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

class FFTTask extends RecursiveTask<double[]> {
    private final double[] block;

    public FFTTask(double[] block) {
        this.block = block;
    }

    @Override
    protected double[] compute() {
        DoubleFFT_1D fft = new DoubleFFT_1D(block.length);
        fft.realForward(block);
        return block;
    }
}
