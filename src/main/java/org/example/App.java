package org.example;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jtransforms.fft.DoubleFFT_1D;

public class App {

    public static void main(String[] args) throws Exception {
        String filePath = "src/main/java/org/example/audio.wav";
        int blockSize = 512;
        int shift = 1;
        int duration = 5;
        double threshold = 1000000.0; // Example threshold, adjust as needed

        long startTime = System.currentTimeMillis();

        List<double[]> frequencyAmplitudePairs = blockFourierAnalysis(filePath, blockSize, shift, duration, threshold);
        for (double[] pair : frequencyAmplitudePairs) {
            System.out.printf("%.2f Hz - %.2f\n", pair[0], pair[1]);
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.printf("Analysis completed in %.2f seconds.\n", elapsedTime / 1000.0);
    }

    private static List<double[]> blockFourierAnalysis(String filePath, int blockSize, int shift, int duration, double threshold) throws Exception {
        List<double[]> frequencyAmplitudePairs = new ArrayList<>();

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
        double[] amplitudeSum = new double[blockSize / 2];

        for (int i = 0; i <= n - blockSize; i += shift) {
            double[] block = new double[blockSize];
            System.arraycopy(data, i, block, 0, blockSize);

            DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
            fft.realForward(block);

            double[] localAmplitudeSum = new double[blockSize / 2];
            for (int j = 0; j < blockSize / 2; j++) {
                double real = block[2 * j];
                double imag = block[2 * j + 1];
                double amplitude = Math.sqrt(real * real + imag * imag);
                localAmplitudeSum[j] = amplitude;
            }

            for (int j = 0; j < blockSize / 2; j++) {
                amplitudeSum[j] += localAmplitudeSum[j];
            }
        }

        double samplingRate = getSamplingRate(filePath);
        for (int j = 0; j < blockSize / 2; j++) {
            double frequency = j * samplingRate / blockSize;
            double averageAmplitude = amplitudeSum[j] / totalIterations;
            if (averageAmplitude > threshold) {
                frequencyAmplitudePairs.add(new double[]{frequency, averageAmplitude});
            }
        }

        return frequencyAmplitudePairs;
    }

    private static double getSamplingRate(String filePath) throws Exception {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();
        return format.getSampleRate();
    }
}
