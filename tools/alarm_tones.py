#!/usr/bin/env python3
"""Draws Weft's two alarm tones and writes them to app/src/main/res/raw (1.6.2).

They are made here from sine waves, so they are Weft's own (no licence to credit) and can be
remade. Each file is one loop; AlarmSoundService plays it on repeat.

- alarm_weft.m4a      "Weft": a bright three-note chime (C6 E6 G6, then G6 again higher), firm
                      enough to wake you.
- alarm_weft_soft.m4a "Weft · Soft": two slow, round notes (A5, E6) that swell in.

Needs numpy and macOS's afconvert (AAC in .m4a, which every Android version plays).
Run: python3 tools/alarm_tones.py
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

RATE = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw")


def note(freq, length, decay, attack=0.004, partials=((1, 1.0), (2, 0.35), (3, 0.12), (4.2, 0.06))):
    """One struck note: a few partials (a soft bell), a short attack and an exponential decay."""
    t = np.arange(int(RATE * length)) / RATE
    wave_ = sum(a * np.sin(2 * np.pi * freq * m * t) * np.exp(-t * decay * (1 + 0.6 * (m - 1))) for m, a in partials)
    env = np.minimum(1.0, t / attack)
    return wave_ * env


def place(track, sound, at):
    start = int(RATE * at)
    end = min(len(track), start + len(sound))
    track[start:end] += sound[: end - start]


def weft():
    """2.4 s: C6 E6 G6 (0.15 s apart), C7, then quiet until the loop starts again."""
    track = np.zeros(int(RATE * 2.4))
    for i, f in enumerate((1046.5, 1318.5, 1568.0, 2093.0)):
        place(track, note(f, 1.4, 4.5), i * 0.15)
    return track


def weft_soft():
    """3.2 s: A5 then E6, each swelling in over 0.25 s and fading slowly; pure tones, no bite."""
    track = np.zeros(int(RATE * 3.2))
    round_ = ((1, 1.0), (2, 0.12))
    place(track, note(880.0, 2.0, 1.6, attack=0.25, partials=round_), 0.0)
    place(track, note(1318.5, 2.0, 1.6, attack=0.25, partials=round_), 0.8)
    return track


def write(name, track, peak):
    # Fade the last 20 ms so the loop point doesn't click.
    fade = int(RATE * 0.02)
    track[-fade:] *= np.linspace(1, 0, fade)
    track = track / np.max(np.abs(track)) * peak
    pcm = (track * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        out = os.path.join(OUT, name + ".m4a")
        subprocess.run(["afconvert", "-f", "m4af", "-d", "aac", "-b", "64000", wav, out], check=True)
    print(out, os.path.getsize(out), "bytes")


if __name__ == "__main__":
    if sys.platform != "darwin":
        sys.exit("needs macOS's afconvert")
    os.makedirs(OUT, exist_ok=True)
    write("alarm_weft", weft(), 0.9)
    write("alarm_weft_soft", weft_soft(), 0.7)
