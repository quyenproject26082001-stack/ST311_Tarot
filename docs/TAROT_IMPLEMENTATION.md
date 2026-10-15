# ST311 — Tarot implementation

## Screenshot-based XML revision (2026-10-14)

Tarot screens, shared styling, vector icons, overlays and the Celtic diagram now
use XML with View Binding. See `design/README.md` for the screen-to-file mapping,
measured colors, extracted-art coordinates, runtime captures and visual limits.
This revision does not change reading rules or add the missing content below.

## Navigation

All title-bearing Tarot destinations and both profile steps include the existing
`layout_actionbar.xml`, also used by Language and Permission. The shared
`btnActionBarLeft`, `tvCenter` and `btnActionBarRight` provide back/title/Settings;
the same XML additionally hosts Home branding and Cards list/grid actions.
`actionbar_styles.xml` scopes Tarot typography, icons and colors with an include
theme overlay, without changing the retained onboarding styling or header heights.
Profile navigation/title stay above its scrolling form. Splash and Intro remain
full-screen, without adding a title bar or changing their navigation.

The original Splash, initial Language, Intro and Permission screens are retained.
Permission continues to TarotProfileActivity if there is no saved profile; otherwise
it opens HomeActivity. The Intro permission-skip branch follows the same profile gate.
HomeActivity retains the original class name and now hosts the Tarot application.
Splash preloads the supplied Tarot JSON instead of calling the old Avatar API.

Profile setup has two steps: select a star sign, then enter name and birthday.
The birthday wheel only offers calendar dates within that sign, using the fixed
date ranges displayed by the app (not astronomical birth-chart calculations).
Settings uses the same constraint. Selecting a different sign shows a matching
birthday picker and an explanation; both values are saved together on Submit,
while cancelling leaves the stored profile unchanged.
Home has Home / Spreads / Cards / History tabs and a Settings destination.

## Reading rules

- Daily: one card, one result per local calendar day. Reopening displays the saved result.
- Weekly: two cards, also one result per local calendar day, as specified by the user.
- Universe: two cards, locked for 23 hours from the completed draw. Rewarded ads allow
  a new selection; the subsequent completed reading starts a new 23-hour lock.
- Remaining 13 spreads: unlimited draws because no other limits were provided.
- A reading is committed when View Reading is pressed after selecting enough cards.
  Leaving an unfinished selection does not consume a daily reading or start a lock.
- Scroll and Classic share the same shuffled deck and selection. No duplicate cards.
- Reading snapshots store cards, orientations, position labels and displayed text in
  a separate Room database, so History reopens the original result.

## Screens and overlays

- Home: profile greeting, three quick actions, recent readings, Horoscope destination.
- Spreads: all 16 spreads from the supplied table.
- Selection: shared deck, correct required count, Scroll/Classic modes, explicit View Reading.
- Result: common result sections; Yes/No answer; Universe pair; 10-card Celtic diagram.
- Cards: five categories, list/grid toggle, card detail.
- History: empty state, saved results and favorites.
- Settings: edit profile, light/dark, guidance toggle, reversal probability, language,
  share and an explanatory local privacy dialog.
- Bottom sheets: edit name, birthday, star sign, reversal explanation and reading lock.
- Center dialog: pre-reading guidance.

## Data and remaining integration

`app/src/main/assets/tarot/tarot_data.json` is an unchanged copy of the user's 78-card
dataset. Explicit Yes/No conclusions are normalized at runtime because several raw
answer labels conflict with their explanation. Ambiguous conclusions remain Maybe.

The dataset provides English general meanings, Past/Present/Future text and Yes/No.
Specialized positions, combined two-card interpretations, reversed interpretations
and weekly Horoscope content are not supplied. The app labels general-meaning
fallbacks visibly instead of claiming specialized interpretations. Horoscope shows
an unavailable-content message until an appropriate source is supplied.

Card images use the supplied Trusted Tarot URLs through Glide and its disk cache;
they are not bundled for first-launch offline access. The existing Language flow is
reused, but the new Tarot strings currently have English defaults and the card content
has no translations. The app has local profile setup, not server authentication.

Debug builds use Google's test rewarded ad and test app ID. A production release
requires `tarot_rewarded_id` and an AdMob application ID in the release manifest.
No unlock is granted for an ad load/show failure or dismissal without a reward.
The informational privacy dialog is a sample; no production policy URL was provided.

## Verification

### Birthday and zodiac consistency (2026-10-15)

This replaces the earlier independent birthday/sign rule. `ZodiacDates` is the
single source for the 12 displayed ranges, picker limits and persistence checks.
Capricorn correctly offers January/December; Pisces respects leap years. Current
year months/days are restricted to today, and a sign that has not started yet
cannot offer the current year. The selected birth year is kept where valid.

New saves reject mismatched birthday/sign pairs without overwriting existing data.
For older mismatched profiles, reads keep the name and birthday, derive the correct
sign and leave the original raw record untouched; the next explicit valid save
persists the consistent values. This avoids discarding old profiles or silently
changing a person's birthday.

Debug app/test APK builds and 38 JVM tests passed, including ten new date-range
tests for all calendar days in common/leap/century years, sign endpoints,
year-wrapping, today/future limits and initial-date adjustment.
All 17 instrumentation tests passed on Pixel_10_Pro_XL_2: five birthday-wheel
tests, seven isolated profile-persistence tests (including old mismatched data)
and five shared ActionBar regression tests. Wheel tests instantiate the actual
NumberPickers and inspect their bounds/values; gesture interactions on a physical
device and release/R8 were not revalidated in this revision.

### Shared ActionBar (2026-10-15)

Debug assembly and all 28 existing JVM tests passed. Five new instrumentation
tests passed on Pixel_10_Pro_XL_2: shared XML/style inflation in light and dark,
profile reuse in both themes, unchanged Language/Permission title sizing and
hidden Tarot actions, and a long title fitting beside Cards actions at 320dp.
All six isolated profile-persistence instrumentation tests also passed again.
An interactive emulator smoke check confirmed Home's Settings action opens
Settings with the shared title/back views, and the ActionBar back button returns
to Home. No profile or reading records were modified by these checks.
These checks do not establish complete pixel parity or release/R8 correctness.

### Profile validation (2026-10-14)

Onboarding and Settings share `ProfileValidator`. `TarotPreferences.saveProfile`
also validates before writing, so an invalid edit cannot overwrite a valid profile.
Names are trimmed and NFC-normalized, with the existing XML limit of 24 UTF-16
units (not 24 grapheme clusters). Empty/invisible-only names, unsupported hidden
formatting/control characters and unpaired surrogates are rejected. Vietnamese,
international names and ordinary emoji remain supported.

Birthdays must be real `YYYY-MM-DD` dates from 1900 through today in the local
timezone. Zodiac must be one of the 12 supported choices. The original independent
birthday/sign rule was superseded by the consistency revision above. Invalid saved
JSON or preference types return no usable profile without crashing or deleting
the original record.

Verified: debug app and instrumentation APK builds; all 28 JVM tests passed
(20 new validation tests, seven reading-rule tests and one existing template test).
Six isolated SharedPreferences instrumentation tests passed on the
Pixel_10_Pro_XL_2 emulator, covering normalization, rejection without overwriting,
malformed/legacy data and reversal-probability bounds. These tests use separate
UUID-named preference files, not the real profile. Interactive error presentation
on a physical device and release/R8 were not rechecked in this validation revision.

### Existing reading and screen checks

Build using the existing wrapper JAR (this checkout has no gradlew script):

```sh
/home/lequyen/android-studio/jbr/bin/java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest --offline
```

Seven TarotRules tests cover deck/category integrity, local-day reset, exact 23-hour
cooldown, unique draws and reversal endpoints, all 16 spread counts, Yes/No conflicts,
and the distinction between supplied positional content and general fallback text.

Device smoke checks use a newly created local `ST311_Demo` profile; these are sample
readings, not the user's personal profile. Original unrelated template changes are
preserved; old Avatar screens/resources remain in the checkout but are not linked
from the new Home.

Verified on Samsung SM-X210: initial Language → three Intro pages → Permission →
both profile steps → Home; Daily draw and reopening the identical daily result;
Weekly two-card result; Celtic ten-card selection, diagram and History reopening;
Cards list/grid/detail; History favorites; light/dark switching; all five bottom
sheets; Universe two-card result followed by a 23-hour lock. Profile and reading
history survived force-stop/relaunch. System-bar insets and Celtic diagram spacing
were corrected after device inspection. Rewarded-ad completion, date rollover on
a physical device, release/R8 and complete visual parity were not verified.

Device screenshots: `st311-home.png` and `st311-celtic.png` in this directory.
