---
name: FIAP X
description: Authenticated owner UI for sending videos and following still-image extraction.
colors:
  ink: "#12161c"
  rail: "#161b22"
  stage: "#f4f5f7"
  surface: "#ffffff"
  muted: "#5b6570"
  line: "#e4e7eb"
  indigo: "#233a86"
  signal: "#e23b24"
  focus: "#3d5bd4"
  nav-text: "#c5ccd4"
  session-text: "#9aa4b2"
  pending-fill: "#f3ead0"
  pending-text: "#6a5416"
  processing-fill: "#e7edff"
  processing-text: "#233a86"
  processed-fill: "#e7f3dc"
  processed-text: "#2d5a18"
  risk-fill: "#fde8e4"
  risk-text: "#9b1c10"
  expired-fill: "#ece7e1"
  expired-text: "#5c4a3a"
  field-error: "#b42318"
  hover-ink: "#2a3139"
  nav-hover: "#232a33"
  row-hover: "#fafbfc"
  quiet-hover: "#eef1f8"
typography:
  page-title:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1.65rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.02em"
  section-title:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.3
  lead:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 400
    lineHeight: 1.45
  body:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.4
rounded:
  control: "6px"
  surface: "8px"
  pill: "999px"
spacing:
  xs: "0.4rem"
  sm: "0.7rem"
  md: "0.9rem"
  lg: "1.25rem"
  xl: "1.5rem"
  shell-main: "2rem 2.25rem 3rem"
  table-cell: "0.9rem 1.15rem"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "#ffffff"
    rounded: "{rounded.control}"
    padding: "0.7rem 1rem"
    typography: "{typography.body}"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.indigo}"
    rounded: "{rounded.control}"
    padding: "0.4rem 0.55rem"
  status-badge:
    backgroundColor: "{colors.pending-fill}"
    textColor: "{colors.pending-text}"
    rounded: "{rounded.pill}"
    padding: "0.28rem 0.7rem"
    typography: "{typography.body}"
  table-surface:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.surface}"
---

<!-- impeccable:design-schema 1 -->

# Design System: FIAP X

This file is the visual source of truth for `services/video-api-frontend`. Future UI work, including polish and refinement, must preserve these decisions unless a human explicitly instructs otherwise. Do not reinterpret the product as a new visual world on each iteration.

## Overview

**Creative North Star: "Professional product tool"**

Authenticated FIAP X is a restrained operations surface in the same family as Linear, Stripe Dashboard, and Frame.io. Navigation lives in a charcoal rail. Work happens on an off-white stage with white table surfaces and 1px hairlines. Brand is quiet: Source Sans 3, ink primary actions, and a signal-red **X**. Accents appear on current navigation, links, focus, and risk status — never as decoration.

The owner’s job on Meus vídeos is to scan a filename, read one lifecycle status, and open the file. Hierarchy comes from weight, contrast, and spacing, not from shrinking secondary text. Readability outranks extreme density.

Login may keep a split access panel. That editorial split must not leak into the authenticated shell (no 48ch manifesto measure, no uppercase micro-kickers, no paper/beige/handwriting).

**Key Characteristics:**

- Charcoal rail + off-white stage + white surfaces
- Filename-first table, one owner-facing status per row
- Sentence-case product chrome
- Flat surfaces, 1px borders, 6–8px radius
- No search, no filter chips, no extra chrome unless a feature requires it

**The Standing World Rule.** Do not introduce editorial, paper, handwritten, retro, scrapbook, beige, or experimental aesthetics. Do not add decorative illustration, gradients-as-identity, glass, or metaphor-driven layout.

## Colors

A cool charcoal-and-paper palette. Color is semantic: indigo for in-progress and links, green for done, sand for waiting, stone for expired, signal red for brand X and failure.

### Primary
- **Ink** (`#12161c`): body text, primary buttons, selected filter/nav emphasis.
- **Rail** (`#161b22`): authenticated sidebar and mobile top bar.

### Secondary
- **Indigo** (`#233a86`): current-page affordance, quiet row actions, Processando badges, notices.
- **Signal** (`#e23b24`): the **X** in FIAP X, and risk (Rejeitado, Falha no processamento).

### Neutral
- **Stage** (`#f4f5f7`): authenticated main background (`--paper`).
- **Surface** (`#ffffff`): tables, cards, inputs, empty states.
- **Muted** (`#5b6570`): leads, dates, counts, notes, table headers.
- **Line** (`#e4e7eb`): 1px borders and row dividers.
- **Nav text** (`#c5ccd4`) / **session text** (`#9aa4b2`): sidebar secondary copy on rail.

### Status fills
Use these pairs only on pills. Do not invent extra status colors.

| Status | Fill | Text |
|---|---|---|
| Pendente | `#f3ead0` | `#6a5416` |
| Processando | `#e7edff` | `#233a86` |
| Processado | `#e7f3dc` | `#2d5a18` |
| Rejeitado / Falha no processamento | `#fde8e4` | `#9b1c10` |
| Expirado | `#ece7e1` | `#5c4a3a` |

**The Quiet Accent Rule.** Signal red and indigo are scarce. Never paint large regions with them. Never use color alone to mean status; the label is required.

**The Focus Rule.** Keyboard focus is a 2px ring (`#3d5bd4`) with 2px offset. Do not replace it with a glow.

## Typography

**Display Font:** none in the authenticated app  
**Body Font:** Source Sans 3 (`'Source Sans 3', 'Segoe UI', system-ui, sans-serif`), weights 400 / 500 / 600 / 700  
**Character:** Neutral product sans. No display serif, no monospace costume, no uppercase table or profile labels.

### Hierarchy

Authenticated UI never sets normal interface text below **15px** (`0.9375rem`). Primary work and navigation are **16px** (`1rem`). Page titles stay larger; they are not a density lever.

| Role | Size | Weight | Line-height | Use |
|---|---|---|---|---|
| Page title | `1.65rem` / 26.4px | 600 | 1.25 | `.shell-main h1` only |
| Section title | `1.125rem` / 18px | 600 | 1.3 | `h2` in page blocks |
| Page lead | `1.125rem` / 18px | 400 | 1.45 | supporting sentence under the title |
| Body / nav / filename / badge / action | `1rem` / 16px | 400–600 | 1.4–1.5 | table work, sidebar nav, primary buttons, pills, Ver detalhes |
| Metadata | `0.9375rem` / 15px | 400 | 1.4 | dates, counts, session email, notes, field labels |

Filenames and table headers: 600. Dates: 400, muted, `font-variant-numeric: tabular-nums`. Wordmark: `1.05rem` / 700; the **X** is signal red.

**The Floor Rule.** Do not use type under 15px for authenticated chrome. Do not recreate hierarchy by shrinking secondary text. Use weight, muted color, and space.

**The Sentence-Case Rule.** Column titles are `Arquivo`, `Atualizado em`, `Status`. No `letter-spacing` tracking on table heads. No `text-transform: uppercase` on product chrome.

## Layout

### Application shell
Two columns: sidebar `15.5rem` + fluid main (`minmax(0, 1fr)`), full viewport height. Main padding `2rem 2.25rem 3rem`. Content column max-width **`68rem`**, centered.

### Sidebar / navigation
Charcoal rail. Brand (FIAP **X**) at top. Text nav: Meus vídeos, Enviar vídeo, Meu perfil. Session email + Sair at the bottom. Current item: `#232a33` fill, white text. Idle nav: transparent, `#c5ccd4`, hover `#232a33`. Email ellipsizes; it does not mid-word wrap.

### Page headers
Title and optional lead on the left; one primary action on the right (`Enviar vídeo` on the list). Align the action with the title block. Lead has **no 48ch cap** on authenticated pages. Keep the lead to one line when the viewport allows.

### List structure
The approved list is a single white table on the stage:

1. Arquivo (filename, 600, ellipsis)
2. Atualizado em (tabular activity datetime)
3. Status (one lifecycle pill)
4. Ver detalhes (quiet indigo text button)

The named `prd-filtro-busca-videos` feature adds one compact toolbar before the table: a visually quiet filename field with a search icon, a funnel-triggered single-select status menu, and a quiet clear action. The field applies prefix matching after a 300 ms pause and exact matching on Enter without explanatory helper copy or a visible result counter. Status and Updated at are sortable table headers, with ordering applied before pagination. Criteria remain visible while paging, loading, or retrying. This toolbar is a documented exception and must not expand into unrelated actions, advanced filters, or extra columns.

### Column alignment
Header row and every data row **must share the exact same grid**:

```
minmax(0, 1fr) 10.5rem 13.5rem 7.25rem
```

Gap `1.25rem`. Horizontal padding `1.15rem`. Vertical padding `0.9rem`. `align-items: center`.

- Date, status, and action tracks are shared widths, not per-row `max-content`.
- Status header, status cell, and badge left edges share the same x.
- The action header and every `Ver detalhes` label share the fourth track's left edge; retain the button hit area with an optical negative margin rather than shifting the text.
- The action track reserves a stable icon slot after `Ver detalhes`: show download when the result is available, a compact loading indicator only while the result can still become available, and no misleading loading state for terminal failures.
- Filename uses `minmax(0, 1fr)`, `nowrap`, `ellipsis` — never `overflow-wrap: anywhere`.

**The Shared Grid Rule.** Independent CSS grids that size columns from their own content are forbidden for this table. Header and rows must paint on one template.

### Spacing principles
Tight within a cluster (title + lead, label + value). Generous between clusters (header vs table, table vs following section). Typical steps: `0.4rem`, `0.7rem`, `0.85–0.9rem`, `1.25rem`, `1.5rem`. More space above a heading than below it.

### Text wrapping
- Authenticated leads: no artificial `max-width` that forces two lines beside unused space.
- Filenames, dates, badges, row actions: `white-space: nowrap`. Filenames ellipsis.
- Session email: ellipsis, not `overflow-wrap: anywhere`.
- Wrap only when the viewport is too narrow for the shared desktop grid (see Responsive).

### Responsive behavior
Breakpoint: **`980px`**.

Below 980px: hide the persistent sidebar; show a charcoal top bar with menu toggle and wordmark; drawer slides in from the left with a dim backdrop; Esc closes it. Page header stacks; primary button stays `width: auto`, not full-bleed. Table headers hide; each row becomes a two-by-two card (`name + status` / `date + action`) with `minmax(0, 1fr) auto`. Do not invent a different information architecture at this breakpoint.

### Login (out of authenticated scope)
The unauthenticated split panel may remain. It is not a license to restyle Meus vídeos, detalhe, envio, or perfil as an editorial poster.

## Elevation & Depth

Flat by default. Depth is tonal: rail vs stage vs white surface, separated by 1px `#e4e7eb`. No drop shadows, no offset hard shadows, no colored halos.

Row hover: `#fafbfc`. Quiet action hover: `#eef1f8`. Primary hover: `#2a3139`. Disabled: opacity `0.38`.

Progress fills use `transform`, not width animation. No page-load choreography. Prefer 150–250ms only when a state change needs it.

**The Flat Surface Rule.** Do not add shadow, blur, or gradient to create hierarchy. Use border, background, and space.

## Shapes

Controls `6px`. Surfaces (table, empty state, timeline, history, profile, upload cards) `8px`. Status pills fully rounded (`999px`). Inputs: 1px `#e4e7eb`, `6px` radius, padding `0.7rem 0.8rem`. Invalid input border uses signal/error (`#b42318` / `#e23b24`).

No neobrutal offsets, no squircle fashion, no mixed radius scales on one surface.

## Components

### Buttons
- **Primary:** ink fill, white 16px/600 text, `6px` radius, padding about `0.7rem 1rem`. Hover `#2a3139`.
- **Quiet:** transparent, indigo text, 16px/600, used for Ver detalhes, Voltar, paginação numerada, Sair (ghost on rail).
- One primary per page header. Do not add a second competing fill button in the list.

### Status badges
Compact pills, 16px/600, padding `0.28rem 0.7rem`, `nowrap`. One badge per video. The pill is not a heading and not a full-width bar. On detail, the same lifecycle pill sits with `justify-self: start` near the filename — not a kicker stretched across the column.

Owner-facing lifecycle comes from the library contract (`GET /v1/videos`). Do not show English enums.

| Label | Contract status |
|---|---|
| Pendente | `AWAITING_UPLOAD` |
| Processando | `UPLOADED` or `PROCESSING` |
| Processado | `AVAILABLE` |
| Rejeitado | `REJECTED` |
| Expirado | `EXPIRED` |
| Falha no processamento | `FAILED` |

Do not show separate upload vs processing statuses on the list.

### Table / list
White surface, 1px line border, `8px` radius. Header row `#f8f9fb` with muted 16px/600 labels. Body rows 16px filename, 15px date. Row min-height about `3.75rem`. No card gallery, no icon columns, no avatars.

### Page empty / filter-empty
If the owner has no videos, explain that and offer Enviar o primeiro vídeo. When active criteria hide every row, explain that no video matches and offer to clear the criteria instead of reusing the genuine-empty copy.

### Inputs
Surface fill, 1px line, 16px text. Labels 15px/600. Errors in `#b42318` next to the field.

### Navigation
Text buttons, not icons. Current page uses `aria-current="page"`. Mobile: labeled “Abrir menu” / “Fechar menu”, not a mystery icon.

## Do's and Don'ts

### Do:
- **Do** keep the charcoal rail, off-white stage, white table, and 1px hairlines.
- **Do** keep Source Sans 3 and the type scale above; floor 15px, primary 16px.
- **Do** keep one lifecycle status per video, with the six labels in this file.
- **Do** keep header and body rows on the same column template.
- **Do** prefer ellipsis and nowrap over mid-word wrapping when space exists.
- **Do** preserve sentence-case headers (`Arquivo`, `Enviado em`, `Status`).
- **Do** treat polish as alignment, spacing, and wrap fixes inside this world.

### Don't:
- **Don't** redesign the shell, replace the table with cards, or change the visual world during polish, typeset, layout, or critique follow-ups.
- **Don't** introduce search, filters, bulk actions, extra columns, or new chrome unless a named feature requires it.
- **Don't** use editorial, paper, handwritten, retro, beige, scrapbook, or experimental looks.
- **Don't** add decorative illustration, drop shadows, gradient text, or a second type family in the authenticated app.
- **Don't** set authenticated UI text below 15px.
- **Don't** force leads to wrap with `48ch` or similar manifesto measures.
- **Don't** size table columns independently per row (`max-content` on disconnected grids).
- **Don't** show dual status systems (upload + processing) on the list.
- **Don't** invent backend statuses or API fields to feed the UI.
- **Don't** leak job, sourceKey, UUID, PENDING, FAILED, or English `error` into owner copy.
