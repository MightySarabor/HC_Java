package org.example;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import org.jtransforms.fft.DoubleFFT_1D;

public class AppParamPara {

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

        double[] frequencies = new double[blockSize / 2];
        double[] amplitudes = blockFourierAnalysis(filePath, blockSize, shift, duration, threshold, frequencies);

        // Ausgabe der Frequenz-Amplituden-Paare
        System.out.println("Frequency (Hz) - Average Amplitude");
        for (int i = 0; i < frequencies.length; i++) {
            if (amplitudes[i] > threshold) {
                System.out.printf("%.2f Hz - %.2f\n", frequencies[i], amplitudes[i]);
            }
        }
    }

    private static double[] blockFourierAnalysis(String filePath, int blockSize, int shift, int duration, double threshold, double[] frequencies) throws Exception {
        double[] amplitudes = new double[blockSize / 2];

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

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.printf("Available processors: %d\n", availableProcessors);

        ForkJoinPool forkJoinPool = new ForkJoinPool(availableProcessors);

        forkJoinPool.submit(() -> {
            IntStream.range(0, totalIterations).parallel().forEach(i -> {
                int start = i * shift;
                if (start + blockSize <= data.length) {
                    double[] block = new double[blockSize];
                    System.arraycopy(data, start, block, 0, blockSize);

                    // Apply a Hann window
                    for (int j = 0; j < block.length; j++) {
                        block[j] *= 0.5 * (1 - Math.cos(2 * Math.PI * j / (block.length - 1)));
                    }

                    DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
                    fft.realForward(block);

                    double[] localAmplitudeSum = new double[blockSize / 2];
                    for (int j = 0; j < blockSize / 2; j++) {
                        double real = block[2 * j];
                        double imag = block[2 * j + 1];
                        double amplitude = Math.sqrt(real * real + imag * imag);
                        localAmplitudeSum[j] = amplitude;
                    }

                    synchronized (amplitudeSum) {
                        for (int j = 0; j < blockSize / 2; j++) {
                            amplitudeSum[j] += localAmplitudeSum[j];
                        }
                    }
                }
            });
        }).get();

        double samplingRate = getSamplingRate(filePath);
        for (int j = 0; j < blockSize / 2; j++) {
            frequencies[j] = j * samplingRate / blockSize;
            amplitudes[j] = amplitudeSum[j] / totalIterations;
        }

        return amplitudes;
    }

    private static double getSamplingRate(String filePath) throws Exception {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();
        return format.getSampleRate();
    }
}
