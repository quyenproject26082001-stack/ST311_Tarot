# Spread lock — XML implementation

- The green padlock and gold keyhole are an Android XML vector, redrawn from
  `drawable/lock_spread1.png`; they are not the original Figma SVG.
- A rounded gray overlay covers the row artwork and text. The original outer
  border and the light countdown badge remain clear.
- Badge background, border and corners are XML shapes. The short badge displays
  rounded-up hours, or minutes below one hour. The unlock sheet retains its
  detailed hours/minutes text.
- Existing cooldown rules, row clicks and rewarded-ad handling are unchanged.
- This change does not yet integrate `guide_spread1` / `spread_guide` backgrounds
  or illustrations into the Spreads list.

Verification: debug build and 38 JVM tests passed; three `SpreadLockUiTest`
instrumentation tests passed on the Pixel emulator. Geometry is checked at
320/360/800dp widths in both themes; this is not physical-tablet testing.

`unlocked.png` and `locked.png` are isolated Android View/Canvas renders of the
XML row, not full-screen captures or pixel-perfect baseline comparisons. The
fixture uses the current row background and a `20h` sample label. No profile,
reading or ad action is invoked by these tests. No physical phone was modified.
