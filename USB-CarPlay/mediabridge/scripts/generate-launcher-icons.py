"""Separate the existing silver artwork from its blue rounded tile (Pillow + numpy)."""
from pathlib import Path
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCE = ROOT / "docs/icon-source/original-blue.webp"


def main():
    source = Image.open(SOURCE).convert("RGB")
    pixels = np.asarray(source)
    # The original artwork is silver over a saturated blue tile. Keep its
    # original shading; the red channel separates silver from the blue field.
    # Limit extraction to the artwork area, excluding the outer white corners
    # and the tile's cyan rim. Feather only the antialiased artwork boundary.
    alpha = np.clip((pixels[:, :, 0].astype(float) - 12) / 40, 0, 1)
    artwork_area = np.zeros(alpha.shape, dtype=bool)
    artwork_area[50:355, 50:370] = True
    alpha = np.where(artwork_area, alpha, 0)
    # Keep only the note/ring and the two stars; discard disconnected cyan
    # highlights from the old tile. Seeds lie inside the three silver shapes.
    components = Image.fromarray((alpha > 0).astype("uint8")).copy()
    for seed in ((86, 140), (162, 103), (272, 170)):
        ImageDraw.floodfill(components, seed, 2)
    alpha = np.where(np.asarray(components) == 2, alpha, 0)
    foreground = source.convert("RGBA")
    foreground.putalpha(Image.fromarray(np.round(alpha * 255).astype("uint8")))
    foreground.save(RES / "drawable-nodpi/icon_blue.webp", lossless=True, exact=True)

    colors = ET.parse(RES / "values/colors.xml").getroot()
    background = next(c.text for c in colors if c.attrib.get("name") == "ic_launcher_background")
    legacy = Image.new("RGBA", foreground.size, background)
    legacy.alpha_composite(foreground)
    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96,
                          "xxhdpi": 144, "xxxhdpi": 192}.items():
        tile = legacy.convert("RGB").resize((size, size), Image.Resampling.LANCZOS)
        for name in ("ic_launcher_blue", "ic_launcher_blue_round"):
            tile.save(RES / f"mipmap-{density}/{name}.webp", lossless=True)


if __name__ == "__main__":
    main()
