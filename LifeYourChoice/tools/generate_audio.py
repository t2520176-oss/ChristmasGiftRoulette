#!/usr/bin/env python3
"""Synthesises the game's sound effects and background music (original, no samples, no licences).

Usage: python3 tools/generate_audio.py   ->  writes app/src/main/res/raw/*.wav
Only the Python standard library is needed.
"""
import math, os, random, struct, wave

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(OUT, exist_ok=True)


def write(name, samples, peak=0.85):
    m = max(1e-9, max(abs(s) for s in samples))
    scale = min(1.0, peak / m) if m > peak else 1.0
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in samples))


def note(freq, dur, amp=0.5, attack=0.005, decay=6.0, harm=(1.0, 0.35, 0.12)):
    n = int(dur * SR)
    out = []
    for i in range(n):
        t = i / SR
        env = min(1.0, t / attack) * math.exp(-decay * t)
        v = sum(h * math.sin(2 * math.pi * freq * (k + 1) * t) for k, h in enumerate(harm))
        out.append(amp * env * v)
    return out


def mix(*tracks):
    n = max(len(t[1]) + int(t[0] * SR) for t in tracks)
    out = [0.0] * n
    for start, samples in tracks:
        o = int(start * SR)
        for i, s in enumerate(samples):
            out[o + i] += s
    return out


NOTE = {"C4": 261.63, "D4": 293.66, "E4": 329.63, "F4": 349.23, "G4": 392.0, "A4": 440.0, "B4": 493.88,
        "C5": 523.25, "D5": 587.33, "E5": 659.25, "G5": 783.99, "C6": 1046.5, "A3": 220.0, "E3": 164.81, "G3": 196.0, "D3": 146.83}

# ---- sound effects
write("sfx_tap", note(880, 0.07, 0.5, 0.002, 38, (1.0, 0.2)))
write("sfx_select", mix((0, note(NOTE["E5"], 0.14, 0.45, decay=14)), (0.07, note(NOTE["A4"] * 2, 0.2, 0.4, decay=10))))
write("sfx_good", mix(*[(i * 0.09, note(f, 0.45, 0.4, decay=5)) for i, f in enumerate([NOTE["C5"], NOTE["E5"], NOTE["G5"]])]))
write("sfx_bad", mix((0, note(NOTE["A3"], 0.5, 0.5, decay=4, harm=(1.0, 0.6, 0.3))), (0.12, note(NOTE["E3"], 0.6, 0.5, decay=3.5, harm=(1.0, 0.6, 0.3)))))
write("sfx_achievement", mix(*[(i * 0.1, note(f, 0.7, 0.35, decay=3.5)) for i, f in enumerate([NOTE["C5"], NOTE["E5"], NOTE["G5"], NOTE["C6"]])],
                             (0.4, note(NOTE["G5"] * 2, 0.9, 0.18, decay=3))))

rnd = random.Random(5)
page = []
lp = 0.0
for i in range(int(0.28 * SR)):
    t = i / (0.28 * SR)
    lp += (rnd.uniform(-1, 1) - lp) * (0.08 + 0.5 * t)
    page.append(lp * math.sin(math.pi * t) * 0.35)
write("sfx_page", page)

chord = [NOTE["C4"], NOTE["E4"], NOTE["G4"], NOTE["C5"]]
motto = []
for f in chord:
    n = int(2.2 * SR)
    track = []
    for i in range(n):
        t = i / SR
        env = (1 - math.exp(-t * 2.5)) * math.exp(-t * 1.1)
        track.append(0.22 * env * (math.sin(2 * math.pi * f * t) + 0.3 * math.sin(2 * math.pi * f * 2 * t)))
    motto.append((0, track))
write("sfx_motto", mix(*motto), 0.8)

# ---- cinematic scene sounds (Version 2A)
def noise_burst(dur, lowpass, amp, decay, seed):
    r = random.Random(seed)
    out, lp = [], 0.0
    for i in range(int(dur * SR)):
        t = i / SR
        lp += (r.uniform(-1, 1) - lp) * lowpass
        out.append(lp * amp * math.exp(-decay * t))
    return out


# knock: two short wooden thumps
write("sfx_knock", mix((0, note(190, 0.12, 0.7, 0.001, 38, (1.0, 0.5, 0.3))), (0.0, noise_burst(0.08, 0.25, 0.5, 40, 1)),
                       (0.22, note(175, 0.12, 0.7, 0.001, 38, (1.0, 0.5, 0.3))), (0.22, noise_burst(0.08, 0.25, 0.5, 40, 2))))

# whoosh: filtered noise swell (a train passing, a quick movement)
whoosh, lp = [], 0.0
rw = random.Random(11)
for i in range(int(0.9 * SR)):
    t = i / (0.9 * SR)
    lp += (rw.uniform(-1, 1) - lp) * (0.04 + 0.35 * math.sin(math.pi * t))
    whoosh.append(lp * math.sin(math.pi * t) ** 1.5 * 0.9)
write("sfx_whoosh", whoosh)

# phone: two short electronic rings
ring = lambda t0: (t0, [0.25 * math.sin(2 * math.pi * 1320 * i / SR) * (0.6 + 0.4 * math.sin(2 * math.pi * 22 * i / SR)) * min(1, i / (0.01 * SR)) * max(0, 1 - i / (0.32 * SR))
                         for i in range(int(0.32 * SR))])
write("sfx_phone", mix(ring(0), ring(0.45)))

# cheer: a bright rising arpeggio with sparkle
write("sfx_cheer", mix(*[(i * 0.07, note(f, 0.5, 0.3, 0.004, 5)) for i, f in enumerate([NOTE["G4"], NOTE["C5"], NOTE["E5"], NOTE["G5"], NOTE["C6"]])],
                       (0.1, noise_burst(0.7, 0.5, 0.18, 4, 3))))

# door: low thud with a latch click
write("sfx_door", mix((0, note(95, 0.35, 0.8, 0.002, 12, (1.0, 0.6, 0.3))), (0, noise_burst(0.15, 0.15, 0.6, 18, 4)),
                      (0.16, note(1900, 0.03, 0.25, 0.001, 90, (1.0,)))))

# bell: soft double chime
write("sfx_bell", mix((0, note(NOTE["G5"], 1.2, 0.35, 0.002, 3.2, (1.0, 0.5, 0.25))), (0.5, note(NOTE["E5"], 1.4, 0.35, 0.002, 2.8, (1.0, 0.5, 0.25)))))

# ---- background music: a calm, seamless loop (Am - F - C - G), 32 s
BAR = 8.0
CHORDS = [
    (110.0, [220.0, 261.63, 329.63]),   # Am
    (87.31, [174.61, 261.63, 349.23]),  # F
    (130.81, [196.0, 261.63, 329.63]),  # C
    (98.0, [196.0, 246.94, 293.66]),    # G
]
total = int(BAR * len(CHORDS) * SR)
buf = [0.0] * total


def add_wrapped(start_sample, samples):
    for i, s in enumerate(samples):
        buf[(start_sample + i) % total] += s


for ci, (root, tones) in enumerate(CHORDS):
    length = int((BAR + 4.0) * SR)  # overlaps the neighbours for smooth crossfades
    pad = []
    for i in range(length):
        t = i / SR
        env = math.sin(math.pi * i / length) ** 2
        v = 0.5 * math.sin(2 * math.pi * root * t)
        for k, f in enumerate(tones):
            v += 0.22 * math.sin(2 * math.pi * f * t + k) + 0.12 * math.sin(2 * math.pi * f * 1.003 * t)
        pad.append(0.16 * env * v)
    add_wrapped(int((ci * BAR - 2.0) * SR), pad)
    # slow plucks drifting over the chord
    r = random.Random(ci * 31 + 7)
    for step in range(16):
        f = r.choice(tones) * r.choice([1, 2])
        add_wrapped(int((ci * BAR + step * 0.5) * SR), note(f, 1.4, 0.07, 0.01, 2.8, (1.0, 0.25)))

write("music_calm", buf, 0.55)
print("wrote audio to", os.path.abspath(OUT))
