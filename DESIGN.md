---
name: FloraCare
description: A quiet, photo-first houseplant catalog with adaptive care reminders.
colors:
  forest-deep: "#1F3A2E"
  paper-cream: "#F5EFE0"
  paper-cream-dim: "#E8E0CC"
  terracotta: "#C66B3D"
  sage-muted: "#8BA888"
  sage-deep: "#6E8F74"
  charcoal-dark: "#1A1C1A"
  ink-near-black: "#121412"
  ink-soft: "#3B3F3B"
  warning-amber: "#D19A3C"
  danger-clay: "#A34A2E"
typography:
  display:
    fontFamily: "Fraunces, Georgia, serif"
    fontSize: "48sp"
    fontWeight: 600
    lineHeight: "54sp"
    letterSpacing: "-0.5sp"
  headline:
    fontFamily: "Fraunces, Georgia, serif"
    fontSize: "26sp"
    fontWeight: 600
    lineHeight: "32sp"
    letterSpacing: "normal"
  title:
    fontFamily: "Plus Jakarta Sans, system-ui, sans-serif"
    fontSize: "18sp"
    fontWeight: 600
    lineHeight: "24sp"
    letterSpacing: "normal"
  body:
    fontFamily: "Plus Jakarta Sans, system-ui, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "normal"
  label:
    fontFamily: "Plus Jakarta Sans, system-ui, sans-serif"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "0.5sp"
rounded:
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  lg: "20dp"
  xl: "28dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
  xxl: "48dp"
  gutter: "20dp"
  section: "64dp"
components:
  button-primary:
    backgroundColor: "{colors.forest-deep}"
    textColor: "{colors.paper-cream}"
    rounded: "{rounded.lg}"
    padding: "12dp 24dp"
  button-tertiary:
    backgroundColor: "{colors.terracotta}"
    textColor: "{colors.paper-cream}"
    rounded: "{rounded.lg}"
    padding: "12dp 24dp"
  button-outlined-danger:
    backgroundColor: "transparent"
    textColor: "{colors.danger-clay}"
    rounded: "{rounded.lg}"
    padding: "12dp 24dp"
  card-surface:
    backgroundColor: "{colors.paper-cream}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  chip-filter:
    backgroundColor: "{colors.paper-cream-dim}"
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.xl}"
    padding: "6dp 12dp"
  chip-filter-selected:
    backgroundColor: "{colors.sage-muted}"
    textColor: "{colors.forest-deep}"
    rounded: "{rounded.xl}"
    padding: "6dp 12dp"
  input-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.ink-near-black}"
    rounded: "{rounded.xs}"
    padding: "12dp 16dp"
  hero-photo:
    backgroundColor: "{colors.sage-muted}"
    rounded: "0dp"
    padding: "{spacing.lg}"
---

# Design System: FloraCare

## 1. Overview

**Creative North Star: "The Field Journal"**

FloraCare is a quiet hardback gardening book on a phone. Forest-deep ink on warm paper-cream stock, set in a serif display face (Fraunces) with a humanist sans (Plus Jakarta Sans) for everything else. The aesthetic register is herbarium and editorial — a cataloguer's notebook, not a consumer plant-care feed. The user is browsing their own garden, not being sold to.

Density is low and intentional. Most surfaces have one anchor — a photo, a number, a title — and let the rest of the page breathe. Where Material 3 demos stack four equal-weight cards, FloraCare picks one card and lets it speak. The list is photo-and-text. The detail view earns its full-bleed hero. Settings is sectioned but unhurried. Onboarding is quiet text on cream.

This system explicitly rejects the "wellness app" pastel-gradient look, generic shadcn dashboard uniformity, hero-metric SaaS clichés, gradient text, glassmorphism, and anything that reads as "AI-generated indie app." Streaks are recorded but never used as guilt. The terracotta accent is rare and hot — a bookbinder's stamp, not a brand color sprayed across every surface.

**Key Characteristics:**
- Paper-cream surfaces, never `#fff`. Forest-deep text, never `#000`.
- Serif display (Fraunces) on hero text, headlines, and the streak number. Plus Jakarta Sans everywhere else.
- Photos are the catalog. Where a plant photo or species image exists, it is the visual anchor.
- Terracotta is rare. It marks one accent per surface — a labelLarge, an FAB, a single line of supporting metadata.
- Cards are flat, slightly elevated (1–2dp), and large-radius (20dp). They are not stacked at uniform weight; one earns the visual emphasis on each screen.
- Motion is restrained: state changes only. No choreography, no spring physics, no decorative entrances.

## 2. Colors: The Herbarium Palette

The palette is borrowed from a botanical journal: forest-leaf inks, paper that has yellowed slightly, a single hot terracotta for callouts, and two grades of sage for soft secondary surfaces. All darks are tinted toward the brand hue — there is no neutral grey in this system.

### Primary
- **Forest Deep** (#1F3A2E): The ink of the system. Used for primary buttons in light mode, body text headers, the FAB content color, and anywhere a "first voice" is needed. In dark mode this becomes the `primaryContainer` instead — see Neutral.

### Secondary
- **Sage Deep** (#6E8F74): The supporting voice. Used for the secondary M3 role: filled-tonal icon buttons, AssistChip backgrounds (translucent at 22% alpha for icon plates), the streak hero's iconography. Sage Muted (#8BA888) is its lighter sibling, used as `primaryContainer` in light mode and as the dashboard sparkline stroke.

### Tertiary
- **Terracotta** (#C66B3D): The bookbinder's stamp. Used for the FAB on PlantList, for `labelLarge` accent text on cards (e.g. "Plant of the month", "Current weather"), for the next-task hint on PlantCard, and for the rare error-tinted badge. Never used decoratively; never used in 30%+ of any surface.

### Neutral
- **Paper Cream** (#F5EFE0): The default light-mode surface and background. Replaces white everywhere.
- **Paper Cream Dim** (#E8E0CC): One step warmer/darker. Used for `surfaceVariant` — input field backgrounds, AcquiredDate tile, SectionCard internal contrast. Also lives in `FloraAccents.paperGrain` for future textural use.
- **Charcoal Dark** (#1A1C1A): The default dark-mode surface and background. Tinted toward green; never `#000`.
- **Ink Near Black** (#121412): `onBackground` in light mode; the deepest ink in the system.
- **Ink Soft** (#3B3F3B): `onSurface` and `onSurfaceVariant` body text in light mode. In dark mode it is the dim surface variant.

### Status
- **Warning Amber** (#D19A3C): Reserved. Currently provided in `FloraAccents.warning` but unused on any shipped surface. When used, it should mark a recoverable schedule slip ("watering overdue by 2 days"), not generic alarms.
- **Danger Clay** (#A34A2E): Used on the Archive button text in EditPlant and on the M3 `error` role. Never paired with terracotta — they fight; pick one per screen.

### Named Rules

**The One Stamp Rule.** Terracotta appears on at most one element per surface as the user's eye hits it. The "Plant of the month" labelLarge, the FAB on PlantList, the next-task hint on a card — these are mutually exclusive on any single screen. If two terracotta elements would appear on one viewport, demote one to onSurfaceVariant.

**The No White Rule.** `#FFFFFF` and `#000000` are forbidden in this system. Even white image scrims use `Color.Black.copy(alpha = 0.25f)` against tinted surfaces, never raw black. Light backgrounds are always Paper Cream. Dark backgrounds are always Charcoal Dark.

**The Photo-First Rule.** Where a plant cover photo or species `imageUrl` exists, it owns the visual real estate of its container. Solid colored blocks are not a substitute for absent photos — they are an admission that no photo is available. Treat absence as white space, not as decoration.

## 3. Typography

**Display Font:** Fraunces (Georgia, serif fallback) — variable serif from Undercase Type, optical-size aware, with a slightly idiosyncratic italic. Loaded via Google Fonts provider `com.google.android.gms.fonts`.
**Body Font:** Plus Jakarta Sans (system-ui, sans-serif fallback) — humanist geometric sans, four weights in use (400/500/600). Loaded via the same provider.

**Character:** A field journal's title page paired with a fountain-pen note in the margin. Fraunces brings warmth and authority where it appears; Plus Jakarta Sans handles the everyday work without calling attention to itself.

### Hierarchy
- **Display Large** (Fraunces SemiBold 600, 48sp / 54sp, -0.5sp tracking): The streak number on Dashboard and the WeatherCard temperature. Reserved for *one* hero number per surface, no more.
- **Display Medium** (Fraunces SemiBold 600, 36sp / 42sp, -0.25sp tracking): Onboarding "Welcome to FloraCare" / "You're set"; PlantDetail Hero nickname over the photo. The most editorial moment in any flow.
- **Display Small** (Fraunces Normal 400, 28sp / 34sp): Onboarding "A few permissions" pane; reserved for second-tier introductions.
- **Headline Large** (Fraunces SemiBold 600, 26sp / 32sp): TopAppBar titles on primary destinations (PlantList, Dashboard, Settings).
- **Headline Medium / Small** (Fraunces 22sp / 18sp): "Plant of the month" headline tier; AddPlant / EditPlant TopAppBar titles when the surface is sub-flow rather than primary.
- **Title Large** (Plus Jakarta Sans SemiBold, 18sp / 24sp): Card titles (PlantCard nickname, SectionCard heading), action tile titles.
- **Title Medium / Small** (Plus Jakarta Sans Medium, 15sp / 13sp): Section headings inside cards ("Upcoming care", "Notes"), prediction-row labels.
- **Body Large** (Plus Jakarta Sans Regular, 16sp / 24sp): Long-form copy in onboarding panes and helper sentences. Aim for ~50–65 character lines on phone widths.
- **Body Medium / Small** (Plus Jakarta Sans Regular, 14sp / 12sp): Default body text, supporting captions, error messages.
- **Label Large** (Plus Jakarta Sans SemiBold, 13sp, 0.5sp tracking): Terracotta accent labels — "Plant of the month", "Current weather", "Cover photo". The signature "stamp" label of the system.
- **Label Medium / Small** (Plus Jakarta Sans Medium, 11sp / 10sp, 0.5sp tracking): Microcopy — "30d ago", "today", form section headings.

### Named Rules

**The One Serif Per Card Rule.** Inside a single Card, only one element uses Fraunces — the title or the hero number, never both. If a card has a serif headline, all numbers and supporting metadata are sans. If a card leads with a serif number, the labels around it are sans.

**The Stamp-Label Rule.** Label Large in Terracotta is the system's signature mark for "this card has a name." It tags exactly one element per card, always at the top, always in `labelLarge` weight and `0.5sp` tracking. Imitating it without tracking or weight breaks the editorial register.

**The No-Display-In-Buttons Rule.** Fraunces never appears in buttons, form fields, chips, or any interactive component. Display type is for reading, not tapping.

## 4. Elevation

The system is mostly flat with subtle tonal layering. Cards rest at 1dp by default and 2dp when they carry an interactive role (a tappable PlantCard, the recommended ActionTile in AddPlant chooser). The `primaryContainer` ActionTile lifts to 4dp to signal "this is the path most users take" — the only place in the system where elevation does meaningful hierarchy work.

There are no ambient drop shadows, no glow effects, no glassmorphic blurs. Depth is conveyed through tinted surface contrast (Paper Cream → Paper Cream Dim → translucent-sage plate) and full-bleed photography, not through shadow ramps.

### Tonal Layers
- **Background** (Paper Cream / Charcoal Dark): the page itself.
- **Surface** (Paper Cream / Charcoal Dark): cards and sheets.
- **Surface Variant** (Paper Cream Dim / Ink Soft): nested fields inside cards — input field backgrounds, the AcquiredDate tile, AssistChip containers.
- **Translucent Plates** (sage at 22% alpha): icon plates inside the StreakHero / WeatherCard. These are decorative warmth, not depth.

### Named Rules

**The Flat-By-Default Rule.** Surfaces sit at 1dp. Hover, focus, and pressed states do not lift higher than 2dp. The only justified 4dp surface is the recommended path on a chooser screen, used at most once per flow.

**The No-Shadow-On-Photos Rule.** Hero photos and plant cover images do not sit inside cards with shadows. They use full-bleed boxes with a 25% black scrim for legibility — depth comes from the photo's content, not from a frame around it.

## 5. Components

### Buttons
- **Shape:** Material 3 default — large rounded ends (`shapes.large` → 20dp). Buttons inherit from M3 `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`.
- **Primary** (`Button`): Forest Deep background, Paper Cream text. Used for confirming a flow ("Save plant", "Save changes", "Grant camera"). Width: `Modifier.fillMaxWidth()` on form screens; intrinsic width on dialogs. Padding: M3 default (12dp vertical, 24dp horizontal).
- **Tertiary FAB** (`FloatingActionButton` with `containerColor = tertiary`): Terracotta on Paper Cream content. Anchored bottom-end on PlantList. The single most prominent terracotta element in the app — never duplicated on the same screen.
- **Outlined Danger** (`OutlinedButton` with `contentColor = error`): Used for "Archive plant" — destructive intent. Always paired with a confirmation `AlertDialog`.
- **Filled Tonal** (`FilledTonalButton`, `FilledTonalIconButton`): Used for secondary actions (PlantDetail "Diagnose"), TopAppBar overflow icons (Dashboard, Settings on PlantList top bar). Soft sage tint, no high contrast.
- **Text Button** (`TextButton`): Used for "Cancel", "Back", "Retake", "Remove photo". Never appears on the primary CTA position.
- **Hover / Focus:** Material 3 default ripple in light forest-tinted overlay. No custom hover treatments.

### Chips
- **AssistChip** (PlantDetail "Vital" chips for Water / Light / Humidity): `surfaceVariant` background, two-line label with sans-medium label + accent-colored value. Currently used as a display element — see the Don'ts section about clickable chips that don't navigate.
- **FilterChip** (LocationSelector on AddPlant / EditPlant): M3 default `FilterChip` API. Selected: Sage Muted background with Forest Deep text. Unselected: transparent surface with onSurface text.

### Cards / Containers
- **Corner Style:** `RoundedCornerShape(20.dp)` for primary cards (`shapes.large`). 16dp for nested surfaces. 12dp for small inset surfaces (input field stand-ins, prediction sheets).
- **Background:** Paper Cream in light mode, Charcoal Dark in dark mode. Surface Variant (Paper Cream Dim / Ink Soft) for nested cards.
- **Shadow Strategy:** 1dp default, 2dp for interactive (tappable) cards, 4dp for one "recommended path" card per chooser surface. See Elevation.
- **Internal Padding:** `spacing.lg` (24dp) by default. Compact cards (PlantCard, ActionTile) use 14–16dp.
- **Border:** None. Cards rely on the tonal contrast between Paper Cream surface and Paper Cream Dim variant rather than strokes.

### Inputs / Fields
- **Style:** M3 `OutlinedTextField` — paper-cream background, 1dp Forest Deep stroke when focused, Ink Soft when unfocused. Corner radius 8dp (M3 small).
- **Focus:** Stroke shifts to Forest Deep at 2dp; label floats to top-left in `labelMedium` Plus Jakarta.
- **Error:** Stroke and label switch to Danger Clay; supporting text appears below in body small / Danger Clay.
- **Disabled:** Stroke fades to onSurfaceVariant at 30% alpha. Background unchanged.

### Navigation
- **Top app bar:** `containerColor = MaterialTheme.colorScheme.background` — paper-cream, no surface tint, no scroll-elevation lift. Title in `headlineLarge` (Fraunces SemiBold) on primary destinations, `headlineSmall` on sub-flows. Back navigation is an `IconButton` with `Icons.Outlined.ArrowBack`.
- **No bottom nav, no drawer.** The app is hub-and-spoke from PlantList. Dashboard and Settings are reached via `FilledTonalIconButton` actions on the PlantList top bar; Add via the FAB.
- **Active state:** N/A — there is no persistent navigation surface.

### Plant Card *(signature component)*
The square 2-column grid card on PlantList. **Today's implementation is provisional** — see Don'ts. The intended treatment is a square `AsyncImage` of the cover photo or species image as the top half (no flat color block as a stand-in), then a column with `titleLarge` nickname, `bodySmall` species name in onSurfaceVariant, and a single `labelMedium` next-task hint in Terracotta. No decorative pill, no invented affordances. Card itself is `shapes.large` at 2dp elevation, Paper Cream surface.

### Hero Photo *(signature component)*
The PlantDetail hero. Full-bleed `Box` at 1.4:1 aspect ratio. `AsyncImage` from `plant.coverPhotoUri ?: species.imageUrl`, scaled `Crop`. Fall-through is a solid Sage at 35% alpha (acceptable here because the surface is meant to hold a photo and the absence is short-lived). 25% black scrim under the text. `displayMedium` Fraunces nickname bottom-left, `bodyMedium` subtitle below. This is the most editorial surface in the system; it earns its display type by being one per detail screen.

## 6. Do's and Don'ts

### Do:
- **Do** use Paper Cream (#F5EFE0) for all light-mode backgrounds and surfaces. Never `#FFFFFF`.
- **Do** use Charcoal Dark (#1A1C1A) for all dark-mode backgrounds and surfaces. Never `#000000`.
- **Do** reserve Fraunces for one element per card — either the title or the hero number, never both.
- **Do** apply Terracotta on exactly one element per visible surface, sized to `labelLarge` with `0.5sp` tracking when used as the "stamp" label.
- **Do** show a real photo on every plant surface where one exists in `coverPhotoUri` or `species.imageUrl`. Use Coil's `AsyncImage`, `ContentScale.Crop`.
- **Do** keep card elevations flat (1dp default, 2dp for interactive) and reserve 4dp for the single "recommended path" tile on a chooser.
- **Do** use Material 3 default ripple — no custom hover or pressed treatments.
- **Do** treat archive, delete, and other destructive actions with an `AlertDialog` confirm and an in-screen Snackbar Undo, matching the EditPlant precedent.
- **Do** use `FilterChip` for location selection and `SegmentedButton` for binary preferences (theme mode, units), matching the Settings precedent.
- **Do** write copy in the voice of a careful field-journal entry — confident, low-frequency, no exclamation points, no engagement hooks.

### Don't:
- **Don't** stack four cards of identical shape, surface color, elevation, and padding on a single screen. The current Dashboard and Settings layouts violate this; future revisions must give one card the visual emphasis.
- **Don't** ship the hero-metric SaaS template (huge `displayLarge` number + tiny supporting label + accent stat). The current Dashboard streak card is the example to remove.
- **Don't** use a flat colored block as a substitute for an absent plant photo. Where a photo could go, render the photo or leave the space genuinely empty — never an alpha-tinted fill standing in for an image.
- **Don't** put a clickable affordance on an element that does nothing. The PlantDetail Vital chips currently use `AssistChip(onClick = {})` — replace with a non-clickable `Surface` or wire the click to a real explanation sheet.
- **Don't** leak implementation copy into UX. "Fires once per day at 07:00 local", "falls back to a mock species list", "73% confidence" — these phrasings do not appear in the field-journal voice. Translate to human language or delete.
- **Don't** mix `headlineLarge` and `headlineSmall` for the same role across screens. Primary-destination top bars use `headlineLarge`. Sub-flow top bars use `headlineSmall`. Pick once and ship it.
- **Don't** use gradient text, `background-clip: text`, or any decorative gradient. Emphasis is weight or scale, never gradient.
- **Don't** apply glassmorphism, blur backgrounds, or decorative blobs anywhere. The herbarium register is paper, not frosted glass.
- **Don't** use motion as decoration. Onboarding's `AnimatedContent` fade is the maximum — no orchestrated entrances, scroll-driven sequences, or spring physics.
- **Don't** use streak counts, watering totals, or low-confidence percentages as guilt or pressure. They are recorded for reflection, not weaponized as engagement.
- **Don't** ape Plantix's ad-heavy feed, generic shadcn dashboards, "wellness app" pastel gradients, or Material 3 demo conventions. PRODUCT.md's anti-references apply here verbatim.
- **Don't** pair Terracotta with Danger Clay on the same surface. They share a hue family and fight visually. Pick one per screen.
