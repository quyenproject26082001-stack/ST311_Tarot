import concurrent.futures
import json
from pathlib import Path
import re
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape


ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'
ASSETS = ROOT / 'app/src/main/assets/tarot'
KOTLIN = ROOT / 'app/src/main/java/com/aaa/bbb/ccc/tarot'
LANGUAGES = {'de': 'de', 'es': 'es', 'fr': 'fr', 'hi': 'hi', 'pt': 'pt', 'id': 'in'}
CACHE = Path('/tmp/st311-tarot-translation-cache')
GLOSSARY = json.loads(Path(__file__).with_name('tarot_translation_glossary.json').read_text())
UI_OVERRIDES = json.loads(Path(__file__).with_name('tarot_ui_overrides.json').read_text())


def source_strings():
    values = set()
    for name in ['TarotModels.kt', 'TarotInterpretations.kt', 'WeeklyHoroscope.kt', 'TarotUi.kt', 'TarotCardsActivity.kt', 'TarotViewModel.kt']:
        for raw in re.findall(r'"((?:[^"\\]|\\.)*)"', (KOTLIN / name).read_text()):
            try:
                value = json.loads('"' + raw.replace('\\$', '$') + '"')
            except ValueError:
                continue
            if re.search(r'(?<!%)\$', value.replace('$s', '').replace('$d', '')):
                continue
            if re.search('[a-z]', value) and (re.search(r'[A-Z]', value) or ' ' in value):
                values.add(value)
    catalog = json.loads((ASSETS / 'original_interpretations_en.json').read_text())
    for card in catalog['cards']:
        for orientation in ['upright', 'reversed']:
            values.update(value for key, value in card[orientation].items() if key != 'theme')
    cards = json.loads((ASSETS / 'tarot_data.json').read_text())
    values.update(card['title'] for card in cards)
    values.update(['Aries', 'Taurus', 'Gemini', 'Cancer', 'Leo', 'Virgo', 'Libra', 'Scorpio', 'Sagittarius', 'Capricorn', 'Aquarius', 'Pisces',
        'Major Arcana', 'Pentacles', 'Wands', 'Cups', 'Swords', 'Yes', 'No', 'Maybe'])
    return sorted(values)


def resource_strings():
    entries = []
    for element in ET.parse(RES / 'values/tarot_strings.xml').getroot():
        if element.tag != 'string' or element.attrib.get('translatable') == 'false':
            continue
        content = (element.text or '') + ''.join(ET.tostring(child, encoding='unicode') for child in element)
        entries.append((element.attrib, content))
    return entries


def translate_batch(strings, language):
    protected = []
    placeholders = []
    for value in strings:
        tokens = []

        def protect(match):
            tokens.append(match.group())
            return f'ZXQPH{len(tokens) - 1}QXZ'

        text = re.sub(r'%\d+\$[sd]|<[^>]+>|\\n', protect, value.strip())
        protected.append(text)
        placeholders.append(tokens)
    query = '\n'.join(f'[{index:04d}]\n{value}' for index, value in enumerate(protected))
    url = 'https://translate.googleapis.com/translate_a/single?' + urllib.parse.urlencode(
        {'client': 'gtx', 'sl': 'en', 'tl': language, 'dt': 't', 'q': query})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(url, timeout=45) as response:
                payload = json.load(response)
            translated = ''.join(segment[0] for segment in payload[0] if segment[0])
            matches = list(re.finditer(r'\[\s*(\d+)\s*\]', translated))
            if len(matches) != len(strings):
                raise ValueError('Translation lost an entry boundary')
            result = []
            for index, match in enumerate(matches):
                if int(match.group(1)) != index:
                    raise ValueError('Translation changed entry order')
                end = matches[index + 1].start() if index + 1 < len(matches) else len(translated)
                value = translated[match.end():end].strip()
                for token_index, token in enumerate(placeholders[index]):
                    pattern = rf'ZXQ\s*PH\s*{token_index}\s*QXZ'
                    value, count = re.subn(pattern, lambda _: token, value, flags=re.I)
                    if count != 1:
                        raise ValueError('Translation changed a placeholder')
                original = strings[index]
                value = original[:len(original) - len(original.lstrip())] + value + original[len(original.rstrip()):]
                result.append(value)
            return result
        except Exception:
            if attempt == 3:
                if len(strings) > 1:
                    middle = len(strings) // 2
                    return translate_batch(strings[:middle], language) + translate_batch(strings[middle:], language)
                raise
            time.sleep(attempt + 1)


def translate_language(language, directory, catalog, resources):
    cache_path = CACHE / f'{language}.json'
    translated = json.loads(cache_path.read_text()) if cache_path.exists() else {}
    translated.update(GLOSSARY[language])
    translated.update(card_titles(language))
    translated.update({value: UI_OVERRIDES[language][attributes['name']]
        for attributes, value in resources if attributes['name'] in UI_OVERRIDES[language]})
    entries = list(dict.fromkeys(catalog + [value for _, value in resources]))
    remaining = [value for value in entries if value not in translated]
    batch = []
    length = 0
    batches = []
    for value in remaining:
        if batch and length + len(value) > 3000:
            batches.append(batch)
            batch, length = [], 0
        batch.append(value)
        length += len(value) + 12
    if batch:
        batches.append(batch)
    for index, batch in enumerate(batches):
        translated.update(zip(batch, translate_batch(batch, language)))
        cache_path.write_text(json.dumps(translated, ensure_ascii=False, indent=2) + '\n')
        print(f'{language}: {index + 1}/{len(batches)} batches', flush=True)
    (ASSETS / f'translations_{language}.json').write_text(
        json.dumps({value: translated[value] for value in catalog}, ensure_ascii=False, indent=2) + '\n')
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for attributes, original in resources:
        value = translated[original]
        value = value.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
        value = escape(value).replace('&lt;b&gt;', '<b>').replace('&lt;/b&gt;', '</b>')
        value = value.replace("'", "\\'").replace('"', '\\"').replace('\n', '\\n')
        extra = ' formatted="false"' if attributes.get('formatted') == 'false' else ''
        lines.append(f'    <string name="{attributes["name"]}"{extra}>{value}</string>')
    lines.append('</resources>')
    (RES / f'values-{directory}/tarot_strings.xml').write_text('\n'.join(lines) + '\n')
    print(f'{language}: complete ({len(catalog)} content entries, {len(resources)} UI strings)', flush=True)


def card_titles(language):
    ranks = ['Ace', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Page', 'Knight', 'Queen', 'King']
    localized_ranks = {
        'de': ['Ass', 'Zwei', 'Drei', 'Vier', 'Fünf', 'Sechs', 'Sieben', 'Acht', 'Neun', 'Zehn', 'Bube', 'Ritter', 'Königin', 'König'],
        'es': ['As', 'Dos', 'Tres', 'Cuatro', 'Cinco', 'Seis', 'Siete', 'Ocho', 'Nueve', 'Diez', 'Sota', 'Caballero', 'Reina', 'Rey'],
        'fr': ['As', 'Deux', 'Trois', 'Quatre', 'Cinq', 'Six', 'Sept', 'Huit', 'Neuf', 'Dix', 'Valet', 'Cavalier', 'Reine', 'Roi'],
        'pt': ['Ás', 'Dois', 'Três', 'Quatro', 'Cinco', 'Seis', 'Sete', 'Oito', 'Nove', 'Dez', 'Valete', 'Cavaleiro', 'Rainha', 'Rei'],
        'hi': ['इक्का', 'दो', 'तीन', 'चार', 'पाँच', 'छह', 'सात', 'आठ', 'नौ', 'दस', 'संदेशवाहक', 'योद्धा', 'रानी', 'राजा'],
        'id': ['As', 'Dua', 'Tiga', 'Empat', 'Lima', 'Enam', 'Tujuh', 'Delapan', 'Sembilan', 'Sepuluh', 'Pemuda', 'Kesatria', 'Ratu', 'Raja'],
    }
    result = {}
    for suit in ['Pentacles', 'Wands', 'Cups', 'Swords']:
        translated_suit = GLOSSARY[language][suit]
        for rank, local_rank in zip(ranks, localized_ranks[language]):
            if language == 'de':
                value = f'{local_rank} der {translated_suit}'
            elif language in ['es', 'pt']:
                value = f'{local_rank} de {translated_suit}'
            elif language == 'fr':
                value = f'{local_rank} d’Épées' if suit == 'Swords' else f'{local_rank} de {translated_suit}'
            elif language == 'hi':
                if rank in ['Ace', 'Page', 'Knight', 'Queen', 'King']:
                    possessive_suit = 'तलवारों' if suit == 'Swords' else translated_suit
                    particle = 'की' if rank == 'Queen' else 'का'
                    value = f'{possessive_suit} {particle} {local_rank}'
                else:
                    value = f'{local_rank} {translated_suit}'
            else:
                value = f'{local_rank} {translated_suit}'
            result[f'{rank} of {suit}'] = value
    return result


if __name__ == '__main__':
    CACHE.mkdir(exist_ok=True)
    catalog = source_strings()
    resources = resource_strings()
    print(f'Source: {len(catalog)} content entries and {len(resources)} UI strings', flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        futures = [executor.submit(translate_language, language, directory, catalog, resources)
            for language, directory in LANGUAGES.items()]
        for future in concurrent.futures.as_completed(futures):
            future.result()
