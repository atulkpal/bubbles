"""Generate Hushpop's sounds procedurally (no third-party samples, no licensing questions).

Outputs (OGG Vorbis, mono) into app/src/main/res/raw/:
  soap_pop1..5   soft wet "plip" pops (pitch-shifted at runtime by bubble size + pentatonic step)
  blow_loop      breathy air loop played while a bubble is being blown (rate rises as it grows)
  blow_release   soft upward "bloop" when a blown bubble lets go
  music_zen      rain-on-glass bed mixed under the existing zen music, ~2 min seamless loop

Run:  python tools/sound/make_soap_sounds.py   (needs numpy, scipy, ffmpeg on PATH)
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
rng = np.random.default_rng(20261124)


def bandpass(x, lo, hi, order=2):
    sos = butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return sosfilt(sos, x)


def lowpass(x, f, order=2):
    return sosfilt(butter(order, f, btype="low", fs=SR, output="sos"), x)


def highpass(x, f, order=2):
    return sosfilt(butter(order, f, btype="high", fs=SR, output="sos"), x)


def normalize(x, peak_db=-3.0):
    peak = np.max(np.abs(x)) or 1.0
    return x / peak * (10 ** (peak_db / 20))


def write_ogg(name, x, quality=4, sr=SR):
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + ".wav")
        wavfile.write(wav, sr, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        out = os.path.join(RAW, name + ".ogg")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", str(quality), out], check=True)
    print(f"{name}.ogg  {os.path.getsize(out) // 1024} KB  {len(x) / sr:.2f}s")


def soap_pop(f_start, f_end, body_ms, tail_ms, wet):
    """A wet plip: a fast downward pitch glide (the film snapping), a tiny click and an airy tail."""
    n = int(SR * (body_ms + tail_ms) / 1000)
    t = np.arange(n) / SR
    glide_t = body_ms / 1000
    f = f_end + (f_start - f_end) * np.exp(-t / (glide_t * 0.35))
    phase = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(phase) * np.exp(-t / (glide_t * 0.55))
    body += 0.35 * np.sin(2 * phase + 0.3) * np.exp(-t / (glide_t * 0.3))   # a little "water" harmonic
    click = bandpass(rng.standard_normal(n), 2500, 7000) * np.exp(-t / 0.0025) * 0.35
    air = bandpass(rng.standard_normal(n), 1800, 6000) * np.exp(-t / (tail_ms / 1000 * 0.4)) * 0.10 * wet
    x = body + click + air
    attack = np.minimum(1, t / 0.0015)
    return normalize(x * attack, -3.0)


def blow_loop(seconds=1.6):
    """Breathy air through a wand: band-passed noise with slow flutter; seamless loop."""
    n = int(SR * seconds)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n + SR)
    breath = bandpass(noise, 700, 2600, order=3)[SR:]
    flutter = 1 + 0.18 * np.sin(2 * np.pi * (1 / seconds) * 3 * t) + 0.08 * np.sin(2 * np.pi * (1 / seconds) * 7 * t)
    x = breath * flutter
    # equal-power crossfade end into start so the loop has no seam
    fade = int(SR * 0.2)
    w = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
    x[:fade] = x[:fade] * w + x[-fade:] * (1 - w)
    x = x[: n - fade]
    return normalize(x, -9.0)


def blow_release():
    n = int(SR * 0.32)
    t = np.arange(n) / SR
    f = 260 + 420 * (1 - np.exp(-t / 0.06))
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.09)
    x += bandpass(rng.standard_normal(n), 900, 3000) * np.exp(-t / 0.05) * 0.15
    return normalize(x * np.minimum(1, t / 0.004), -6.0)


def rain(seconds):
    """Rain on a window: soft broadband hiss + many tiny droplet ticks + occasional heavier drops."""
    n = int(SR * seconds)
    hiss = lowpass(highpass(rng.standard_normal(n), 300), 6000) * 0.10
    hiss *= 1 + 0.15 * np.sin(2 * np.pi * 0.07 * np.arange(n) / SR)  # slow gusts
    drops = np.zeros(n)
    count = int(seconds * 55)
    for pos in rng.integers(0, n - 2000, count):
        length = int(rng.integers(200, 900))
        tt = np.arange(length) / SR
        freq = rng.uniform(2500, 7000)
        amp = rng.uniform(0.02, 0.09)
        drops[pos:pos + length] += np.sin(2 * np.pi * freq * tt) * np.exp(-tt / rng.uniform(0.001, 0.004)) * amp
    for pos in rng.integers(0, n - 6000, int(seconds * 3)):
        length = 5000
        tt = np.arange(length) / SR
        f = rng.uniform(700, 1400) * (1 + 1.2 * np.exp(-tt / 0.01))
        drops[pos:pos + length] += np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 0.02) * rng.uniform(0.03, 0.07)
    return hiss + highpass(drops, 1200)


def decode(path):
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "x.wav")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", path, "-ac", "1", "-ar", str(SR), wav], check=True)
        sr, x = wavfile.read(wav)
    return x.astype(np.float64) / 32768.0


def seamless(x, fade_s=2.0):
    fade = int(SR * fade_s)
    w = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
    y = x.copy()
    y[:fade] = x[:fade] * w + x[-fade:] * (1 - w)
    return y[:-fade]


def main():
    pops = [
        (1150, 520, 55, 120, 1.0),
        (1000, 460, 60, 130, 0.9),
        (1300, 600, 50, 110, 1.1),
        (900, 420, 65, 140, 0.8),
        (1220, 560, 52, 125, 1.0),
    ]
    for i, p in enumerate(pops, 1):
        write_ogg(f"soap_pop{i}", soap_pop(*p), quality=5)
    write_ogg("blow_loop", blow_loop(), quality=4)
    write_ogg("blow_release", blow_release(), quality=5)

    # Ambient bed: keep the original zen music (if not already rebuilt) and lay rain under it.
    src = os.path.join(ROOT, "tools", "sound", "zen_music_source.ogg")
    if not os.path.exists(src):
        import shutil
        shutil.copy(os.path.join(RAW, "music_zen.ogg"), src)
    music = decode(src)
    target = int(SR * 122)
    reps = int(np.ceil(target / len(music)))
    music_long = np.tile(music, reps)[:target]
    bed = rain(122) + normalize(music_long, -14.0)
    write_ogg("music_zen", normalize(seamless(bed), -4.0), quality=3)


if __name__ == "__main__":
    main()
