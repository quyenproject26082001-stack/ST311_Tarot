# Tarot testing switches

In `app/src/main/java/com/aaa/bbb/ccc/tarot/TarotTestConfig.kt`:

```kotlin
const val REVEAL_ALL_CARDS = false
const val BYPASS_READING_LIMITS = false
```

- `true`: all 78 faces are visible in the Scroll and Classic selection decks.
  Picked cards are dimmed; the counter and required card count still apply.
- `false`: the normal face-down deck; only picked cards reveal their faces.

Rebuild and reinstall after changing the constant. This is a code switch, not a
Settings button, and applies to every build type. Set both flags to `false` before release.

The current defaults restore normal play: shuffle each new deck, keep unpicked
cards face down, and enforce the existing reading limits. Daily and Weekly reuse
the current day's saved result, Celtic Cross stays unlimited, and spreads 4–16
lock individually for 23 hours after a completed reading.

Locked readings show the in-Activity XML overlay
`overlay_tarot_reading_locked.xml`, with content in `dialog_tarot_reading_locked.xml`.
It mirrors the supplied unlock popup without using a BottomSheet window. The
countdown closes the popup and refreshes the lock state on expiry. Closing the
popup does not unlock a reading. For now, tapping Watch Ad to Unlock only shows
the English toast "Ads are not available yet." It does not check the network,
load an ad, dismiss the popup or bypass the lock. The existing rewarded-ad helper
is not called from this button and remains available for later integration.

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
