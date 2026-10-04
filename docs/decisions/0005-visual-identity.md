# ADR 0005: Visual identity

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #33, #34, #35, #36

## Context

ADR 0003 chose Angular Material (Material 3) as the component library but left the
visual identity open. Until it exists the app looks like a generic Google Material
app, and screens cannot be built with their final colors and typography.

Apotheca deals with health data and is used by families to check whether a medicine
is still safe to take. Three personalities were discussed: calm/trustworthy,
friendly/colorful and minimalist/modern.

The design was explored in Claude's Design canvas. Reference mockup:
<https://claude.ai/artifact/5EUGTb5cb1cfaaQMK2cket>

## Decision

### 1. Personality: calm and trustworthy

The app should feel like a well-organized pharmacy shelf: quiet surfaces, one
primary color, and color used mainly to communicate status, not for decoration.

### 2. Palette

Primary color is a teal / petrol blue.

| Role | Light theme | Dark theme |
|---|---|---|
| Primary | `#1F6F6B` | `#7FD9D1` |
| Status: expired | `#B3261E` | `#FF9C8D` |
| Status: expiring soon / low stock | `#8A5A00` | `#FFC876` |
| Status: ok | `#2E7D53` | `#8FE3B0` |

"Expiring soon" and "low stock" share the same warning color: both mean "act soon",
and they are told apart by icon and text.

Status must **never rely on color alone** (about 1 in 12 men has some form of color
blindness). Every status is shown as **icon + text + color**, for example a warning
icon with the label "Vence em breve".

### 3. Typography

- **Newsreader** (serif) for titles and highlights, which gives the calm,
  editorial tone.
- **Work Sans** (sans serif) for body text, lists and forms, chosen for legibility
  on small screens.

Both are free Google Fonts. They are served by the app itself through the `@fontsource`
npm packages, not by the Google Fonts CDN: they work offline in the PWA and no request
with the user's IP goes to a third party.

### 4. Dark mode from the start

Both themes are built together. Every token has a light and a dark value, and the
app follows the operating system preference (`prefers-color-scheme`).

### 5. Implementation: tokens and Material 3 theme

The identity becomes an Angular Material M3 theme plus CSS custom properties:

- The primary color feeds the Material theme, which generates the `--mat-sys-*`
  system tokens used by the components.
- Status colors, which Material does not provide, become app tokens such as
  `--apotheca-status-expired`, each with a light and a dark value.
- Components use the tokens, never hard-coded hex values. Changing a color is a
  one-line change in the theme.

The theme is created in the same step that installs Angular Material (ADR 0003).

## Alternatives considered

- **Friendly / colorful.** Warmer, but many colors compete with the status colors,
  which are the most important information on the screen.
- **Minimalist / modern (grayscale plus one accent).** Clean, but tends to look
  cold and generic, and gives the brand little personality.
- **Default Material palette (purple).** No design work, but it is recognizably
  "an Angular Material demo", which is weak for a portfolio.

## Consequences

- ✅ Screens can be built with their final look from the first component.
- ✅ Dark mode costs little now and would be expensive to add later.
- ✅ Status is accessible to color-blind users by design.
- ⚠️ Every new color needs two values (light and dark), and both must keep enough
  contrast against their background.
- ⚠️ Two web fonts add download size; they must be loaded with `font-display: swap`
  so text appears before the fonts arrive on slow phone connections.
