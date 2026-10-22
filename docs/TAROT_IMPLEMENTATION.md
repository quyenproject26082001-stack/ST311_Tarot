# ST311 — Tarot implementation

## Original reading content (2026-10-17)

All 16 spreads now use a separately authored English catalog for new readings,
with upright/reversed profiles for all 78 cards, topic/position-aware text and
combined Universe reflections. Cards detail includes both orientations. New
snapshots preserve the displayed content and its origin; old History entries,
database schema, reading limits and the supplied JSON are unchanged.
See [content composition, provenance and verification](ORIGINAL_TAROT_CONTENT.md).
This supersedes the missing specialized/pair/reversed-content notes in the older
sections below, not the separate missing weekly Horoscope integration.

## Shared 70dp ActionBar (2026-10-16)

All existing ActionBar consumers now share 70dp height, a title band 10dp above
the bottom and buttons top/bottom aligned to that band. This includes Language
and Permission under the latest all-screens request; Splash/Intro remain barless.
See [geometry and scope](design/ACTIONBAR_70DP.md). It supersedes older 40/50/105dp
header measurements in the historical sections below.

## Full Tarot design pass (2026-10-16)

Profile, Home/Horoscope, Settings/sheets, Spreads/selection, result layouts,
Cards and History were revised against the supplied PNG boards. Splash,
Language, Intro and Permission source screens are unchanged in this pass.
See [native captures and measured alignment](design/review_20261016/README.md).
Reading rules, local snapshots and validation are retained; missing horoscope,
pair and reversed content is still labeled, not invented.

## Screenshot-based XML revision (2026-10-14)

Tarot screens, shared styling, vector icons, overlays and the Celtic diagram now
use XML with View Binding. See `design/README.md` for the screen-to-file mapping,
measured colors, extracted-art coordinates, runtime captures and visual limits.
This revision does not change reading rules or add the missing content below.

## Navigation

Settings popups (name, birthday, zodiac, reverse-chance information and privacy)
use `overlay_tarot_settings.xml` inside the Activity's `settingsOverlayHost`.
They do not create a Dialog or BottomSheetDialog window. The overlay blocks
outside taps without dismissing, retains Close/Submit actions, handles Back and
keyboard insets, and hides the underlying page from accessibility while open.
Other Tarot bottom sheets are unchanged.

Each Tarot destination now has its own Activity: HomeActivity (via
TarotHomeActivity), TarotSpreadsActivity, TarotCardsActivity, TarotHistoryActivity,
TarotSettingsActivity, TarotHoroscopeActivity, TarotSelectionActivity,
TarotResultActivity and TarotCardDetailActivity. Their screen bindings and actions
live in their respective classes. TarotActivity is an abstract shared base for
the action bar, bottom tabs, insets, reading dialogs and rewarded ads.

Each Activity has its own SavedStateHandle-backed TarotViewModel. Navigation
passes only the saved reading/selection and display state through Intent extras;
cards and history are loaded from the repository, never held in a global Activity
or shared ViewModel. Android restores each Activity's state after recreation.
Back returns to the previous Activity; leaving a partially selected deck still
asks for confirmation. Results replace Selection so Back cannot reopen a completed
deck. Switching bottom tabs replaces the outgoing non-Home tab without stacking
duplicate tabs. Theme changes reinflate a fully populated binding in the existing
Activity window without recreation, preserving the ViewModel and view hierarchy
state. Returning hosts apply the same in-place update. The content XML files use
the activity_tarot_ prefix; activity_tarot.xml
remains the shared action-bar/content/bottom-tab shell.

All Tarot Activities and TarotProfileActivity inherit BaseActivity<ViewBinding>
and retain hideNavigation() on resume/window focus. Insets protect the status bar,
cutout and keyboard without reserving space for hidden system navigation. Alerts
and bottom sheets hide navigation on their own windows.

All title-bearing Tarot destinations and both profile steps include the existing
`layout_actionbar.xml`, also used by Language and Permission. The shared
`btnActionBarLeft`, `tvCenter` and `btnActionBarRight` provide back/title/Settings;
the same XML additionally hosts Home branding and Cards list/grid actions.
`actionbar_styles.xml` scopes Tarot typography, icons and colors with an include
theme overlay, without changing the retained onboarding styling or header heights.
Profile navigation/title stay above its scrolling form. Splash and Intro remain
full-screen, without adding a title bar or changing their navigation.

Language (both entry modes) and Permission use the centered title variant in the
same 70dp action bar/title band as Tarot. Their action-bar hosts account for the
status-bar/cutout top inset without accumulating it across repeated dispatches;
their existing fonts, colors and Back/Done behavior remain scoped to onboarding.

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
- Spreads 4–16 (Universe through What Is Your Calling): each spread is locked
  independently for 23 hours from its latest completed draw. Confirmed by the
  user's merged-cell screenshot on 2026-10-17. Midnight does not reset this timer.
  Rewarded ads allow a new selection of the spread whose ad was watched, not
  always Universe; the subsequent completed reading starts its new 23-hour lock.
- Celtic Cross is outside that merged cell and retains its existing unlimited
  rule; the screenshot does not specify an additional limit for it.
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

### Shared BaseActivity / hideNavigation (2026-10-16)

Debug app/test APK builds, 38 JVM tests and all 21 instrumentation tests passed.
The four new lifecycle/window tests passed on Pixel_10_Pro_XL_2: nine Tarot destinations retain the base binding and
app tabs; both profile steps survive Back/recreation without duplicated form views;
bottom sheets and light/dark alerts hide actual navigation insets and recover on
dismiss; system Back during a partial Celtic selection shows the discard dialog
and Keep Selecting preserves the selection instead of finishing Home.

These checks do not write profiles or insert readings. Main-screen tests use the
emulator's existing profile; the result-screen fixture is opened in memory only.
Tests await RESUMED/drawn state rather than ActivityScenario's global-idle waits,
which can hang with continuously redrawn/animated titles. Keyboard show/hide,
physical-device rendering and release/R8 were not revalidated in this revision.

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
