"""Renders the nametag logo glyphs for both mod versions.

The logo is drawn in each player's own launcher theme colors, so it is split into six white glyphs
that the mod stacks on top of each other and tints one color each (ClientUserBadges.java):
  U+E000 octagon fill   - background1 (the launcher background the logo normally sits on)
  U+E001 octagon outline - background2
  U+E002 top beam        - accent1
  U+E003 left leg        - accent2
  U+E004 right leg       - accent3
  U+E005 diagonal        - accent4
Same shapes as N-Logo-icon-only.svg / Logo.tsx (600x600 view box); the outline is drawn one texel
wide, the SVG's 4-unit stroke would vanish at this size.

Stacking relies on every glyph having the same advance: Minecraft measures a bitmap glyph up to its
rightmost column with non-zero alpha, so each cell gets one pixel of alpha 1/255 in its (cut-off)
top right corner - full width for all six, and far below the text shader's 0.1 alpha discard, so it
never shows. With 32 texels at 8 font pixels, every glyph advances (int)(0.5 + 32 * 0.25) + 1 = 9,
undone by the -9 space U+E006; U+E007 is the gap before the name. This script also writes that font
definition (font/client_badge.json) next to the texture.

The texture has 4 texels per font pixel so it stays crisp up close, rendered 16x oversampled and
downscaled for smooth edges.

Run from anywhere: python designs/branding/make_client_badge.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 32
OVERSAMPLE = 16
# The octagon spans 40..560 in the view box on both axes - that span becomes the full cell.
VIEW_MIN = 40.0
VIEW_SPAN = 520.0

OCTAGON = [(200, 40), (400, 40), (560, 200), (560, 400), (400, 560), (200, 560), (40, 400), (40, 200)]
TOP_BEAM = [(150, 150), (450, 150), (450, 200), (150, 200)]
LEFT_LEG = [(185, 200), (230, 200), (230, 440), (185, 440)]
RIGHT_LEG = [(370, 200), (415, 200), (415, 440), (370, 440)]
DIAGONAL = [(185, 200), (230, 200), (415, 415), (415, 440), (370, 440), (185, 225)]

ROOT = Path(__file__).resolve().parents[2]
ASSETS = [ROOT / 'mod' / version / 'src/main/resources/assets/tntsallin1client' for version in ('1.21.11', '26.1.2')]

# Font pixels the glyphs are drawn at, and the gap between logo and name.
GLYPH_HEIGHT = 8
GLYPH_ASCENT = 7
NAME_GAP = 2
FIRST_CHAR = 0xE000


def to_pixels(points):
    scale = SIZE * OVERSAMPLE / VIEW_SPAN
    return [((x - VIEW_MIN) * scale, (y - VIEW_MIN) * scale) for x, y in points]


def layer(draw_shape):
    big = SIZE * OVERSAMPLE
    mask = Image.new('L', (big, big), 0)
    draw_shape(ImageDraw.Draw(mask))
    alpha = mask.resize((SIZE, SIZE), Image.LANCZOS)
    cell = Image.new('RGBA', (SIZE, SIZE), (255, 255, 255, 0))
    cell.putalpha(alpha)
    cell.putpixel((SIZE - 1, 0), (255, 255, 255, 1))
    return cell


def filled(points):
    return lambda draw: draw.polygon(to_pixels(points), fill=255)


def outlined(points):
    # One texel wide; Pillow draws a polygon's outline width inwards, so it stays inside the fill.
    return lambda draw: draw.polygon(to_pixels(points), outline=255, width=OVERSAMPLE)


LAYERS = [filled(OCTAGON), outlined(OCTAGON), filled(TOP_BEAM), filled(LEFT_LEG), filled(RIGHT_LEG), filled(DIAGONAL)]


def render():
    sheet = Image.new('RGBA', (SIZE * len(LAYERS), SIZE), (255, 255, 255, 0))
    for index, draw_shape in enumerate(LAYERS):
        sheet.paste(layer(draw_shape), (index * SIZE, 0))
    return sheet


def font_definition():
    # BitmapProvider: advance = (int)(0.5 + width * height / cellHeight) + 1, width = SIZE via the marker.
    advance = int(0.5 + SIZE * GLYPH_HEIGHT / SIZE) + 1
    back_char = chr(FIRST_CHAR + len(LAYERS))
    gap_char = chr(FIRST_CHAR + len(LAYERS) + 1)
    return {
        'providers': [
            {
                'type': 'bitmap',
                'file': 'tntsallin1client:font/client_badge.png',
                'height': GLYPH_HEIGHT,
                'ascent': GLYPH_ASCENT,
                'chars': [''.join(chr(FIRST_CHAR + index) for index in range(len(LAYERS)))],
            },
            {'type': 'space', 'advances': {back_char: -advance, gap_char: NAME_GAP}},
        ]
    }


def main():
    sheet = render()
    definition = json.dumps(font_definition(), indent=2) + '\n'
    for assets in ASSETS:
        texture = assets / 'textures/font/client_badge.png'
        texture.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(texture)
        font = assets / 'font/client_badge.json'
        font.parent.mkdir(parents=True, exist_ok=True)
        font.write_text(definition, encoding='ascii', newline='\n')
        print('wrote', texture, 'and', font.name)


if __name__ == '__main__':
    main()
