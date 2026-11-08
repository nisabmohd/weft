# Design rules (2026-11-08)

These come on top of `docs/SPACING.md` and `docs/TYPOGRAPHY.md`. Follow them in every new screen and when touching an old one.

## 1. Sheets, never dialogs
- Every prompt, picker, confirmation and action list is a modal bottom sheet. Don't use `AlertDialog` or a centred card.
- Sheet: 28dp top corners, drag handle (32×4dp, outlineVariant), 20dp side padding, 28dp bottom padding (above the gesture bar).
- Header: a left-aligned title (22sp SemiBold), then optionally a muted subtitle (14sp, onSurfaceVariant, 4–6dp below), then 16–20dp to the content.
- An icon tile on top of the title is not needed. The sheet's title says what it is.

## 2. One button pair
- When there are two actions (cancel + confirm, later + do it, snooze + dismiss), use two equal buttons:
  - 48dp tall, fully rounded (24dp), `weight(1f)` each, 8dp apart, filling the content width
  - left: **secondary**, secondaryContainer / onSecondaryContainer (tone(h,34,87) / tone(h,30,17))
  - right: **primary**, primary / onPrimary (tone(h,55,40) / white)
- With one action, use one full-width 48dp button: primary when it confirms, secondary when it only closes (e.g. "Maybe later").
- Never outlined buttons, never text-only Cancel next to a filled Save, and never two primaries.
- Labels: 16sp SemiBold, sentence case, a verb ("Save", "Update", "Set reminder").

## 3. Action sheets use grouped rows
- Group the rows with 2dp gaps. The first row has 24dp top corners and 6dp bottom corners, the middle rows 6dp, and the last row 6dp top and 24dp bottom. Don't use dividers.
- Row: 60dp tall, surfaceContainerHigh, 16dp horizontal padding, 16dp gap. Use 64dp+ when the row has a second line. Actions are one line: a verb, no subtitle.
- Leading tile: 40dp, 14dp corners, secondaryContainer with a 20dp onSecondaryContainer icon.
- Give the list room: 20dp from the title block to the first row, and clear space above the gesture bar.
- Label: 16sp Medium. An optional second line is 14sp onSurfaceVariant. Add a trailing chevron only when the row opens something.
- Destructive: errorContainer tile, error icon and label, always the last row.

## 4. Choice rows (picking one option)
- Always the existing `ChoiceRow`, exactly as in Backup frequency: edge to edge (no inset, no rounded corners), 24dp side padding, ~56dp tall, 16sp SemiBold title, 14sp muted subtitle, radio on the right. When chosen, it's tinted surfaceContainerHigh with a primary radio. Never cards or rounded tiles for a pick-one list.

## 5. Text fields inside sheets
- Filled, not outlined: surfaceContainerHigh, 24dp corners, 14dp × 18dp padding, 15sp text.
- Placeholder: a question fitting the habit/day, in onSurfaceVariant ("How did the workout go?", "What stopped you from finishing?").

## 6. Colour roles for controls
| Control | Container | Content |
| --- | --- | --- |
| Primary button | primary tone(h,55,40) | onPrimary #fff |
| Secondary button, tonal action | secondaryContainer tone(h,34,87) | onSecondaryContainer tone(h,30,17) |
| Icon button, −/+ step buttons, tracks | surfaceContainerHigh tone(h,28,91) | onSurface |
| Selected nav pill | primaryContainer tone(h,88,91) | onPrimaryContainer tone(h,62,19) |
| Destructive | errorContainer | error |

Habit colours (`hab()`) are only for a habit's own content: its tile, progress, the Needs-focus tile, the alarm ringing screen's background, tile and slide bar. Buttons always use the roles above, even on a habit-coloured screen.

## 7. Icons
- Use Solar icons, Linear (outline) style, everywhere: 24dp grid, 1.5 stroke, round caps. Don't hand-draw icons.
- The ones used in these designs: `alarm`, `check`, `close`, `alt-arrow-right`, `pen`, `share`, `trash-bin-trash`, `transfer-horizontal`, `link-minimalistic-2`, `calendar`, `filter`, `cup-star`, `like`, `star`, `heart`. Nav: `home-smile`, `checklist-minimalistic`, `notebook`, `chart-square`, `tuning-2`. Habits: `alarm`, `waterdrop`, `book-minimalistic`, `pills`, `walking`, `dumbbell-large-minimalistic`, `meditation`, `pen-new-square`.
- Sizes: 22dp in header buttons and habit tiles, 20dp in action-row tiles, 18dp in chips.

## 8. Fonts
- Lexend everywhere (400 / 500 / 600 / 700). The design exports embed Lexend, so the PNGs show the real font.

## 9. Keep the app's look
- Prefer the existing components (`HabitTile`, `ChoiceRow`, grouped rows, section headers) over new ones.
- Change one thing at a time. Try new ideas on a copy first; once one is picked, it replaces the original, so there are no duplicates.
