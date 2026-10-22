# XML design previews

All layout views have `tools:visibility="visible"`. Text controls have sample
`tools:text`, image controls have `tools:src`, and RecyclerViews have sample
items, counts and preview layout managers. These attributes are stripped by
Android and do not change runtime UI, theme colors, sizes or behavior.

Containers populated in Kotlin cannot display their runtime children through
`tools:text` alone. Open these complete, preview-only compositions in Android
Studio's Layout Editor instead:

Settings dialog content now lives in production XML. Edit these files to change
the running UI, not the `preview_*.xml` wrappers:

| Content | Production layout in `app/src/main/res/layout/` |
| --- | --- |
| Select Star Sign description, grid placement, birthday explanation and Submit | `dialog_tarot_star_sign.xml` |
| Edit Name input placement and Submit | `dialog_tarot_edit_name.xml` |
| Edit Birthday description, wheel placement and Submit | `dialog_tarot_edit_birthday.xml` |
| Privacy text and Close button | `dialog_tarot_privacy.xml` |
| Zodiac grid column count | `tarot_zodiac_grid.xml` |
| Zodiac item appearance | `item_tarot_zodiac.xml` |
| Birthday wheel appearance | `tarot_birthday_wheel.xml` |
| Reading Locked description, watch-ad button and wait countdown | `dialog_tarot_reading_locked.xml` |
| Reading Locked bottom-aligned panel and dimmed backdrop | `overlay_tarot_reading_locked.xml` |

The four Settings dialog previews include their production content layouts, so
changing a production description's size or color updates those previews too.
`TarotZodiacGrid` and `BirthdayWheel` populate sample data in edit mode; at runtime
Kotlin binds the saved profile, selection constraints and click handlers.

| Screen or component | Preview layout in `app/src/main/res/layout/` |
| --- | --- |
| Settings: Select Star Sign, description, 12 signs, Submit | `preview_tarot_settings_star_sign.xml` |
| Settings: Edit Name | `preview_tarot_settings_edit_name.xml` |
| Settings: Edit Birthday | `preview_tarot_settings_edit_birthday.xml` |
| Settings: Privacy | `preview_tarot_settings_privacy.xml` |
| Settings with populated personal and additional groups | `preview_tarot_settings.xml` |
| Home with three quick actions and a recent reading | `preview_tarot_home.xml` |
| All 16 spreads | `preview_tarot_spreads.xml` |
| Cards with categories and sample grid items | `preview_tarot_cards.xml` |
| Reading history | `preview_tarot_history.xml` |
| Reading result | `preview_tarot_result.xml` |
| Create Profile with sign grid and birthday wheel | `preview_tarot_profile.xml` |
| All 12 zodiac signs | `preview_tarot_zodiac_grid.xml` |
| Birthday wheel with readable sample dates | `preview_tarot_birthday_wheel.xml` |

The app never inflates `preview_*.xml`. Changes to the preview wrappers themselves
do not customize the running app. The remaining screen compositions mirror
Kotlin-created content for inspection only; keep those compositions in sync with
their production layouts and runtime bindings.

Sample cards use the bundled deck artwork rather than making network requests.
Preview compositions use the light Tarot theme; change the root `tools:theme` to
`@style/TarotThemeDark` to inspect the dark palette. Runtime light/dark mappings
are unchanged.
