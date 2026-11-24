"""Frame real emulator captures into Play Store screenshots (1080x1920, under Play's 2:1 limit).

Every image is a real capture of the app (docs/relaunch-qa); only a caption and a frame are added.
Run: python -I tools/store/make_screenshots.py
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
QA = os.path.join(ROOT, "docs", "relaunch-qa")
OUT = os.path.join(ROOT, "play-listing-assets", "hushpop")
os.makedirs(OUT, exist_ok=True)

SHOTS = [
    ("s7-float.png", "Pop real soap bubbles", "Soft sounds. No timer. No fail."),
    ("s7-blowing.png", "Hold to blow your own", "Let go and watch it float away"),
    ("s5-04-pop.png", "Breathe with a bubble", "In as it grows, out as it rises"),
    ("s4-02-shelf.png", "Fill the jar, collect soaps", "A new soap every few days"),
    ("s4-03-marigold.png", "Every soap is a new room", "Lavender Dusk, Monsoon Glass, Marigold…"),
    ("s4-04-new-soap.png", "Little rewards, no pressure", "Missed a day? It never resets"),
    ("s6-01-menu.png", "Calm by default", "Challenge mode when you want a goal"),
    ("s6-03-challenge.png", "20 Challenge levels", "Timed, never frantic"),
]


def font(size, light=False):
    for name in (["segoeuil.ttf"] if light else []) + ["segoeui.ttf", "arial.ttf"]:
        p = os.path.join(os.environ.get("WINDIR", "C:/Windows"), "Fonts", name)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def background(w, h):
    y = np.linspace(0, 1, h)[:, None, None]
    top = np.array([0x0E, 0x10, 0x22], float)
    bot = np.array([0x2A, 0x23, 0x50], float)
    img = np.broadcast_to(top * (1 - y) + bot * y, (h, w, 3)).copy()
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - 0.9 * w) ** 2 + (yy - 0.05 * h) ** 2) / (0.9 * h)
    g = np.clip(1 - d, 0, 1) ** 2 * 0.25
    img = img * (1 - g[..., None]) + np.array([0xFF, 0xC9, 0x8A]) * g[..., None]
    return Image.fromarray(img.astype(np.uint8), "RGB")


def rounded(img, r):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.width - 1, img.height - 1], radius=r, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    W, H = 1080, 1920
    for i, (src, title, sub) in enumerate(SHOTS, 1):
        path = os.path.join(QA, src)
        if not os.path.exists(path):
            print("missing", src)
            continue
        canvas = background(W, H).convert("RGBA")
        d = ImageDraw.Draw(canvas)
        tf, sf = font(78, light=True), font(40)
        tw = d.textlength(title, font=tf)
        d.text(((W - tw) / 2, 110), title, font=tf, fill=(244, 241, 255))
        sw = d.textlength(sub, font=sf)
        d.text(((W - sw) / 2, 225), sub, font=sf, fill=(200, 194, 235))
        shot = Image.open(path).convert("RGB")
        target_h = H - 340 - 60
        scale = target_h / shot.height
        shot = shot.resize((int(shot.width * scale), target_h), Image.LANCZOS)
        shot = rounded(shot, 44)
        x = (W - shot.width) // 2
        y = 340
        shadow = Image.new("RGBA", (shot.width + 60, shot.height + 60), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle([30, 30, shot.width + 30, shot.height + 30], radius=50, fill=(0, 0, 0, 140))
        shadow = shadow.filter(ImageFilter.GaussianBlur(18))
        canvas.alpha_composite(shadow, (x - 30, y - 18))
        canvas.alpha_composite(shot, (x, y))
        canvas.convert("RGB").save(os.path.join(OUT, f"phone-{i:02d}.png"))
        print("phone-%02d.png  <- %s" % (i, src))


if __name__ == "__main__":
    main()
