#!/usr/bin/env python3
"""Generates the tiny synthesized sound effects bundled in app/src/main/res/raw.

Run from the project root:  python3 tools/generate_sounds.py
Replace the generated .wav files (same names) with your own recordings at any time.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32000)) for s in samples))


def tone(freq, dur, vol=0.6, decay=6.0, harmonics=((1, 1.0),)):
    n = int(RATE * dur)
    out = []
    for i in range(n):
        t = i / RATE
        env = math.exp(-decay * t) * min(1.0, i / 80)
        v = sum(a * math.sin(2 * math.pi * freq * h * t) for h, a in harmonics)
        out.append(vol * env * v / sum(a for _, a in harmonics))
    return out


def mix(parts, total):
    out = [0.0] * int(RATE * total)
    for start, samples in parts:
        off = int(RATE * start)
        for i, s in enumerate(samples):
            if off + i < len(out):
                out[off + i] += s
    return out


# short wooden "tick" played as the wheel passes a segment
write("sfx_tick.wav", tone(1900, 0.045, 0.7, decay=70))

# rising whoosh when the wheel starts
whoosh = []
n = int(RATE * 0.5)
phase = 0.0
for i in range(n):
    t = i / n
    f = 220 + 900 * t * t
    phase += 2 * math.pi * f / RATE
    whoosh.append(0.4 * math.sin(phase) * math.sin(math.pi * t))
write("sfx_spin.wav", whoosh)

# celebratory bell arpeggio (C major) when a winner is revealed
notes = [523.25, 659.25, 783.99, 1046.5]
parts = [(0.12 * i, tone(f, 0.9, 0.55, decay=4.5, harmonics=((1, 1.0), (2, 0.35), (3, 0.15)))) for i, f in enumerate(notes)]
parts.append((0.5, tone(1318.5, 1.0, 0.4, decay=4.0, harmonics=((1, 1.0), (2, 0.3)))))
write("sfx_win.wav", mix(parts, 1.6))
