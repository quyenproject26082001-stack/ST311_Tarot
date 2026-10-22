import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from translate_tarot import LANGUAGES, source_strings


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'app/src/main'
PLACEHOLDERS = re.compile(r'%\d+\$[sd]')


def validate():
    source = ET.parse(MAIN / 'res/values/tarot_strings.xml').getroot()
    resources = {item.attrib['name']: item for item in source
        if item.tag == 'string' and item.attrib.get('translatable') != 'false'}
    expected = set(source_strings())
    cards = json.loads((MAIN / 'assets/tarot/tarot_data.json').read_text())
    meanings = json.loads((MAIN / 'assets/tarot/original_interpretations_en.json').read_text())
    for language, directory in LANGUAGES.items():
        elements = list(ET.parse(MAIN / f'res/values-{directory}/tarot_strings.xml').getroot())
        translated = {item.attrib['name']: item for item in elements}
        assert len(elements) == len(translated), (language, 'Duplicate UI key')
        assert set(resources) == set(translated), (language, 'UI key mismatch')
        for name, element in resources.items():
            original = ''.join(element.itertext())
            value = ''.join(translated[name].itertext())
            assert sorted(PLACEHOLDERS.findall(original)) == sorted(PLACEHOLDERS.findall(value)), (language, name)
            assert original.count('\\n') == value.count('\\n'), (language, name, 'Paragraph mismatch')
            assert [child.tag for child in element] == [child.tag for child in translated[name]], (language, name, 'Markup mismatch')
            assert element.attrib.get('formatted') == translated[name].attrib.get('formatted'), (language, name, 'Formatting mismatch')
        catalog = json.loads((MAIN / f'assets/tarot/translations_{language}.json').read_text())
        assert set(catalog) == expected, (language, 'Content coverage mismatch')
        for original, value in catalog.items():
            assert value.strip() and 'ZXQPH' not in value, (language, original)
            assert sorted(PLACEHOLDERS.findall(original)) == sorted(PLACEHOLDERS.findall(value)), (language, original)
        for card in cards:
            assert card['title'] in catalog, (language, card['title'])
        for card in meanings['cards']:
            for orientation in ['upright', 'reversed']:
                for key, value in card[orientation].items():
                    if key != 'theme':
                        assert value in catalog, (language, card['slug'], orientation, key)
        print(f'{language}: {len(translated)} UI strings, {len(catalog)} content entries, all 78 card names and meanings covered')


if __name__ == '__main__':
    validate()
