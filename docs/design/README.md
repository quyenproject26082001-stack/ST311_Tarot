# ST311 screenshot-based UI revision

The user explicitly chose the PNG boards in `/home/lequyen/Documents/folder311`
as the implementation reference, without requiring access to Figma.
The connected Figma tool required reauthentication; no Figma metadata was obtained.

## Editing the UI

Screen layouts are native Android XML with View Binding:

- `layout_actionbar.xml`: shared title/back/actions for Tarot, profile, Language and Permission.
- `actionbar_styles.xml`: XML styles and a Tarot-only overlay; onboarding styling is retained.
- `activity_tarot.xml`: shared ActionBar include, page host, bottom navigation.
- `activity_tarot_profile.xml`: zodiac selection / profile steps.
- `fragment_tarot_home.xml`: greeting, Horoscope panel, quick actions, recent readings.
- `fragment_tarot_spreads.xml`, `item_tarot_spread.xml`: spread list.
- `fragment_tarot_selection.xml`: Scroll / Classic selection.
- `fragment_tarot_library.xml`, `item_tarot_library*.xml`: Cards list / grid.
- `item_tarot_result_card.xml`, `tarot_pair.xml`, `tarot_celtic_diagram.xml`: results.
- `fragment_tarot_settings.xml`, `item_tarot_setting.xml`: grouped Settings rows.
- `sheet_tarot.xml`, `tarot_field_name.xml`, `tarot_birthday_wheel.xml`: sheets / fields.
- `tarot_design.xml`, `tarot_dimens.xml`, `tarot_bg_*.xml`: styles, colors, dimensions.
- `tarot_ic_*.xml`: native vector icons, not emoji or Kotlin Canvas paths.

`TarotActivity` binds content / handles navigation. `TarotSettingsScreen` groups
Settings actions. `TarotProfileActivity` validates and saves the local profile.
The model, reading rules, ViewModel and Room repository were not changed.
Splash, Language, Intro and Permission source/layout changes belonging to the user
were preserved. No new Horoscope, translations or production ads were added.

## Measurements and assets

Each reference frame is 720 px wide; the baseline is 360 dp. Light palette samples:
ink `#172A2A`, muted `#60706D`, gold `#DDBE5C`, pale gold `#F6E9B6`, panel `#FCFBF7`.
Nunito regular/medium/semibold/bold are reused from the user's supplied font files.
Text-width comparisons informed the weight selection; the original font metadata
cannot be proven from a flattened screenshot.

`measure_reference.py` records source rectangles in `asset_measurements.json`.
Only static artwork is extracted into `drawable-nodpi`: illustrations, card back,
zodiac glyphs, branding and decorative ornaments. Whole-screen screenshots are
never used as application views. Text, controls and hit targets remain native.
The background is the existing `bg_app.png` provided by the user.

## Verification and limits

- Debug assembly and all seven Tarot rule unit tests pass.
- Pixel phone emulator, 720 x 1600 at 320 dpi: profile selection / input, Home,
  Spreads, Cards, History, Settings, Daily result, ten-card Celtic result and
  switching Scroll / Classic with the same selection were checked.
- The XML Celtic diagram renders ten slots with the second card crossing the first.
- Runtime images are saved as `st311-*-after.png`; reference images have a separate
  `reference_` prefix. Dynamic names, drawn cards and history differ intentionally.
- Original onboarding, rewarded-ad completion, release/R8 and physical-device
  rendering were not revalidated in this revision.

This is not a claim of a 100% pixel-identical implementation. Flattened screenshots
do not provide original vectors, all texture layers or responsive constraints.
The supplied Spreads board shows artwork for only the first eight spreads; the
remaining eight use a neutral card icon, not another spread's illustration.
Some decorative texture, line icons and native date-wheel details remain approximate.
Missing Horoscope / specialized / reversed interpretations remain explicitly
identified as missing, rather than being filled with invented production content.

`format_xml.py` formats only the new Tarot XML files, leaving unrelated layouts alone.
