"""Measure supplied screenshot boards and extract only static artwork, never UI screens.

Coordinates below are in the original 720 px reference frame. The source boards
remain unchanged. UI is implemented with native views, not screenshot overlays.
"""
from pathlib import Path
from PIL import Image
import json

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path('/home/lequyen/Documents/folder311')
OUT = ROOT / 'app/src/main/res/drawable-nodpi'
REFERENCE = Path(__file__).resolve().parent
OUT.mkdir(parents=True, exist_ok=True)

BOARDS = {
    'home': ('home.png', [180, 942, 1704]),
    'login': ('login.png', [102, 880, 1656]),
    'settings': ('Settings.png', [160, 924, 1688, 2452, 3216, 3980]),
    'spreads': ('Spreads.png', [136, 904, 1708, 2476, 3280, 4050, 4820, 5590, 6360]),
    'cards': ('Cards.png', [94, 852, 1610]),
    'history': ('History.png', [168, 944, 1720, 2496, 3272, 4048]),
    'view': ('View.png', [106, 882, 1650, 2422, 3192]),
}
frames = {}
for board, (filename, starts) in BOARDS.items():
    image = Image.open(SOURCE / filename).convert('RGBA')
    for index, left in enumerate(starts):
        # Ignore the faint outer border of the entire exported board.
        white_rows = [y for y in range(20, image.height - 20)
                      if sum(min(image.getpixel((left + offset, y))[:3]) > 245
                             for offset in (80, 360, 640)) >= 2]
        top, bottom = min(white_rows), max(white_rows) + 1
        frame = image.crop((left, top, left + 720, bottom))
        frames[board, index] = frame
        frame.save(REFERENCE / f'reference_{board}_{index}.png')

ART = {
    # Rectangles are measured separately from Android dp geometry.
    'tarot_brand': ('home', 0, (35, 118, 455, 170)),
    'tarot_action_daily': ('home', 0, (42, 796, 232, 980)),
    'tarot_action_weekly': ('home', 0, (265, 796, 455, 980)),
    'tarot_action_celtic': ('home', 0, (488, 796, 678, 980)),
    'tarot_empty_reading': ('home', 0, (242, 1134, 490, 1350)),
    'tarot_empty_history': ('history', 0, (160, 350, 560, 720)),
    'tarot_guidance_art': ('spreads', 3, (174, 200, 540, 492)),
    'tarot_deck_back': ('spreads', 4, (38, 446, 138, 620)),
    'tarot_horoscope_sun': ('home', 0, (40, 590, 244, 714)),
    'tarot_horoscope_moon': ('home', 0, (600, 322, 678, 397)),
}
for index in range(8):
    ART[f'tarot_spread_icon_{index}'] = ('spreads', 2, (64, 324 + index * 152, 144, 404 + index * 152))
manifest = {}
for name, (board, index, box) in ART.items():
    crop = frames[board, index].crop(box)
    if name in ('tarot_horoscope_sun', 'tarot_horoscope_moon'):
        # Keep only the warm ornament strokes; do not carry a rectangular white
        # screenshot background over the native XML gradient.
        crop.putdata([(209, 191, 167, min(130, max(0, r - b - 5) * 3))
                      for r, g, b, a in crop.getdata()])
    crop.save(OUT / f'{name}.png')
    manifest[name] = {'board': BOARDS[board][0], 'frame': index, 'box_px': box, 'size_px': crop.size}

# These rectangles contain only decorative artwork, never labels or controls.
# The transparent canvas preserves each ornament's position in a 180 x 128 slot.
decorations = [(500, 302, 678, 426), (500, 454, 678, 578),
               (620, 606, 678, 730), (548, 822, 678, 880),
               (540, 910, 678, 1034), (560, 1062, 678, 1186),
               (604, 1214, 678, 1338), (580, 1428, 678, 1480)]
for index, box in enumerate(decorations):
    art = Image.new('RGBA', (180, 128))
    art.paste(frames['spreads', 2].crop(box), (box[0] - 500, box[1] - (300 + index * 152)))
    name = f'tarot_spread_decor_{index}'
    art.save(OUT / f'{name}.png')
    manifest[name] = {'board': 'Spreads.png', 'frame': 2, 'box_px': box, 'slot_px': [180, 128]}

# Zodiac glyphs are illustrations from the supplied source, not platform emoji.
for index, name in enumerate(['aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo', 'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces']):
    x = [110, 334, 558][index % 3]
    y = [308, 484, 660, 836][index // 3]
    box = (x, y, x + 52, y + 52)
    frames['login', 0].crop(box).save(OUT / f'tarot_zodiac_{name}.png')
    manifest[f'tarot_zodiac_{name}'] = {'board': 'login.png', 'frame': 0, 'box_px': box}

(REFERENCE / 'asset_measurements.json').write_text(json.dumps(manifest, indent=2) + '\n')
print('Reference frames and static-art coordinates written to', REFERENCE)
