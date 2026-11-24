"""Render Hushpop's icon set and store graphics from code (matches the in-game soap bubble).

Outputs:
  app/src/main/res/drawable/ic_launcher_foreground.png   432x432 adaptive foreground (transparent)
  app/src/main/res/mipmap-*/ic_launcher(.webp|_round.webp) legacy icons
  play-listing-assets/hushpop/icon-512.png                Play Store icon
  play-listing-assets/hushpop/feature-graphic.png         1024x500 feature graphic

Run: python -I tools/icon/make_icon.py   (numpy + Pillow)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
OUT = os.path.join(ROOT, "play-listing-assets", "hushpop")
os.makedirs(OUT, exist_ok=True)

NIGHT_TOP = np.array([0x0E, 0x10, 0x22], float)
NIGHT_BOTTOM = np.array([0x1C, 0x1B, 0x3A], float)
LAMP = np.array([0xFF, 0xC9, 0x8A], float)
FILM = np.array([
    [0xFF, 0x5F, 0xD2], [0xFF, 0xD3, 0x6B], [0x7C, 0xF5, 0xC4],
    [0x4F, 0xD8, 0xFF], [0x8A, 0x6B, 0xFF], [0xFF, 0x5F, 0xD2],
], float)


def night_bg(w, h, lamp_xy=(0.86, 0.08), lamp_strength=0.18):
    y = np.linspace(0, 1, h)[:, None, None]
    img = NIGHT_TOP * (1 - y) + NIGHT_BOTTOM * y
    img = np.broadcast_to(img, (h, w, 3)).copy()
    yy, xx = np.mgrid[0:h, 0:w]
    lx, ly = lamp_xy[0] * w, lamp_xy[1] * h
    d = np.sqrt((xx - lx) ** 2 + (yy - ly) ** 2) / (0.9 * max(w, h))
    glow = np.clip(1 - d, 0, 1) ** 2 * lamp_strength
    img = img * (1 - glow[..., None]) + LAMP * glow[..., None]
    return img


def film_color(angle01):
    """Sample the looped film palette at positions 0..1."""
    n = len(FILM) - 1
    p = (angle01 % 1.0) * n
    i = np.floor(p).astype(int)
    f = (p - i)[..., None]
    return FILM[i] * (1 - f) + FILM[np.minimum(i + 1, n)] * f


def soap_bubble_rgba(size, phase=0.0, tint=np.array([0xB9, 0xA6, 0xFF], float)):
    """RGBA soap bubble filling a size x size square (bubble radius = 0.46 size)."""
    s = size
    yy, xx = np.mgrid[0:s, 0:s].astype(float)
    c = s / 2
    r = s * 0.46
    dx, dy = (xx - c) / r, (yy - c) / r
    d = np.sqrt(dx ** 2 + dy ** 2)
    inside = d <= 1.0
    ang = (np.arctan2(dy + 0.18 * math.sin(phase), dx + 0.18 * math.cos(phase)) / (2 * math.pi)) + phase * 0.1
    film = film_color(ang) * 0.75 + tint * 0.25
    # film strength: clear centre, strong rim (same curve as the in-game mask)
    film_a = np.interp(d, [0, 0.55, 0.85, 1.0], [0.04, 0.16, 0.62, 0.85]) * 0.75
    body_a = np.interp(d, [0, 0.7, 1.0], [0.03, 0.07, 0.20])
    rgb = film * film_a[..., None] + tint * body_a[..., None]
    a = film_a + body_a
    # fresnel rim + crisp edge
    rim = np.interp(d, [0.84, 0.97, 1.0], [0, 0.26, 0.06])
    edge = np.clip(1 - np.abs(d - 0.995) / 0.012, 0, 1) * 0.5
    white = rim + edge
    rgb = rgb + 255 * white[..., None]
    a = a + white
    rgba = np.zeros((s, s, 4))
    rgba[..., :3] = np.clip(rgb / np.maximum(a, 1e-6)[..., None], 0, 255)
    rgba[..., 3] = np.clip(a, 0, 1) * 255 * inside
    img = Image.fromarray(rgba.astype(np.uint8), "RGBA")
    # window highlight + small dot + lamp reflection
    hl = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    dr = ImageDraw.Draw(hl)
    hx, hy = c - r * 0.42, c - r * 0.48
    w2, h2 = r * 0.17, r * 0.08
    box = Image.new("RGBA", (int(w2 * 2) + 4, int(h2 * 2) + 4), (0, 0, 0, 0))
    ImageDraw.Draw(box).rounded_rectangle([2, 2, w2 * 2 + 2, h2 * 2 + 2], radius=h2, fill=(255, 255, 255, 205))
    box = box.rotate(38, expand=True, resample=Image.BICUBIC)
    hl.alpha_composite(box, (int(hx - box.width / 2), int(hy - box.height / 2)))
    dr.ellipse([c - r * 0.16 - r * 0.045, c - r * 0.66 - r * 0.045, c - r * 0.16 + r * 0.045, c - r * 0.66 + r * 0.045],
               fill=(255, 255, 255, 140))
    dr.arc([c - r * 0.8, c - r * 0.8, c + r * 0.8, c + r * 0.8], start=18, end=70,
           fill=(int(LAMP[0]), int(LAMP[1]), int(LAMP[2]), 110), width=max(2, int(r * 0.055)))
    img.alpha_composite(hl)
    return img


def supersampled_bubble(size, **kw):
    return soap_bubble_rgba(size * 4, **kw).resize((size, size), Image.LANCZOS)


def save_webp(img, path):
    img.save(path, "WEBP", quality=95, method=6)


def main():
    # Adaptive foreground: 108dp canvas @4x = 432 px; keep the bubble inside the 66dp safe circle.
    fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    bubble = supersampled_bubble(280, phase=0.6)
    fg.alpha_composite(bubble, ((432 - 280) // 2, (432 - 280) // 2))
    fg.save(os.path.join(RES, "drawable", "ic_launcher_foreground.png"))

    # Legacy + Play icons: full composite on the night background
    def composite(size, round_mask=False, rounded=True):
        bg = Image.fromarray(night_bg(size, size).astype(np.uint8), "RGB").convert("RGBA")
        b = supersampled_bubble(int(size * 0.74), phase=0.6)
        bg.alpha_composite(b, ((size - b.width) // 2, (size - b.height) // 2))
        if round_mask or rounded:
            mask = Image.new("L", (size * 4, size * 4), 0)
            md = ImageDraw.Draw(mask)
            if round_mask:
                md.ellipse([0, 0, size * 4 - 1, size * 4 - 1], fill=255)
            else:
                md.rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=size * 4 * 0.22, fill=255)
            mask = mask.resize((size, size), Image.LANCZOS)
            out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            out.paste(bg, (0, 0), mask)
            return out
        return bg

    for folder, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        d = os.path.join(RES, f"mipmap-{folder}")
        save_webp(composite(px), os.path.join(d, "ic_launcher.webp"))
        save_webp(composite(px, round_mask=True), os.path.join(d, "ic_launcher_round.webp"))

    composite(512, rounded=False).convert("RGB").save(os.path.join(OUT, "icon-512.png"))

    # Feature graphic 1024x500
    w, h = 1024, 500
    fgimg = Image.fromarray(night_bg(w, h, lamp_xy=(0.9, 0.1), lamp_strength=0.22).astype(np.uint8), "RGB").convert("RGBA")
    for (x, y, sz, ph) in [(150, 250, 300, 0.6), (330, 120, 150, 2.0), (300, 400, 110, 4.1), (60, 70, 90, 5.0)]:
        b = supersampled_bubble(sz, phase=ph)
        fgimg.alpha_composite(b, (x - sz // 2, y - sz // 2))
    draw = ImageDraw.Draw(fgimg)
    font_big = font_small = None
    for name in ["segoeuil.ttf", "segoeui.ttf", "arial.ttf"]:
        p = os.path.join(os.environ.get("WINDIR", "C:/Windows"), "Fonts", name)
        if os.path.exists(p):
            font_big = ImageFont.truetype(p, 120)
            font_small = ImageFont.truetype(p, 38)
            break
    draw.text((520, 150), "Hushpop", fill=(244, 241, 255), font=font_big)
    draw.text((526, 300), "pop · breathe · relax", fill=(200, 194, 235), font=font_small)
    fgimg.convert("RGB").save(os.path.join(OUT, "feature-graphic.png"))
    print("icons + store graphics written")


if __name__ == "__main__":
    main()
