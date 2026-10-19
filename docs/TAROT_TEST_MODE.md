# Tarot testing switches

In `app/src/main/java/com/aaa/bbb/ccc/tarot/TarotTestConfig.kt`:

```kotlin
const val REVEAL_ALL_CARDS = true
const val BYPASS_READING_LIMITS = true
```

- `true`: all 78 faces are visible in the Scroll and Classic selection decks.
  Picked cards are dimmed; the counter and required card count still apply.
- `false`: the normal face-down deck; only picked cards reveal their faces.

Rebuild and reinstall after changing the constant. This is a code switch, not a
Settings button, and applies to every build type. Set both flags to `false` before release.

`BYPASS_READING_LIMITS` is independent of card visibility:

- `true`: every spread can be drawn again immediately. Daily/Weekly open a new
  selection instead of reusing today's result, including at completion. All
  23-hour lock badges/overlays are hidden and no unlock ad is required.
- `false`: normal calendar-day and 23-hour rules apply again to saved history.

Neither switch auto-selects cards, changes shuffle order/reversal probability,
or clears history. New readings are still saved normally. When the bypass is
disabled, the most recent saved test reading can therefore trigger the normal
limit/cooldown. Use a dedicated test install. The required card count and busy
guards are unchanged. Card-face images still use the normal image loader and
may require a network connection when not cached.

To compare content, pick the same card in different spreads and compare matching
orientations (upright versus upright, reversed versus reversed). Set reversed
chance to 0% or 100% through the existing Settings control for a consistent
orientation; the test switch itself never changes that preference.
