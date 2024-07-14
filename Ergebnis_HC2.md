## Ergebnisbericht HC Aufgabe 2

In dieser Aufgabe haben wir uns noch weiter mit der Fourier Transformation auseinandergesetzt. Neben der sequenziellen Umsetzung haben wir auch eine parallele. Ich habe mit jeweils den gleichen Parametern die Transformation ausgeführt und die Geschwindigkeit der Ausführung verglichen. Die Hypothese ist, dass für jeden weiteren Thread sich die Geschwindigkeit linear erhöht. Mein Laptop hat 16 Kerne, bei der Ausführung der parallelen Berechnung habe ich auch alle verwendet.

Ich habe die Codes mit zwei Audiodateien getestet. Einmal mit der aus Aufgabe 1. Dort habe ich eine Tendenz entdeckt, dass sich die parallele Berechnung mehr lohnt, je länger die Berechnung dauert. Deswegen habe ich noch eine eigene Datei erstellt, die statt 120 Sekunden 300 Sekunden lang ist. Blockgröße, Shift und Schwellwert sind dabei gleich geblieben. Block: 512, Shift: 1, Schwellwert: 1000000. Das sind die Ergebnisse:

### Experiment 1 (120 Sekunden)

- Sequenziell: 38,68 Sekunden
- Parallel: 6,40 Sekunden
- Geschwindigkeitssteigerung: ca. 6,05-fach

### Experiment 2 (60 Sekunden)

- Sequenziell: 19,12 Sekunden
- Parallel: 3,19 Sekunden
- Geschwindigkeitssteigerung: ca. 5,99-fach

### Experiment 3 (20 Sekunden)

- Sequenziell: 6,38 Sekunden
- Parallel: 1,45 Sekunden
- Geschwindigkeitssteigerung: ca. 4,40-fach

### Experiment 4 (5 Sekunden)

- Sequenziell: 1,70 Sekunden
- Parallel: 0,53 Sekunden
- Geschwindigkeitssteigerung: ca. 3,21-fach

Zu sehen ist, dass der Faktor mit längeren Dateien steigt. Wie sieht es mit noch längeren Dateien aus?

## Vergleich der Analysezeiten

### Sequenzielle vs. Parallele Berechnung

#### Experiment 1 (300 Sekunden)

- Sequenziell: 128,68 Sekunden
- Parallel: 28,94 Sekunden (Durchschnitt der beiden Messungen)
- Geschwindigkeitssteigerung: ca. 4,45-fach

#### Experiment 2 (200 Sekunden)

- Sequenziell: 71,55 Sekunden
- Parallel: 19,35 Sekunden
- Geschwindigkeitssteigerung: ca. 3,70-fach

#### Experiment 3 (100 Sekunden)

- Sequenziell: 32,85 Sekunden
- Parallel: 8,11 Sekunden
- Geschwindigkeitssteigerung: ca. 4,05-fach

#### Experiment 4 (50 Sekunden)

- Sequenziell: 16,77 Sekunden
- Parallel: 3,11 Sekunden
- Geschwindigkeitssteigerung: ca. 5,39-fach

#### Experiment 5 (5 Sekunden)

- Sequenziell: 1,88 Sekunden
- Parallel: 0,57 Sekunden
- Geschwindigkeitssteigerung: ca. 3,30-fach

Zu sehen ist, dass der Faktor nicht konstant ist. Das könnte daran liegen, dass die Frequenz eine andere ist. Wenn ich das Thema weiter untersuchen würde, würde ich neben der Zeit auch die anderen Parameter verändern und beobachten, wie die Verarbeitungszeit skaliert.
