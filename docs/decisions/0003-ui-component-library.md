# ADR 0003: UI component library

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #30

## Context

The frontend needs ready-made components (buttons, forms, dialogs, lists, date
pickers) so that the MVP is not spent building them from scratch. The visual
identity of Apotheca is not decided yet, so the library must let a custom theme
be applied later without rewriting screens.

This ADR covers only the component library. The theme (colors, typography,
personality) is still an open decision and will get its own ADR.

## Decision

Use **Angular Material**, with its **Material 3** theming system.

- It is maintained by the Angular team, so it follows Angular's release cycle and
  `ng update` migrates it together with the framework.
- Its components are standalone, so each screen imports only the components it uses.
- Material 3 themes are exposed as CSS variables (`--mat-sys-*` system tokens).
  When the visual identity is decided, it becomes a theme that overrides those
  tokens, and the existing screens change without being rewritten.
- Components come with keyboard navigation and ARIA support, which helps with the
  rule that status must never rely on color alone.
- It is the most recognized Angular UI library in job postings, which matters for
  a junior portfolio.

The library is installed (`ng add @angular/material`) in the step that creates
the first screen that uses it, not before.

## Alternatives considered

- **PrimeNG.** Many more components, but maintained by a third party, with its own
  theming system and a less predictable upgrade path across Angular versions.
- **Tailwind CSS with custom components.** Full visual freedom, but every button,
  dialog and date picker would be built and made accessible by hand.
- **Bootstrap (ng-bootstrap).** Familiar, but less integrated with Angular and more
  associated with server-rendered sites than with Angular apps.

## Consequences

- ✅ Official, accessible components available from the first screen.
- ✅ The visual identity can be decided later and applied through theme tokens.
- ⚠️ The default look is recognizably "Google Material". Until the identity ADR
  exists, the app will look generic.
- ⚠️ Deep visual customization beyond the theme tokens is harder than with
  Tailwind. If the chosen identity needs that, this decision will be revisited in
  a new ADR.
