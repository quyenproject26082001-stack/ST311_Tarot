# ST311 screenshot-based UI revision

Latest header requirement: [shared 70dp ActionBar](ACTIONBAR_70DP.md).
This supersedes the older screenshot-based header heights/vertical offsets below.

Latest full Tarot UI pass (2026-10-16): see
[`review_20261016/README.md`](review_20261016/README.md) for the current native
captures, measured alignment anchors, verification and remaining visual differences.
The older `st311-*-after.png` files below are historical captures, not this revision.

The user explicitly chose the PNG boards in `/home/lequyen/Documents/folder311`
as the implementation reference, without requiring access to Figma.
The connected Figma tool required reauthentication; no Figma metadata was obtained.

## Editing the UI

Screen layouts are native Android XML with View Binding:

- `layout_actionbar.xml`: shared title/back/actions for Tarot, profile, Language and Permission.
- `actionbar_styles.xml`: XML styles and a Tarot-only overlay; onboarding styling is retained.
- `activity_tarot.xml`: shared ActionBar include, page host, bottom navigation.
- `activity_tarot_profile.xml`: zodiac selection / profile steps.
- `activity_tarot_home.xml`: greeting, Horoscope panel, quick actions, recent readings.
- `activity_tarot_spreads.xml`, `item_tarot_spread.xml`: spread list.
- `activity_tarot_selection.xml`: Scroll / Classic selection.
- `activity_tarot_library.xml`, `item_tarot_library*.xml`: Cards list / grid.
- `item_tarot_result_card.xml`, `tarot_pair.xml`, `tarot_celtic_diagram.xml`: results.
- `activity_tarot_settings.xml`, `item_tarot_setting.xml`: grouped Settings rows.
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

The zodiac PNG crops are 52x52px, but the colored disc is only `(2,1)-(50,49)`.
The source disc's outer pixels are blended with white. At the user's request,
`TarotZodiacIcon` now clips 2 source pixels further inside the disc, to
`(4,3)-(48,47)`: left/right 2dp, top 1.5dp and bottom 2.5dp at the 26dp picker size.
Image padding is explicitly zero so the mask does not also
shrink the artwork. Home uses the proportionally scaled `TarotZodiacBadgeIcon`
mask at 16dp. Original PNGs and the selection callback remain unchanged.

Picker tiles use the reference's 16dp corners and 2dp selected stroke. Name/date
gaps are 2.5dp/1dp, with top/bottom padding 4.5dp/5.5dp for the measured alignment;
at the reference 2x scale the 52px image starts at `(78,20)`
inside a 208x160px tile. The visible disc is now 44px (22dp) to discard the pale
edge, without enlarging or rescaling the glyph. These changes also apply to
Settings' shared picker.

## Verification and limits

The initial full-PNG circular mask passed property tests but left a white rim;
that implementation has been replaced. `ZodiacIconClippingTest` now checks inset
outline geometry for all 12 signs in both themes and captures hardware-composited
screens from the actual profile Activity. Its selected-edge assertions require
gold `#F6E9B6`, not white. It also checks measured icon placement at the 2x scale.

Native screenshots for visual comparison are `st311-zodiac-selected-corrected.png`
and `st311-zodiac-unselected-corrected.png`, captured at 720x1600 / 320dpi.
Compare the Pisces icon against `(558,836)-(610,888)` in `reference_login_1.png`.
The corrected native capture has icon bounds `(558,874)-(610,926)` inside tile
`(480,854)-(688,1014)`, preserving the reference's relative offset `(78,20)`.
The earlier 48px crop passed outer-edge probes but still retained pale source-edge
pixels. The tighter 44px revision adds checks at the original disc's edge, not
just at the 52px ImageView's perimeter. Both native screenshots were refreshed
after this change; the earlier pixel-error figure no longer applies.
Debug/app-test assembly and all 3 targeted zodiac instrumentation tests passed
on the emulator on 2026-10-16. The connected physical phone was not modified.
This is not a claim of whole-screen identity: the shared ActionBar requested later
differs intentionally from the original left-aligned title; status bars and text
rasterization also differ by platform. The zodiac glyph/halo comparison is scoped
to its own measured bounds, not a shifted whole-screen comparison.

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
