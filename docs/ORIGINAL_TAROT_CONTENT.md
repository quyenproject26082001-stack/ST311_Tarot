# Original Tarot content — ST311

The user approved independently authored content instead of copying the Trusted
Tarot website. No new website readings were downloaded to produce this catalog.

## What is authored

- `app/src/main/assets/tarot/original_interpretations_en.json`: 78 card records,
  each with a distinct upright and reversed profile. Each profile has a general
  reflection, relationship context, work/resources context, practical prompt,
  thematic tag and symbolic Yes/No/Maybe answer.
- `TarotInterpretations.kt`: editorial position guidance for all 16 app spreads,
  preserving the user's 49 card positions. Money & Career uses Past/Present/Future
  as the user specified, not the website's different position layout.
- Relationship and work readings include the respective card-specific context,
  not merely a different heading. Other positions combine the card's general
  reflection and advice with the position's specific question.
- What Is Your Calling uses three individual meanings without invented
  Past/Present/Future roles. Future positions describe possibilities, not facts.
- Universe readings combine two profiles and an additional relationship between
  their themes, including pause/action, connection/boundaries, change/stability,
  uncertainty/clarity and recovery/action. Other pairs use an open reflective
  comparison. Reversals are included in both the wording and the stored message.

This is a compositional content system: **156 authored orientation profiles plus
position guidance**, not 7,644 separately hand-written essays or 24,024 bespoke
pair readings. The engine can resolve 78 × 2 × 49 = 7,644 card/position cases and
78 × 77 × 4 = 24,024 ordered, distinct-card Universe cases. Exhaustive routing
checks do not substitute for a human editorial review of every resulting reading.

## Runtime and provenance

The catalog is bundled in the APK and loaded on IO. There are no new network,
backend, AI-service or personal-data requests. Card images still use the existing
remote-image loader; this does not make every part of the app offline.

Production loading checks the exact 78 slugs, both orientations, required text,
theme tags, content version and allowed answers. An incomplete original catalog
is rejected rather than silently presented as complete. Cards list/detail use
the original upright text, and detail additionally shows the reversed reflection.

New result cards store `contentId = st311-original-en-v1`. The displayed notice
identifies them as original app-authored reflections for entertainment, not
predictions or professional advice. Content was drafted with AI assistance and
should receive editorial review before release. Yes/No is symbolic, not verified
knowledge about another person, an event, a diagnosis or an investment.

The supplied `tarot_data.json` is not edited. Its identity, category and image
metadata remain in use; original authored text replaces its displayed card
meaning, positional text and Yes/No interpretation in memory. The new writing
does not establish rights to pre-existing images, metadata, fonts or legacy text.

## Persistence and scope

Existing database schema and reading rules are unchanged. New snapshots store
their exact displayed text and, for Universe, `combinedInterpretation`. Missing
new fields deserialize as null for old snapshots; History does not regenerate
them or falsely label them as newly authored. Legacy fallback notices remain
available specifically for old/imported records.

Daily/Weekly remain once per local day. The user's merged-cell screenshot on
2026-10-17 subsequently clarified that spreads 4–16 all use independent 23-hour
cooldowns, not only Universe. Celtic Cross retains its existing unlimited rule.
Rewarded-ad routing preserves the selected spread. Horoscope is a separate
feature and still has no fabricated weekly prediction.

## Verification

Verified locally on 2026-10-17: debug app/test APK builds passed; all 49 JVM
tests passed, including 11 new content tests. Both `OriginalTarotContentTest`
instrumentation tests passed on the Pixel emulator. The supplied JSON retained
SHA-256 `936614178342a9432596c3803851d36d6f56e79ebf60a5d1280d27496a97f863`.
No physical phone installation, full visual review or release/R8 test was run.

Follow-up for the merged 23-hour rule: the debug build and all 52 JVM tests
passed after updating spreads 4–16 and preserving the rewarded spread in ad
callbacks. Actual rewarded-ad playback was not exercised in that verification.

- `OriginalTarotInterpretationsTest`: all 7,644 routing cases; all 24,024 ordered
  pair/orientation cases; card-specific topic differences; reversal differences;
  Yes/No policy; exact calling roles; malformed catalogs; old/new JSON snapshots.
- `OriginalTarotContentTest`: actual packaged Android assets through the
  production repository, complete routing and old/new snapshot parsing, without
  inserting readings or changing profile/preferences.
- Existing rules, profile validation, zodiac and UI tests remain separate.

These checks demonstrate loading, routing and compatibility, not supernatural
accuracy, expert Tarot certification, complete editorial quality, release/R8
behaviour, or pixel-perfect screen layout.
