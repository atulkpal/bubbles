"""Record a real Hushpop gameplay clip on the emulator and give it the game's real sound.

The emulator's screen recorder has no audio, so the debug build runs in CaptureMode, which
logs every sound the game plays (with its sample, pitch rate and time). This script:
  1. records the screen with `adb screenrecord` while driving real taps / holds,
  2. finds the white SYNC flash in the video to line up device time with video time,
  3. rebuilds the soundtrack from the same .ogg files the game plays (same pitch rates),
  4. cuts the clip so the first frame is already mid-pop, crops 9:16 (1080x1920), encodes mp4.

Usage:
  python -I tools/video/make_short.py --soap LAVENDER_DUSK --record 26 --cut 15 --out <file.mp4>
Requires: debug build installed on emulator-5554, ffmpeg, numpy.
"""
import argparse
import io
import os
import re
import subprocess
import sys
import tempfile
import time

import numpy as np
from scipy.io import wavfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
ADB = r"C:\Users\Atul\AppData\Local\Android\Sdk\platform-tools\adb.exe"
DEV = ["-s", "emulator-5554"]
SR = 44100
PKG = "com.ashwathai.bubbles"


def adb(*a, capture=True, check=False):
    return subprocess.run([ADB, *DEV, *a], capture_output=capture, check=check)


def screenshot():
    from PIL import Image
    return Image.open(io.BytesIO(adb("exec-out", "screencap", "-p").stdout)).convert("RGB")


def bubble_targets(im, limit):
    """Bubbles are found by their bright window highlight (same as the QA tapper)."""
    w, h = im.size
    px = im.load()
    pts = [(x, y) for y in range(300, h - 300, 6) for x in range(0, w, 6) if min(px[x, y]) > 200]
    clusters = []
    for x, y in pts:
        for c in clusters:
            if abs(c[0] - x) < 60 and abs(c[1] - y) < 60:
                c[2].append((x, y))
                break
        else:
            clusters.append([x, y, [(x, y)]])
    out = []
    for c in clusters:
        if len(c[2]) >= 3:
            xs = [p[0] for p in c[2]]
            ys = [p[1] for p in c[2]]
            out.append((sum(xs) // len(xs) + 45, sum(ys) // len(ys) - 90))
    out.sort(key=lambda p: -p[1])
    return out[:limit]


def decode(name):
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "x.wav")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", os.path.join(RAW, name), "-ac", "1", "-ar", str(SR), wav], check=True)
        _, x = wavfile.read(wav)
    return x.astype(np.float64) / 32768.0


def rate_shift(x, rate):
    """SoundPool's playback rate changes speed and pitch together; resample accordingly."""
    n = max(1, int(len(x) / rate))
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x)


def frame_lumas(video):
    """Mean luma per frame + its timestamp (decoded straight from pixels)."""
    ts = [float(x) for x in subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "frame=pts_time", "-of", "csv=p=0", video],
        capture_output=True, text=True).stdout.split() if x.strip() not in ("", "N/A")]
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", video, "-vf", "scale=54:120,format=gray", "-f", "rawvideo", "-"],
                         capture_output=True).stdout
    frames = np.frombuffer(raw, dtype=np.uint8).reshape(-1, 120, 54).mean(axis=(1, 2))
    n = min(len(ts), len(frames))
    return ts[:n], list(frames[:n])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--soap", default="CLEAR_NIGHT")
    ap.add_argument("--record", type=int, default=26, help="seconds to record")
    ap.add_argument("--cut", type=float, default=15.0, help="seconds in the final clip")
    ap.add_argument("--out", required=True)
    ap.add_argument("--blow-at", type=float, default=7.0, help="seconds into play when to hold-to-blow (0 = never)")
    ap.add_argument("--music", type=float, default=0.22, help="rain/music bed level 0..1")
    ap.add_argument("--reuse", default="", help="process an existing capture dir (skip recording)")
    ap.add_argument("--skip", type=float, default=0.0, help="extra seconds to skip after the first pop")
    a = ap.parse_args()

    os.environ["MSYS_NO_PATHCONV"] = "1"
    if a.reuse:
        work = a.reuse
        video = os.path.join(work, "screen.webm")
        log = open(os.path.join(work, "events.log"), encoding="utf-8").read()
        return finish(a, work, video, log)
    adb("shell", "am", "force-stop", PKG)
    adb("logcat", "-c")
    # The emulator's own recorder (host side) keeps a steady 24 fps; `adb shell screenrecord`
    # drops frames heavily on this machine. Its audio track is silent, so sound is rebuilt below.
    work = tempfile.mkdtemp(prefix="hushpop_cap_")
    video = os.path.join(work, "screen.webm")
    adb("emu", "screenrecord", "start", "--time-limit", str(a.record), "--bit-rate", "20000000", video)
    time.sleep(0.8)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", "--ez", "capture", "true", "--es", "soap", a.soap)
    # Wait for the SYNC flash (cold start on the emulator can take several seconds),
    # then play; pops before the flash are ignored anyway.
    for _ in range(40):
        if "EVT SYNC" in adb("logcat", "-d", "-s", "HushpopCapture:I").stdout.decode("utf-8", "ignore"):
            break
        time.sleep(0.5)
    time.sleep(0.5)
    t0 = time.time()
    blew = a.blow_at <= 0
    while time.time() - t0 < a.record - 5:
        el = time.time() - t0
        if not blew and el >= a.blow_at:
            adb("shell", "input", "swipe", "540", "1500", "560", "1470", "2300")
            blew = True
            continue
        for x, y in bubble_targets(screenshot(), 3):
            adb("shell", "input", "tap", str(x), str(y))
        time.sleep(0.6)
    while time.time() - t0 < a.record - 2:
        time.sleep(0.5)
    adb("emu", "screenrecord", "stop")
    for _ in range(30):  # wait for the file to be finalised
        if os.path.exists(video) and os.path.getsize(video) > 0:
            break
        time.sleep(0.5)
    time.sleep(2.0)
    log = adb("logcat", "-d", "-s", "HushpopCapture:I").stdout.decode("utf-8", "ignore")
    open(os.path.join(work, "events.log"), "w", encoding="utf-8").write(log)
    return finish(a, work, video, log)


def finish(a, work, video, log):
    events = [(m.group(1), int(m.group(2)), m.group(3)) for m in re.finditer(r"EVT (\w+) t=(\d+) ?(.*)", log)]
    sync = [t for n, t, _ in events if n == "SYNC"]
    if not sync:
        raise SystemExit("no SYNC event in log (is the debug build installed?)")
    sync_ms = sync[-1]

    ts, ys = frame_lumas(video)
    flash_frames = [t for t, y in zip(ts, ys) if y > 200]
    if not flash_frames:
        raise SystemExit("white sync flash not found in the video")
    flash_t = flash_frames[0]
    flash_end = flash_frames[-1]

    def vt(ms):  # device ms -> video seconds
        return flash_t + (ms - sync_ms) / 1000.0

    pops = [(vt(t), extra) for n, t, extra in events if n == "POP"]
    after = [p for p in pops if p[0] > flash_end + 0.5]
    if not after:
        raise SystemExit("no pops after the flash")
    later = [p for p in after if p[0] >= after[0][0] + a.skip]
    start = later[0][0] + 0.06  # first frame already mid-pop
    dur = min(a.cut, max(ts) - start - 0.2)

    # ── soundtrack from the game's own files ──
    total = int((dur + 1.0) * SR)
    mix = np.zeros(total)
    samples = {i: decode(f"soap_pop{i}.ogg") for i in range(1, 6)}
    release = decode("blow_release.ogg")
    blow = decode("blow_loop.ogg")
    for n, t, extra in events:
        at = vt(t) - start
        if n == "POP":
            m = dict(kv.split("=") for kv in extra.split())
            s = rate_shift(samples[int(m["sample"])], float(m["rate"])) * float(m.get("vol", 0.9))
        elif n == "BLOW_RELEASE":
            s = release * 0.8
        else:
            continue
        i = int(at * SR)
        if i < 0 or i >= total:
            continue
        seg = s[: total - i]
        mix[i:i + len(seg)] += seg
    starts = [vt(t) - start for n, t, _ in events if n == "BLOW_START"]
    stops = [vt(t) - start for n, t, _ in events if n == "BLOW_STOP"]
    for b0 in starts:
        b1 = min([s for s in stops if s > b0] or [b0 + 2.5])
        i0, i1 = int(max(0, b0) * SR), int(min(dur, b1) * SR)
        if i1 > i0:
            n = i1 - i0
            loop = np.tile(blow, n // len(blow) + 1)[:n]
            ramp = np.linspace(0.8, 1.3, n)  # rising pitch while the bubble grows
            idx = np.clip(np.cumsum(ramp).astype(int), 0, len(loop) - 1)
            mix[i0:i1] += loop[idx] * 0.55
    bed = decode("music_zen.ogg")
    bed = np.tile(bed, total // len(bed) + 1)[:total] * a.music
    mix = mix + bed
    fade = int(0.25 * SR)
    mix[:fade] *= np.linspace(0, 1, fade)
    end = int(dur * SR)
    mix[end - fade:end] *= np.linspace(1, 0.3, fade)  # soft tail for looping
    peak = np.max(np.abs(mix[:end])) or 1
    mix = mix[:end] / peak * 0.89
    wav = os.path.join(work, "audio.wav")
    wavfile.write(wav, SR, (mix * 32767).astype(np.int16))

    out = os.path.abspath(a.out)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", f"{start:.3f}", "-i", video, "-i", wav,
                    "-t", f"{dur:.3f}", "-map", "0:v", "-map", "1:a",
                    "-vf", "crop=1080:1920:0:252,fps=30,format=yuv420p",
                    "-c:v", "libx264", "-preset", "slow", "-crf", "18", "-c:a", "aac", "-b:a", "192k",
                    "-movflags", "+faststart", out], check=True)
    print(f"wrote {out}  ({dur:.1f}s, {len(after)} pops, soap {a.soap})")


if __name__ == "__main__":
    main()
