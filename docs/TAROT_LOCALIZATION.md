# Tarot localization

The Tarot UI supports English, German, Spanish, French, Hindi, Portuguese, and Indonesian.

- UI text lives in `res/values/tarot_strings.xml` and each language's `tarot_strings.xml`.
- Content translations live in `assets/tarot/translations_<language>.json`. Keys are the original English text; values are the translated text. Indonesian uses `id` for assets and Android's `values-in` resource directory.
- `TarotTranslations` translates display text without changing spread identifiers, card slugs, zodiac identifiers, or database records. Current app-authored reading snapshots are rendered in the selected language from their card identities and orientations. Imported or legacy interpretation text without the app's content ID remains as saved.
- `TarotInterpretations` and `WeeklyHoroscopeGenerator` accept an optional translation callback. Their default output stays English for persistence and existing callers. The Horoscope's card selection remains independent of language.
- Language Done applies the locale and returns to Settings. Existing Tarot Activities refresh their views on resume when the saved language changes.
- The Home header uses its existing text view for translated languages because the English image cannot change its embedded lettering.

The initial prose translations were machine-assisted. The glossary supplies reviewed Tarot names, suits, spread names, and core UI terminology; longer interpretations can be edited directly in the JSON catalogs for further language review.

`docs/design/translate_tarot.py` regenerates the catalogs and UI translations using the Google translation endpoint during authoring. It requires network access and uses a temporary cache under `/tmp/st311-tarot-translation-cache`. The app itself reads only bundled translations. The script applies `tarot_translation_glossary.json` and `tarot_ui_overrides.json` after generating translations, so curated terminology takes precedence.

Static validation is available through `python3 docs/design/validate_tarot_translations.py`. It checks resource coverage, placeholders, markup, paragraphs, card coverage, and content catalogs. Build and device verification must be performed separately.
