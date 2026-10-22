from pathlib import Path
import json

from PIL import Image, ImageChops, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path('/home/lequyen/Documents/folder311')
OUT = ROOT / 'app/src/main/res/drawable-nodpi'


def extract():
    illustrations = Image.open(SOURCE / 'spread_guide.png').convert('RGBA')
    backgrounds = Image.open(SOURCE / 'guide_spread.png').convert('RGBA')
    icon_mask = Image.new('L', (320, 320))
    ImageDraw.Draw(icon_mask).rounded_rectangle((0, 0, 319, 319), radius=96, fill=255)
    icon_mask = icon_mask.resize((80, 80), Image.Resampling.LANCZOS)
    background_mask = Image.new('L', (648, 128))
    ImageDraw.Draw(background_mask).rounded_rectangle((6, 6, 641, 121), radius=28, fill=255)
    manifest = {}
    for index in range(8, 16):
        top = index * 152
        icon_box = (28, top + 24, 108, top + 104)
        icon_name = f'tarot_spread_icon_{index}'
        icon = illustrations.crop(icon_box)
        icon.putalpha(ImageChops.darker(icon.getchannel('A'), icon_mask))
        icon.save(OUT / f'{icon_name}.png')
        manifest[icon_name] = {'source': 'spread_guide.png', 'box_px': icon_box}

        decoration_box = (464, top, 644, top + 128)
        row = backgrounds.crop((0, top, 648, top + 128))
        row.putalpha(ImageChops.darker(row.getchannel('A'), background_mask))
        art = row.crop((464, 0, 644, 128))
        decoration_name = f'tarot_spread_decor_{index}'
        art.save(OUT / f'{decoration_name}.png')
        manifest[decoration_name] = {
            'source': 'guide_spread.png',
            'box_px': decoration_box,
            'slot_px': [180, 128],
        }

    manifest_path = Path(__file__).with_name('remaining_spread_measurements.json')
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')


if __name__ == '__main__':
    extract()
