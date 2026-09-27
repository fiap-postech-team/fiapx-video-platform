---
name: FIAP X
description: A precise operational workspace for sending videos, tracking processing, and retrieving image packages.
colors:
  ink: "#12161c"
  control-room-charcoal: "#161b22"
  work-surface: "#f4f5f7"
  surface: "#ffffff"
  muted: "#5b6570"
  divider: "#e4e7eb"
  operational-indigo: "#233a86"
  recording-red: "#e23b24"
  completion-lime: "#c5d15c"
  focus-blue: "#3d5bd4"
typography:
  display:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "clamp(2rem, 4.4vw, 3.4rem)"
    fontWeight: 600
    lineHeight: 1.15
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1.65rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Source Sans 3, Segoe UI, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: "0.035em"
rounded:
  control: "6px"
  container: "8px"
  prominent-container: "12px"
  pill: "999px"
spacing:
  xs: "0.3rem"
  sm: "0.5rem"
  md: "1rem"
  lg: "1.5rem"
  xl: "2rem"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.surface}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0.7rem 1rem"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.operational-indigo}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0.4rem 0.55rem"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0.7rem 0.8rem"
  status-badge:
    backgroundColor: "{colors.work-surface}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "0.28rem 0.7rem"
  content-container:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.container}"
    padding: "1rem 1.1rem"
---

# Design System: FIAP X

## Overview

**Creative North Star: "The Editorial Control Room"**

FIAP X is a precise, operational workspace with the composure of a well-directed production desk. Dark control surfaces frame a bright work area, hierarchy is explicit, and status color communicates system state without turning the interface into a dashboard of competing signals.

The system should feel tactile and assertive at the point of action, then quiet everywhere else. Refined spacing, restrained radii, crisp dividers, and concise typography keep dense workflows readable. Subtle ambient shadows are reserved for genuinely elevated states; the resting interface relies on tonal separation and borders.

**Key Characteristics:**

- High-contrast dark navigation surrounding a bright operational canvas.
- Compact, disciplined typography with clear weight and size steps.
- Tactile controls that remain visually light and workmanlike.
- Semantic status color used locally, never as broad decoration.
- Responsive structures that preserve labels and action clarity.

## Colors

The palette combines production-floor neutrals with scarce, purposeful signals: charcoal establishes control, white and cool gray support sustained work, and saturated colors only identify action, focus, or state.

### Primary

- **Control Room Charcoal:** The navigation rail, mobile bar, and other persistent control surfaces.
- **Recording Red:** The brand signal, invalid input emphasis, and rare attention cue.

### Secondary

- **Operational Indigo:** Quiet actions, processing states, progress, and informational notices.
- **Completion Lime:** A small brand accent for completed output, not a general-purpose success fill.

### Neutral

- **Ink:** Primary copy, primary controls, and selected states.
- **Work Surface:** The application canvas behind white content containers.
- **Surface:** Forms, tables, cards, and elevated content regions.
- **Muted:** Supporting copy, timestamps, and secondary navigation information.
- **Divider:** Boundaries between rows, fields, and grouped information.

**The Scarce Signal Rule.** Recording Red and Completion Lime are compact signals; they do not become page backgrounds or decorative washes.

**The Semantic State Rule.** Processing, success, warning, failure, and expiry colors retain one meaning across badges, notices, and progress feedback.

## Typography

**Display Font:** Source Sans 3 (with Segoe UI, system UI, and sans-serif fallbacks)  
**Body Font:** Source Sans 3 (with Segoe UI, system UI, and sans-serif fallbacks)

**Character:** A single humanist sans-serif keeps the system direct and consistent. Hierarchy comes from weight, scale, spacing, and controlled tracking rather than from multiple font personalities.

### Hierarchy

- **Display:** Semibold, responsive, tightly tracked; reserved for the access experience's main statement.
- **Headline:** Semibold and compact; the primary title for authenticated product views.
- **Title:** Semibold; card headings and secondary section titles.
- **Body:** Regular with a relaxed line height; task guidance, values, and descriptive copy.
- **Label:** Semibold with modest positive tracking; table headers and small operational labels. Uppercase is appropriate only for compact structural labels.

**The One Voice Rule.** Use the same family throughout; introduce hierarchy through size, weight, and spacing before introducing a new typographic treatment.

**The Data Clarity Rule.** Dates and measurements use tabular numerals, while filenames may truncate only where an adjacent detail path exposes the full value.

## Layout

Authenticated screens use a fixed 15.5rem navigation rail beside a fluid content canvas. Main content is centered within a 76rem maximum and receives responsive outer padding between 2rem and 4rem on wide screens. Page headers align the title group and primary action at their lower edge, with generous separation before operational content.

Tables use explicit columns for file, update time, status, and actions. The Actions header and each detail label share the column's leading edge, followed by a stable icon slot for download or in-progress preparation. Below 980px, navigation becomes an off-canvas panel and the table becomes a two-column summary. Below 620px, each row becomes a single-column labeled panel, preserving the meaning of every value and making actions full-width.

The named `prd-filtro-busca-videos` feature approves one compact query toolbar immediately before the video table. It contains a visually quiet filename field with an accessible hidden label and search icon, a funnel-triggered single-select status menu, and a quiet clear action. Typing applies prefix matching after 300 ms; Enter applies an exact full-name match. No explanatory helper or visible result count accompanies the controls. Status and Updated at are sortable headers, and the selected order is applied before pagination. The controls retain the existing surface, divider, radius, type, focus, and responsive rules and remain visible during loading and retry. This is a named exception, not a pattern for unrelated table chrome.

Spacing follows a compact working rhythm: 0.3–0.5rem within tightly related controls, 1rem within standard containers, 1.5rem between related groups, and 2rem or more between major page regions.

**The Operational Alignment Rule.** Columns, labels, and repeated actions share stable edges; optical alignment takes priority over decorative symmetry.

## Elevation & Depth

The resting interface is flat. Depth is communicated through dark-versus-light regions, surface color, and single-pixel dividers. Subtle ambient shadows may appear only when a surface is temporarily elevated—such as an open overlay or a lifted interactive state—and must include both offset and soft blur.

**The Flat-at-Rest Rule.** Cards, tables, and content panels use either a border or an ambient shadow, never both at rest.

## Shapes

Controls use a tight 6px radius; standard containers use 8px; prominent tables and responsive row panels use 12px. Status badges and progress tracks use pill geometry because they represent compact state or continuous progress. Dashed boundaries are reserved for file-selection drop zones.

**The Radius Hierarchy Rule.** Larger radii indicate larger or more prominent containers; do not apply pill shapes to ordinary buttons or cards.

## Components

### Buttons

- **Shape:** Compact rounded controls with a 6px radius and decisive internal padding.
- **Primary:** Ink background with white semibold text; used for the next meaningful task action.
- **Hover / Focus:** Hover shifts to a slightly lighter charcoal. Keyboard focus uses a visible 2px Focus Blue outline with a 2px offset.
- **Quiet:** Transparent with Operational Indigo text; hover adds a pale cool fill. In narrow table rows, quiet actions gain a divider-colored border and full-width alignment.
- **Disabled:** Preserves the component shape, removes pointer affordance, and reduces opacity.

### Chips

- **Style:** Unselected filters use a white surface, Ink copy, a divider border, and compact padding.
- **State:** Selected filters invert to Ink with white text. Status badges use soft semantic fills with dark, readable text and never behave as controls.

### Cards / Containers

- **Corner Style:** Gently curved, using 8px for standard containers and 12px for prominent tables or mobile row panels.
- **Background:** White Surface on the cool Work Surface canvas.
- **Shadow Strategy:** Flat at rest; use tonal layering and a single divider border.
- **Border:** One pixel in Divider.
- **Internal Padding:** Usually 1rem to 1.4rem, with denser row padding where repeated scanning matters.

### Inputs / Fields

- **Style:** White background, one-pixel Divider border, 6px radius, and comfortable 0.7rem vertical padding.
- **Focus:** The global Focus Blue outline remains visible and offset; do not suppress it.
- **Error / Disabled:** Invalid fields use Recording Red on the border. Disabled controls retain their label and shape at reduced opacity.

### Navigation

The rail uses Control Room Charcoal with muted cool-gray labels. Hover and current states share a slightly lighter charcoal panel; the current state additionally uses white copy. On narrower screens, a compact dark header opens the same navigation as an off-canvas panel over a dimmed backdrop.

### Video Table

The video table is the signature operational component. Its header uses compact uppercase labels on a cool neutral strip, its rows provide restrained hover feedback, and its action column stays visibly named and leading-aligned. A spinner is reserved for non-terminal states whose result is still being prepared; failed, rejected, and expired rows do not imply future availability. Mobile rows expose field labels rather than relying on desktop column position.

The genuine-empty state continues to invite the first upload. A filtered-empty state instead explains that no video matches and offers to clear the active search and status without presenting the query as an error.

## Do's and Don'ts

### Do:

- **Do** preserve a bright, low-noise work canvas framed by dark navigation.
- **Do** keep primary actions visually decisive and secondary actions quieter.
- **Do** use status colors only where they communicate an actual lifecycle state.
- **Do** retain visible labels and generous touch targets when tables adapt to narrow screens.
- **Do** use subtle ambient shadow only for temporary elevation or interaction response.

### Don't:

- **Don't** use Recording Red or Completion Lime as broad decorative surfaces.
- **Don't** combine a resting container border with a wide soft shadow.
- **Don't** hide action-column meaning or allow repeated row actions to drift out of alignment.
- **Don't** turn standard controls into pills; pill geometry belongs to status and progress.
- **Don't** add decorative gradients, glass effects, or ornamental motion to operational screens.
