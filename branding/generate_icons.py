"""Generates StepLord launcher icon resources from the source artwork in this folder.

Run from the repository root after changing ico-blue.png or ico-trans.png:

    pip install pillow
    python branding/generate_icons.py

Writes the adaptive icon layers and legacy bitmaps to app/src/main/res/mipmap-*, the Play
Store icon to app/src/main/ic_launcher-playstore.png, and a preview of the icon under common
launcher shapes to build/icon_preview.png. If the background colour of ico-blue.png changes,
update ic_launcher_background in app/src/main/res/values/ic_launcher_background.xml as well.
"""
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

DEFAULT_ARGS = [
    "branding/ico-blue.png",
    "branding/ico-trans.png",
    "app/src/main/res",
    "app/src/main/ic_launcher-playstore.png",
    "build",
]
blue_path, trans_path, res_dir, playstore_path, preview_dir = (sys.argv[1:6] or DEFAULT_ARGS)

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
# Share of the 108dp adaptive canvas the artwork's height may use (54dp). The safe zone is a 66dp
# circle (61%); the artwork's widest parts are narrower than its height, so this stays inside.
ARTWORK_HEIGHT = 0.50

# --- Clean the transparent artwork -------------------------------------------------------
art = Image.open(trans_path).convert("RGBA")
alpha = art.getchannel("A")
# Opening (erode then dilate) removes isolated specks left from background removal; the final
# dilation keeps the anti-aliased edges of the real shapes.
mask = alpha.point(lambda v: 255 if v > 128 else 0)
mask = mask.filter(ImageFilter.MinFilter(7)).filter(ImageFilter.MaxFilter(7))
mask = mask.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.GaussianBlur(1))
alpha = ImageChops.multiply(alpha, mask).point(lambda v: 0 if v < 24 else v)
art.putalpha(alpha)
art = art.crop(alpha.getbbox())
print("artwork size after cleanup:", art.size)


def foreground(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    h = round(size * ARTWORK_HEIGHT)
    w = round(art.width * h / art.height)
    scaled = art.resize((w, h), Image.LANCZOS)
    canvas.alpha_composite(scaled, ((size - w) // 2, (size - h) // 2))
    return canvas


def monochrome(size: int) -> Image.Image:
    # Themed icons (Android 13+) use only the alpha channel, tinted by the system
    fg = foreground(size)
    white = Image.new("RGBA", fg.size, (255, 255, 255, 0))
    white.putalpha(fg.getchannel("A"))
    return white


def circle_crop(img: Image.Image) -> Image.Image:
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, img.width - 1, img.height - 1), fill=255)
    out = img.convert("RGBA")
    out.putalpha(m)
    return out


blue = Image.open(blue_path).convert("RGB")

for name, scale in DENSITIES.items():
    d = os.path.join(res_dir, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    adaptive = round(108 * scale)
    foreground(adaptive).save(os.path.join(d, "ic_launcher_foreground.webp"), lossless=True)
    monochrome(adaptive).save(os.path.join(d, "ic_launcher_monochrome.webp"), lossless=True)
    # Legacy bitmaps: not used on API 26+ (adaptive icon) but kept as a complete fallback set
    legacy = round(48 * scale)
    square = blue.resize((legacy, legacy), Image.LANCZOS)
    square.save(os.path.join(d, "ic_launcher.webp"), quality=95)
    circle_crop(square).save(os.path.join(d, "ic_launcher_round.webp"), quality=95)

blue.resize((512, 512), Image.LANCZOS).save(playstore_path)

# --- Previews: adaptive icon under common launcher masks ----------------------------------
os.makedirs(preview_dir, exist_ok=True)
size = 432
bg = Image.new("RGBA", (size, size), (0x25, 0x80, 0xFD, 255))
bg.alpha_composite(foreground(size))
inset = round(size * 18 / 108)  # launchers show the inner 72dp of the 108dp layers
visible = bg.crop((inset, inset, size - inset, size - inset))
sheet = Image.new("RGBA", (visible.width * 3 + 80, visible.height + 40), (240, 240, 240, 255))
shapes = []
for i, kind in enumerate(["circle", "squircle", "square"]):
    m = Image.new("L", visible.size, 0)
    dr = ImageDraw.Draw(m)
    if kind == "circle":
        dr.ellipse((0, 0, visible.width - 1, visible.height - 1), fill=255)
    elif kind == "squircle":
        dr.rounded_rectangle((0, 0, visible.width - 1, visible.height - 1), radius=visible.width // 3, fill=255)
    else:
        dr.rounded_rectangle((0, 0, visible.width - 1, visible.height - 1), radius=visible.width // 10, fill=255)
    tile = visible.copy()
    tile.putalpha(m)
    sheet.alpha_composite(tile, (20 + i * (visible.width + 20), 20))
sheet.save(os.path.join(preview_dir, "icon_preview.png"))
print("done")
