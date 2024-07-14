import numpy as np
import wave
import struct

def generate_wave_file(output_file, duration, sample_rate, num_channels, amplitude, frequency):
    # Anzahl der Samples
    num_samples = duration * sample_rate

    # Zeitarray erzeugen
    time = np.arange(num_samples) / sample_rate

    # Sinuswelle generieren
    signal = np.sin(2 * np.pi * frequency * time)

    # Skalierten und quantisierten Sample-Wert erzeugen
    scaled_samples = (signal * amplitude).astype(np.int16)

    # WAV-Datei erstellen
    with wave.open(output_file, 'w') as wav_file:
        wav_file.setnchannels(num_channels)
        wav_file.setsampwidth(2)  # 2 Bytes für 16-Bit-Audio
        wav_file.setframerate(sample_rate)

        # Daten schreiben
        for sample in scaled_samples:
            wav_file.writeframes(struct.pack('h', sample))

    print(f"Die WAV-Datei '{output_file}' wurde erfolgreich erstellt.")

# Parameter für die WAV-Datei
duration = 300  # Dauer in Sekunden
sample_rate = 44100  # Abtastrate in Hz
num_channels = 1  # Mono
amplitude = 32767  # Maximaler Amplitudenwert für 16-Bit-Audio
frequency = 20000 # Konstante Frequenz in Hz (A4)

# WAV-Datei erstellen
output_file = "constant_frequency.wav"
generate_wave_file(output_file, duration, sample_rate, num_channels, amplitude, frequency)
